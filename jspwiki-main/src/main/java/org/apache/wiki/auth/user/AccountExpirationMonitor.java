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

import jakarta.mail.MessagingException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.acl.AclEntryImpl;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.util.MailUtil;

/**
 * periodically, daily for example, checks over all user accounts, specifically
 * the {@link UserProfile#getLastModified() } date. The time delta between now
 * and last modified is greater than a threshold, an email is dispatched to the
 * user reminding them to sign in, otherwise their account will be disabled
 * after X days and then auto deleted after Y days.
 *
 * It also checks for expired passwords
 */
public class AccountExpirationMonitor implements Runnable {

    private static final Logger LOG = LogManager.getLogger(AccountExpirationMonitor.class);

    private Engine engine;
    //all units of time are in days
    private int accountInactivityReminder = 180;
    private int accountDeletionReminder = 270;
    private int accountAutomaticDeletion = 365;
    private int passwordExpiration = 90;
    private int passwordExpirationReminder = 75;
    private static final long UNITS = 24 * 60 * 60 * 1000;
    private static final String PW_REMINDER_SUBJECT = "reminder.passwordExpiration.subject";
    private static final String PW_REMINDER_BODY = "reminder.passwordExpiration.body";
    private static final String PW_EXPIRED_BODY = "reminder.passwordExpirated.body";
    private static final String PW_ACCOUNT_INACTIVITY_SUBJECT = "reminder.accountInactivity.subject";
    private static final String PW_ACCOUNT_INACTIVITY_SUBJECT_DELETED = "reminder.accountInactivity.body.deleted";
    private static final String PW_ACCOUNT_INACTIVITY_BODY_DELETED = "reminder.accountInactivity.subject.deleted";
    private static final String PW_ACCOUNT_INACTIVITY_SUBJECT_FINAL = "reminder.accountInactivity.subject.final";
    private static final String PW_ACCOUNT_INACTIVITY_BODY = "reminder.accountInactivity.body";
    private InternationalizationManager i18n;
    private UserManager mgr;
    
    public void initialize(Engine engine, Properties wikiProperties) {
        if ("true".equalsIgnoreCase(wikiProperties.getProperty("jspwiki.security.accountmonitor.enable"))) {
            this.engine = engine;
            mgr = engine.getManager(UserManager.class);
            i18n = engine.getManager(InternationalizationManager.class);
            exec = new ScheduledThreadPoolExecutor(2);
            accountInactivityReminder = Integer.parseInt(wikiProperties.getProperty("jspwiki.security.accountmonitor.accountInactivityReminder", "180"));
            accountDeletionReminder = Integer.parseInt(wikiProperties.getProperty("jspwiki.security.accountmonitor.accountDeletionReminder", "270"));
            accountAutomaticDeletion = Integer.parseInt(wikiProperties.getProperty("jspwiki.security.accountmonitor.accountAutomaticDeletion", "365"));
            passwordExpiration = Integer.parseInt(wikiProperties.getProperty("jspwiki.security.accountmonitor.passwordExpiration", "90"));
            passwordExpirationReminder = Integer.parseInt(wikiProperties.getProperty("jspwiki.security.accountmonitor.passwordExpirationReminder", "75"));
            exec.scheduleAtFixedRate(this, 0, 1, TimeUnit.DAYS);
        }
    }
    private ScheduledThreadPoolExecutor exec = null;

    public void shutdown() {

        if (exec != null) {
            exec.shutdownNow();
        }
    }

    private void checkAccounts() throws WikiSecurityException {
        LOG.info("checking accounts for expiration noticies");
        
        int offset = 0;
        List<UserProfile> profiles = mgr.getUserDatabase().query(new UserQuery(offset, 100));
        while (!profiles.isEmpty()) {
            for (UserProfile profile : profiles) {
                if (check(profile)) {
                    offset--;
                }
            }

            offset += 100;
            profiles = mgr.getUserDatabase().query(new UserQuery(offset, 100));
        }
    }

