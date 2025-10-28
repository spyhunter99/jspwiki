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
package org.apache.jspwiki.plugins.dbpages;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 */
public class JDBCPageProviderTest {

    static {
        File f = new File("./target/jspwiki.db");
        if (f.exists()) {
            f.delete();
        }
    }
    private static TestEngine engine;
    private JDBCPageProvider provider;

    public JDBCPageProviderTest() {
        Properties props = TestEngine.getTestProperties();

        TestEngine.build(props);
    }

    @BeforeEach
    void setupProvider() throws Exception {

        Properties props = TestEngine.getTestProperties();
        props.setProperty(JDBCPageProvider.PROP_DRIVER, JDBCPageProvider.SQLType.SQLITE.getDriverClass());
        props.setProperty(JDBCPageProvider.PROP_URL, "jdbc:sqlite:./target/jspwiki.db");
        props.setProperty(JDBCPageProvider.PROP_C3P0, "mypool");

        engine = TestEngine.build(props);
        provider = new JDBCPageProvider();
        provider.initialize(engine, props);
    }

    @Test
    void testPutAndGetPageText() throws Exception {
        WikiPage page = new WikiPage(engine, "FrontPage");
        String text = "Welcome to the test wiki!";

        provider.putPageText(page, text);
        assertTrue(provider.pageExists("FrontPage"), "Page should exist after save");
        List<WikiPage> list = provider.getVersionHistory("FrontPage");
        
        String loaded = provider.getPageText("FrontPage", list.get(0).getVersion());
        assertEquals(text, loaded, "Loaded text should match what was saved");
    }

    @Test
    void testGetPageInfo() throws Exception {
        WikiPage page = new WikiPage(engine, "InfoPage");
        String text = "Info content";

        provider.putPageText(page, text);
        WikiPage info = provider.getPageInfo("InfoPage", -1);

        assertNotNull(info, "Page info should not be null");
        assertEquals("InfoPage", info.getName());
        assertNotNull(info.getLastModified(), "Should have a modified timestamp");
    }

    @Test
    void testDeletePage() throws Exception {
        WikiPage page = new WikiPage(engine, "DeleteMe");
        provider.putPageText(page, "temporary content");
        assertTrue(provider.pageExists("DeleteMe"));

        provider.deletePage("DeleteMe");
        assertFalse(provider.pageExists("DeleteMe"), "Page should no longer exist after delete");
    }

    @Test
    void testVersionHistory() throws Exception {
        WikiPage page = new WikiPage(engine, "VersionedPage");

        provider.putPageText(page, "v1");
        provider.putPageText(page, "v2");
        provider.putPageText(page, "v3");

        List<?> history = provider.getVersionHistory("VersionedPage");
        assertTrue(history.size() >= 3, "Should have multiple versions in history");
    }

    @Test
    void testGetProviderInfo() {
        String info = provider.getProviderInfo();
        assertNotNull(info);
        assertTrue(info.toLowerCase().contains("jdbc"), "Provider info should mention JDBC");
    }

}
