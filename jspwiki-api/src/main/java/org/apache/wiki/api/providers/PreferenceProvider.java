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
package org.apache.wiki.api.providers;

import jakarta.servlet.jsp.PageContext;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.InternationalizationManager;

/**
 *
 * Server side user preference storage and retrieval services.
 *
 * @since 3.0.0
 */
public interface PreferenceProvider extends WikiProvider {
    /**
     * The name under which a Preferences object is stored in the HttpSession. Its value is {@value}.
     */
    String SESSIONPREFS = "prefs";
    /**
     * cookie user preference 
     */
    String COOKIE_USER_PREFS_NAME = "JSPWikiUserPrefs";


    /**
     *  This is an utility method which is called to make sure that the
     *  JSP pages do have proper access to any user preferences.  It should be
     *  called from the commonheader.jsp.
     *  <p>
     *  This method reads user cookie preferences and mixes them up with any
     *  default preferences (and in the future, any user-specific preferences)
     *  and puts them all in the session, so that they do not have to be rewritten
     *  again.
     *  <p>
     *  This method will remember if the user has already changed his prefs.
     *
     *  @param pageContext The JSP PageContext.
     */
    void setupPreferences( final PageContext pageContext );

    /**
     *  Reloads the preferences from the PageContext into the WikiContext.
     *
     *  @param pageContext The page context.
     */
    // FIXME: The way that date preferences are chosen is currently a bit wacky: it all gets saved to the cookie based on the browser state
    //        with which the user happened to first arrive to the site with.  This, unfortunately, means that even if the user changes e.g.
    //        language preferences (like in a web cafe), the old preferences still remain in a site cookie.
    void reloadPreferences( final PageContext pageContext );


   

    /**
     *  Returns a preference value programmatically.
     *  FIXME
     *
     *  @param wikiContext
     *  @param name
     *  @return the preference value or null if not defined
     */
    public String getPreference( final Context wikiContext, final String name );

    /**
     *  Returns a preference value programmatically.
     *  FIXME
     *
     *  @param pageContext
     *  @param name
     *  @return the preference value or null if not defined
     */
    String getPreference( final PageContext pageContext, final String name );

    /**
     * Get Locale according to user-preference settings or the user browser locale
     *
     * @param context The context to examine.
     * @return a Locale object.
     * @since 2.8
     */
    Locale getLocale( final Context context );

    /**
     * Locates the i18n ResourceBundle given.  This method interprets the request locale, and uses that to figure out which language the
     * user wants.
     *
     * @param context {@link Context} holding the user's locale
     * @param bundle  The name of the bundle you are looking for.
     * @return A localized string (or from the default language, if not found)
     * @throws MissingResourceException If the bundle cannot be found
     * @see org.apache.wiki.i18n.InternationalizationManager
     */
  default  ResourceBundle  getBundle( final Context context, final String bundle ) throws MissingResourceException  {
        final Locale loc = getLocale( context );
        final InternationalizationManager i18n = context.getEngine().getManager( InternationalizationManager.class );
        return i18n.getBundle( bundle, loc );
    }

    
}
