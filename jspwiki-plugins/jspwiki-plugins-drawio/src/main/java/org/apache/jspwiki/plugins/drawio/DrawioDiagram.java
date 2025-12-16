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
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;

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
        String attachmentName = params.get("name");
        
        return "";
    }

    @Override
    public String getSnipExample() {
        return Plugin.super.getSnipExample() + " name=MyDiagram.svg";
    }

}
