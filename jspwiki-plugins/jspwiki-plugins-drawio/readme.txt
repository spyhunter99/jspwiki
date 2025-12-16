Drawio Integration

Install guide (self hosted draw.io, recommended)

copy diagram.jsp from this directory to JSPWIKI
copy the jspwiki-plugins-drawio-VERSION.jar to JSPWIKI/WEB-INF/lib
download the latest available drawio.war file from github at https://github.com/jgraph/drawio/releases
deploy draw.war to the same tomcat instance as JSPWIKI (could also be a separate one)
It should be something like ./webapps/draw
	if it's somewhere else, you can set the URL in the jspwiki properties file using the following
		drawio.url=/draw/
edit jspwiki's web.xml in JSPWIKI/WEB-INF/
	remove the COEPFilter filter and filter-mapping lines.
start up the server.
ensure the /draw or whatever you selected is accessible.
navigate to your jspwiki instance.

to insert a new diagram (or reference an existing one)
[{DrawioDiagram name=mydiagram.svg}]

Where {mydiagram.svg} is the attachment name.


Install guide (using the cloud hosted draw.io)

If you want to use the cloud hosted instance, follow the above guidance 
and instead use the following URL

	drawio.url=https://app.diagrams.net/
	
in your jspwiki properties file.

Unforutnately, app.diagrams.net sets a CSP for specific domains that are allowed
to iframe the website.. and your wiki is probably not on the list. If it fails to load, you might 
see an error message like this in the browser console log.
	Content-Security-Policy: The page’s settings blocked the loading of a resource (frame-ancestors) at <unknown> because it violates the following directive: “frame-ancestors 'self' https://teams.microsoft.com https://*.cloud.microsoft”