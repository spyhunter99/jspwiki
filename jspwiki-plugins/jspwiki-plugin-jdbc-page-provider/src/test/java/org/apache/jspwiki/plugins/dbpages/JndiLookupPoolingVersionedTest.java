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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.sql.SQLException;
import javax.naming.Context;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 *
 *
 */
public class JndiLookupPoolingVersionedTest extends JDBCPageProviderBase {

    static ComboPooledDataSource cpds;

    @BeforeAll
    public static void setup2() throws Exception {

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InitialContextFactoryForTest.class.getName());
        cpds = new ComboPooledDataSource();

        cpds.setDriverClass(SQLType.SQLITE.getDriverClass());

        cpds.setJdbcUrl("jdbc:sqlite:./target/" + JndiLookupPoolingVersionedTest.class.getSimpleName() + ".db");
        cpds.setMinPoolSize(0);
        cpds.setAcquireIncrement(1);
        cpds.setMaxPoolSize(10);
        // Construct DataSource 
        InitialContextFactoryForTest.bind("java:/comp/env/jdbc/JSPWIKI", cpds);

    }

    @Override
    public boolean withJndiLookup() {
        return true;
    }

    @AfterAll
    public static void teardown() {
        cpds.close();
    }

    @Override
    public boolean withPooling() {
        return true;
    }

    @Override
    public boolean withVersionSupport() {
        return true;
    }

    @Override
    public SQLType getType() {
        return SQLType.SQLITE;
    }

}
