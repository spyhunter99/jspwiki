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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wiki.api.exceptions.PluginException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import static org.apache.jspwiki.plugins.dbpages.Props.PROP_DRIVER;
import static org.apache.jspwiki.plugins.dbpages.Props.PROP_MAXRESULTS;
import static org.apache.jspwiki.plugins.dbpages.Props.PROP_PASSWORD;
import static org.apache.jspwiki.plugins.dbpages.Props.PROP_URL;
import static org.apache.jspwiki.plugins.dbpages.Props.PROP_USER;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.render.RenderingManager;

/**
 * queries a database, then formats the results into a wiki table
 *
 */
public class JDBCPlugin implements Plugin {

    private final Logger LOG = Logger.getLogger(JDBCPlugin.class);

    public static final SQLType DEFAULT_TYPE = SQLType.MYSQL;
    public static final String DEFAULT_URL = "";
    public static final String DEFAULT_USER = "";
    public static final String DEFAULT_PASSWORD = "";
    public static final Integer DEFAULT_MAXRESULTS = 50;
    public static final String DEFAULT_CLASS = "sql-table";
    public static final String DEFAULT_SQL = "select 1";
    public static final Boolean DEFAULT_HEADER = true;
    public static final String DEFAULT_SOURCE = null;

    private static final String PARAM_CLASS = "class";
    private static final String PARAM_SQL = "sql";
    private static final String PARAM_HEADER = "header";

    private SQLType sqlType = DEFAULT_TYPE;
    private String dbUrl = DEFAULT_URL;
    private String dbUser = DEFAULT_USER;
    private String dbPassword = DEFAULT_PASSWORD;
    private Integer maxResults = DEFAULT_MAXRESULTS;
    private String className = DEFAULT_CLASS;
    private String sql = DEFAULT_SQL;
    private Boolean header = DEFAULT_HEADER;
    private String jndiJdbcSource = DEFAULT_SOURCE;
    private DataSource ds = null;

