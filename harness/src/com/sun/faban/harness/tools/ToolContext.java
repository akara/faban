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
import com.sun.faban.harness.services.ServiceDescription;

import java.io.IOException;
import java.util.List;

/**
 * This class is a subclass of MasterToolContext.
 * @author Sheetal Patil
 */
public class ToolContext extends MasterToolContext {

    String localOutputFile =
            Config.TMP_DIR + getToolName() + ".out." + this.hashCode();
    ToolWrapper wrapper;

    /**
     * Constructs the tool context.
     * @param tool The tool name
     * @param ctx The service context, if any
     * @param desc The tool description
     * @param wrapper The tool wrapper responsible for invoking the tool.
     */
    public ToolContext(String tool, ServiceContext ctx, ToolDescription desc,
                       ToolWrapper wrapper) {
        super(tool, ctx, desc);
        this.wrapper = wrapper;
    }

    /**
     * Returns tool status.
     * @return tool status as String
     */
    public int getToolStatus(){
            return wrapper.toolStatus;
    }

    /**
     * Returns tool name.
     * @return tool name as String
     */
    public String getToolName(){
        return super.getToolId();
    }

    /**
     * Returns list of tool arguments.
     * @return list of tool args
     */
    public List<String> getToolArgs(){
        List<String> argsList = null;
        String toolParams = super.getToolParams();
        if(toolParams != null)
            argsList = Command.parseArgs(toolParams);
        return argsList;
    }

    /**
     * Returns name of output file.
     * @return output file name
     */
    public String getOutputFile(){
        return localOutputFile;
    }

    /**
     * Sets the output file with the given path.
     * @param path The output file path.
     */
    public void setOutputFile(String path) {
        localOutputFile = path;
    }

    /**
     * Obtains the service property for the given key.
     * @param key The service property name
     * @return property as string
     */
    public String getServiceProperty(String key) {
        return super.getToolServiceContext().getProperty(key);
    }

    /**
     * Returns ServiceContext for the tool.
     * @return ServiceContext The service context
     */
    public ServiceContext getServiceContext() {
        return super.getToolServiceContext();
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
        if(getServiceContext() != null) {
            ServiceDescription sd = getServiceContext().desc;
            String fullPath = null;
            if(sd != null)
                fullPath = sd.locationType + '/' + sd.location;
                return wrapper.cmdAgent.execute(cmd, fullPath);
            } else {
                return wrapper.cmdAgent.execute(cmd, null);
        }
    }

    /**
     * Executes a command, optionally use the stdout from this command as the
     * tool output.
     * @param cmd The command to execute
     * @param useOutput Whether to use the output from this command
     * @return The command handle to this command
     * @throws IOException The command failed to execute
     * @throws InterruptedException Interrupted waiting for the command
     */
    public CommandHandle exec(Command cmd, boolean useOutput)
            throws IOException, InterruptedException {
        return exec(cmd, useOutput, Command.STDOUT);
    }

    /**
     * Executes a command, optionally use the stdout or stderr from this
     * command as the tool output.
     * @param cmd The command to execute
     * @param useOutput Whether to use the output from this command
     * @param stream The stream to use as the output, STDOUT or STDERR
     * @return The command handle to this command
     * @throws IOException The command failed to execute
     * @throws InterruptedException Interrupted waiting for the command
     */
    public CommandHandle exec(Command cmd, boolean useOutput, int stream)
            throws IOException, InterruptedException {
        if (useOutput) {
            cmd.setStreamHandling(stream, Command.CAPTURE);
            cmd.setOutputFile(stream, localOutputFile);
            if(getServiceContext() != null) {
                ServiceDescription sd = getServiceContext().desc;
                String fullPath = null;
                if(sd != null)
                    fullPath = sd.locationType + '/' + sd.location;
                wrapper.outputHandle = wrapper.cmdAgent.execute(cmd, fullPath);
            } else {
                wrapper.outputHandle = wrapper.cmdAgent.execute(cmd, null);
            }
            wrapper.outputStream = stream;
            return wrapper.outputHandle;
        } else {
            return wrapper.cmdAgent.execute(cmd, null);
        }
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

    /**
     * Obtains the temporary directory to be used for storing temporary files.
     * @return Name of the temporary directory
     */
    public String getTmpDir() {
        return Config.TMP_DIR;
    }
}
