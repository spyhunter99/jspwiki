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
import java.io.IOException;
import java.io.InputStream;
import java.security.ProviderException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.api.providers.AttachmentProvider;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.pages.PageManager;

/**
 * retruns a specific page's named attachment
 *
 * @since 3.0.0
 */
public class LoadAttachmentAjax implements WikiAjaxServlet, Plugin {

    private static final Logger LOG = LogManager.getLogger(LoadAttachmentAjax.class);

    @Override
    public String getServletMapping() {
        return "loadDrawioDiagram";
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response, String actionName, List<String> params) throws ServletException, IOException {
        // 1. Setup and Security Checks
        Context context = (Context) request.getAttribute(Context.ATTR_CONTEXT);
        Engine engine = context.getEngine();
        // Must be a POST request for saving data
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        // Basic permission check (customize as needed)
        final String fileName = request.getParameter("attachmentName");
        final String pageName = request.getParameter("pageName");
        // Check if user has permission to edit the target page
        // (You might want a more granular check for attachments)
        PagePermission perm = new PagePermission(pageName, "view");
        if (!engine.getManager(AuthorizationManager.class).checkPermission(context.getWikiSession(), perm)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            LOG.warn("User attempted to save a diagram without edit permission to " + pageName);
            return;
        }
        try {

            AttachmentManager attachmentManager = engine.getManager(AttachmentManager.class);
            AttachmentProvider provider = attachmentManager.getCurrentProvider();
            Page page = engine.getManager(PageManager.class).getPage(pageName);
            if (page == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Attachment att = provider.getAttachmentInfo(page, fileName, WikiProvider.LATEST_VERSION);
            InputStream attachmentData = provider.getAttachmentData(att);
            response.setStatus(HttpServletResponse.SC_OK);
            IOUtils.copy(attachmentData, response.getOutputStream());

        } catch (ProviderException e) {
            LOG.error("Error reading input stream or writing response:", e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
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

    @Override
    public String execute(Context context, Map<String, String> params) throws PluginException {
        throw new PluginException("Not supported yet.");
    }

}