    @Override
    public String execute(org.apache.wiki.api.core.Context wikiContext, Map<String, String> params) throws PluginException {
        setLogForDebug(params.get(PluginManager.PARAM_DEBUG));
        LOG.info("STARTED");
        String result = "";
        StringBuffer buffer = new StringBuffer();
        Engine engine = wikiContext.getEngine();
        Properties props = engine.getWikiProperties();

        // Validate all parameters
        validateParams(props, params);

        Connection conn = null;
        try {

            if (ds == null) {
                if (StringUtils.isBlank(dbUser) && StringUtils.isBlank(dbPassword)) {
                    conn = DriverManager.getConnection(dbUrl);
                } else {
                    conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                }
                if (conn == null) {
                    throw new Exception("Could not create connection for url=" + dbUrl + " user=" + dbUser);
                }
            } else {
                conn = ds.getConnection();
            }

            sql = addLimits(sqlType, sql, maxResults);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            ResultSetMetaData md = rs.getMetaData();
            if (header) {
                for (int i = 0; i < md.getColumnCount(); i++) {
                    String header = md.getColumnLabel(i + 1);
                    buffer.append("|| " + header);
                }
                buffer.append("\n");
            }

            while (rs.next()) {
                for (int i = 0; i < md.getColumnCount(); i++) {
                    String value = rs.getString(i + 1);
                    buffer.append("| " + value);
                }
                buffer.append("\n");
            }

            LOG.info("result=" + buffer.toString());
            result = engine.getManager(RenderingManager.class).textToHTML(wikiContext, buffer.toString());

            result = "<div class='" + className + "'>" + result + "</div>";
        } catch (Exception e) {
            LOG.error("ERROR. " + e.getMessage() + ". sql=" + sql, e);
            throw new PluginException(e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }

        return result;
    }

    protected void validateParams(Properties props, Map<String, String> params) throws PluginException {
        String paramName;
        String param;

        LOG.info("validateParams() START");
        paramName = Props.PARAM_JNDI_SOURCE;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            jndiJdbcSource = param;
            try {
                Context ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:/comp/env/jdbc/" + jndiJdbcSource);
                if (ds == null) {
                    ds = (DataSource) ctx.lookup("java:" + jndiJdbcSource);
                }
                if (ds == null) {
                    throw new PluginException("Could not load jndi data source " + jndiJdbcSource);
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
                    throw new PluginException(paramName + " property is not a valid value. " + e.getMessage() + ":" + driverStr);
                } finally {
                    if (con != null) {
                        try {
                            con.close();
                        } catch (SQLException ex) {

                        }
                    }
                }
            } catch (NamingException e) {
                LOG.error(e.getMessage(), e);
                throw new PluginException("JNDI lookup failed for " + jndiJdbcSource + "!");
            }
        }
        paramName = PROP_DRIVER;
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            try {
                sqlType = SQLType.parse(param);
            } catch (Exception e) {
                throw new PluginException(paramName + " property is not a valid value. " + param);
            }
            try {
                Class.forName(param).newInstance();
            } catch (ClassNotFoundException e) {
                LOG.error("Error: unable to load driver class " + param + "!", e);
                throw new PluginException("Error: unable to load driver class " + param + "!");
            } catch (IllegalAccessException e) {
                LOG.error("Error: access problem while loading " + param + "!", e);
                throw new PluginException("Error: access problem while loading " + param + "!");
            } catch (InstantiationException e) {
                LOG.error("Error: unable to instantiate driver " + param + "!", e);
                throw new PluginException("Error: unable to instantiate driver " + param + "!");
            } catch (Exception e) {
                LOG.error("Error: unable to load driver " + param + "!", e);
                throw new PluginException("Error: unable to load driver " + param + "! " + e.getMessage());
            }
        }
        if (ds == null) {
            paramName = PROP_URL;
            param = props.getProperty(paramName);
            if (StringUtils.isNotBlank(param)) {
                LOG.info(paramName + "=" + param);
                if (!StringUtils.isAsciiPrintable(param)) {
                    throw new PluginException(paramName + " property is not a valid value");
                }
                if (!param.trim().startsWith(sqlType.getStartsWith())) {
                    throw new PluginException("Error: " + paramName + " property has value " + param + ". "
                            + "Expected: " + sqlType.getUrlDefaultPath());
                }
                dbUrl = param;
            }
            param = props.getProperty(PROP_USER);
            if (StringUtils.isNotBlank(param)) {
                LOG.info(paramName + "=" + param);
                if (!StringUtils.isAsciiPrintable(param)) {
                    throw new PluginException(paramName + " property is not a valid value");
                }
                dbUser = param;
            }
            param = props.getProperty(PROP_PASSWORD);
            if (StringUtils.isNotBlank(param)) {
                LOG.info(paramName + "=" + param);
                if (!StringUtils.isAsciiPrintable(param)) {
                    throw new PluginException(paramName + " property is not a valid value");
                }
                dbPassword = param;
            }
        }
        paramName = PROP_MAXRESULTS;
        param = props.getProperty(paramName);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            if (!StringUtils.isNumeric(param)) {
                throw new PluginException(paramName + " property is not a valid value");
            }
            maxResults = Integer.parseInt(param);
        }
        paramName = PARAM_CLASS;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            className = param;
        }
        paramName = PARAM_SQL;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            if (!StringUtils.isAsciiPrintable(param)) {
                throw new PluginException(paramName + " parameter is not a valid value");
            }
            if (!sql.toLowerCase().startsWith("select")) {
                throw new PluginException(paramName + " parameter needs to start with 'SELECT'.");
            }
            sql = param;
        } else {
            throw new PluginException(paramName + " parameter was not defined");
        }
        paramName = PARAM_HEADER;
        param = params.get(paramName);
        if (StringUtils.isNotBlank(param)) {
            LOG.info(paramName + "=" + param);
            if (!param.equalsIgnoreCase("true") && !param.equalsIgnoreCase("false")
                    && !param.equals("0") && !param.equals("1")) {
                throw new PluginException(paramName + " parameter is not a valid boolean");
            }
            header = "true".equalsIgnoreCase(param);
        }
    }

    private String addLimits(SQLType sqlType, String sql, Integer maxResults) {
        String result = sql;
        if (StringUtils.isNotBlank(sql)) {
            result = sql.trim();
            if (result.endsWith(";")) {
                result = result.substring(result.length() - 1);
            }
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
                case POSTGRESQL:
                    if (!result.toLowerCase().contains(" limit ")) {
                        result = result + " limit " + maxResults;
                    }
                    break;
                case DB2:
                    if (!result.toLowerCase().contains(" fetch")) {
                        result = result + " FETCH FIRST " + maxResults + " ROWS ONLY";
                    }
                    break;
                case SYBASE:
                    if (!result.toLowerCase().contains(" top")) {
                        result = result.replace("select", "select top " + maxResults);
                        result += ";";
                    }
                    break;
            }
        }
        return result;
    }

    private String getPropKey(String currentKey, String source) {
        String result = currentKey;
        if (StringUtils.isNotBlank(source)) {
            result += "." + source;
        }
        return result;
    }

    private void setLogForDebug(String value) {
        if (StringUtils.isNotBlank(value) && (value.equalsIgnoreCase("true") || value.equals("1"))) {
            LOG.setLevel(Level.INFO);
        }
    }
}
