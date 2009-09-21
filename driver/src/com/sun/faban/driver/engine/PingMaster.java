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
package com.sun.faban.driver.engine;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import com.sun.faban.common.RegistryLocator;

/**
 * Ping the master, potentially waiting on a state to be achieved.
 * 
 * @author ncampbell
 */
public class PingMaster {
	 private Master master;

	    /**
	     * Constructs the MasterState object to query the master.
	     * @throws RemoteException A communications error occurred
	     */
	    private PingMaster() throws RemoteException {
	       getMaster();
	    }

	    private Master getMaster() throws RemoteException {
	        if (master == null) {
				try {
	                master = (Master) RegistryLocator.getRegistry().
	                        getService("Master");
	            } catch (NotBoundException e) {
	                // Do nothing. State will be NOT_STARTED.
	            }
			}
	        return master;
	    }

	    /**
	     * Obtains the current state of the master.
	     * @return The current state of the master.
         * @throws RemoteException A communications error occurred
	     */
	    MasterState getCurrentState() throws RemoteException {
	        if (getMaster() == null) {
				return MasterState.NOT_STARTED;
			}
	        return master.getCurrentState();
	    }

	    /**
	     * Wait for a certain state on the master.
	     * @param state The state to wait for
         * @throws RemoteException A communications error occurred
	     */
	    void waitForState(MasterState state) throws RemoteException {
	        if (state == MasterState.NOT_STARTED) {
				return;
			}

	        for (int i = 0; i < 120 && getMaster() == null; i++) {
				try { // Keep retry for around 1 minute.
	                Thread.sleep(500);
	            } catch (InterruptedException e) {
	                // Keep looping.
	            }
			}
	        master.waitForState(state);
	    }

	    /**
	     * Main method to enquire master state or wait for a master state.
	     * @param args Command line args
         * @throws RemoteException A communications error occurred
	     */
	    public static void main(String[] args) throws RemoteException {
	        PingMaster mState = new PingMaster();
	        if (args.length == 0) {
	            MasterState state = mState.getCurrentState();
	            System.out.println(state);
	        } else {
	        	try {
	        		MasterState state = Enum.valueOf(MasterState.class, args[0]);
	        		mState.waitForState(state);
	        	} catch (IllegalArgumentException e) {
					printUsage();
				} 
	        }
	    }
	    
	    private static void printUsage() {
	        System.err.println("java " + PingMaster.class.getName() +
	                           " <stateToWait>");
	        System.err.println("Prints current master state (no-args) or " +
	                           "waits for state");
	        System.err.print("stateToWait in:");
	        
	        for (MasterState s : MasterState.values()) {
	            System.err.print(' ');
	            System.err.print(s);
	        }
	        System.err.println(".");
	    }
}
