<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>
<%@ page language="java" pageEncoding="UTF-8"%>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ page import="java.util.Properties"%>
<%@ page import="org.apache.commons.lang3.*" %>
<%@ page import="org.apache.wiki.api.core.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="org.apache.wiki.filters.*" %>
<%@ page import="org.apache.wiki.pages.PageManager" %>
<%@ page import="org.apache.wiki.parser.MarkupParser" %>
<%@ page import="org.apache.wiki.render.*" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.wiki.variables.VariableManager" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<wiki:RequestResource type="stylesheet" resource="templates/default/haddock-wysiwyg.css" />
<wiki:RequestResource type="script" resource="scripts/haddock-wysiwyg.js" />
<%--
    This provides a wysiwy editor for JSPWiki. (based on mooeditable)
--%>
<%
    Context context = Context.findContext(pageContext);
    Engine engine = context.getEngine();

    context.setVariable(Context.VAR_WYSIWYG_EDITOR_MODE, Boolean.TRUE);
    context.setVariable(VariableManager.VAR_RUNFILTERS, "false");

    Page wikiPage = context.getPage();
    String originalCCLOption = (String) wikiPage.getAttribute(MarkupParser.PROP_CAMELCASELINKS);
    wikiPage.setAttribute(MarkupParser.PROP_CAMELCASELINKS, "false");

    String usertext = EditorManager.getEditedText(pageContext);
%>
<c:set var='context'><wiki:Variable var='requestcontext' /></c:set>
<wiki:CheckRequestContext context="edit">
    <wiki:NoSuchPage> <%-- this is a new page, check if we're cloning --%>
        <%
            String clone = request.getParameter("clone");
            if (clone != null) {
                Page p = engine.getManager(PageManager.class).getPage(clone);
                if (p != null) {
                    AuthorizationManager mgr = engine.getManager(AuthorizationManager.class);
                    PagePermission pp = new PagePermission(p, PagePermission.VIEW_ACTION);

                    try {
                        if (mgr.checkPermission(context.getWikiSession(), pp)) {
                            usertext = engine.getManager(PageManager.class).getPureText(p);
                        }
                    } catch (Exception e) {
                        /*log.error( "Accessing clone page "+clone, e );*/ }
                }
            }
        %>
    </wiki:NoSuchPage>
    <%
        if (usertext == null) {
            usertext = engine.getManager(PageManager.class).getPureText(context.getPage());
        }
    %>
</wiki:CheckRequestContext>
<%
    if (usertext == null) {
        usertext = "";
    }

    String pageAsHtml;
    try {
        pageAsHtml = engine.getManager(RenderingManager.class).getHTML(context, usertext);
    } catch (Exception e) {
        pageAsHtml = "<div class='error'>Error in converting wiki-markup to well-formed HTML <br/>" + e.toString() + "</div>";
        /*
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        pageAsHtml += "<pre>" + sw.toString() + "</pre>";
         */
    }

    // Disable the WYSIWYG_EDITOR_MODE and reset the other properties immediately
    // after the XHTML for wysiwyg editor has been rendered.
    context.setVariable(Context.VAR_WYSIWYG_EDITOR_MODE, Boolean.FALSE);
    context.setVariable(VariableManager.VAR_RUNFILTERS, null);
    wikiPage.setAttribute(MarkupParser.PROP_CAMELCASELINKS, originalCCLOption);

    /*not used
   String templateDir = (String)engine.getWikiProperties().get( Engine.PROP_TEMPLATEDIR );
   String protocol = "http://";
   if( request.isSecure() ) {
       protocol = "https://";
   }
     */

%>