    //return ture if it was deleted
    private boolean check(UserProfile profile) {
        final Locale loc = Locale.forLanguageTag((String) profile.getAttributes().getOrDefault(Preferences.USERPREF_LANGUAGE,
                Locale.getDefault().toLanguageTag()));

        ResourceBundle bundle = i18n.getBundle(InternationalizationManager.CORE_BUNDLE, loc);
        SimpleDateFormat sdf = new SimpleDateFormat(
                (String) profile.getAttributes().
                        getOrDefault(Preferences.USERPREF_DATEFORMAT, Preferences.DEFAULT_DATEFORMAT));

        final long delta = System.currentTimeMillis() - profile.getLastModified().getTime();

        boolean reminderSent = false;
        //NOTE: this only applies to JSPWIKI's built in user account databases.
        //container based stuff does not apply here since we don't manage the password.

        //if the account has a special flag to prevent password expiration, check that
        if (!"true".equalsIgnoreCase((String) profile.getAttributes().get("PWD_NEVER_EXPIRE"))) {

            if (profile.getAttributes().get("PWD_EXPIRED") == null
                    || Boolean.FALSE == profile.getAttributes().get("PWD_EXPIRED")) {
                //if we are not expired. check for password expiration
                Long setTime = (Long) profile.getAttributes().get("PASSWORD_SET_TIME");
                if (setTime == null) {
                    //could be a jspwiki upgrade situation, old account etc. force
                    //the pwd reset
                    return false;
                }
                final long passwordExpirationDate = setTime + passwordExpiration;
                final String formatedDate = sdf.format(new Date(passwordExpirationDate));
                final long pwdDelta = System.currentTimeMillis() - setTime;
                boolean sendReminder = false;
                if ((passwordExpirationReminder * UNITS) > pwdDelta && pwdDelta < ((passwordExpirationReminder + 1) * UNITS)) {
                    //it's the day of the reminder, fire off the email.
                    sendReminder = true;
                    String subject = MessageFormat.format(
                            bundle.getString(PW_REMINDER_SUBJECT),
                            engine.getWikiProperties().getProperty(Engine.PROP_APPNAME));
                    String body = MessageFormat.format(
                            bundle.getString(PW_REMINDER_BODY),
                            profile.getFullname(),
                            formatedDate,
                            engine.getWikiProperties().getProperty("jspwiki.publicUrl"));
                    try {
                        MailUtil.sendMessage(engine.getWikiProperties(), profile.getEmail(), subject, body);
                    } catch (MessagingException ex) {
                        LOG.warn("failed to email to " + profile.getEmail() + " " + ex.getMessage());
                    }
                }
                if (((passwordExpiration - 1) * UNITS) > pwdDelta && pwdDelta < ((passwordExpiration) * UNITS)) {
                    //another reminder the day before
                    sendReminder = true;
                    String subject = MessageFormat.format(
                            bundle.getString(PW_REMINDER_SUBJECT),
                            engine.getWikiProperties().getProperty(Engine.PROP_APPNAME));
                    String body = MessageFormat.format(
                            bundle.getString(PW_REMINDER_BODY),
                            profile.getFullname(),
                            formatedDate,
                            engine.getWikiProperties().getProperty("jspwiki.publicUrl"));
                    try {
                        MailUtil.sendMessage(engine.getWikiProperties(), profile.getEmail(), subject, body);
                    } catch (MessagingException ex) {
                        LOG.warn("failed to email to " + profile.getEmail() + " " + ex.getMessage());
                    }
                }

                if (((passwordExpiration) * UNITS) > pwdDelta) {
                    //password is now expired and will need to be changed at sign in time.
                    LOG.info(profile.getLoginName() + " password has expired");
                    String subject = MessageFormat.format(
                            bundle.getString(PW_REMINDER_SUBJECT),
                            engine.getWikiProperties().getProperty(Engine.PROP_APPNAME));
                    String body = MessageFormat.format(
                            bundle.getString(PW_EXPIRED_BODY),
                            profile.getFullname(),
                            formatedDate,
                            engine.getWikiProperties().getProperty("jspwiki.publicUrl"));
                    try {
                        MailUtil.sendMessage(engine.getWikiProperties(), profile.getEmail(), subject, body);
                    } catch (MessagingException ex) {
                        LOG.warn("failed to email to " + profile.getEmail() + " " + ex.getMessage());
                    }

                }
            }
        }
        if (reminderSent) {
            //no need to check account expiration at this point.
            return false;
        }

        //if the account has a special flag that prevents account deletion, check that
        if ("true".equalsIgnoreCase((String) profile.getAttributes().get("NEVER_DELETE"))) {
            return false;
        }
        //check for account activity
        final long accountDeletionDate = profile.getLastModified().getTime() + (accountAutomaticDeletion * UNITS);
        final String formatedDate = sdf.format(new Date(accountDeletionDate));
        if (delta > accountInactivityReminder * UNITS && delta < (accountInactivityReminder + 1) * UNITS) {
            //send reminder to use the account within accountAutomaticDeletion-accountInactivityReminder days
            //or it will be deleted
          
            String subject = MessageFormat.format(
                    bundle.getString(PW_ACCOUNT_INACTIVITY_SUBJECT),
                    engine.getWikiProperties().getProperty(Engine.PROP_APPNAME));
            String body = MessageFormat.format(
                    bundle.getString(PW_ACCOUNT_INACTIVITY_BODY),
                    profile.getFullname(),
                    formatedDate,
                    engine.getWikiProperties().getProperty("jspwiki.publicUrl"));
            try {
                MailUtil.sendMessage(engine.getWikiProperties(), profile.getEmail(), subject, body);
            } catch (MessagingException ex) {
                LOG.warn("failed to email to " + profile.getEmail() + " " + ex.getMessage());
            }

        }
        if (delta > accountDeletionReminder * UNITS && delta < (accountDeletionReminder + 1) * UNITS) {
            //send reminder to use the account within accountAutomaticDeletion-accountInactivityReminder days
            //or it will be deleted
            String subject = MessageFormat.format(
                    bundle.getString(PW_ACCOUNT_INACTIVITY_SUBJECT),
                    engine.getWikiProperties().getProperty(Engine.PROP_APPNAME));
            String body = MessageFormat.format(
                    bundle.getString(PW_ACCOUNT_INACTIVITY_BODY),
                    profile.getFullname(),
                    formatedDate,
                    engine.getWikiProperties().getProperty("jspwiki.publicUrl"));
            try {
                MailUtil.sendMessage(engine.getWikiProperties(), profile.getEmail(), subject, body);
            } catch (MessagingException ex) {
                LOG.warn("failed to email to " + profile.getEmail() + " " + ex.getMessage());
            }
        }

        if (delta > (accountDeletionReminder - 1) * UNITS && delta < (accountDeletionReminder) * UNITS) {
            //send final reminder to use the account within accountAutomaticDeletion-accountInactivityReminder days
            //or it will be deleted
          
            String subject = MessageFormat.format(
                    bundle.getString(PW_ACCOUNT_INACTIVITY_SUBJECT_FINAL),
                    engine.getWikiProperties().getProperty(Engine.PROP_APPNAME));
            String body = MessageFormat.format(
                    bundle.getString(PW_ACCOUNT_INACTIVITY_BODY),
                    profile.getFullname(),
                    formatedDate,
                    engine.getWikiProperties().getProperty("jspwiki.publicUrl"));
            try {
                MailUtil.sendMessage(engine.getWikiProperties(), profile.getEmail(), subject, body);
            } catch (MessagingException ex) {
                LOG.warn("failed to email to " + profile.getEmail() + " " + ex.getMessage());
            }
        }

        if (delta > accountAutomaticDeletion * UNITS) {
            //delete the account
            String subject = MessageFormat.format(
                    bundle.getString(PW_ACCOUNT_INACTIVITY_SUBJECT_DELETED),
                    engine.getWikiProperties().getProperty(Engine.PROP_APPNAME));
            String body = MessageFormat.format(
                    bundle.getString(PW_ACCOUNT_INACTIVITY_BODY_DELETED),
                    profile.getFullname(),
                    engine.getWikiProperties().getProperty("jspwiki.publicUrl"));
            try {
                MailUtil.sendMessage(engine.getWikiProperties(), profile.getEmail(), subject, body);
            } catch (MessagingException ex) {
                LOG.warn("failed to email to " + profile.getEmail() + " " + ex.getMessage());
            }
            mgr.deleteUser(profile);
            PageManager manager = engine.getManager(PageManager.class);
            Collection<Page> allPages = manager.getAllPages();
            for (Page p : allPages) {
                if (p.getAcl()!=null) {
                    Enumeration<AclEntry> aclEntries = p.getAcl().aclEntries();
                    boolean changed=false;
                    while (aclEntries.hasMoreElements()) {
                        AclEntry nextElement = aclEntries.nextElement();
                        if (nextElement.getPrincipal().getName().equals(profile.getLoginName()) ||
                                nextElement.getPrincipal().getName().equals(profile.getFullname()) ||
                                nextElement.getPrincipal().getName().equals(profile.getWikiName()) ||
                                nextElement.getPrincipal().getName().equals(profile.getEmail())) {
                            //remove the entry
                        }
                    }
                    if (changed) {
                        //how do we set the page ACLs?
                    }
                    
                }
            }
            //update the groups that they were in, if any
            //and if there's no one else in the group, delete the group.
            //send an email, sorry to see you go. you can always sign up again.
            //but we might have a new risk. pages with explicit user account permissions for the 
            //now deleted account...if someone makes a new account with the same name,
            //it might not be the same person. so... we need to check every page
            //then alter the permission statement to remove them.
            //if the page had permissions to group that might have been deleted, nuke that too
            //however if there's no more users in the permission list, set it to an admin
            //account and email the admins about this. effectively the page is now orphaned
            //and instead of making it public access (removing the permission check)
            //we have to attach it to someone.
            return true;
        }

        return false;
    }

    @Override
    public void run() {
        try {
            checkAccounts();
        } catch (WikiSecurityException ex) {
            LOG.error("error trapped checking user accounts for expiration notifies", ex);
        }
    }

    private void sendAlert(UserProfile profile, String subjectKey, String bodyKey, long accountDeletionDate) {

    }
}
