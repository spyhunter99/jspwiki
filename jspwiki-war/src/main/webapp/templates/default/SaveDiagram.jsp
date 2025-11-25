<%@page import="org.apache.wiki.attachment.AttachmentManager"%>
<%@page import="org.apache.wiki.pages.PageManager"%>
<%@ page import="org.apache.wiki.api.core.*, 
         org.apache.wiki.WikiEngine, 
         org.apache.wiki.WikiContext, 
         org.apache.wiki.attachment.Attachment,
         org.apache.wiki.pages.PageManager,
         java.io.*" %>
<%
    request.setCharacterEncoding("UTF-8");

    Engine engine = WikiEngine.getInstance(config);
    String pageName = request.getParameter("page");
    String id = request.getParameter("id");
    String fileName = id + ".drawio";

    String xml = request.getParameter("xml");

    Page wikipage = engine.getManager(PageManager.class).getPage(pageName);
    Context wikiContext = new WikiContext(engine, wikipage);

    org.apache.wiki.attachment.Attachment att = new org.apache.wiki.attachment.Attachment(engine, 
            wikipage.getName(), fileName);
    //att.setContentType("application/xml");

    byte[] data = xml.getBytes("UTF-8");

    engine.getManager(AttachmentManager.class).storeAttachment(
         att, new ByteArrayInputStream(data)
    );

    response.sendRedirect("Wiki.jsp?page=" + pageName);
%>