<form method="post" accept-charset="<wiki:ContentEncoding/>"
      action="<wiki:CheckRequestContext
      context='edit'><wiki:EditLink format='url'/></wiki:CheckRequestContext><wiki:CheckRequestContext
      context='comment'><wiki:CommentLink format='url'/></wiki:CheckRequestContext>"
      class="editform wysiwyg"
      id="editform"
      enctype="application/x-www-form-urlencoded" >

    <wiki:CsrfProtection/>
    <%-- Edit.jsp relies on these being found.  So be careful, if you make changes. --%>
    <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
    <input type="hidden" name="action" value="save" />
    <wiki:SpamFilterInputs/>

    <div class="form-inline form-group">

        <div class="form-group dropdown">
            <button class="btn btn-success" type="submit" name="ok" accesskey="s">
                <fmt:message key='editor.plain.save.submit${ context == "edit" ? "" : ".comment" }'/>
                <span class="caret"></span>
            </button>
            <ul class="dropdown-menu" data-hover-parent="div">
                <wiki:CheckRequestContext context="!comment">
                    <li class="dropdown-header">
                        <input class="form-control" type="text" name="changenote" id="changenote" size="80" maxlength="80"
                               placeholder="<fmt:message key='editor.plain.changenote'/>"
                               value="${changenote}" />
                    </li>
                </wiki:CheckRequestContext>
                <wiki:CheckRequestContext context="comment">
                    <li class="divider" />
                    <li class="dropdown-header">
                    <fmt:message key="editor.commentsignature"/>
                    </li>
                    <li class="dropdown-header">
                        <input class="form-control" type="text" name="author" id="authorname"  size="80" maxlength="80"
                               placeholder="<fmt:message key='editor.plain.name'/>"
                               value="${author}" />
                    </li>
                    <li  class="dropdown-header">
                        <label class="btn btn-default btn-xs" for="rememberme">
                            <input type="checkbox" name="remember" id="rememberme" ${ remember ? "checked='checked'" : "" } />
                            <fmt:message key="editor.plain.remember"/>
                        </label>
                    </li>
                    <li  class="dropdown-header">
                        <input class="form-control" type="text" name="link" id="link" size="80" maxlength="80"
                               placeholder="<fmt:message key='editor.plain.email'/>"
                               value="${link}" />
                    </li>
                </wiki:CheckRequestContext>
            </ul>
        </div>

        <div class="btn-group editor-tools">

            <div class="btn-group config">
                <%-- note: 'dropdown-toggle' is only here to style the last button properly! --%>
                <button class="btn btn-default dropdown-toggle"><span class="icon-wrench"></span><span class="caret"></span></button>
                <ul class="dropdown-menu" data-hover-parent="div">
                    <li>
                        <a>
                            <label for="livepreview">
                                <input type="checkbox" data-cmd="livepreview" id="livepreview" ${prefs.livepreview ? 'checked="checked"' : ''}/>
                                <fmt:message key='editor.plain.livepreview'/> <span class="icon-refresh"/>
                            </label>
                        </a>
                    </li>
                    <li>
                        <a>
                            <label for="previewcolumn">
                                <input type="checkbox" data-cmd="previewcolumn" id="previewcolumn" ${prefs.previewcolumn ? 'checked="checked"' : ''}/>
                                <fmt:message key='editor.plain.sidebysidepreview'/> <span class="icon-columns"/>
                            </label>
                        </a>
                    </li>

                </ul>
            </div>

            <c:set var="editors" value="<%= engine.getManager(EditorManager.class).getEditorList()%>" />
            <c:if test='${fn:length(editors)>1}'>
                <div class="btn-group config">
                    <%-- note: 'dropdown-toggle' is only here to style the last button properly! --%>
                    <button class="btn btn-default dropdown-toggle"><span class="icon-pencil"></span><span class="caret"></span></button>
                    <ul class="dropdown-menu" data-hover-parent="div">
                        <c:forEach items="${editors}" var="edt">
                            <c:choose>
                                <c:when test="${edt != prefs.editor}">
                                    <li>
                                    <wiki:Link context="edit" cssClass="editor-type">${edt}</wiki:Link>
                                    </li>
                                </c:when>
                                <c:otherwise>
                                    <li class="active"><a>${edt}</a></li>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </ul>
                </div>
            </c:if>

        </div>

        <div class="form-group pull-right">

            <%-- is PREVIEW functionality still needed - with livepreview ?
            <input class="btn btn-primary" type="submit" name="preview" accesskey="v"
                   value="<fmt:message key='editor.plain.preview.submit'/>"
                   title="<fmt:message key='editor.plain.preview.title'/>" />
            --%>
            <input class="btn btn-danger" type="submit" name="cancel" accesskey="q"
                   value="<fmt:message key='editor.plain.cancel.submit'/>"
                   title="<fmt:message key='editor.plain.cancel.title'/>" />

        </div>


    </div>

    <div class="row edit-area livepreview previewcolumn"><%-- .livepreview  .previewcolumn--%>
        <div>
            <%--
            XSS note
            Textareas automatically decodes html entities : so &lt;  is converted to <
            To avoid this, double escape the & char =>  so &amp;lt; is converted to &lt;
             pageAsHtml.replace("&", "&amp;")
            --%>
            <div class="tiptap-toolbar">
                <button type="button" onclick="window.editor.chain().focus().toggleBold().run()" title="Bold"><b>B</b></button>
                <button type="button" onclick="window.editor.chain().focus().toggleItalic().run()" title="Italic"><i>I</i></button>
                <button type="button" onclick="window.editor.chain().focus().toggleStrike().run()" title="Strikethrough"><s>S</s></button>

                <span class="separator">|</span>

                <button type="button" onclick="window.editor.chain().focus().toggleHeading({level: 1}).run()">H1</button>
                <button type="button" onclick="window.editor.chain().focus().toggleHeading({level: 2}).run()">H2</button>

                <span class="separator">|</span>

                <button type="button" onclick="window.editor.chain().focus().toggleBulletList().run()">• List</button>
                <button type="button" onclick="window.editor.chain().focus().toggleOrderedList().run()">1. List</button>

                <span class="separator">|</span>

                <button type="button" onclick="window.editor.chain().focus().undo().run()">Undo</button>
                <button type="button" onclick="window.editor.chain().focus().redo().run()">Redo</button>
            </div>
            <div name="htmlPageText" id='editor' class="tiptap-editor"
                 autofocus="autofocus"></div>
        </div>
        <div class="ajaxpreview">Preview comes here</div>
    </div>

    <div class="resizer" data-pref="editorHeight"
         title="<fmt:message key='editor.plain.edit.resize'/>"></div>

