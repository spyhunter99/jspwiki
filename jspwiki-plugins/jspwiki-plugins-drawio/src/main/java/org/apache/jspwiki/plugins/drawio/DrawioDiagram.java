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
package org.apache.jspwiki.plugins.drawio;

import java.util.Map;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.attachment.AttachmentManager;

/**
 * concept here is to use something like [{DrawioDiagram name=attachment.svg}]
 * which will then render the svg as a img tag along with an edit this diagram
 * link, linking to the diagram.jsp page that we have.
 *
 * @since 3.0.0
 */
public class DrawioDiagram implements Plugin {

    @Override
    public String execute(Context context, Map<String, String> params) throws PluginException {
        String src = params.get("name");
        String imgUrl = null;
        final Engine engine = context.getEngine();
        StringBuilder result = new StringBuilder();
        if (src == null) {
            throw new PluginException("Parameter 'src' is required for Image plugin");
        }
        if (!src.endsWith(".svg")) {
            src = src + ".svg";
        }
        Attachment att;
        try {
            final AttachmentManager mgr = engine.getManager(AttachmentManager.class);
            att = mgr.getAttachmentInfo(context, src);

            if (att != null) {
                imgUrl = context.getURL(ContextEnum.PAGE_ATTACH.getRequestContext(), att.getName());
            }
        } catch (final ProviderException e) {
            throw new PluginException("Attachment info failed: " + e.getMessage());
        }
        result.append("<table border=\"0\" class=\"imageplugin\"");
        result.append(">\n");
        result.append("<caption>").append("<a href=\"diagram.jsp?pageName="
                + context.getPage().getName() + "&attachment=" + src
                + "\">Edit this diagram</a>"
        ).append("</caption>\n");
        result.append("<tr><td>");
        result.append("<img src=\"").append(imgUrl).append("\">");
        result.append("</td></tr></table>");

        return result.toString();
    }

    @Override
    public String getSnipExample() {
        return Plugin.super.getSnipExample() + " name=MyDiagram.svg";
    }

}
