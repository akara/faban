/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.sun.com/cddl/cddl.html or
 * install_dir/legal/LICENSE
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at install_dir/legal/LICENSE.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.harness.services.ServiceContext;
import java.io.IOException;

import java.io.Serializable;
import java.util.Set;
import java.util.StringTokenizer;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Sheetal Patil
 */
public class MasterToolContext implements Serializable {

    private static final long serialVersionUID = 20090504L;
    private String toolId;
    private String tool;
    ToolDescription toolDesc;
    ServiceContext serviceCtx;
    String params = null;

    public MasterToolContext(String tool, ServiceContext ctx, ToolDescription desc) {
        StringTokenizer tt = new StringTokenizer(tool);
        String toolName = tt.nextToken().trim();
        this.toolId = toolName;
        if(tool.length() > toolName.length())
            this.params = tool.substring(toolName.length()+1, tool.length()).trim();
        this.toolDesc = desc;
        this.serviceCtx = ctx;
        this.tool = tool;
    }

    public String getToolId() {
        return toolId;
    }

    public String getTool() {
        return tool;
    }

    public ToolDescription getToolDescription() {
            return toolDesc;    
    }

    public ServiceContext getToolServiceContext() {
       return serviceCtx;     
    }

    public String getToolParams() {
        return params;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MasterToolContext other = (MasterToolContext) obj;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + (this.toolId != null ? this.toolId.hashCode() : 0);
        hash = 61 * hash + (this.toolDesc != null ? this.toolDesc.hashCode() : 0);
        hash = 61 * hash + (this.serviceCtx != null ? this.serviceCtx.hashCode() : 0);
        return hash;
    }    
}