</form>
<style>
    .tiptap-toolbar {
        background: #f1f1f1;
        border: 1px solid #ccc;
        border-bottom: none;
        padding: 5px;
        display: flex;
        gap: 4px;
        flex-wrap: wrap;
        align-items: center;
    }

    .tiptap-toolbar button {
        background: white;
        border: 1px solid #ccc;
        padding: 4px 8px;
        cursor: pointer;
        font-size: 14px;
        border-radius: 3px;
    }

    .tiptap-toolbar button:hover {
        background: #e1e1e1;
    }

    .tiptap-toolbar .separator {
        margin: 0 5px;
        color: #ccc;
    }

    /* Style the surface to connect to the toolbar */
    #tiptap-surface {
        border: 1px solid #ccc;
        padding: 15px;
        background: white;
        min-height: 300px;
    }

    .tiptap-content {
        border: 1px solid #ccc;
        padding: 1rem;
        min-height: 300px;
        background: white;
    }

    .tiptap-content:focus-within {
        outline: none;
        border-color: #007bff;
    }

    /* Ensure common tags look right inside the editor */
    .tiptap-content p {
        margin: 1em 0;
    }
    .tiptap-content ul {
        padding-left: 20px;
    }

    /* IMPORTANT: You must provide styles or it will be invisible */
    .tiptap-editor {
        border: 1px solid #ccc;
        min-height: 200px;
        padding: 10px;
        background: white;
    }
    /* Style the actual editable area inside TipTap */
    .tiptap-editor .ProseMirror {
        outline: none;
        min-height: 180px;
    }
</style>

<script src="./scripts/tiptap-bundle.js"></script>
<script type="module">
      //import { Editor, StarterKit, Table, TableRow } from './scripts/tiptap-bundle.js';
      const preview = document.getElementsByClassName("ajaxpreview")[0];
      const {Editor, StarterKit, Table, TableRow, TableCell, TableHeader, Image} = window.TiptapEditor;
      window.editor = new Editor({
          element: document.querySelector('#editor'),
          extensions: [StarterKit.configure({
                  // Disable things that might conflict with JSPWiki's global styles
                  history: true
              }),
              Table.configure({resizable: true}),
              TableRow,
              TableHeader,
              TableCell,
              Image
          ],
          content: '<p>Hello JSPWiki!</p>',
          onUpdate( { editor }) {
              const htmlContent = editor.getHTML(); // Gets the live HTML output

              // Trigger the JSPWiki preview endpoint
              Wiki.getXHRPreview(htmlContent, preview);
          }
      });
