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
public class Props {

    public static final String PROP_MAXRESULTS = "jdbc.maxresults";
    public static final Integer DEFAULT_MAXRESULTS = 500;
    public static final String DEFAULT_URL = "";
    public static final String PROP_PASSWORD = "jdbc.password";
    public static final String PROP_URL = "jdbc.url";
    public static final String DEFAULT_PASSWORD = "";
    public static final String PROP_DRIVER = "jdbc.driver";
    public static final String PROP_USER = "jdbc.user";
    public static final String PARAM_JNDI_SOURCE = "jdbc.existingDataSourceJNDILookup";

}
