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
package org.apache.wiki.plugin;

import org.apache.wiki.WikiContext;

import org.apache.wiki.attachment.AttachmentManager;

import java.util.Map;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;

public class DiagramPlugin implements Plugin {

    @Override
    public String execute(Context context, Map<String, String> params)
            throws PluginException {

        String diagramId = params.get("id");
        if (diagramId == null) {
            return "<div class='error'>Diagram id is required</div>";
        }

        String pageName = context.getPage().getName();
        String attachmentName = diagramId + ".drawio";

        Engine engine = context.getEngine();
        AttachmentManager am = engine.getManager(AttachmentManager.class);

        Attachment att = null;
        try {
            att = am.getAttachmentInfo(context, pageName + "/" + attachmentName);
        } catch (Exception ignored) {
        }

        StringBuilder out = new StringBuilder();

        if (att != null) {
            // Render as an image (rendering handled by diagrams.net-compatible SVG)
            out.append("<img src='")
                    .append(context.getURL(WikiContext.ATTACH, pageName, "file=" + attachmentName))
                    .append("' style='max-width:100%; border:1px solid #ccc;' />");
        } else {
            out.append("<div>No diagram found for id ").append(diagramId).append("</div>");
        }

        // Add edit link
        out.append("<div><a href='")
                .append("EditDiagram.jsp?page=")
                .append(pageName)
                .append("&id=")
                .append(diagramId)
                .append("'>Edit Diagram</a></div>");

        return out.toString();
    }
}
