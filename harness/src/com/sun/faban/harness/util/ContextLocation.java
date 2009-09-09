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
 * $Id$
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.util;

/**
 * Context location for services and tools.
 * @author Akara Sucharitakul
 */
public class ContextLocation {

    private static final InheritableThreadLocal<String> location =
            new InheritableThreadLocal<String>();

    /**
     * Sets a new context location.
     * @param newLocation The new context location
     */
    public static void set(String newLocation) {
        location.set(newLocation);
    }

    /**
     * Obtains the current context location.
     * @return The current context location
     */
    public static String get() {
        return location.get();
    }
}
