/*
 * Copyright 2025 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.preferences;

import com.google.gson.Gson;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimeZone;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.InternationalizationManager;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.providers.PreferenceProvider;
import org.apache.wiki.util.HttpUtil;
import org.apache.wiki.util.PropertyReader;
import org.apache.wiki.util.TextUtil;

/**
 * Default user preference manager. provides server side user preference storage
 * to avoid issues such as cookie expiration or blocked cookies on the browser
 * side. See JSPWIKI-653 for more information.
 *
 * This implementation uses a server side (jspwiki-custom.properties) defined
 * file path to store a JSON file, one per user containing their user
 * preferences.
 */
public class DefaultPreferencesManager implements PreferenceProvider {

    private static final Logger LOG = LogManager.getLogger(DefaultPreferencesManager.class);
    private File storageDir = null;

    @Override
    public void setupPreferences(PageContext pageContext) {
        reloadPreferences(pageContext);
    }

    //reload from disk/defaults and apply to current page cookie response
    @Override
    public void reloadPreferences(PageContext pageContext) {
        final Properties props = PropertyReader.loadWebAppProps(pageContext.getServletContext());
        final Context ctx = Context.findContext(pageContext);
        final String dateFormat = ctx.getEngine().getManager(InternationalizationManager.class)
                .get(InternationalizationManager.CORE_BUNDLE, getLocale(ctx), "common.datetimeformat");
        Map<String, String> prefs = new HashMap<>();
        prefs.put("SkinName", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.skinname", "PlainVanilla"));
        prefs.put("DateFormat", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.dateformat", dateFormat));
        prefs.put("TimeZone", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.timezone", TimeZone.getDefault().getID()));
        prefs.put("Orientation", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.orientation", "fav-left"));
        prefs.put("Sidebar", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.sidebar", "active"));
        prefs.put("Layout", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.layout", "fluid"));
        prefs.put("Language", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.language", getLocale(ctx).toString()));
        prefs.put("SectionEditing", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.sectionediting", "true"));
        prefs.put("Appearance", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.appearance", "true"));

        //editor cookies
        prefs.put("autosuggest", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.autosuggest", "true"));
        prefs.put("tabcompletion", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.tabcompletion", "true"));
        prefs.put("smartpairs", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.smartpairs", "false"));
        prefs.put("livepreview", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.livepreview", "true"));
        prefs.put("previewcolumn", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.previewcolumn", "true"));

        // FIXME: editormanager reads jspwiki.editor -- which of both properties should continue
        prefs.put("editor", TextUtil.getStringProperty(props, "jspwiki.defaultprefs.template.editor", "plain"));
        parseJSONPreferences(prefs, (HttpServletRequest) pageContext.getRequest());
        pageContext.getSession().setAttribute(SESSIONPREFS, this);
        if (storageDir != null) {
            ServletRequest request = pageContext.getRequest();
            if (request instanceof HttpServletRequest req) {
                Principal userPrincipal = req.getUserPrincipal();
                if (userPrincipal != null) {
                    String username = userPrincipal.getName();
                    //translate the username via hash to get a save file name
                    String safeName = DigestUtils.shaHex(username);
                    File destination = new File(storageDir, safeName + ".json");
                    try {
                        FileUtils.writeStringToFile(destination, new Gson().toJson(this), "UTF-8");
                    } catch (Exception ex) {
                        LOG.warn("failed to persist user preferences for " + username, ex);
                    }
                }
            }

        }
    }

    /**
     * Parses new-style preferences stored as JSON objects and stores them in
     * the session. Everything in the cookie is stored.
     *
     * @param request
     * @param prefs The default hashmap of preferences
     */
    private void parseJSONPreferences(Map<String, String> prefs, final HttpServletRequest request) {
        final String prefVal = TextUtil.urlDecodeUTF8(HttpUtil.retrieveCookieValue(request, COOKIE_USER_PREFS_NAME));
        if (prefVal != null) {
            // Convert prefVal JSON to a generic hashmap
            @SuppressWarnings("unchecked")
            final Map< String, String> map = new Gson().fromJson(prefVal, Map.class);
            for (String key : map.keySet()) {
                key = TextUtil.replaceEntities(key);
                // Sometimes this is not a String as it comes from the Cookie set by Javascript
                final Object value = map.get(key);
                if (value != null) {
                    prefs.put(key, value.toString());
                }
            }
        }
    }

    @Override
    public String getPreference(Context wikiContext, String name) {
        final HttpServletRequest request = wikiContext.getHttpRequest();
        if (request == null) {
            return null;
        }

        final Map<String, String> prefs = (Map<String, String>) request.getSession().getAttribute(SESSIONPREFS);
        if (prefs != null) {
            return prefs.get(name);
        }

        return null;
    }

    @Override
    public String getPreference(PageContext pageContext, String name) {

        final Map<String, String> prefs = (Map<String, String>) pageContext.getSession().getAttribute(SESSIONPREFS);
        if (prefs != null) {
            return prefs.get(name);
        }

        return null;
    }

    @Override
    public Locale getLocale(Context context) {
        Locale loc = null;

        final String langSetting = getPreference(context, "Language");

        // parse language and construct valid Locale object
        if (langSetting != null) {
            String language = "";
            String country = "";
            String variant = "";

            final String[] res = StringUtils.split(langSetting, "-_");
            final int resLength = res.length;
            if (resLength > 2) {
                variant = res[2];
            }
            if (resLength > 1) {
                country = res[1];
            }
            if (resLength > 0) {
                language = res[0];
                loc = new Locale(language, country, variant);
            }
        }

        // see if default locale is set server side
        if (loc == null) {
            final String locale = context.getEngine().getWikiProperties().getProperty("jspwiki.preferences.default-locale");
            try {
                loc = LocaleUtils.toLocale(locale);
            } catch (final IllegalArgumentException iae) {
                LOG.error(iae.getMessage());
            }
        }

        // otherwise try to find out the browser's preferred language setting, or use the JVM's default
        if (loc == null) {
            final HttpServletRequest request = context.getHttpRequest();
            loc = (request != null) ? request.getLocale() : Locale.getDefault();
        }

        LOG.debug("using locale " + loc.toString());
        return loc;
    }

    @Override
    public void initialize(Engine engine, Properties properties) throws NoRequiredPropertyException, IOException {
        String path = properties.getProperty("jspwiki.userPref.storge");
        if (path != null) {
            File target = new File(path);
            if (!target.exists()) {
                if (!target.mkdirs()) {
                    LOG.warn("unable to mkdirs at {} user preferences will not be persisted", target.getAbsolutePath());
                }
            }
            storageDir = target;
        } else {
            throw new NoRequiredPropertyException("The Default User preference implementation now requires a persistence location", "jspwiki.userPref.storge");
        }
    }

    @Override
    public String getProviderInfo() {
        return "Default User Preference Manager (disk backed)";
    }

}
