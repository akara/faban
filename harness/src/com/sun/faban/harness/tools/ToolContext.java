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
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.services.ServiceContext;

import java.util.List;
import java.io.IOException;

/**
 *
 * @author Sheetal Patil
 */
public class ToolContext extends MasterToolContext {

    String localOutputFile =
            Config.TMP_DIR + getToolName() + ".out." + this.hashCode();
    ToolWrapper wrapper;

    public ToolContext(String tool, ServiceContext ctx, ToolDescription desc,
                       ToolWrapper wrapper) {
        super(tool, ctx, desc);
        this.wrapper = wrapper;
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

    public ServiceContext getServiceContext() {
        return serviceCtx;
    }

    /**
     * Executes a command. The tool always executes the command locally.
     * @param cmd The command to execute
     * @return The command handle to this command
     * @throws IOException The command failed to execute
     * @throws InterruptedException Interrupted waiting for the command
     */
    public CommandHandle exec(Command cmd)
            throws IOException, InterruptedException {
        return wrapper.cmdAgent.execute(cmd);
    }

    /**
     * Executes a command and use the stdout from this command as the
     * tool output.
     * @param cmd The command to execute
     * @return The command handle to this command
     * @throws IOException The command failed to execute
     * @throws InterruptedException Interrupted waiting for the command
     */
    public CommandHandle execSetOutputStream(Command cmd)
            throws IOException, InterruptedException {
        return execSetOutputStream(cmd, Command.STDOUT);
    }

    /**
     * Executes a command and use an output stream from this command as
     * the tool output.
     * @param cmd The command to execute
     * @param stream The stream to use as the output, STDOUT or STDERR
     * @return The command handle to this command
     * @throws IOException The command failed to execute
     * @throws InterruptedException Interrupted waiting for the command
     */
    public CommandHandle execSetOutputStream(Command cmd, int stream)
            throws IOException, InterruptedException {
        cmd.setStreamHandling(stream, Command.CAPTURE);
        cmd.setOutputFile(stream, localOutputFile);        
        wrapper.outputHandle = wrapper.cmdAgent.execute(cmd);
        wrapper.outputStream = stream;
        return wrapper.outputHandle;
    }

    /**
     * Sets the stdout from a command to be used as the tool output.
     * @param handle The command handle to the command to capture.
     */
    public void setOutputStream(CommandHandle handle) {
        setOutputStream(handle, Command.STDOUT);
    }

    /**
     * Sets the stdout or stderr from a command to be used as the tool output.
     * @param handle The command handle to the command to capture
     * @param stream The stream to use as the output, STDOUT or STDERR
     */
    public void setOutputStream(CommandHandle handle, int stream) {
        wrapper.outputHandle = handle;
        wrapper.outputStream = stream;
    }

}
