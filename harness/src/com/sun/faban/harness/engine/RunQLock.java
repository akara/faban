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
 * $Id: RunQLock.java,v 1.2 2006/06/29 19:38:42 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import java.lang.*;
import java.util.logging.Logger;

/**
 * This class implements a monitor to provide mutually exclusive access to
 * critical sections. It provides two methods, one to grab a lock and the 
 * other to release the lock.
 *
 */
public class RunQLock {

    private boolean locked = false;
    private Logger logger;

    /**
     * Constructor
     *
     */
    public RunQLock() {
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
     * Method to grab the lock for mutually exclusive access while entering a
     * critical section.
     *
     */
    public synchronized void grabLock() {

        while (true) {
            if (!locked) {
                locked = true;
                return;
            }
            else {
                try {
                    logger.fine("Waiting");
                    wait();
                }
                catch (InterruptedException ie) {
                    logger.severe("RunQLock.grabLock : Thread interrupted");
                }
            }
        }
    }

    /**
     *  Method to release the lock while exit from the critical section.
     *
     */
    public synchronized void releaseLock() {
        locked = false;
        notify();
    }

}
