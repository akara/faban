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
import java.util.logging.Logger;

/**
 * This class provides the description for each service.
 *
 * @author Sheetal Patil
 */
public class ServiceDescription implements Serializable {

    private static final long serialVersionUID = 20090504L;
    static final Logger logger = Logger.getLogger(
            ServiceDescription.class.getName());
    public String id;
    public String serviceClass;
    public String locationType;
    public String location;

    /**
     * Constructor.
     * @param id
     * @param serviceClass
     * @param type
     * @param location
     */
    ServiceDescription(String id, String serviceClass, String type, String location) {
        this.id = id;
        this.serviceClass = serviceClass;
        locationType = type;
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ServiceDescription) {
            ServiceDescription other = (ServiceDescription) o;
            return id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }
}