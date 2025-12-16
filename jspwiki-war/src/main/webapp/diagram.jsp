<%@page import="org.apache.wiki.api.spi.Wiki"%>
<%@page import="org.apache.wiki.WikiContext"%>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ page import="org.apache.wiki.api.core.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.user.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ page import="jakarta.servlet.jsp.jstl.fmt.*" %>
<%
  Context c = Context.findContext(pageContext);
  Engine engine = Wiki.engine().find(this.getServletConfig());
  if (c==null) {
      c = Wiki.context().create(engine, request, "");
  }
  String url = engine.getWikiProperties().getProperty("drawio.url", "/draw/");
  if (!url.endsWith("/")) {
      url = url + "/";
  }
  
%>
<c:set var="csrfProtection" value="<%= c.getWikiSession().antiCsrfToken() %>" />

<html lang="en" id="top" xmlns="http://www.w3.org/1999/xhtml" xmlns:jspwiki="http://jspwiki.apache.org">

    <head>
        <title>
            <fmt:message key="view.title.view">
                <fmt:param><wiki:Variable var="ApplicationName" default="Apache JSPWiki" /></fmt:param>
                Diagram Editor
            </fmt:message>
        </title>
        <wiki:Include page="commonheader.jsp"/>
    </head>

    <body>
        <iframe id='drawio-editor-iframe' src="${url}index.html?embed=1&ui=atlas&spin=1&modified=unsavedChanges&proto=json&saveAndExit=1"
                width="100%" height="100%" ></iframe>
        <script type="text/javascript">
            //TODO add configuration checks to figure out where the drawio is being hosted from.
            const queryString = window.location.search;
            const urlParams = new URLSearchParams(queryString);
            const pageName = urlParams.get('pageName');
            if (pageName == null || pageName.trim().length == 0) {
                //need the page name...
                alert("no page name");
            }
            const token = "${csrfProtection}";
            const existingAttachmentName = urlParams.get('attachment');

            //
            //if loading an existing one...
            //const initialDiagramXML_JSON = '${org.apache.commons.lang3.StringEscapeUtils.escapeJson(diagramXML)}';

            // --- Configuration ---
            // 1. Get a reference to your iframe
            const iframe = document.getElementById('drawio-editor-iframe');
            // 2. Placeholder for the diagram XML content read from your JSPWiki file.
            //    This should be populated by your JSPWiki Plugin/Action.
            const initialDiagramXML = '';

            // --- Step 1: Listen for Messages from the IFrame ---
            window.addEventListener('message', receiveMessage, false);

            function receiveMessage(event) {
                // SECURITY CHECK: Always verify the origin of the message
                // Replace the URL below with the exact origin of your self-hosted draw.io instance
                //if (event.origin !== 'http://your-local-drawio-host:port') {
                // console.error('Message origin mismatch:', event.origin);
                //  return;
                //}

                // Parse the JSON data from the message
                const data = JSON.parse(event.data);

                console.log('Received Message from draw.io:', data.event);

                switch (data.event) {

                    // --- Step 2: The editor is ready. Send the load action. ---
                    case 'init':
                        // The editor sends 'init' when it's fully loaded and ready to receive commands.
                        if (existingAttachmentName == null || existingAttachmentName.length == 0) {
                            sendDrawioMessage({
                                action: 'load',
                                xml: initialDiagramXML // Pass the XML content here
                            });
                        } else {
                            //load the attachment, once loaded send the load message.
                            fetch('ajax/loadDrawioDiagram?pageName=' + encodeURIComponent(pageName) + 
                                    "&attachmentName=" + encodeURIComponent(existingAttachmentName)
                                    '&X-XSRF-TOKEN=' + encodeURIComponent(token), {method: 'GET'})
                                .then(response => {
                                    if (response.ok) {
                                        console.log('Diagram loaded successfully!');
                                        sendDrawioMessage({
                                            action: 'load',
                                            xml: response.text()
                                        });
                                    } else {
                                        console.error('load failed with status:', response.status);
                                        alert('Failed to load the diagram: ' + response.statusText);
                                    }
                                });

                        }
                        break;

                        // --- Step 3: User clicked Save/Exit. Request the final data. ---
                    case 'save':
                        // This is triggered when the user saves (often combined with exit=true).
                        // We request the final image data (SVG with XML embedded) for saving to JSPWiki.
                        sendDrawioMessage({
                            action: 'export',
                            format: 'svg', // Or 'png' if you prefer
                            embedImages: true, // Includes base64 images within the SVG/PNG
                            xml: true, // Ensures the editable XML source is embedded
                            base64: true         // Returns the data as a base64 string
                        });
                        break;

                        // --- Step 4: Final export data received. Persist to JSPWiki. ---
                    case 'export':
                        // This is the complete file content (base64 string) we need to save.
                        const newDiagramData = data.data; // This is the 'data:image/svg+xml;base64,...' string

                        if (newDiagramData) {
                            // Call your JSPWiki persistence function (e.g., via AJAX to your Custom Action/Servlet)
                            saveToJspWiki(newDiagramData);
                        }
                        break;

                        // --- Cleanup/Exit ---
                    case 'exit':
                        // User clicked the close/exit button without saving.
                        console.log('Draw.io editor exited.');
                        closeEditorWindow(); // Your function to close the editor view
                        break;

                        // Add other event handlers (e.g., 'autosave', 'error') as needed...
                }
            }

            // Helper function to send messages to the iframe
            function sendDrawioMessage(message) {
                // Convert the JS object to a JSON string
                const jsonMessage = JSON.stringify(message);

                // Send the message to the iframe
                // * and event.origin are security sensitive. event.origin is safer, 
                //   but '*' is often used in development.
                iframe.contentWindow.postMessage(jsonMessage, '*');
            }

            // --- Placeholder for your JSPWiki saving logic ---
            function saveToJspWiki(base64Data) {
                // 1. Extract the raw base64 string from the data URI (e.g., remove 'data:image/svg+xml;base64,')
                const rawBase64 = base64Data.split(',')[1];

                // 2. Prepare the data payload for your JSPWiki Action/Servlet
                const payload = {
                    pageName: pageName,
                    attachmentName: 'MyDiagram.svg',
                    fileContent: rawBase64
                };

                // 3. Use fetch/XMLHttpRequest to POST the payload to your JSPWiki save endpoint
                fetch('ajax/saveDrawioDiagram?X-XSRF-TOKEN=' + encodeURIComponent(token), {method: 'POST', body: JSON.stringify(payload)})
                        .then(response => {
                            if (response.ok) {
                                console.log('Diagram saved successfully!');
                                // After successful save, close the editor and reload
                                closeEditorWindow();
                            } else {
                                console.error('Save failed with status:', response.status);
                                alert('Failed to save diagram: ' + response.statusText);
                            }
                        });

                //alert('Diagram saved! Refreshing the page...');
                //closeEditorWindow();
            }

            function closeEditorWindow() {
                // Logic to close the editor view and return to the main wiki page
                window.location.href="./Wiki.jsp";
            }
        </script>
    </body>
</html>