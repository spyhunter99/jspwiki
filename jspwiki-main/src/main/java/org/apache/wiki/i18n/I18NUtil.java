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
package org.apache.wiki.i18n;

import java.util.Locale;
import java.util.ResourceBundle;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.InternationalizationManager;
import org.apache.wiki.api.providers.PreferenceProvider;

/**
 *
 */
public class I18NUtil {

    /**
     * 
     * @param context WikiContext
     * @param bundleName typically Plugin.CORE_PLUGINS_RESOURCEBUNDLE
     * @return  ResourceBundle
     */
    public static ResourceBundle getBundle(Context context, String bundleName) {
        InternationalizationManager i18n = context.getEngine().getManager(InternationalizationManager.class);
        PreferenceProvider pref = context.getEngine().getManager(PreferenceProvider.class);
        Locale locale = pref.getLocale(context);
        return i18n.getBundle(bundleName, locale);
    }
}
