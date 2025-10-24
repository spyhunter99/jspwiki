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
package org.apache.jspwiki.plugins.jira;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import org.apache.jspwiki.plugins.jira.JiraPlugin.MetadataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JiraPluginTest {

    Logger log = Logger.getLogger(JiraPluginTest.class);
    JiraRestClient restClient;

    @BeforeEach
    public void setUp() throws Exception {
        restClient = JiraPlugin.getRestClient(JiraPlugin.DEFAULT_JIRA_BASEURL);
    }

    @AfterEach
    public void tearDown() throws Exception {
        restClient = null;
    }

    @Test
    public void testJiraConnection() throws URISyntaxException, JSONException {
        Assertions.assertNotNull(restClient);
        Issue issue1 = restClient.getIssueClient().getIssue("JSPWIKI-864").claim();
        Assertions.assertNotNull(issue1);
        Assertions.assertEquals("JSPWIKI-864", issue1.getKey());
        String expected = "https://issues.apache.org/jira/images/icons/statuses/closed.png";
        String iconUrl = JiraPlugin.getIconUrl(restClient, MetadataType.STATUS, issue1.getStatus().getSelf());
        Assertions.assertEquals(expected, iconUrl);
//        System.out.println("issue ="+issue1.getKey()+" "+issue1.getSummary());
    }

    @Test
    public void testSearch() throws URISyntaxException {
        int max = 10;
        int start = 0;
        List<Issue> issues = JiraPlugin.doJQLSearch(restClient, "JSPWIKI", max, start, JiraPlugin.DEFAULT_JQL);

        Assertions.assertEquals(max, issues.size());
        for (Issue issue : issues) {
            Assertions.assertNotNull(issue);
            Assertions.assertNotNull(issue.getKey());
            Assertions.assertNotNull(issue.getSummary());
            Assertions.assertNotNull(issue.getSelf());

            Assertions.assertEquals("Open", issue.getStatus().getName());
            log.info(issue.getKey() + " " + issue.getSummary() + " " + issue.getStatus());
        }
    }

    @Test
    public void testPrintIssue() throws JSONException {
        Issue issue = restClient.getIssueClient().getIssue("JSPWIKI-123").claim();
        Assertions.assertNotNull(issue);
        log.debug(issue.getKey() + " " + issue.getSummary() + " " + issue.getStatus().getName());
        log.trace(issue);
        // | ID | Type | Priority | Summary | Status | Resolution | Assignee | Reporter | Comments
        String expected = "| [JSPWIKI-123|https://issues.apache.org/jira/browse/JSPWIKI-123] | [https://issues.apache.org/jira/images/icons/priorities/minor.svg] | [https://issues.apache.org/jira/secure/viewavatar?size=xsmall&avatarId=21140&avatarType=issuetype] | missing german date format | [https://issues.apache.org/jira/images/icons/statuses/closed.png] | Fixed |  | Florian Holeczek | 11";
        String actual = JiraPlugin.getIssueAsWikiText(restClient, JiraPlugin.DEFAULT_JIRA_BASEURL, issue);
        //System.out.println(actual);
        Assertions.assertEquals(expected, actual);
    }

}
