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
package com.sun.faban.harness;

import java.io.Serializable;

/**
 * An interface used for executing a piece of code remotely.<br>
 * Notes:<ul>
 * <li>The class that implements this interface needs to be cautious about
 * platform path differences. A path passed to this class to executed on a
 * remote system with different OS styles, especially a Windows master and
 * Unix agents, will need to be converted. This can be done by calling
 * Utilities.convertPath from inside the implementation of the call method.
 * The path conversion should be called no matter the actual platform. The
 * path conversion call does nothing if no conversion is needed.<br></li>
 * <li>The only RunContext methods available to the code block are the local
 * exec and java calls. All other calls to RunContext will result in a
 * NullPointerException.</li></ul>
 *  
 * @see com.sun.faban.common.Utilities#convertPath(java.lang.String)
 */
public interface RemoteCallable<V extends Serializable>
        extends Serializable {

    /**
     * Computes a result, or throws an exception if unable to do so.
     * Note that The only RunContext methods available to the call
     * implementation are the local exec and java calls. All other
     * calls to RunContext will result in a NullPointerException.
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    public V call() throws Exception;
}
