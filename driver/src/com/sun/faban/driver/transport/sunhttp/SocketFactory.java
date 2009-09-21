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
package com.sun.faban.driver.transport.sunhttp;

import java.net.Socket;
import java.net.Proxy;

/**
 * This SocketFactory is the javax.net.SocketFactory with one additional
 * createSocket interface allowing the creation of a socket with the proxy.
 *
 * @author Akara Sucharitakul
 */
public abstract class SocketFactory extends javax.net.SocketFactory {

    /**
     * Creates a new socket.
     * @param proxy The proxy to use
     * @return The newly created socket
     */
    public abstract Socket createSocket(Proxy proxy);
}