</script>

<script type="text/javascript">
//<![CDATA[

    const wikiSerializer = (node) => {
        if (node.type === 'text') {
            let text = node.text;
            if (node.marks) {
                node.marks.forEach(mark => {
                    if (mark.type === 'bold')
                        text = '__' + text + '__';
                    if (mark.type === 'italic')
                        text = "''" + text + "''";
                    if (mark.type === 'code')
                        text = '{{' + text + '}}';
                    if (mark.type === 'link')
                        text = '[' + text + '|' + mark.attrs.href + ']';
                });
            }
            return text;
        }

        const content = node.content ? node.content.map(wikiSerializer).join('') : '';

        switch (node.type) {
            case 'doc':
                return content;
            case 'paragraph':
                return content + '\n\n';

                // JSPWiki Headings: !!! is H1, !! is H2, ! is H3
            case 'heading':
                const wikiLevel = '!'.repeat(4 - node.attrs.level);
                return wikiLevel + ' ' + content + '\n';

                // Tables: || cell 1 || cell 2
            case 'table':
                return '\n' + content + '\n';
            case 'tableRow':
                return content + ' ||\n'; // JSPWiki rows end with ||
            case 'tableCell':
            case 'tableHeader':
                const prefix = node.type === 'tableHeader' ? '||' : '|';
                return '|| ' + content + ' ';

                // Images: [{Image src='...'}]
            case 'image':
                const src = node.attrs.src;
                const alt = node.attrs.alt ? " alt='" + node.attrs.alt + "'" : "";
                return "[{Image src='" + src + "'" + alt + "}]";

            case 'bulletList':
                return content;
            case 'listItem':
                return '* ' + content + '\n';
            case 'horizontalRule':
                return '----\n';

            default:
                return content;
        }
    };
    window.prepareSave = function () {
        // 1. Get JSON from TipTap
        const json = window.editor.getJSON();

        // 2. Convert to JSPWiki Syntax
        const wikiText = wikiSerializer(json);

        // 3. Inject into JSPWiki's form
        document.getElementById('wikitext').value = wikiText.trim();
    };

    /*
     Wiki.add("[name=htmlPageText]", function (element) {
     
     function containerHeight() {
     return editor.container.getStyle("height");
     }
     function editorHeight() {
     return editor.iframe.getStyle("height");
     }
     function editorContent() {
     return editor.getContent();
     }
     function resizePreview() {
     preview.setStyle("height", containerHeight());
     form.htmlPageText.setStyle("height", editorHeight());
     }
     
     var form = element.form,
     editor,
     preview = form.getElement(".ajaxpreview"),
     resizer = form.getElement(".resizer"),
     resizeCookie = "editorHeight",
     html2markup = Wiki.getXHRPreview(editorContent, preview);
     
     Wiki.configPrefs(form, function (cmd, isChecked) {
     if (isChecked && (cmd == "livepreview")) {
     html2markup();
     }
     });
     
     element.mooEditable({
     dimensions: {
     x: "100%",
     y: "100%"
     },
     extraCSS: "body{padding:.5em;}",
     externalCSS: $("main-stylesheet").href,
     onAttach: function () {
     
     editor = this;
     Wiki.resizer(resizer, $$(editor.iframe), resizePreview);
     html2markup();
     resizePreview();
     
     },
     onChange: html2markup,
     onEditorKeyUp: html2markup,
     onEditorPaste: html2markup,
     actions: 'formatBlock | bold italic strikethrough | justifyleft justifyright justifycenter justifyfull | insertunorderedlist insertorderedlist indent outdent insertHorizontalRule / undo redo removeformat | createlink unlink | urlimage | toggleview'
     });
     
     });
     */

</script>
