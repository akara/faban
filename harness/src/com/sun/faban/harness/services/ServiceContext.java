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
package com.sun.faban.harness.services;

import com.sun.faban.common.NameValuePair;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.Command;
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.RemoteCallable;
import com.sun.faban.harness.agent.CmdAgentImpl;
import com.sun.faban.harness.engine.CmdService;

import java.io.Serializable;
import java.io.IOException;
import java.util.*;

import org.w3c.dom.Element;

/**
 * This class provides the context for every service. Each service has only one
 * context.
 * @author Sheetal Patil
 */
public class ServiceContext implements Serializable {

    private static final long serialVersionUID = 20090504L;

    /** The service description describing this context. */
    public ServiceDescription desc;
    String role;
    String[] hosts;
    String[] uniqueHosts;
    List<NameValuePair<Integer>> hostPorts;
    List<NameValuePair<Integer>> uniqueHostPorts;
    String steadyState;
    String servicePath = null;
    boolean restart;

    private Properties properties = new Properties();

    /**
     * Constructs the service context.
     * @param desc The service description
     * @param par The parameter repository
     * @param roleElement The top level element defining the role
     * @param properties The service configuration properties
     * @throws com.sun.faban.harness.ConfigurationException
     */
    ServiceContext(ServiceDescription desc, ParamRepository par, 
                    Element roleElement, Properties properties, boolean restart)
            throws ConfigurationException {
        this.desc = desc;

        // The servicePath is only there if the service is deployed separately
        if ("services".equals(desc.locationType))
            servicePath = desc.location;
        role = roleElement.getLocalName();
        this.restart = restart;
        this.properties = properties;
        steadyState = par.getParameter(
                "fa:runConfig/fa:runControl/fa:steadyState");

        hosts = par.getEnabledHosts(roleElement);

        // Obtain unique hosts
        CmdService cmds = CmdService.getHandle();
        ArrayList<String> hostList = new ArrayList<String>(hosts.length);
        HashSet<String> realHostSet = new HashSet<String>(hosts.length);

        for (String host : hosts) {
            if (realHostSet.add(cmds.getHostName(host)))
                hostList.add(host);
        }
        uniqueHosts = new String[hostList.size()];
        uniqueHosts = hostList.toArray(uniqueHosts);

        hostPorts = par.getEnabledHostPorts(roleElement);
        if (hostPorts != null && hostPorts.size() > 0) {
            hostPorts = Collections.unmodifiableList(
                    par.getEnabledHostPorts(roleElement));

            uniqueHostPorts =
                    new ArrayList<NameValuePair<Integer>>(hostPorts.size());
            HashSet<NameValuePair<Integer>> hostPortSet =
                    new HashSet<NameValuePair<Integer>>(hostPorts.size());
            for (NameValuePair<Integer> hostPort : hostPorts) {
                if (hostPortSet.add(new NameValuePair<Integer>(
                        cmds.getHostName(hostPort.name), hostPort.value)))
                    uniqueHostPorts.add(hostPort);
            }
            uniqueHostPorts = Collections.unmodifiableList(uniqueHostPorts);
        }
    }

    /**
     * Obtains the list of hosts as configured in the configuration file
     * for the role this service is associated.
     * @return string array of hosts
     */
    public String[] getHosts() {
        return hosts.clone();
    }

    /**
     * Obtains a list of non-duplicate hosts. If two or more host names
     * refer to the same physical or virtual system (OS instance), only the
     * first of such host names is returned as part of the list. This is used
     * by services to avoid unwanted multiple starts of a service.
     * @return The unique list of host names
     */
    public String[] getUniqueHosts() {
        return uniqueHosts.clone();
    }

    /**
     * Obtains a list of host:ports as configured in the configuration file
     * to be used by the role this service is associated.
     * @return List of host:ports
     */
    public List<NameValuePair<Integer>> getHostPorts() {
        return hostPorts;
    }

    /**
     * Obtains a list of hostports non-duplicate hosts:ports. If two or more
     * host names with the same port refer to the same physical or virtual
     * system (OS instance), only the first of such host:port pairs is
     * returned as part of the list.
     * @return Unique list of host:port pairs
     */
    public List<NameValuePair<Integer>> getUniqueHostPorts() {
        return uniqueHostPorts;
    }

    /**
     * Obtains host role.
     * @return host role
     */
    public String getHostRole() {
        return role;
    }

    /**
     * Obtains the property for a given key.
     * @param key
     * @return the property value
     */
    public String getProperty(String key) {
        if(properties != null){
            return properties.getProperty(key);
        }else{
            return null;
        }
    }

    /**
     * Obtains the run duration.
     * @return duration of steady state as String
     */
    public String getRunDuration() {
        return steadyState;
    }


    /**
     * Executes a command on the master.
     * @param c The command to be executed
     * @return  A handle to the command
     * @throws java.io.IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle exec(Command c)
            throws IOException, InterruptedException {
        CmdAgentImpl agent = CmdAgentImpl.getHandle();
        if (agent != null) // Running on agent
            return CmdAgentImpl.getHandle().execute(c, servicePath);
        else // Running on master
            return CmdService.getHandle().execute(c, servicePath);

    }

    /**
     * Executes a command on a remote host.
     * @param host The target machine to execute the command
     * @param c The command to be executed
     * @return A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle exec(String host, Command c)
            throws IOException, InterruptedException {
        return CmdService.getHandle().execute(host, c, servicePath);
    }

    /**
     * Executes a command on a set of remote hosts.
     * @param hosts The target machines to execute the command
     * @param c The command to be executed
     * @return Handles to the command on each of the target machines
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle[] exec(String[] hosts, Command c)
            throws IOException, InterruptedException {
        return CmdService.getHandle().execute(hosts, c, servicePath);
    }

    /**
     * Executes a java command on the master.
     *
     * @param java The command to be executed
     * @return A handle to the command
     * @throws java.io.IOException  Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle java(Command java)
            throws IOException, InterruptedException {
        return CmdService.getHandle().java(java, servicePath);
    }

    /**
     * Executes a java command on a remote host.
     *
     * @param host The target machine to execute the command
     * @param java The command to be executed
     * @return A handle to the command
     * @throws java.io.IOException  Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle java(String host, Command java)
            throws IOException, InterruptedException {
        return CmdService.getHandle().java(host, java, servicePath);
    }

    /**
     * Executes a java command on a set of remote hosts.
     *
     * @param hosts The target machines to execute the command
     * @param java  The command to be executed
     * @return Handles to the command on each of the target machines
     * @throws java.io.IOException  Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle[] java(String[] hosts, Command java)
            throws IOException, InterruptedException {
      return CmdService.getHandle().java(hosts, java, servicePath);
    }

    /**
     * Execute a code block defined as a RemoteCallable on a remote host.
     * @param host The remote host
     * @param callable The callable defining the code block
     * @return The result of the callable
     * @throws Exception An error occurred making the call
     */
    public <V extends Serializable> V
                        exec(String host, RemoteCallable<V> callable)
            throws Exception {
        return CmdService.getHandle().execute(host, callable);
    }

    /**
     * Execute a code block defined as a RemoteCallable on a set of
     * remote hosts.
     * @param hosts The remote hosts
     * @param callable The callable defining the code block
     * @return The result of the callable
     * @throws Exception An error occurred making the call
     */
    public <V extends Serializable> List<V>
                        exec(String[] hosts, RemoteCallable<V> callable)
            throws Exception {
        return CmdService.getHandle().execute(hosts, callable);
    }
}