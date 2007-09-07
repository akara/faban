package com.sun.faban.driver.core;

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
	     * @throws RemoteException 
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
	     */
	    MasterState getCurrentState() throws RemoteException {
	        if (getMaster() == null) {
				return MasterState.NOT_STARTED;
			}
	        return master.getCurrentState();
	    }

	    /**
	     * Wait for a certain state on the master.
	     * @param state
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
	     * @throws RemoteException 
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
