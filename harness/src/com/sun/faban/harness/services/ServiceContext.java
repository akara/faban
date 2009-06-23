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
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.ParamRepository;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.w3c.dom.Element;

/**
 *
 * @author Sheetal Patil
 */
public class ServiceContext implements Serializable {

    private static final long serialVersionUID = 20090504L;
    public ServiceDescription desc;
    String role;
    String[] hosts;
    List<NameValuePair<Integer>> hostPorts;
    String steadyState;

    private Properties properties = new Properties();

    ServiceContext(ServiceDescription desc, ParamRepository par, 
                    Element roleElement, Properties properties)
            throws ConfigurationException {
        this.desc = desc;
        role = roleElement.getLocalName();
        this.properties = properties;
        steadyState = par.getParameter("fa:runConfig/fa:runControl/fa:steadyState");
        hosts = par.getEnabledHosts(roleElement);
        hostPorts = par.getEnabledHostPorts(roleElement);
        if (hostPorts != null && hostPorts.size() > 0)
            hostPorts = Collections.unmodifiableList(par.getEnabledHostPorts(roleElement));
    }    

    public String[] getHosts() {
        return hosts.clone();
    }

    public List<NameValuePair<Integer>> getHostPorts() {
        return hostPorts;
    }

    public String getHostRole() {
        return role;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getRunDuration() {
        return steadyState;
    }

}