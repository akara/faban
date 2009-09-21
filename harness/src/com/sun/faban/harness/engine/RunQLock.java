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
package com.sun.faban.harness.engine;

import java.lang.*;
import java.util.logging.Logger;

/**
 * The RunQLock implements monitors to provide mutually exclusive access to
 * critical sections. One lock provides run queue access locking for submitting
 * and fetching runs. The other lock is a wait lock to wait for new run
 * arrivals.
 */
public class RunQLock {

    private Object queueLock = new Object();
    private Object waitLock = new Object();
    private boolean locked = false;
    private Logger logger;

    /**
     * Constructor.
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
    public void grabLock() {

        synchronized (queueLock) {
            while (true) {
                if (!locked) {
                    locked = true;
                    return;
                }
                else {
                    try {
                        logger.fine("Waiting");
                        queueLock.wait();
                    }
                    catch (InterruptedException ie) {
                        logger.severe("RunQLock.grabLock : Thread interrupted");
                    }
                }
            }
        }
    }

    /**
     *  Method to release the lock while exit from the critical section.
     *
     */
    public void releaseLock() {
        synchronized (queueLock) {
            if (locked) {
                locked = false;
                queueLock.notify();
            }
        }
    }

    /**
     * Signals that a run is submitted.
     */
    public void signal() {
        synchronized (waitLock) {
            waitLock.notify();
        }
    }

    /**
     * Sleeps for the given time, or until a new run is submitted.
     * @param sleep The max time to sleep, if nothing is submitted.
     */
    public void waitForSignal(long sleep) {
        synchronized (waitLock) {
            try {
                waitLock.wait(sleep);
            } catch (InterruptedException e) {
            }
        }
    }
}
