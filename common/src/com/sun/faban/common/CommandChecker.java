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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.util.List;
import java.util.Map;

/**
 * An interface impolemented by the command agent allowing it to check the
 * command before it gets executed to ensure it executes in the agent's
 * environment.
 *
 * @author Akara Sucharitakul
 */
public interface CommandChecker {

    /**
     * Checks and completes the command, if possible.
     * @param cmd The original command
     * @param extMap The external map, if any
     * @return The completed command
     */
    public List<String> checkCommand(List<String> cmd, Map<String, List<String>> extMap);

    /**
     * Checks and completes the java command, if possible.
     * @param cmd The original command
     * @param extClassPath The external class path.
     * @return The completed java command
     */
    public List<String> checkJavaCommand(List<String> cmd, List<String> extClassPath);
}
