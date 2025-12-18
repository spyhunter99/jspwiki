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
package org.apache.wiki.auth.user;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.login.WebContainerLoginModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 *
 */
public class AccountExpirationMonitorTest {

    private final SimpleDateFormat sdf;

    public AccountExpirationMonitorTest() {
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Test of isNeverExpires method, of class AccountExpirationMonitor.
     */
    @Test
    public void testIsNeverExpires() {

        UserProfile profile = new DefaultUserProfile();
        profile.getAttributes().put(UserProfile.PWD_NEVER_EXPIRES, "true");
        Assertions.assertTrue(AccountExpirationMonitor.isNeverExpires(profile));

        profile.getAttributes().remove(UserProfile.PWD_NEVER_EXPIRES);
        Assertions.assertFalse(AccountExpirationMonitor.isNeverExpires(profile));
    }

    /**
     * Test of isNeverDelete method, of class AccountExpirationMonitor.
     */
    @Test
    public void testIsNeverDelete() {

        UserProfile profile = new DefaultUserProfile();
        profile.getAttributes().put(UserProfile.NEVER_DELETE, "true");
        Assertions.assertTrue(AccountExpirationMonitor.isNeverDelete(profile));

        profile.getAttributes().remove(UserProfile.NEVER_DELETE);
        Assertions.assertFalse(AccountExpirationMonitor.isNeverDelete(profile));
    }

    /**
     * Test of isPasswordExpired method, of class AccountExpirationMonitor.
     */
    @Test
    public void testIsPasswordExpired() {
        UserProfile profile = new DefaultUserProfile();
        profile.getAttributes().put(UserProfile.PWD_EXPIRED, "true");
        Assertions.assertTrue(AccountExpirationMonitor.isPasswordExpired(profile));

        profile.getAttributes().remove(UserProfile.PWD_EXPIRED);
        Assertions.assertFalse(AccountExpirationMonitor.isPasswordExpired(profile));

    }

    /**
     * ;
     * Test of isPasswordNearReminder method, of class AccountExpirationMonitor.
     */
    @Test
    public void testIsPasswordNearReminder() throws Exception {
        Date currentTime;
        Date passwordSetTime;
        synchronized (sdf) {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            //yyyy-MM-ddTHH:mm:ssZ
            currentTime = sdf.parse("2025-12-17T12:00:00");
            Calendar instance = Calendar.getInstance();
            instance.setTimeInMillis(currentTime.getTime());
            instance.add(Calendar.DATE, -74);
            //-75 days is expiration timestamp...
            passwordSetTime = new Date(instance.getTimeInMillis());
        }
        UserProfile profile = new DefaultUserProfile();
        profile.getAttributes().put(UserProfile.PASSWORD_SET_TIME, passwordSetTime.getTime());
        Assertions.assertTrue(AccountExpirationMonitor.isPasswordNearReminder(profile, currentTime.getTime()));

        profile.getAttributes().put(UserProfile.PASSWORD_SET_TIME, currentTime.getTime());
        Assertions.assertFalse(AccountExpirationMonitor.isPasswordNearReminder(profile, currentTime.getTime()));

        profile.getAttributes().put(UserProfile.PASSWORD_SET_TIME, passwordSetTime.getTime() + DAYS2X);
        Assertions.assertFalse(AccountExpirationMonitor.isPasswordNearReminder(profile, currentTime.getTime()));

    }
    final long DAYS2X = 2 * 24 * 60 * 60 * 1000;

    /**
     * Test of isPasswordAboutToExpire method, of class
     * AccountExpirationMonitor.
     */
    @Test
    public void testIsPasswordAboutToExpire() throws ParseException {
        Date currentTime;
        Date passwordSetTime;
        synchronized (sdf) {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            currentTime = sdf.parse("2025-12-17T12:00:00");
            //-89 days
            passwordSetTime = sdf.parse("2025-09-19T12:00:00");
        }
        UserProfile profile = new DefaultUserProfile();
        profile.getAttributes().put(UserProfile.PASSWORD_SET_TIME, passwordSetTime.getTime());

        Assertions.assertTrue(AccountExpirationMonitor.isPasswordAboutToExpire(profile, currentTime.getTime()));

        profile.getAttributes().put(UserProfile.PASSWORD_SET_TIME, currentTime.getTime());

        Assertions.assertFalse(AccountExpirationMonitor.isPasswordAboutToExpire(profile, currentTime.getTime()));

    }

    /**
     * Test of isPasswordNowExpired method, of class AccountExpirationMonitor.
     */
    @Test
    public void testIsPasswordNowExpired() throws ParseException {
        Date currentTime;
        Date passwordSetTime;
        synchronized (sdf) {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            currentTime = sdf.parse("2025-12-17T12:00:00");
            //-90 days
            passwordSetTime = sdf.parse("2025-09-18T12:00:00");
        }
        UserProfile profile = new DefaultUserProfile();
        profile.getAttributes().put(UserProfile.PASSWORD_SET_TIME, passwordSetTime.getTime());
        Assertions.assertTrue(AccountExpirationMonitor.isPasswordNowExpired(profile, currentTime.getTime()));

        profile.getAttributes().put(UserProfile.PASSWORD_SET_TIME, currentTime.getTime());
        Assertions.assertFalse(AccountExpirationMonitor.isPasswordNowExpired(profile, currentTime.getTime()));

    }

    /**
     * Test of shouldSendAccountInactiviedReminder method, of class
     * AccountExpirationMonitor.
     */
    @Test
    public void testShouldSendAccountInactiviedReminder() throws ParseException {
        Date currentTime;
        Date lastModified;
        synchronized (sdf) {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            currentTime = sdf.parse("2025-12-17T12:00:00");
            //-180 days
            Calendar instance = Calendar.getInstance();
            instance.setTimeZone(TimeZone.getTimeZone("UTC"));
            instance.setTimeInMillis(currentTime.getTime());

            instance.add(Calendar.DATE, -AccountExpirationMonitor.getAccountInactivityReminder());
            lastModified = instance.getTime();
        }
        UserProfile profile = new DefaultUserProfile();
        profile.setLastModified(lastModified);
        System.out.println(currentTime.toString());
        System.out.println(lastModified.toString());
        //Wed Dec 17 07:00:00 EST 2025
        //Fri Jun 20 07:00:00 EDT 2025
        Assertions.assertTrue(AccountExpirationMonitor.shouldSendAccountInactivityReminder(profile, currentTime.getTime()));

        profile.setLastModified(currentTime);
        Assertions.assertFalse(AccountExpirationMonitor.shouldSendAccountInactivityReminder(profile, currentTime.getTime()));

    }

    /**
     * Test of shouldSend2ndAccountReminder method, of class
     * AccountExpirationMonitor.
     */
    @Test
    public void testShouldSend2ndAccountReminder() throws ParseException {
        Date currentTime;
        Date lastModified;
        synchronized (sdf) {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            currentTime = sdf.parse("2025-12-17T12:00:00");
            //-269 days
            Calendar instance = Calendar.getInstance();
            instance.setTimeZone(TimeZone.getTimeZone("UTC"));
            instance.setTimeInMillis(currentTime.getTime());

            instance.add(Calendar.DATE, -AccountExpirationMonitor.getAccountDeletionReminder());
            lastModified = instance.getTime();
        }
        UserProfile profile = new DefaultUserProfile();
        profile.setLastModified(lastModified);
        Assertions.assertTrue(AccountExpirationMonitor.shouldSend2ndAccountReminder(profile, currentTime.getTime()));

        profile.setLastModified(currentTime);
        Assertions.assertFalse(AccountExpirationMonitor.shouldSend2ndAccountReminder(profile, currentTime.getTime()));

    }

    /**
     * Test of shouldSendFinalReminder method, of class
     * AccountExpirationMonitor.
     */
    @Test
    public void testShouldSendFinalReminder() throws ParseException {
        Date currentTime;
        Date lastModified;
        synchronized (sdf) {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            currentTime = sdf.parse("2025-12-17T12:00:00");
            Calendar instance = Calendar.getInstance();
            instance.setTimeZone(TimeZone.getTimeZone("UTC"));
            instance.setTimeInMillis(currentTime.getTime());

            instance.add(Calendar.DATE, -AccountExpirationMonitor.getAccountAutomaticDeletion());
            lastModified = instance.getTime();
        }
        UserProfile profile = new DefaultUserProfile();
        profile.setLastModified(lastModified);
        Assertions.assertTrue(AccountExpirationMonitor.shouldSendFinalReminder(profile, currentTime.getTime()));

        profile.setLastModified(currentTime);
        Assertions.assertFalse(AccountExpirationMonitor.shouldSendFinalReminder(profile, currentTime.getTime()));

    }

    /**
     * Test of shouldDeleteUserProfile method, of class
     * AccountExpirationMonitor.
     */
    @Test
    public void testShouldDeleteUserProfile() throws ParseException {
        Date currentTime;
        Date lastModified;
        synchronized (sdf) {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            currentTime = sdf.parse("2025-12-17T12:00:00");
            //2 years old
            lastModified = sdf.parse("2023-12-17T12:00:00");
        }
        UserProfile profile = new DefaultUserProfile();
        profile.setLastModified(lastModified);
        Assertions.assertTrue(AccountExpirationMonitor.shouldDeleteUserProfile(profile, currentTime.getTime()));

        profile.setLastModified(currentTime);
        Assertions.assertFalse(AccountExpirationMonitor.shouldDeleteUserProfile(profile, currentTime.getTime()));

    }

    /**
     * Test of deleteUserProfile method, of class AccountExpirationMonitor.
     *
     * @Test public void testDeleteUserProfile() {
     * System.out.println("deleteUserProfile"); UserProfile profile = null;
     * UserManager mgr = null; Engine engine = null; boolean expResult = false;
     * boolean result = AccountExpirationMonitor.deleteUserProfile(profile, mgr,
     * engine); assertEquals(expResult, result); // TODO review the generated
     * test code and remove the default call to fail. fail("The test case is a
     * prototype."); }
     */
    @Test
    public void testInitDisabled() throws Exception {
        AccountExpirationMonitor monitor = new AccountExpirationMonitor();
        Properties testProperties = TestEngine.getTestProperties();
        testProperties.setProperty("jspwiki.security.accountmonitor.enable", "false");
        TestEngine testEngine = new TestEngine(testProperties);
        Assertions.assertFalse(monitor.initialize(testEngine, testProperties));
        monitor.shutdown();
    }

    @Test
    public void testInitEnabled() throws Exception {
        AccountExpirationMonitor monitor = new AccountExpirationMonitor();
        Properties testProperties = TestEngine.getTestProperties();
        testProperties.setProperty("jspwiki.security.accountmonitor.enable", "true");
        TestEngine testEngine = new TestEngine(testProperties);
        Assertions.assertTrue(monitor.initialize(testEngine, testProperties));
        monitor.shutdown();
    }

    public static void main(String[] args0) throws Exception {
        //a utility to patch up the user database
        // File userDatabase = new File("target/" + this.getClass().getSimpleName() + "-" + UUID.randomUUID().toString() + ".xml");
        //FileUtils.copyFile(new File("src/test/resources/userdatabase.xml"), userDatabase);

        Properties testProperties = TestEngine.getTestProperties();
        testProperties.setProperty(XMLUserDatabase.PROP_USERDATABASE, "src/test/resources/userdatabase.xml");
        TestEngine testEngine = new TestEngine(testProperties);
        UserManager manager = testEngine.getManager(UserManager.class);
        XMLUserDatabase xml = (XMLUserDatabase) manager.getUserDatabase();
        List<UserProfile> query = xml.query(new UserQuery(0, 100));
        for (UserProfile p : query) {
            switch (p.getLoginName()) {
                case "admin":
                    p.getAttributes().put(UserProfile.NEVER_DELETE, "true");
                    p.getAttributes().put(UserProfile.PWD_NEVER_EXPIRES, "true");
                    xml.save(p);
                    break;
            }
        }
    }

    @Test
    public void testRun() throws Exception {
        File userDatabase = new File("target/" + this.getClass().getSimpleName() + "-" + UUID.randomUUID().toString() + ".xml");
        FileUtils.copyFile(new File("src/test/resources/userdatabase.xml"), userDatabase);

        AccountExpirationMonitor monitor = new AccountExpirationMonitor();
        Properties testProperties = TestEngine.getTestProperties();
        testProperties.setProperty(XMLUserDatabase.PROP_USERDATABASE, userDatabase.getAbsolutePath());
        testProperties.setProperty("jspwiki.security.accountmonitor.enable", "true");
        TestEngine testEngine = new TestEngine(testProperties);
        //TODO insert a test page with specific permissions
        Assertions.assertTrue(monitor.initialize(testEngine, testProperties));

        monitor.run();

        monitor.shutdown();
        UserManager manager = testEngine.getManager(UserManager.class);
        UserDatabase userDatabase1 = manager.getUserDatabase();
        List<UserProfile> query = userDatabase1.query(new UserQuery(0, 100));
        Assertions.assertFalse(query.isEmpty());
        for (UserProfile profile : query) {
            if (profile.getLoginName().equalsIgnoreCase("admin")) {
                Assertions.assertFalse(AccountExpirationMonitor.isPasswordExpired(profile));
            }
        }
        //at this point, most of the users should be marked as 
        //a) passwords expired
        //b) probably should be all deleted too.
        //TODO check to ensure the test page has the permission removed for the deleted account.
    }

    @Test
    public void testInitEnabledWithBadSettings() throws Exception {
        AccountExpirationMonitor monitor = new AccountExpirationMonitor();
        Properties testProperties = TestEngine.getTestProperties();
        testProperties.setProperty("jspwiki.security.accountmonitor.enable", "true");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_AUTO_DELETION, "-99");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_DELETION_REMINDER, "-99");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_INACTIVITY_REMINDER, "-99");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION, "-99");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION_REMINDER, "-99");
        TestEngine testEngine = new TestEngine(testProperties);
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_INACTIVITY_REMINDER, "1");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_DELETION_REMINDER, "1");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_AUTO_DELETION, "1");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION, "1");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION_REMINDER, "1");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });

        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION, "2");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION_REMINDER, "1");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });

        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_AUTO_DELETION, "3");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION, "2");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION_REMINDER, "1");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });

        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_DELETION_REMINDER, "4");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_AUTO_DELETION, "3");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION, "2");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION_REMINDER, "1");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });

        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_AUTO_DELETION, "5");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_DELETION_REMINDER, "4");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_INACTIVITY_REMINDER, "3");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION, "5");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION_REMINDER, "3");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });
        monitor.shutdown();

        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_AUTO_DELETION, "99");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_DELETION_REMINDER, "70");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_INACTIVITY_REMINDER, "3");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION, "5");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION_REMINDER, "2");
        Assertions.assertThrows(WikiException.class, () -> {
            monitor.initialize(testEngine, testProperties);
        });
        monitor.shutdown();

        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_AUTO_DELETION, "5");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_DELETION_REMINDER, "4");
        testProperties.setProperty(AccountExpirationMonitor.PROP_ACCOUNT_INACTIVITY_REMINDER, "3");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION, "2");
        testProperties.setProperty(AccountExpirationMonitor.PROP_PWD_EXPIRATION_REMINDER, "1");
        Assertions.assertDoesNotThrow(() -> {
            monitor.initialize(testEngine, testProperties);
        });
        monitor.shutdown();
    }

    @Test
    public void testInitWithContainerAuth() throws Exception {
        AccountExpirationMonitor monitor = new AccountExpirationMonitor();
        Properties testProperties = TestEngine.getTestProperties();
        testProperties.setProperty("jspwiki.security.accountmonitor.enable", "true");
        testProperties.setProperty("jspwiki.loginModule.class", WebContainerLoginModule.class.getCanonicalName());
        testProperties.setProperty("jspwiki.authorizer", "org.apache.wiki.auth.authorize.WebContainerAuthorizer");
        TestEngine testEngine = new TestEngine(testProperties);
        Assertions.assertFalse(monitor.initialize(testEngine, testProperties));
        monitor.shutdown();
    }

}
