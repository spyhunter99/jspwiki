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
package org.apache.jspwiki.plugins.dbpages;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.plugin.PluginManager;

/**
 * database backed page provider
 *
 */
public class JDBCPageProvider implements PageProvider {

    private final Logger LOG = Logger.getLogger(JDBCPageProvider.class);

    private void ensureDatabaseSchema() throws Exception {
        Connection con = null;
        Statement smt = null;
        try {
            con = getConnection();
            try {
                smt = con.createStatement();
                if (sqlType == SQLType.DERBY_LOCAL || sqlType == SQLType.DERBY_NETWORK) {

                    smt.executeUpdate("CREATE TABLE " + getTableName()
                            + "    ( " + COLUMN_ID + " INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                            + "    " + COLUMN_PAGENAME + " VARCHAR(255) NOT NULL,"
                            + "    " + COLUMN_VERSION + " INTEGER NOT NULL,"
                            + "    " + COLUMN_TEXT + " CLOB,"
                            + "    " + COLUMN_AUTHOR + " VARCHAR(255),"
                            + "    " + COLUMN_CHANGENOTE + " VARCHAR(512),"
                            + "    " + COLUMN_LASTMODIFIED + " BIGINT,"
                            + "    " + COLUMN_STATUS + " VARCHAR(255) )");
                } else {
                    smt.executeUpdate("CREATE TABLE " + getTableName()
                            + "    ( " + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "    " + COLUMN_PAGENAME + " VARCHAR(255) NOT NULL,"
                            + "    " + COLUMN_VERSION + " INTEGER NOT NULL,"
                            + "    " + COLUMN_TEXT + " CLOB,"
                            + "    " + COLUMN_AUTHOR + " VARCHAR(255),"
                            + "    " + COLUMN_CHANGENOTE + " VARCHAR(512),"
                            + "    " + COLUMN_LASTMODIFIED + " BIGINT,"
                            + "    " + COLUMN_STATUS + " VARCHAR(255) )");
                }
            } catch (Exception ex) {
                LOG.warn("error creating table " + getTableName(), ex);
            } finally {
                if (smt != null) {
                    smt.close();
                }
            }
            try {
                smt = con.createStatement();
                smt.executeUpdate("CREATE UNIQUE INDEX idx_pages_name_version ON " + getTableName() + " ("
                        + COLUMN_PAGENAME + ", " + COLUMN_VERSION + ")");
            } catch (Exception ex) {
                LOG.warn("error creating index " + getTableName(), ex);
            } finally {
                if (smt != null) {
                    smt.close();
                }
            }
        } catch (Exception ex) {
            LOG.warn("error getting connection to " + dbUrl, ex);
            throw ex;
        } finally {
            if (con != null) {
                con.close();
            }
        }

    }

    

    public enum PageStatus {
        ACTIVE("AC"), DELETED("DL");

        private String dbValue;

        PageStatus(String dbValue) {
            this.dbValue = dbValue;
        }
    }

    private static final SQLType DEFAULT_TYPE = SQLType.SQLITE;
    private static final String DEFAULT_URL = "";
    private static final String DEFAULT_USER = "";
    private static final String DEFAULT_PASSWORD = "";
    private static final Integer DEFAULT_MAXRESULTS = 500;
    private static final String DEFAULT_TABLENAME = "jspwiki";
    private static final Boolean DEFAULT_VERSIONING = true;
    private static final Boolean DEFAULT_C3P0 = false;
    private static final Integer DEFAULT_C3P0_MINPOOLSIZE = 5;
    private static final Integer DEFAULT_C3P0_INCREMENT = 5;
    private static final Integer DEFAULT_C3P0_MAXPOOLSIZE = 40;
    private static final String DEFAULT_SOURCE = null;

