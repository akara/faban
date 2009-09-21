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
package com.sun.faban.harness.tools;

import com.sun.faban.harness.services.ServiceDescription;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This class provides the description of a tool.
 *
 * @author Sheetal Patil
 */
public class ToolDescription implements Serializable {

    private static final long serialVersionUID = 20090504L;

    String id;
    String toolClass;
    String serviceName;
    String locationType;
    String location;
    ServiceDescription service;

    /**
     * Constructs a tool description.
     * @param id The tool id
     * @param serviceName The service name
     * @param toolClass The tool class name
     * @param type The location type, services or benchmark
     * @param location The actual location of the tool, if applicable.
     */
    public ToolDescription(String id, String serviceName,
                            String toolClass, String type, String location) {
        this.id = id;
        this.serviceName = serviceName;
        this.toolClass = toolClass;
        this.locationType = type;
        this.location = location;
    }

    /**
     * Binds the tool to the service.
     * @param serviceMap The service map
     * @return boolean true if the tool is bound to a service
     */
    public boolean bind(Map<String, ServiceDescription> serviceMap) {
        boolean bound = true;
        if (serviceName != null) {
            service = serviceMap.get(serviceName);
            if (service == null)
                bound = false;
        }
        return bound;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ToolDescription other = (ToolDescription) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        if (this.service != other.service && (this.service == null ||
                !this.service.equals(other.service))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 37 * hash + (this.service != null ? this.service.hashCode() : 0);
        return hash;
    }

    /**
     * Returns the location type.
     * @return "services" or "benchmarks".
     */
    public String getLocationType() {
        return locationType;
    }

    /**
     * Returns the path.
     * @return location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns the tool id.
     * @return tool id as String.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the service name to which the tool is associated.
     * @return service name as string.
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the tool class.
     * @return toolclass as string.
     */
    public String getToolClass() {
        return toolClass;
    }
}
