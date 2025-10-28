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
package org.apache.jspwiki.plugins.hitcounter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;

public class HitCountPlugin implements Plugin {

    private static final Map<String, AtomicInteger> hits = new HashMap<>();

    private final Logger log = Logger.getLogger(HitCountPlugin.class);
    private static final String KEY_PAGEHITCOUNT = "@pageHitCount";

    @Override
    public String execute(Context wikiContext, Map<String, String> params) throws PluginException {
        log.info("STARTED");
        int pageHitCount = 0;
        try {
            Page currentPage = wikiContext.getPage();
            log.info("currentPage=" + currentPage);
            if (hits.containsKey(currentPage.getName())) {
                pageHitCount = hits.get(currentPage.getName()).incrementAndGet();
            } else {
                hits.put(currentPage.getName(), new AtomicInteger(1));
                pageHitCount = 1;
            }
        } catch (Exception e) {
            log.error(e, e);
        }

        log.info("DONE. pageHitCount=" + pageHitCount);
        return "" + pageHitCount;
    }

}
