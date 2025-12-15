!!! How to include this plugin with your jspwiki deployment (using maven)

If you're building out your infrastructure, maven can help.

You probably have a project that includes the jspwiki war file as a dependency.

All that's needed to add a dependency for this library.

	<dependency>
		<groupId>org.apache.jspwiki</groupId>
		<artifactId>jspwiki-plugins-mathjax</artifactId>
		<version>LATEST</version>
	</dependency>

Then build as normal.

!!! How to include this plugin with your jspwiki deployment (without maven)

Download the jspwiki mathjax-plugin jar file.
Add it to ./webapps/jspwiki/WEB-INF/lib, replace "jspwiki" with your context path.
Restart the server.

!!! Removing the plugin from your jspwiki deployment.

Just delete the file from ./WEB-INF/lib/jspwiki-plugins-mathjax-VERSION.jar 
and restart the server.

!!! Upgrading

New version? just replace the old jar file with the new one, then restart the server.

!!! Usage

Edit a wiki page. Then include the mathjax plugin. Only one instance is required per wiki page.

	[{org.apache.jspwiki.plugins.mathjax.MathjaxPlugin}]

Then insert the chart anywhere you need it. Copy inbetween example-start and example-end.
Note: when copy and pasting, the plain text editor will auto wrap the text in {{{ and }}}. 
You'll need to remove those. In addition, the plugin reference may get an extra '['. Remove that too.

example-start

%%load-mathjax /%

Look ma, inline math \(P(E)   = {n \choose k} p^k (1-p)^{ n-k}\) \\
The quadratic formula is $-b \pm \sqrt{b^2 - 4ac} \over 2a$

example-end

Then save the page. Hopefully a chart will render. If it does not, check the browser console for potential clues.

!!! Maintenance notes

Currently, this is at Mathjax v2.7.0.
sourced Dec 2025 by pulling following the CDN links for Mathjax.

The webjar, at this point in time, does not appear to have the umd/dist bundle with it unfortunately.

Mathjax is ASF 2.0 licensed.

