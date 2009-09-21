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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The VariableLoadHandlerThread controls the active threads the driver applies
 * and holds the rest of the threads idle. It essentially controls the process
 * of the load variation.
 * @author Hubert Wong
 */
public class VariableLoadHandlerThread extends Thread {

	AgentImpl agent;

	private Logger logger;
	
	VariableLoadHandlerThread(AgentImpl agent) {
		this.agent = agent;
		this.logger = Logger.getLogger(getClass().getName());
	}
	
	public void run() {
		try {
			logger.log(Level.INFO, "Variable load controller started!");

            agent.timeSetLatch.await();

            // By now the time is set. Wake up at start of steady state.
            agent.loadSwitchTime = agent.startTime +
                            agent.runInfo.rampUp * 1000000000l;
            agent.timer.wakeupAt(agent.loadSwitchTime);


			while(agent.runInfo.variableLoadHandler.hasNext()) {
				VariableLoadHandler.VariableLoad load =
                        agent.runInfo.variableLoadHandler.next();
				agent.runningThreads = load.threadCount;
                agent.loadSwitchTime += load.runTime * 1000000000l;
                logger.log(Level.INFO, "Active threads: " + load.threadCount +
                                       " next " + load.runTime + " seconds.");
                agent.timer.wakeupAt(agent.loadSwitchTime);
			}
		} catch(InterruptedException e) {
			logger.log(Level.FINE, e.getMessage(), e);
		}
	}
	
}