    public static final String PROP_DRIVER = "jdbc.driver";
    public static final String PROP_USE_POOLING = "jdbc.poolingEnabled";
    public static final String PROP_URL = "jdbc.url";
    public static final String PROP_USER = "jdbc.user";
    public static final String PROP_PASSWORD = "jdbc.password";
    public static final String PROP_TABLENAME = "jdbc.tablename";
    public static final String PROP_MAXRESULTS = "jdbc.maxresults";
    public static final String PROP_VERSIONING = "jdbc.versioning";
    public static final String PROP_C3P0 = "jdbc.c3p0";
    public static final String PROP_C3P0_MINPOOLSIZE = "jdbc.c3p0.minpoolsize";
    public static final String PROP_C3P0_INCREMENT = "jdbc.c3p0.increment";
    public static final String PROP_C3P0_MAXPOOLSIZE = "jdbc.c3p0.maxpoolsize";

    public static final String PARAM_JNDI_SOURCE = "existingDataSourceJNDILookup";

    private static final String COLUMN_ID = "wikiid";
    private static final String COLUMN_PAGENAME = "wikiname";
    private static final String COLUMN_VERSION = "wikiversion";
    private static final String COLUMN_TEXT = "wikitext";
    private static final String COLUMN_AUTHOR = "wikiauthor";
    private static final String COLUMN_CHANGENOTE = "wikichangenote";
    private static final String COLUMN_LASTMODIFIED = "wikilastmodified";
    private static final String COLUMN_STATUS = "wikistatus";

    private ComboPooledDataSource cpds = null;
    private SQLType sqlType = DEFAULT_TYPE;
    private String dbUrl = DEFAULT_URL;
    private String dbUser = DEFAULT_USER;
    private String dbPassword = DEFAULT_PASSWORD;
    private Integer maxResults = DEFAULT_MAXRESULTS;
    private String tableName = DEFAULT_TABLENAME;
    private Boolean isVersioned = DEFAULT_VERSIONING;
    private Boolean c3p0 = DEFAULT_C3P0;
    private Integer c3p0MinPoolSize = DEFAULT_C3P0_MINPOOLSIZE;
    private Integer c3p0Increment = DEFAULT_C3P0_INCREMENT;
    private Integer c3p0MaxPoolSize = DEFAULT_C3P0_MAXPOOLSIZE;
    private String jndiJdbcSource = DEFAULT_SOURCE;
    private DataSource ds = null;
    private Engine wikiEngine = null;

