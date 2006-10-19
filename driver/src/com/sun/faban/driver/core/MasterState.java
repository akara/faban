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
 * $Id: MasterState.java,v 1.1 2006/10/19 18:48:19 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

import com.sun.faban.common.RegistryLocator;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * MasterState allows querying the state of the master.
 *
 * @author Akara Sucharitakul
 */
public class MasterState {

    public static final int NOT_STARTED = 0;
    public static final int CONFIGURING = 1;
    public static final int STARTING = 2;
    public static final int RAMPUP = 3;
    public static final int STEADYSTATE = 4;
    public static final int RAMPDOWN = 5;
    public static final int RESULTS = 6;
    public static final int ABORTED = 7;

    public static final String[] states = { "NOT_STARTED",
                                            "CONFIGURING",
                                            "STARTING",
                                            "RAMPUP",
                                            "STEADYSTATE",
                                            "RAMPDOWN",
                                            "RESULTS",
                                            "ABORTED"};

    private Master master;

    /**
     * Constructs the MasterState object to query the master.
     */
    public MasterState() throws RemoteException {
        getMaster();
    }

    private Master getMaster() throws RemoteException {
        if (master == null)
            try {
                master = (Master) RegistryLocator.getRegistry().
                        getService("Master");
            } catch (NotBoundException e) {
                // Do nothing. State will be NOT_STARTED.
            }
        return master;
    }

    /**
     * Obtains the current state of the master.
     * @return The current state of the master.
     */
    int getCurrentState() throws RemoteException {
        if (getMaster() == null)
            return NOT_STARTED;
        return master.getCurrentState();
    }

    /**
     * Wait for a certain state on the master.
     * @param state
     */
    void waitForState(int state) throws RemoteException {
        if (state == NOT_STARTED)
            return;

        for (int i = 0; i < 120 && getMaster() == null; i++)
            try { // Keep retry for around 1 minute.
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Keep looping.
            }
        master.waitForState(state);
    }

    /**
     * Main method to enquire master state or wait for a master state.
     * @param args Command line args
     */
    public static void main(String[] args) throws RemoteException {
        MasterState mState = new MasterState();
        if (args.length == 0) {
            int state = mState.getCurrentState();
            System.out.println(states[state]);
        } else {
            int i;
            for (i = 0; i < states.length; i++)  // Sequential search.
                if (states[i].equalsIgnoreCase(args[0]))
                    break;
            if (i >= states.length) // Not found.
                printUsage();
            else
                mState.waitForState(i);
        }
    }

    private static void printUsage() {
        System.err.println("java " + MasterState.class.getName() +
                           " <stateToWait>");
        System.err.println("Prints current master state (no-args) or " +
                           "waits for state");
        System.err.print("stateToWait in:");
        for (int i = 0; i < states.length; i++) {
            System.err.print(' ');
            System.err.print(states[i]);
        }
        System.err.println(".");
    }
}