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

import com.sun.faban.common.Command;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.services.ServiceContext;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Sheetal Patil
 */
public class ToolContext extends MasterToolContext {

    String localOutputFile =
            Config.TMP_DIR + getToolName() + ".out." + this.hashCode();
    public ToolContext(String tool, ServiceContext ctx, ToolDescription desc)
            throws ParserConfigurationException, SAXException, IOException {
        super(tool, ctx, desc);
    }

    public String getToolName(){
        return super.getToolId();
    }

    public List<String> getToolArgs(){
        List<String> argsList = null;
        String toolParams = super.getToolParams();
        if(toolParams != null)
            argsList = Command.parseArgs(toolParams);
        return argsList;
    }

    public String getOutputFile(){
        return localOutputFile;
    }
    
    public void setOutputFile(String path) {
        localOutputFile = path;
    }

    public String getServiceProperty(String key) {
        return serviceCtx.getProperty(key);
    }

}
