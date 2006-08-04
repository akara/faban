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
 * $Id: LogConfig.java,v 1.3 2006/08/04 07:37:53 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.logging;

import java.util.concurrent.ExecutorService;

/**
 * The log server configuration class.
 *
 * @author Akara Sucharitakul
 */
public class LogConfig {
    public int port = -1;
    public int listenerThreads = 1;
    public int listenQSize = -1;
    public int coreServiceThreads = 2;
    public int maxServiceThreads = 10;
    public int serviceThreadTimeout = 300; // timeout after 5 minutes.
    public PrimaryListener primaryListener = null;
    public ExecutorService threadPool = null;
    public int readBufferSize = 2048;
}
