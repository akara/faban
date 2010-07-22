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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.services;

import com.sun.faban.common.NameValuePair;
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.engine.CmdService;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.*;

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

    private Properties properties;
    private HashMap<String, Object> attributeMap = new HashMap<String, Object>();

    /**
     * Constructs the service context.
     * @param desc The service description
     * @param par The parameter repository
     * @param roleElement The top level element defining the role
     * @param properties The service configuration properties
     * @param restart Whether the server should be restarted or not
     * @throws ConfigurationException A configuration error occurred
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
     * Obtains the name (aka id) of the service.
     * @return The name of the service
     */
    public String getName() {
        return desc.id;
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
     * @param key The property key
     * @return The property value
     */
    public String getProperty(String key) {
        if(properties != null) {
            return properties.getProperty(key);
        } else {
            return null;
        }
    }

    /**
     * Sets a property in the service context. This can then be accessed by
     * tools through the tool context.
     * @param key The property key
     * @param value The property value
     */
    public void setProperty(String key, String value) {
        if (properties == null)
            properties = new Properties();
        properties.setProperty(key, value);
    }

    /**
     * Obtains the attribute of a given key.
     * @param key They attribute key
     * @return The attribute
     */
    public Object getAttribute(String key) {
        return attributeMap.get(key);
    }

    /**
     * Sets an object attribute in the service context.
     * This can then be accessed by tools through the tool context.
     * @param key The attribute key
     * @param value The attribute
     */
    public void setAttribute(String key, Object value) {
        attributeMap.put(key, value);
    }

    /**
     * Obtains the run duration.
     * @return duration of steady state as String
     */
    public String getRunDuration() {
        return steadyState;
    }

}