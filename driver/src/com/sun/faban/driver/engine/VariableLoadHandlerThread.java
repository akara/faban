package com.sun.faban.driver.engine;

import java.util.logging.Level;
import java.util.logging.Logger;

public class VariableLoadHandlerThread extends Thread {
	
	public AgentImpl agent;
	public Logger logger;
	
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