/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.crypto;

import net.sf.ehcache.CacheManager;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.exceptions.EncryptionException;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefaultCryptoProviderTest {

    TestEngine m_engine;

    @BeforeEach
    protected void setUp() throws Exception {

        Properties props = TestEngine.getTestProperties();
        CacheManager.getInstance().removeAllCaches();
        TestEngine.emptyWorkDir();

        // Create crypto properties and write to file
        Properties cryptoProps = new Properties();
        cryptoProps.setProperty("crypto.key", "xyzpwd");

        String tmpdir = System.getProperties().getProperty("java.io.tmpdir");
        File cryptoFile = new File(tmpdir + CryptoProvider.DEFAULT_CRYPTO_FILENAME);
        cryptoProps.store(new FileWriter(cryptoFile), "Save to file");

        //props.setProperty("crypto.file",f.getAbsolutePath());
        m_engine = new TestEngine(props);
    }

    @Test
    public void testEncryptAndDecrypt() throws Exception {
        DefaultCryptoProvider provider = new DefaultCryptoProvider();
        provider.initialize(m_engine, m_engine.getWikiProperties());
        char[] password1 = "P@ssword1".toCharArray();
        char[] password2 = "Pa$$w0rd2".toCharArray();
        String content1 = "This is simple";
        String content2 = "This may work";

        // Test content1&2 with password1
        byte[] secret1 = provider.encrypt(password1, content1.getBytes());
        byte[] secret2 = provider.encrypt(password1, content2.getBytes());
        String result1 = new String(provider.decrypt(password1, secret1));
        String result2 = new String(provider.decrypt(password1, secret2));
        Assertions.assertNotNull(result1);
        Assertions.assertNotNull(result2);
        Assertions.assertEquals(content1, result1);
        Assertions.assertEquals(content2, result2);

        try {
            result1 = new String(provider.decrypt(password2, secret1));
        } catch (EncryptionException e) {
            Assertions.assertTrue(e.getMessage().contains("BadPaddingException"));
        }
        Assertions.assertEquals(content1, result1);
    }
}
