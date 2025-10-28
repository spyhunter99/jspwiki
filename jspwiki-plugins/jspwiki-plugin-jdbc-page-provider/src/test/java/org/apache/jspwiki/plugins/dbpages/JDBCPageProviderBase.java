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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.search.QueryItem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 */
public abstract class JDBCPageProviderBase {

    private static TestEngine engine;
    private JDBCPageProvider provider;

    public abstract boolean withPooling();

    public boolean withJndiLookup() {
        return false;
    }

    public abstract boolean withVersionSupport();

    public abstract SQLType getType();

    @BeforeEach
    void setupProvider() throws Exception {

        Properties props = TestEngine.getTestProperties();
        props.setProperty(JDBCPageProvider.PROP_DRIVER, getType().getDriverClass());
        if (!withJndiLookup()) {
            if (getType() == SQLType.SQLITE) {
                props.setProperty(JDBCPageProvider.PROP_URL, "jdbc:sqlite:./target/" + this.getClass().getSimpleName() + ".db");
            } else if (getType() == SQLType.DERBY_LOCAL) {
                File target = new File("./target/derby" + this.getClass().getSimpleName());
                String path = target.getCanonicalPath();
                path = path.replace("\\", "/");
                props.setProperty(JDBCPageProvider.PROP_URL, "jdbc:derby:" + path + ";create=true");
            }
        } else {
            props.setProperty(JDBCPageProvider.PARAM_JNDI_SOURCE, "JSPWIKI");
            
        }
        if (withVersionSupport()) {
            props.setProperty(JDBCPageProvider.PROP_VERSIONING, "true");
        } else {
            props.setProperty(JDBCPageProvider.PROP_VERSIONING, "false");
        }
        if (withVersionSupport()) {
            props.setProperty(JDBCPageProvider.PROP_USE_POOLING, "true");
        } else {
            props.setProperty(JDBCPageProvider.PROP_USE_POOLING, "false");
        }

        engine = TestEngine.build(props);
        provider = new JDBCPageProvider();
        provider.initialize(engine, props);
    }

    static Stream<Object> getTestParameters() {
        return Stream.of(
                new Object[]{true, true, SQLType.SQLITE}
        );
    }

    @Test
    void testPutAndGetPageText() throws Exception {
        String id = UUID.randomUUID().toString();

        WikiPage page = new WikiPage(engine, id);
        String text = "Welcome to the test wiki!";

        provider.putPageText(page, text);
        //looks like a typo but the base case is jdbc without versioning support
        //just to trigger the update clause
        provider.putPageText(page, text);
        assertTrue(provider.pageExists(id), "Page should exist after save");
        List<WikiPage> list = provider.getVersionHistory(id);

        String loaded = provider.getPageText(id, list.get(0).getVersion());
        assertEquals(text, loaded, "Loaded text should match what was saved");
    }

    @Test
    void testGetPageInfo() throws Exception {
        String id = UUID.randomUUID().toString();

        WikiPage page = new WikiPage(engine, id);
        String text = "Info content";

        provider.putPageText(page, text);
        WikiPage info = provider.getPageInfo(id, -1);

        assertNotNull(info, "Page info should not be null");
        assertEquals(id, info.getName());
        assertNotNull(info.getLastModified(), "Should have a modified timestamp");
    }

    @Test
    void testDeletePage() throws Exception {
        String id = UUID.randomUUID().toString();

        WikiPage page = new WikiPage(engine, id);
        provider.putPageText(page, "temporary content");
        assertTrue(provider.pageExists(id));

        provider.deletePage(id);
        assertFalse(provider.pageExists(id), "Page should no longer exist after delete");
    }

    @Test
    void testVersionHistory() throws Exception {
        Assumptions.assumeTrue(withVersionSupport());
        String id = UUID.randomUUID().toString();
        WikiPage page = new WikiPage(engine, id);

        provider.putPageText(page, "v1");
        provider.putPageText(page, "v2");
        provider.putPageText(page, "v3");

        List<?> history = provider.getVersionHistory(id);
        assertTrue(history.size() >= 3, "Should have multiple versions in history");
    }

    @Test
    void testGetProviderInfo() {
        String info = provider.getProviderInfo();
        assertNotNull(info);
        assertTrue(info.toLowerCase().contains("jdbc"), "Provider info should mention JDBC");
    }

    @Test
    void testGetAllPagesAndCount() throws Exception {
        provider.putPageText(new WikiPage(engine, "PageA"), "A");
        provider.putPageText(new WikiPage(engine, "PageB"), "B");

        Collection<?> pages = provider.getAllPages();
        assertTrue(pages.size() >= 2);

        int count = provider.getPageCount();
        assertEquals(pages.size(), count);
    }

    @Test
    void testFindPagesByKeyword() throws Exception {
        provider.putPageText(new WikiPage(engine, "FindMe"), "contains keyword orange");
        provider.putPageText(new WikiPage(engine, "OtherPage"), "no match here");
        QueryItem queryItem = new QueryItem();
        queryItem.word = "orange";
        QueryItem[] items = {queryItem};
        Collection<?> results = provider.findPages(items);

        assertEquals(1, results.size());
    }

    @Test
    void testGetAllChangedSince() throws Exception {
        WikiPage page1 = new WikiPage(engine, "OldPage");
        provider.putPageText(page1, "v1");
        Thread.sleep(1000);
        long cutoff = System.currentTimeMillis();
        Thread.sleep(1000);
        WikiPage page2 = new WikiPage(engine, "NewPage");
        provider.putPageText(page2, "v1");

        Date cutoffDate = new Date(cutoff);
        Collection<?> changed = provider.getAllChangedSince(cutoffDate);

        assertTrue(changed.stream().anyMatch(p -> ((WikiPage) p).getName().equals("NewPage")));
    }

    @Test
    void testMovePage() throws Exception {
        provider.putPageText(new WikiPage(engine, "OldName"), "original text");
        provider.movePage("OldName", "NewName");

        assertFalse(provider.pageExists("OldName"));
        assertTrue(provider.pageExists("NewName"));
        assertEquals("original text", provider.getPageText("NewName", -1));
    }

    @Test
    void testDeleteSpecificVersion() throws Exception {
        String id = UUID.randomUUID().toString();
        Assumptions.assumeTrue(withVersionSupport());
        WikiPage page = new WikiPage(engine, id);
        provider.putPageText(page, "v1");
        provider.putPageText(page, "v2");

        List<?> historyBefore = provider.getVersionHistory(id);
        assertTrue(historyBefore.size() >= 2);

        int ver = provider.findLatestVersion(id);
        provider.deleteVersion(id, ver);
        List<?> historyAfter = provider.getVersionHistory(id);

        assertTrue(historyAfter.size() < historyBefore.size());
    }

    @Test
    void testValidateParamsMissingUrlThrows() {
        Assumptions.assumeFalse(withJndiLookup());
        Properties badProps = new Properties();
        badProps.setProperty(JDBCPageProvider.PROP_DRIVER, "org.sqlite.JDBC");
        assertThrows(org.apache.wiki.api.exceptions.NoRequiredPropertyException.class, () -> provider.validateParams(badProps));
    }

    @Test
    void testAddLimitsForDifferentDialects() throws Exception {
        var method = JDBCPageProvider.class.getDeclaredMethod("addLimits",
                Enum.valueOf(SQLType.class, "SQLITE").getClass(),
                String.class, Integer.class);
        method.setAccessible(true);

        String sql = (String) method.invoke(provider, SQLType.SQLITE, "SELECT * FROM pages", 5);
        assertTrue(sql.toLowerCase().contains("limit"));
    }

}
