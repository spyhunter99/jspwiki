!!! JDBC Plugins

!! JDBC Page Storage Adapter 

This plugin enables data storage of wiki pages (not attachments)
via most JDBC (relational databases) connections. Some drivers are 
included already on the classpath, others you will to obtain manually.
This is due to licensing issues.

Included drivers
* Apache Derby Embedded and Network client
* Sqlite via xerial
* PostgreSQL 
* Maria

Usage: 

In your database server
- create a username/password for jspwiki
- create a database for jspwiki and assign permissions of the user account to connect to it and to create tables.

In your jspwiki-custom.properties

	jspwiki.pageProvider=org.apache.jspwiki.plugins.dbpages.JDBCPageProvider
	

Option A) use a container managed JDBC connection pool (for tomcat, see context.xml documentation)

	jdbc.existingDataSourceJNDILookup
	
Option B) JSPWiki will manage the connection pool internally, then the following are required

	jdbc.driver=
	jdbc.poolingEnabled=true
	jdbc.url=
	jdbc.user=username
	jdbc.password=pass
	jdbc.tablename=jspwiki
	jdbc.maxresults=50
	jdbc.versioning=true
	jdbc.c3p0.minpoolsize=1
	jdbc.c3p0.increment=1
	jdbc.c3p0.maxpoolsize=30


!! JDBC Query Plugin 

A user facing plugin that enables the wiki page to 
query a JDBC table, then format the results in the wiki page.

Usage:
{{{
[{org.apache.jspwiki.plugins.dbpages.JDBCPlugin (inputs here) }]
}}}

Available Inputs:

Required

* sql - the query to run. I.e. `select * from table table1`
* jdbc.maxresults - integer, maximum rows to retrieve. Default is 50

Option A) container managed connection pool (for tomcat, see context.xml)

* jdbc.existingDataSourceJNDILookup

Option B) JSPWiki will manage the connection

* jdbc.driver - fully qualified JDBC driver classname (required in all cases)
* jdbc.url - driver connection URL (required if not using JNDI lookups)
* jdbc.user - username if needed
* jdbc.password - password if needed 

Styling settings
* class - a CSS styling class name, default is sql-table
* header - true/false. If true, the column names are used as the tables header row.



!! Where to find JDBC Drivers

Included drivers
* Sqlite 
* Maria
* PostgreSQL
* Apache Derby 

Oracle Drivers: http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html

MySQL Drivers: http://dev.mysql.com/downloads/connector/j/

Microsoft JDBC Driver: http://www.microsoft.com/en-au/download/details.aspx?id=11774

MSSQL and Sybase jTDS Driver: http://jtds.sourceforge.net/

DB2 JDBC Driver: http://www-01.ibm.com/support/docview.wss?uid=swg21363866
