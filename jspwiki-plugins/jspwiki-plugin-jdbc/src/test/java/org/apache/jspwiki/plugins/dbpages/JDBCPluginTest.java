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
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import org.apache.wiki.TestEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

/**
 * test cases the JDCP query to wiki table plugin
 */
public class JDBCPluginTest {

    private TestEngine testEngine = TestEngine.build();
    static ComboPooledDataSource cpds;

    @BeforeAll
    public static void setup2() throws Exception {

        System.setProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY, InitialContextFactoryForTest.class.getName());
        cpds = new ComboPooledDataSource();

        cpds.setDriverClass(SQLType.SQLITE.getDriverClass());
        File db = new File("./target/" + JDBCPluginTest.class.getSimpleName() + ".db");
        if (db.exists()) {
            db.delete();
        }
        cpds.setJdbcUrl("jdbc:sqlite:./target/" + JDBCPluginTest.class.getSimpleName() + ".db");
        cpds.setMinPoolSize(0);
        cpds.setAcquireIncrement(1);
        cpds.setMaxPoolSize(10);
        // Construct DataSource 
        InitialContextFactoryForTest.bind("java:/comp/env/jdbc/" + JDBCPluginTest.class.getSimpleName(), cpds);

        //create a table
        Connection con = cpds.getConnection();
        Statement createStatement = con.createStatement();
        createStatement.execute("""
                                CREATE TABLE Students (
                                        StudentID INTEGER PRIMARY KEY AUTOINCREMENT,
                                        FirstName TEXT NOT NULL,
                                        LastName TEXT NOT NULL,
                                        Age INTEGER,
                                        Major TEXT
                                    )""");
        //seed some records

        createStatement.execute("""
                                 INSERT INTO Students (FirstName, LastName, Age, Major) VALUES
                                    ('Alice', 'Smith', 20, 'Computer Science'),
                                    ('Bob', 'Johnson', 22, 'Electrical Engineering'),
                                    ('Charlie', 'Brown', 19, 'Mathematics'),
                                    ('Diana', 'Prince', 21, 'Physics')
                                """);
    }

    public JDBCPluginTest() {
    }

    @Test
    public void testExecute() throws Exception {

        //check the whole table
        String src = "[{"
                + JDBCPlugin.class.getCanonicalName()
                + " "
                + Props.PARAM_JNDI_SOURCE + "=" + JDBCPluginTest.class.getSimpleName()
                + " "
                + JDBCPlugin.PARAM_SQL + "=\"select * from Students\" "
                + "}]\n";

        testEngine.saveText(JDBCPluginTest.class.getSimpleName(), src);

        String res = testEngine.getI18nHTML(JDBCPluginTest.class.getSimpleName());
        Assertions.assertTrue(res.contains("Alice"), res);
        Assertions.assertTrue(res.contains("Bob"), res);
        Assertions.assertTrue(res.contains("Charlie"), res);
        Assertions.assertTrue(res.contains("Diana"), res);

        //check with a select where clause
        src = "[{"
                + JDBCPlugin.class.getCanonicalName()
                + " "
                + Props.PARAM_JNDI_SOURCE + "=" + JDBCPluginTest.class.getSimpleName()
                + " "
                + JDBCPlugin.PARAM_SQL + "=\"select * from Students where LastName='Smith'\" "
                + "}]\n";

        testEngine.saveText(JDBCPluginTest.class.getSimpleName(), src);

        res = testEngine.getI18nHTML(JDBCPluginTest.class.getSimpleName());
        Assertions.assertTrue(res.contains("Alice"), res);
        Assertions.assertFalse(res.contains("Bob"), res);
        Assertions.assertFalse(res.contains("Charlie"), res);
        Assertions.assertFalse(res.contains("Diana"), res);

    }

}
