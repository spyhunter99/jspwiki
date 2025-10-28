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

/**
 *
 */
public enum SQLType {
    MYSQL("com.mysql.jdbc.Driver", "jdbc:mysql:", "jdbc:mysql://hostname:portNumber/databaseName", "SELECT 1"),
    MSSQL("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:sqlserver:", "jdbc:sqlserver://serverName\\instanceName:portNumber", "SELECT 1"),
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql:", "jdbc:postgresql://hostname:portNumber/databaseName", "SELECT 1"),
    ORACLE("oracle.jdbc.driver.OracleDriver", "jdbc:oracle:", "jdbc:oracle:thin:@hostname:portNumber:databaseName", "SELECT 1"),
    DB2("COM.ibm.db2.jdbc.net.DB2Driver", "jdbc:db2:", "jdbc:db2:hostname:portNumber/databaseName", "SELECT 1 FROM SYSIBM.SYSDUMMY1"),
    SYBASE("com.sybase.jdbc.SybDriver", "jdbc:sybase:", "jdbc:sybase:Tds:hostname:portNumber/databaseName", "SELECT 1"),
    SQLITE("org.sqlite.JDBC", "jdbc:sqlite:", "jdbc:sqlite:my_database.sqlite", "SELECT 1"),
    DERBY_LOCAL("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:", "jdbc:derby:myDatabase;create=true", "VALUES 1"),
    DERBY_NETWORK("org.apache.derby.jdbc.ClientDriver", "jdbc:derby://", "jdbc:derby://hostname:portNumber/myDatabase", "VALUES 1");

    private String driverClass;
    private String validationQuery;

    public String getValidationQuery() {
        return validationQuery;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getStartsWith() {
        return startsWith;
    }

    public String getUrlDefaultPath() {
        return urlDefaultPath;
    }
    private String startsWith;
    private String urlDefaultPath;

    SQLType(String driverClass, String startsWith, String urlDefaultPath, String validationQuery) {
        this.driverClass = driverClass;
        this.startsWith = startsWith;
        this.urlDefaultPath = urlDefaultPath;
        this.validationQuery = validationQuery;
    }

    public static SQLType parse(String input) throws Exception {
        for (SQLType type : SQLType.values()) {
            if (type.name().equalsIgnoreCase(input) || type.driverClass.equalsIgnoreCase(input)) {
                return type;
            }
        }
        throw new Exception("Could not find SQLType of value: " + input);
    }
}
