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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.api.providers.AttachmentProvider;
import org.apache.wiki.api.spi.Wiki;
import static org.apache.wiki.api.spi.Wiki.engine;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Draw IO servlet. Basically whenever the user hits save or save and exit, the
 * front end will post the diagram's data as an attachment to a page. Assuming
 * all the security checks pass, the attachment is persisted.
 *
 * @since 3.0.0
 */
public class SaveDrawIoAjax implements WikiAjaxServlet, Plugin {

    private static final Logger LOG = LogManager.getLogger(SaveDrawIoAjax.class);

    @Override
    public String getServletMapping() {
        return "saveDrawioDiagram";
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response, String actionName, List<String> params) throws ServletException, IOException {
        // 1. Setup and Security Checks
        Context context = (Context) request.getAttribute(Context.ATTR_CONTEXT);
        Engine engine = Wiki.engine().find(request.getServletContext(), new Properties());
        if (context == null) {

            context = Wiki.context().create(engine, request, "");
        }
        // Must be a POST request for saving data
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        // Basic permission check (customize as needed)
        //final String fileName = request.getParameter("attachmentName");
        //final String pageName = request.getParameter("pageName");
        // Check if user has permission to edit the target page
        // (You might want a more granular check for attachments)

        try {
            // 2. Read and Parse the JSON Payload
            // Note: The client JS sends raw Base64 data, so we read the stream directly.
            // Assuming client sends a simple JSON like: {"attachmentName": "MyDiagram.svg", "pageName": "Page", "fileContent": "rawBase64String"}
            JsonNode node = new ObjectMapper().readTree(request.getInputStream());
            String rawBase64 = extractBase64FromJson(node); // Implement this helper
            if (rawBase64 == null || rawBase64.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                LOG.error("Received null or empty Base64 content from Draw.io save.");
                return;
            }
            final String fileName = node.get("attachmentName").asString();
            final String pageName = node.get("pageName").asString();
            PagePermission perm = new PagePermission(pageName, "edit");
            
            /*
            FIXME this needs to be addressed.
            if (!engine.getManager(AuthorizationManager.class).checkPermission(context.getWikiSession(), perm)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                LOG.warn("User attempted to save a diagram without edit permission to " + pageName);
                return;
            }*/

            // 3. Decode Base64 and Prepare InputStream
            byte[] decodedBytes = Base64.getDecoder().decode(rawBase64);
            InputStream dataStream = new ByteArrayInputStream(decodedBytes);
            // 4. Persistence using AttachmentProvider
            AttachmentManager attachmentManager = engine.getManager(AttachmentManager.class);
            AttachmentProvider provider = attachmentManager.getCurrentProvider();
            // Create a temporary Attachment object (version 0 means a new version will be created)
            Attachment att = new Attachment(engine, pageName, fileName);
            // This is the core save call. It handles versioning.
            provider.putAttachmentData(att, dataStream);
            // 5. Success Response
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            //TODO I18N
            response.getWriter().write("Diagram saved successfully.");
        } catch (IOException e) {
            LOG.error("Error reading input stream or writing response:", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid Base64 string received:", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            LOG.error("Unhandled exception :", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // --- Helper Method (Requires a JSON library like Gson or Jackson) ---
    private String extractBase64FromJson(JsonNode jsonPayload) {
        // *** IMPLEMENT REAL JSON PARSING HERE ***
        // Example with Gson:
        // JsonObject json = new Gson().fromJson(jsonPayload, JsonObject.class);
        // return json.get("fileContent").getAsString();
        return jsonPayload.get("fileContent").asString();
    }

    @Override
    public String execute(Context context, Map<String, String> params) throws PluginException {
        throw new PluginException("Not supported yet.");
    }

}
