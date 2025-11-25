<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="org.apache.wiki.attachment.AttachmentManager"%>
<%@ page import="org.apache.wiki.api.core.*, 
         org.apache.wiki.WikiEngine, 
         org.apache.wiki.WikiContext, 
         org.apache.wiki.pages.PageManager,
         java.io.*" %>
<%
    request.setCharacterEncoding("UTF-8");

    Engine engine = WikiEngine.getInstance(config);
    String pageName = request.getParameter("page");
    String id = request.getParameter("id");
    String fileName = id + ".drawio";

    Page wikipage = engine.getManager(PageManager.class).getPage(pageName);
    Context wikiContext = new WikiContext(engine, wikipage);
    
    AttachmentManager am = engine.getManager(AttachmentManager.class);
    String xml = "<mxGraphModel><root></root></mxGraphModel>"; // default
    String loadUrl="";
    try {
        //am.getAttachmentStream(wikiContext, att)
        Attachment att = am.getAttachmentInfo(wikiContext, pageName + "/" + fileName);
        if (att != null) {
            loadUrl = wikiContext.getURL(WikiContext.ATTACH, att.getName());
            xml = IOUtils.toString(am.getAttachmentStream(att), "UTF-8");
        }
    } catch (Exception ignored) {}
    //src="/draw/?embed=1&proto=json&spin=1&ui=dark&saveAndExit=1">
%>

<html>
<head>
<title>Edit Diagram - <%= id %></title>
</head>
<body style="margin:0;">

<iframe id="editorframe"
        style="width:100%; height:100vh; border:0;"
        src="/draw/?embed=1&proto=json<%= loadUrl.isEmpty() ? "" : "&url=" + loadUrl %>&modified=1">
        
</iframe>

<form id="saveForm" method="POST" action="SaveDiagram.jsp">
    <input type="hidden" name="page" value="<%= pageName %>" />
    <input type="hidden" name="id" value="<%= id %>" />
    <input type="hidden" name="xml" id="xmlField" />
</form>

<script>
window.addEventListener("message", function(evt){
    console.log(evt);
    if (evt.data == null) return;
    var source = evt.srcElement || evt.target;
    let evt2 = JSON.parse(evt.data);
    // Received if the editor is ready
    if (evt2.event === "init") {
        console.log("Sending action=load");
        evt.source.postMessage({ action: "load" }, "*");
        return;
    }
    // draw.io save request
    else if (evt2.event === "save") {
        var xml = evt.data.xml;

        fetch("SaveDiagram.jsp", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: "page=<%=page%>&name=<%=pageName%>&xml=" + encodeURIComponent(xml)
        })
        .then(()=> {
            document.getElementById("drawio").contentWindow.postMessage(
                { event: "saveDone" }, "*");
        });
    }
});
</script>

</body>
</html>
