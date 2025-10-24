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
package org.apache.jspwiki.plugins.autolink;

import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AutoLinkHtmlFilterTest {

    @Test
    public void testFindURL() {
        String content = "This is a http://www.google.com link to a page on [http://digitalspider.com.au] and http://test/abc.com?abc='21'"
                + " in other news [prelinked|http://prelinked.com] values should not autolink."
                + " but https://link.for.me/test/ should. "
                + " {{{ However content within http://noformat.com and [http://noformatlinked.com] should not link }}}";

        Collection<String> htmlStrings = AutoLinkHtmlFilter.findByRegex(content, AutoLinkHtmlFilter.REGEX_HTML);
//        System.out.println("htmlStrings="+htmlStrings);
        Assertions.assertEquals(5, htmlStrings.size());
        Assertions.assertTrue(htmlStrings.contains("http://www.google.com"));
        Assertions.assertTrue(htmlStrings.contains("http://digitalspider.com.au"));
        Assertions.assertTrue(htmlStrings.contains("http://test/abc.com?abc='21'"));
        Assertions.assertTrue(htmlStrings.contains("http://prelinked.com"));
        Assertions.assertTrue(htmlStrings.contains("https://link.for.me/test/"));

        Collection<String> linkedHtmlStrings = AutoLinkHtmlFilter.findByRegex(content, AutoLinkHtmlFilter.REGEX_HTML_LINKED);
//        System.out.println("linkedHtmlStrings="+linkedHtmlStrings);
        Assertions.assertEquals(2, linkedHtmlStrings.size());
        Assertions.assertTrue(linkedHtmlStrings.contains("[http://digitalspider.com.au]"));
        Assertions.assertTrue(linkedHtmlStrings.contains("|http://prelinked.com]"));
        linkedHtmlStrings = AutoLinkHtmlFilter.getUnlinkedCollection(linkedHtmlStrings);
        Assertions.assertTrue(linkedHtmlStrings.contains("http://digitalspider.com.au"));
        Assertions.assertTrue(linkedHtmlStrings.contains("http://prelinked.com"));

        htmlStrings = AutoLinkHtmlFilter.removeAll(htmlStrings, linkedHtmlStrings);
        Assertions.assertEquals(3, htmlStrings.size());
        Assertions.assertTrue(htmlStrings.contains("http://www.google.com"));
        Assertions.assertTrue(htmlStrings.contains("http://test/abc.com?abc='21'"));
        Assertions.assertTrue(htmlStrings.contains("https://link.for.me/test/"));

        for (String link : htmlStrings) {
            content = content.replace(link, "[" + link + "]");
        }
        String expectedContent = "This is a [http://www.google.com] link to a page on [http://digitalspider.com.au] and [http://test/abc.com?abc='21']"
                + " in other news [prelinked|http://prelinked.com] values should not autolink."
                + " but [https://link.for.me/test/] should. "
                + " {{{ However content within http://noformat.com and [http://noformatlinked.com] should not link }}}";
        Assertions.assertEquals(expectedContent, content);

        htmlStrings = AutoLinkHtmlFilter.findByRegex(content, AutoLinkHtmlFilter.REGEX_HTML);
        Assertions.assertEquals(5, htmlStrings.size());
        linkedHtmlStrings = AutoLinkHtmlFilter.findByRegex(content, AutoLinkHtmlFilter.REGEX_HTML_LINKED);
        Assertions.assertEquals(5, linkedHtmlStrings.size());
    }

    public void testFindURLInEmbeddedPlugin() {
        String content = "This is a [{ImageGallery url=http://www.embed.com/ items=3 width=900 steps=3 autoplay=2000 speed=200 arrows=true nav=true sortby=name sortdesc=true}] embedded plugin with http://test.com parameters. With {{{ http://noformat.com text }}} inside.";

        Collection<String> htmlStrings = AutoLinkHtmlFilter.findByRegex(content, AutoLinkHtmlFilter.REGEX_HTML);
        //System.out.println("htmlStrings="+htmlStrings);
        Assertions.assertEquals(1, htmlStrings.size());
        Assertions.assertTrue(htmlStrings.contains("http://test.com"));
        Assertions.assertFalse(htmlStrings.contains("http://www.embed.com"));
        Assertions.assertFalse(htmlStrings.contains("http://noformat.com"));
    }
}
