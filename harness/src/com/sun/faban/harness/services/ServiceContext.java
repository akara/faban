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

import java.io.Serializable;

import java.util.List;
import java.util.Properties;

/**
 *
 * @author Sheetal Patil
 */
public class ServiceContext implements Serializable {

    private static final long serialVersionUID = 20090504L;
    private String role;
    public ServiceDescription desc;
    private List<String> hosts;
    private Properties properties = new Properties();

    ServiceContext(ServiceDescription desc, List<String> hosts, Properties properties) {
        this.desc = desc;
        this.hosts = hosts;
        this.properties = properties;
    }    

    public String[] getHosts() {
        String[] hostNames = new String[hosts.size()];
        for(int i=0; i<hosts.size(); i++)
            hostNames[i] = hosts.get(i);
        return hostNames;
    }

    public String getHostRole() {
        return role;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }


}