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
 * $Id: VariableLoadHandlerThread.java,v 1.3 2009/07/28 22:53:31 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
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
			logger.log(Level.INFO, "Variable load controller thread started!");
			/*
			long currentTime = agent.timer.getTime();
			long timeDifference = agent.runInfo.benchStartTime - currentTime;
			logger.info("Current time: " + currentTime + " runInfo.start: " + agent.runInfo.start + "benchStartTime: " + agent.runInfo.benchStartTime);
			if(timeDifference >= 0) {
				logger.info("Run hasn't started yet! Variable load controller sleeping for " + timeDifference + " milliseconds.");
				sleep(timeDifference);
			}
			*/
			while(agent.runInfo.variableLoadHandler.hasNext()) {
				VariableLoadHandler.VariableLoad load = agent.runInfo.variableLoadHandler.next();
				logger.log(Level.INFO, "Variable load controller thread advanced!");
				agent.timeToRunFor = load.runTime;
				agent.runningThreads = load.threadCount;
				logger.log(Level.INFO, "Variable load controller thread sleeping for " + load.runTime + " seconds. Setting for " + load.threadCount + " active threads.");
				sleep(load.runTime * 1000);
			}
		} catch(InterruptedException e) {
			logger.log(Level.FINE, e.getMessage(), e);
		}
	}
	
}