/*
 * Copyright (C) 2014 David Vittor http://digitalspider.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jspwiki.plugins.maps;

import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.util.TextUtil;

/**
 * Google maps in an iframe, api key required
 *
 */
public class GoogleMapsPlugin implements Plugin {

    private static final Logger log = Logger.getLogger(GoogleMapsPlugin.class);

    public static final String PARAM_URL = "url";
    public static final String PARAM_MAP_MODE = "mode";
    public static final String PARAM_API_KEY = "apiKey";

    public static final String PARAM_ALIGN = "align";
    public static final String PARAM_BORDER = "border";
    public static final String PARAM_WIDTH = "width";
    public static final String PARAM_HEIGHT = "height";
    public static final String PARAM_MARGINWIDTH = "marginwidth";
    public static final String PARAM_MARGINHEIGHT = "marginheight";
    public static final String PARAM_SCROLLING = "scrolling";

    private static final String DEFAULT_URL = "https://www.google.com/maps/embed/v1/";
    private static final String DEFAULT_MAP_MODE = "view";

    private static final String DEFAULT_BORDER = "0";
    private static final String DEFAULT_WIDTH = "100%";
    private static final String DEFAULT_HEIGHT = "100%";
    private static final String DEFAULT_MARGINWIDTH = "10";
    private static final String DEFAULT_MARGINHEIGHT = "10";
    private static final String DEFAULT_SCROLLING = "auto";

    @Override
    public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");
        StringBuffer result = new StringBuffer();

        // Validate all parameters
        String mapType = getCleanParameter(params, "mapType", "satellite");
        String lat = getCleanParameter(params, "lat", "0.0");
        String lon = getCleanParameter(params, "lon", "0.0");
        String zoom = getCleanParameter(params, "zoom", "1");
        String url = DEFAULT_URL;
        url += getCleanParameter(params, PARAM_MAP_MODE, DEFAULT_MAP_MODE);
        url += "?key=" + getCleanParameter(params, PARAM_API_KEY);
        url += "&center+=" + lat + "," + lon;
        url += "&zoom=" + zoom;
        url += "&mapType" + mapType;
        String align = getCleanParameter(params, PARAM_ALIGN, "center").toLowerCase();
        String border = getCleanParameter(params, PARAM_BORDER, DEFAULT_BORDER);
        String width = getCleanParameter(params, PARAM_WIDTH, DEFAULT_WIDTH);
        String height = getCleanParameter(params, PARAM_HEIGHT, DEFAULT_HEIGHT);
        String marginwidth = getCleanParameter(params, PARAM_MARGINWIDTH, DEFAULT_MARGINWIDTH);
        String marginheight = getCleanParameter(params, PARAM_MARGINHEIGHT, DEFAULT_MARGINHEIGHT);
        String scrolling = getCleanParameter(params, PARAM_SCROLLING, DEFAULT_SCROLLING);

        String src = null;

        {
            if (url.startsWith("http")) {
                try {
                    src = new java.net.URL(url).toExternalForm();
                } catch (java.net.MalformedURLException ex) {
                    throw new PluginException("Could not resolve the url: " + ex.getMessage());
                }
            } else {
                src = url;
            }
        }

        result.append("<iframe src=\"" + src + "\" align=\"" + align + "\" frameborder=\"" + border
                + "\" width=\"" + width
                + "\" height=\"" + height
                + "\" marginwidth=\"" + marginwidth
                + "\" marginheight=\"" + marginheight
                + "\" scrolling=\"" + scrolling
                + "\" allowfullscreen referrerpolicy=\"no-referrer-when-downgrade\">\n");
        result.append("    Your browser does not support inline frames.\n");
        result.append("</iframe>\n");

        log.info("result=" + result.toString());
        log.info("DONE.");
        return result.toString();
    }

    private static final String getCleanParameter(Map params, String paramId, String defaultValue) {
        String value = getCleanParameter(params, paramId);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * This method is used to clean away things like quotation marks which a
     * malicious user could use to stop processing and insert javascript.
     */
    private static final String getCleanParameter(Map params, String paramId) {
        return TextUtil.replaceEntities((String) params.get(paramId));
    }
}