    @Override
    public void initialize(Engine wikiEngine, Properties properties) throws NoRequiredPropertyException, IOException {
        setLogForDebug(properties.getProperty(PluginManager.PARAM_DEBUG));
        LOG.info("STARTED");
        this.wikiEngine = wikiEngine;

        // Validate all parameters
        validateParams(properties);

        String sql = sqlType.getValidationQuery();
        ResultSet rs = null;
        Connection con = null;
        PreparedStatement cmd = null;
        try {
            if (c3p0) {
                initialiseConnectionPool();
            }
            ensureDatabaseSchema();
            con = getConnection();
            cmd = con.prepareStatement(sql);
            rs = cmd.executeQuery();
            if (rs.next()) {
                LOG.info("Successfully initialised JDBCPageProvider");
            }
        } catch (Exception e) {
            LOG.error("ERROR. " + e.getMessage() + ". sql=" + sql, e);
            throw new IOException(e.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    LOG.debug(ex.getMessage());
                }
            }
            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException ex) {
                    LOG.debug(ex.getMessage());
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex) {
                    LOG.debug(ex.getMessage());
                }
            }
        }
    }

    protected void validateParams(Properties props) throws NoRequiredPropertyException {
        String paramName;
        String param;

        LOG.info("validateParams() START");
        paramName = PARAM_JNDI_SOURCE;
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new NoRequiredPropertyException(paramName + " parameter is not a valid value", PARAM_JNDI_SOURCE);
            }
            jndiJdbcSource = param;
            try {
                Context ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/" + jndiJdbcSource);
                if (ds == null) {
                    ds = (DataSource) ctx.lookup("java:" + jndiJdbcSource);
                }
                if (ds == null) {
                    LOG.error("Neither jspwiki-custom.properties or conf/context.xml has not been configured for " + jndiJdbcSource + "!");
                    throw new NoRequiredPropertyException("Neither jspwiki-custom.properties or conf/context.xml has not been configured for " + jndiJdbcSource + "!", PARAM_JNDI_SOURCE);
                }
                Connection con = null;
                String driverStr = null;
                try {
                    con = ds.getConnection();
                    driverStr = con.getMetaData().getDriverName();
                    String jdbcUrl = con.getMetaData().getURL();
                    Driver driver = DriverManager.getDriver(jdbcUrl);
                    sqlType = SQLType.parse(driver.getClass().getCanonicalName());
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value. " + e.getMessage() + ":" + driverStr, PROP_DRIVER);
                } finally {
                    if (con != null) {
                        try {
                            con.close();
                        } catch (SQLException ex) {

                        }
                    }
                }
            } catch (NamingException e) {
                LOG.error("Neither jspwiki-custom.properties or conf/context.xml has not been configured for " + jndiJdbcSource + "!");
                throw new NoRequiredPropertyException("Neither jspwiki-custom.properties or conf/context.xml has not been configured for " + jndiJdbcSource + "!", PARAM_JNDI_SOURCE);
            }
        } else {
            jndiJdbcSource = null;
        }
        paramName = (PROP_DRIVER);
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            try {
                sqlType = SQLType.parse(param);
            } catch (Exception e) {
                throw new NoRequiredPropertyException(paramName + " property is not a valid value. " + param, PROP_DRIVER);
            }
            try {
                Class.forName(param).newInstance();
            } catch (ClassNotFoundException e) {
                LOG.error("Error: unable to load driver class " + param + "!", e);
                throw new NoRequiredPropertyException("Error: unable to load driver class " + param + "!", PROP_DRIVER);
            } catch (IllegalAccessException e) {
                LOG.error("Error: access problem while loading " + param + "!", e);
                throw new NoRequiredPropertyException("Error: access problem while loading " + param + "!", PROP_DRIVER);
            } catch (InstantiationException e) {
                LOG.error("Error: unable to instantiate driver " + param + "!", e);
                throw new NoRequiredPropertyException("Error: unable to instantiate driver " + param + "!", PROP_DRIVER);
            } catch (Exception e) {
                LOG.error("Error: unable to load driver " + param + "!", e);
                throw new NoRequiredPropertyException("Error: unable to load driver " + param + "! " + e.getMessage(), PROP_DRIVER);
            }
        }

        if (ds == null) {

            param = props.getProperty(PROP_URL);
            if (StringUtils.isNotBlank(param)) {
                LOG.info(paramName + "=" + param);
                if (!StringUtils.isAsciiPrintable(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value", PROP_URL);
                }
                if (!param.trim().startsWith(sqlType.getStartsWith())) {
                    throw new NoRequiredPropertyException("Error: " + paramName + " property has value " + param + ". "
                            + "Expected: " + sqlType.getUrlDefaultPath(), PROP_URL);
                }
                dbUrl = param;
            } else {
                throw new NoRequiredPropertyException(paramName + " property is not a valid value", PROP_URL);
            }
            param = props.getProperty(PROP_USER);
            if (StringUtils.isNotBlank(param)) {
                LOG.info(paramName + "=" + param);
                if (!StringUtils.isAsciiPrintable(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value", PROP_USER);
                }
                dbUser = param;
            }
            param = props.getProperty(PROP_PASSWORD);
            if (StringUtils.isNotBlank(param)) {
                LOG.info(paramName + "=" + param);
                if (!StringUtils.isAsciiPrintable(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value", PROP_PASSWORD);
                }
                dbPassword = param;
            }
        }
        param = props.getProperty(PROP_TABLENAME, DEFAULT_TABLENAME);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new NoRequiredPropertyException(paramName + " property is not a valid value", PROP_TABLENAME);
            }
            tableName = param;
        }

        param = props.getProperty(PROP_MAXRESULTS, DEFAULT_MAXRESULTS + "");
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new NoRequiredPropertyException(paramName + " property is not a valid value", PROP_MAXRESULTS);
            }
            maxResults = Integer.parseInt(param);
        }
        param = props.getProperty(PROP_VERSIONING);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            try {
                Boolean paramValue = "true".equalsIgnoreCase(param);
                isVersioned = paramValue;
            } catch (Exception e) {
                throw new NoRequiredPropertyException(paramName + " parameter is not true or false", PROP_MAXRESULTS);
            }
        }

        param = props.getProperty(PROP_USE_POOLING);
        if ("true".equalsIgnoreCase(param)) {
            c3p0 = true;
            paramName = props.getProperty(PROP_C3P0_MINPOOLSIZE, DEFAULT_C3P0_MINPOOLSIZE + "");
            param = props.getProperty(paramName);
            if (StringUtils.isNotBlank(param)) {
                LOG.info(paramName + "=" + param);
                if (!StringUtils.isNumeric(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value", PROP_C3P0_MINPOOLSIZE);
                }
                c3p0MinPoolSize = Integer.parseInt(param);
            }

            param = props.getProperty(PROP_C3P0_INCREMENT, DEFAULT_C3P0_INCREMENT + "");
            if (StringUtils.isNotBlank(param)) {
                LOG.info(paramName + "=" + param);
                if (!StringUtils.isNumeric(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value", PROP_C3P0_INCREMENT);
                }
                c3p0Increment = Integer.parseInt(param);
            }

            param = props.getProperty(PROP_C3P0_MAXPOOLSIZE, DEFAULT_C3P0_MAXPOOLSIZE + "");
            if (StringUtils.isNotBlank(param)) {
                LOG.info(paramName + "=" + param);
                if (!StringUtils.isNumeric(param)) {
                    throw new NoRequiredPropertyException(paramName + " property is not a valid value", PROP_C3P0_MAXPOOLSIZE);
                }
                c3p0MaxPoolSize = Integer.parseInt(param);
            }
        }
    }

    protected void initialiseConnectionPool() throws SQLException {
        cpds = new ComboPooledDataSource();
        try {
            cpds.setDriverClass(sqlType.getDriverClass());
        } catch (Exception e) {
            throw new SQLException(e);
        }
        cpds.setJdbcUrl(dbUrl);
        cpds.setUser(dbUser);
        cpds.setPassword(dbPassword);

        cpds.setMinPoolSize(c3p0MinPoolSize);
        cpds.setAcquireIncrement(c3p0Increment);
        cpds.setMaxPoolSize(c3p0MaxPoolSize);
    }

    private String addLimits(SQLType sqlType, String sql, Integer maxResults) {
        String result = sql;
        if (StringUtils.isNotBlank(sql)) {
            result = sql.trim() + " ";

            switch (sqlType) {
                case MSSQL:
                    if (!result.toLowerCase().contains(" top")) {
                        result = sql.replace("select", "select top " + maxResults);

                    }
                    break;
                case MYSQL:
                    if (!result.toLowerCase().contains(" limit ")) {
                        result = result + " limit " + maxResults;
                    }
                    break;
                case ORACLE:
                    if (!result.toLowerCase().contains("rownum")) {
                        result = "select * from ( " + result + " ) where ROWNUM <= " + maxResults;
                    }
                    break;
                case SQLITE:
                case POSTGRESQL:
                    if (!result.toLowerCase().contains(" limit ")) {
                        result = result + " limit " + maxResults;
                    }
                    break;
                case DERBY_LOCAL:
                case DERBY_NETWORK:
                case DB2:
                    if (!result.toLowerCase().contains(" fetch")) {
                        result = result + " FETCH FIRST " + maxResults + " ROWS ONLY";
                    }
                    break;
                case SYBASE:
                    if (!result.toLowerCase().contains(" top")) {
                        result = result.replace("select", "select top " + maxResults);

                    }
                    break;
            }
        }
        return result;
    }

    private String getPropKey(String currentKey, String source) {
        String result = currentKey;
        if (source != null && StringUtils.isNotBlank(source)) {
            result += "." + source;
        }
        return result;
    }

    private void setLogForDebug(String value) {
        if (StringUtils.isNotBlank(value) && (value.equalsIgnoreCase("true") || value.equals("1"))) {
            LOG.setLevel(Level.INFO);
        }
    }

    private Connection getConnection() throws SQLException {
        Connection conn = null;
        if (ds != null) {
            conn = ds.getConnection();
        } else if (c3p0 && cpds != null) {
            conn = cpds.getConnection();
        } else {
            if (StringUtils.isBlank(dbUser) && StringUtils.isBlank(dbPassword)) {
                conn = DriverManager.getConnection(dbUrl);
            } else {
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            }
            if (conn == null) {
                throw new SQLException("Could not create connection for url=" + dbUrl + " user=" + dbUser);
            }
        }
        return conn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderInfo() {
        return "JDBCPageProvider";
    }

    protected int findLatestVersion(String page) {
        int version = -1;
        if (!isVersioned) {
            return version;
        }
        Connection conn = null;
        PreparedStatement cmd = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "select * from " + getTableName() + " where " + COLUMN_PAGENAME + " = ? and "
                    + COLUMN_STATUS + " != ? order by " + COLUMN_VERSION + " DESC";
            String[] args = new String[]{page, PageStatus.DELETED.dbValue};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }
            rs = cmd.executeQuery();
            if (rs.next()) {
                version = rs.getInt(COLUMN_VERSION);
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }

        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putPageText(Page page, String text) throws ProviderException {
        Connection conn = null;
        PreparedStatement cmd = null;
        String sql="TBD";
        try {
            conn = getConnection();
            String changenote = "new page";
            if (page.getAttribute(WikiPage.CHANGENOTE) != null) {
                changenote = page.getAttribute(WikiPage.CHANGENOTE).toString();
            }
            if (isVersioned) {
                int latest = findLatestVersion(page.getName());
                if (pageExists(page.getName(), latest)) {
                    latest++;
                }
                sql = "insert into " + getTableName() + " ("
                        + COLUMN_PAGENAME + ","
                        + COLUMN_VERSION + ","
                        + COLUMN_TEXT + ","
                        + COLUMN_AUTHOR + ","
                        + COLUMN_CHANGENOTE + ","
                        + COLUMN_STATUS + ","
                        + COLUMN_LASTMODIFIED + ")"
                        + " values (?,?,?,?,?,?,?)";
                String[] args = new String[]{
                    page.getName(),
                    String.valueOf(latest),
                    text,
                    page.getAuthor(),
                    changenote,
                    PageStatus.ACTIVE.dbValue,
                    System.currentTimeMillis() + ""};
                cmd = conn.prepareStatement(sql);
                for (int i = 0; i < args.length; i++) {
                    cmd.setString(i + 1, args[i]);
                }
                int result = cmd.executeUpdate();
            } else {
                int latest = -1;
                if (pageExists(page.getName(), latest)) {
                    sql = "update " + getTableName() + " set "
                            + COLUMN_TEXT + "=?, "
                            + COLUMN_AUTHOR + "=?, "
                            + COLUMN_CHANGENOTE + "=?, "
                            + COLUMN_STATUS + "=?, "
                            + COLUMN_LASTMODIFIED + "=? "
                            + " where "
                            + COLUMN_PAGENAME + "=? AND "
                            + COLUMN_VERSION + "=?";
                    String[] args = new String[]{
                        text,
                        page.getAuthor(),
                        changenote,
                        PageStatus.ACTIVE.dbValue,
                        System.currentTimeMillis() + "",
                        page.getName(),
                        "-1"};
                    cmd = conn.prepareStatement(sql);
                    for (int i = 0; i < args.length; i++) {
                        cmd.setString(i + 1, args[i]);
                    }
                    int result = cmd.executeUpdate();
                } else {
                    sql = "insert into " + getTableName() + " ("
                            + COLUMN_PAGENAME + ","
                            + COLUMN_VERSION + ","
                            + COLUMN_TEXT + ","
                            + COLUMN_AUTHOR + ","
                            + COLUMN_CHANGENOTE + ","
                            + COLUMN_STATUS + ","
                            + COLUMN_LASTMODIFIED
                            + ") "
                            + " values (?,?,?,?,?,?,?)";
                    String[] args = new String[]{
                        page.getName(),
                        String.valueOf(latest),
                        text,
                        page.getAuthor(),
                        changenote,
                        PageStatus.ACTIVE.dbValue,
                        System.currentTimeMillis() + ""};
                    cmd = conn.prepareStatement(sql);
                    for (int i = 0; i < args.length; i++) {
                        cmd.setString(i + 1, args[i]);
                    }
                    int result = cmd.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage() + " " + sql);
            throw new ProviderException(e.getMessage());
        } finally {

            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean pageExists(String page) {
        int version = findLatestVersion(page);
        return pageExists(page, version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean pageExists(String page, int version) {
        Connection conn = null;
        PreparedStatement cmd = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "select count(1) from " + getTableName()
                    + " where "
                    + COLUMN_PAGENAME + " = ? and "
                    + COLUMN_VERSION + " = ? and "
                    + COLUMN_STATUS + " != ? ";
            String[] args = new String[]{
                page,
                String.valueOf(version),
                PageStatus.DELETED.dbValue};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }
            rs = cmd.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) >= 1) {
                    return true;
                }
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WikiPage getPageInfo(String page, int version) throws ProviderException {
        Connection conn = null;
        PreparedStatement cmd = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "select * from " + getTableName() + " where "
                    + COLUMN_PAGENAME + " = ? and "
                    + COLUMN_VERSION + " = ? and "
                    + COLUMN_STATUS + " != ? ";
            String[] args = new String[]{page, String.valueOf(version), PageStatus.DELETED.dbValue};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }
            rs = cmd.executeQuery();
            if (rs.next()) {
                WikiPage wikiPage = new WikiPage(wikiEngine, rs.getString(COLUMN_PAGENAME));
                wikiPage.setAuthor(rs.getString(COLUMN_AUTHOR));
                wikiPage.setVersion(version);
                wikiPage.setSize(rs.getString(COLUMN_TEXT).length());
                wikiPage.setAttribute(WikiPage.CHANGENOTE, rs.getString(COLUMN_CHANGENOTE));
                wikiPage.setLastModified(new Date(rs.getLong(COLUMN_LASTMODIFIED)));

                return wikiPage;
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<SearchResult> findPages(org.apache.wiki.api.search.QueryItem[] query) {
        List<SearchResult> pages = new ArrayList<SearchResult>();
        Connection conn = null;
        PreparedStatement cmd = null;
        ResultSet rs = null;

        try {
            conn = getConnection();

            if (query.length > 0 && StringUtils.isNotBlank(query[0].word)) {
                String sql = "select distinct " + COLUMN_PAGENAME + " from " + getTableName()
                        + " where "
                        + COLUMN_TEXT + " like ? and "
                        + COLUMN_STATUS + " != ?";
                sql = addLimits(sqlType, sql, maxResults);
                String[] args = new String[]{"%" + query[0].word + "%", PageStatus.DELETED.dbValue};
                cmd = conn.prepareStatement(sql);
                for (int i = 0; i < args.length; i++) {
                    cmd.setString(i + 1, args[i]);
                }
                rs = cmd.executeQuery();
                while (rs.next()) {
                    String pageName = rs.getString(COLUMN_PAGENAME);
                    WikiPage wikiPage = getPageInfo(pageName, findLatestVersion(pageName));
                    SearchResult result = new SearchResult() {
                        @Override
                        public Page getPage() {
                            return wikiPage;
                        }

                        @Override
                        public int getScore() {
                            return 1;
                        }

                        @Override
                        public String[] getContexts() {
                            return new String[0];
                        }
                    };
                    pages.add(result);
                }
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }

        return pages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection getAllPages() throws ProviderException {
        List<WikiPage> pages = new ArrayList<WikiPage>();
        Connection conn = null;
        PreparedStatement cmd = null;
        ResultSet rs = null;
        String sql = "select distinct " + COLUMN_PAGENAME + " from " + getTableName()
                + " where " + COLUMN_STATUS + " != ?";
        try {
            conn = getConnection();

            sql = addLimits(sqlType, sql, maxResults);
            String[] args = new String[]{PageStatus.DELETED.dbValue};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }
            rs = cmd.executeQuery();
            while (rs.next()) {
                String pageName = rs.getString(COLUMN_PAGENAME);
                WikiPage wikiPage = getPageInfo(pageName, findLatestVersion(pageName));
                pages.add(wikiPage);
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage() + " " + sql, ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }

        return pages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection getAllChangedSince(Date date) {
        List<WikiPage> pages = new ArrayList<WikiPage>();
        Connection conn = null;
        PreparedStatement cmd = null;
        ResultSet rs = null;
        String sql = "select distinct " + COLUMN_PAGENAME + " from " + getTableName()
                + " where " + COLUMN_LASTMODIFIED + " >= ? and " + COLUMN_STATUS + " != ?";
        sql = addLimits(sqlType, sql, maxResults);
        try {
            conn = getConnection();

            String[] args = new String[]{date.getTime() + "", PageStatus.DELETED.dbValue};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }

            rs = cmd.executeQuery();
            while (rs.next()) {
                String pageName = rs.getString(COLUMN_PAGENAME);
                WikiPage wikiPage = getPageInfo(pageName, findLatestVersion(pageName));
                pages.add(wikiPage);
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }

        return pages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPageCount() throws ProviderException {
        int result = 0;

        Connection conn = null;
        PreparedStatement cmd = null;
        ResultSet rs = null;
        try {
            conn = getConnection();

            String sql = "select count(distinct " + COLUMN_PAGENAME + ") from " + getTableName()
                    + " where " + COLUMN_STATUS + " != ?";
            LOG.debug("executeQuery() sql=" + sql);
            String[] args = new String[]{PageStatus.DELETED.dbValue};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }

            rs = cmd.executeQuery();
            while (rs.next()) {
                result = rs.getInt(1);
            }

        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List getVersionHistory(String page) throws ProviderException {
        List<WikiPage> versionHistory = new ArrayList<WikiPage>();
        try {
            int version = findLatestVersion(page);
            while (version >= 0) {
                WikiPage wikiPage = getPageInfo(page, version);
                versionHistory.add(wikiPage);
                version--;
            }
            if (version == -1) {
                WikiPage wikiPage = getPageInfo(page, version);
                versionHistory.add(wikiPage);
            }
        } catch (Exception e) {
            LOG.error(e, e);
        }
        return versionHistory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPageText(String page, int version) throws ProviderException {
        Connection conn = null;
        PreparedStatement cmd = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "select " + COLUMN_TEXT + " from " + getTableName() + " where "
                    + COLUMN_PAGENAME + " = ? and "
                    + COLUMN_VERSION + " = ? and " + COLUMN_STATUS + " != ? ";
            String[] args = new String[]{page, String.valueOf(version), PageStatus.DELETED.dbValue};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }
            rs = cmd.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteVersion(String pageName, int version) throws ProviderException {
        Connection conn = null;
        PreparedStatement cmd = null;
        try {
            conn = getConnection();

            String sql = "update " + getTableName() + " set " + COLUMN_STATUS + " = ? where "
                    + COLUMN_PAGENAME + " = ? and "
                    + COLUMN_VERSION + " = ?";
            String[] args = new String[]{PageStatus.DELETED.dbValue, pageName, String.valueOf(version)};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }

            int result = cmd.executeUpdate();
        } catch (Exception e) {
            throw new ProviderException(e.getMessage());
        } finally {

            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deletePage(String pageName) throws ProviderException {
        Connection conn = null;
        PreparedStatement cmd = null;
        try {
            conn = getConnection();

            String sql = "update " + getTableName() + " set " + COLUMN_STATUS + " = ? where "
                    + COLUMN_PAGENAME + " = ?";
            String[] args = new String[]{PageStatus.DELETED.dbValue, pageName};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }

            int result = cmd.executeUpdate();
        } catch (Exception e) {
            throw new ProviderException(e.getMessage());
        } finally {

            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void movePage(String from, String to) throws ProviderException {
        if (pageExists(to)) {
            throw new ProviderException("The destination page " + to + " already exists");
        }
        Connection conn = null;
        PreparedStatement cmd = null;
        try {
            conn = getConnection();
            String sql = "update " + getTableName() + " set "
                    + COLUMN_PAGENAME + " = ? where "
                    + COLUMN_PAGENAME + " = ?";
            String[] args = new String[]{to, from};
            cmd = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                cmd.setString(i + 1, args[i]);
            }

            int result = cmd.executeUpdate();
        } catch (Exception e) {
            throw new ProviderException(e.getMessage());
        } finally {

            if (cmd != null) {
                try {
                    cmd.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOG.debug(e, e);
                }
            }
        }
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
