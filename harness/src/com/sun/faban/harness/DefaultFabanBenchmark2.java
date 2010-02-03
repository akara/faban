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
package com.sun.faban.harness;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import static com.sun.faban.harness.RunContext.*;
import org.w3c.dom.Element;

import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The default benchmark class(based on annotations) for use with benchmarks
 * implemented with the Faban Driver Framework. This class is designed to be
 * extended if additional features are desired. Do not use the
 * DefaultFabanBenchmark2 if the actual driver is not implemented using the
 * Faban Driver Framework.
 *
 * @author Akara Sucharitakul
 */
public class DefaultFabanBenchmark2 {

    private Logger logger = Logger.getLogger(getClass().getName());

    /** The param repository. */
    protected ParamRepository params;

    /** The agent list. */
    protected List<String> agents;

    /** The agent hosts. */
    protected String[] agentHosts;

    /** The map from host to agents. */
    protected Map<String, List<String>> hostAgents;

    /** Environment to pass to agents when starting. */
    protected Map<String, List<String>> agentEnv;

    /** Command handle to the master. */
    protected CommandHandle masterHandle;
    
    /**
     * Allows benchmark to validate the configuration file. Note that no
     * execution facility is available during validation.
     *
     * @throws Exception if any error occurred.
     * @see RunContext#exec(com.sun.faban.common.Command)
     */
    @Validate public void validate() throws Exception {
        params = getParamRepository();

        // Check and match the hosts and agents
        // First, list the drivers in the config file.
        agents = params.getAttributeValues(
                                    "fa:runConfig/fd:driverConfig", "name");

        // Second, obtain the systems to run the drivers.
        agentHosts = params.getTokenizedValue(
                                        "fa:runConfig/fa:hostConfig/fa:host");

        // A blank means this master system
        if (agentHosts == null || agentHosts.length == 0) {
            agentHosts = new String[1];
            agentHosts[0] = InetAddress.getLocalHost().getHostName();
        }

        // Convert "localhost" to master system name
        InetAddress localhost = InetAddress.getByName(null);
        for (int i = 0; i < agentHosts.length; i++) {
            if ("localhost".equals(agentHosts[i]) ||
                    localhost.equals(InetAddress.getByName(agentHosts[i])))
                agentHosts[i] = InetAddress.getLocalHost().getHostName();
        }

        hostAgents = new HashMap<String, List<String>>(agentHosts.length + 5);

        agentEnv = new HashMap<String, List<String>>();

        HashMap<String, Integer> anyHostAgents = new HashMap<String, Integer>();
        for (String agentName : agents) {

            String qb = "fa:runConfig/fd:driverConfig[@name=\"" + agentName +
                         "\"]/";

            // Obtain the environment needed for the agent type...
            List<String> env = params.getParameters(qb + "fd:environment");
            if (env != null && env.size() > 0) {
                agentEnv.put(agentName, env);
                if (logger.isLoggable(Level.FINER)) {
                    StringBuilder b = new StringBuilder();
                    b.append("Env for ").append(agentName).append("Agents: ");
                    for (String envEntry : env) {
                        b.append('[').append(envEntry).append(']');
                    }
                    logger.finer(b.toString());
                }
            }

            // Prepare the agent distribution...
            String[] agentSpecs = params.getTokenizedValue(qb + "fd:agents");

            switch (agentSpecs.length) {
                case 0: // Default to 1 agent
                    String[] defaultAgentSpecs = { "1" };
                    agentSpecs = defaultAgentSpecs; // fall thru to case 1.
                case 1: // Single value field, could be just count or host:count
                    if (agentSpecs[0].indexOf(':') < 0) { //Just count
                        int agentCnt = 0;
                        try {
                            agentCnt = Integer.parseInt(agentSpecs[0]);
                        } catch (NumberFormatException e) {
                            String msg = "Invalid agents spec " + agentSpecs[0];
                            ConfigurationException ce =
                                    new ConfigurationException(msg);
                            logger.log(Level.SEVERE, msg, ce);
                            throw ce;
                        }
                        // Record the count for this agent.
                        anyHostAgents.put(agentName, agentCnt);
                        break;
                    }
                default: // One or more host:count
                    for (String agentSpec : agentSpecs) {
                        int colIdx = agentSpec.indexOf(':');

                        // Check the spec for anything odd.
                        if (colIdx < 1) {
                            String msg = "Invalid agents spec " + agentSpec;
                            ConfigurationException ce =
                                    new ConfigurationException(msg);
                            logger.log(Level.SEVERE, msg, ce);
                            throw ce;
                        }
                        String hostName = agentSpec.substring(0, colIdx);
                        int agentCnt;
                        try {
                            agentCnt = Integer.parseInt(
                                            agentSpec.substring(colIdx + 1));
                        } catch (NumberFormatException e) {
                            String msg = "Invalid agents spec " + agentSpec;
                            ConfigurationException ce =
                                    new ConfigurationException(msg);
                            logger.log(Level.SEVERE, msg, ce);
                            throw ce;
                        }
                        // Add one entry to the list for each agent.
                        List<String> agentList = null;
                        for (int i = 0; i < agentCnt; i++) {
                            if (agentList == null)
                                agentList = hostAgents.get(hostName);
                            if (agentList == null) {
                                agentList = new ArrayList<String>();
                                hostAgents.put(hostName, agentList);
                            }
                            agentList.add(agentName);
                        }
                    }

            }
        }

        // After we got the host specifics done, we'll need to take care of
        // distributing the anyhost agents fairly.
        if (anyHostAgents.size() > 0) {

            // Ensure all hosts are in hostAgents.
            for (String hostName : agentHosts)
                if (hostAgents.get(hostName) == null)
                    hostAgents.put(hostName, new ArrayList<String>());

            String previousHost = null;
            for (Map.Entry<String, Integer> anyHostEntry :
                                                    anyHostAgents.entrySet()) {
                String agentName = anyHostEntry.getKey();
                int cnt = anyHostEntry.getValue();
                Set<Map.Entry<String, List<String>>> hostAgentsSet =
                                                        hostAgents.entrySet();

                // Find the host with minimum agent for each anyHostAgent
                for (int i = 0; i < cnt; i++) {
                    String leastBusyHost = null;
                    int minAgents = Integer.MAX_VALUE;
                    // Scan the hostAgents to find the least busy.
                    for (Map.Entry<String, List<String>> hostAgentsEntry :
                                                               hostAgentsSet) {
                        int agents = hostAgentsEntry.getValue().size();
                        if (agents < minAgents) {
                            leastBusyHost = hostAgentsEntry.getKey();
                            minAgents = agents;
                        } else if (agents == minAgents) {
                            // If there is more than one least busy, pick
                            // the first one that was not previously assigned.
                            if (leastBusyHost.equals(previousHost))
                                leastBusyHost = hostAgentsEntry.getKey();
                        }
                    }
                    hostAgents.get(leastBusyHost).add(agentName);
                    previousHost = leastBusyHost;
                }
            }
        }

        // In any case, there is a chance that some hosts are not used.
        // we just remove those hosts from hostAgents.
        Set<Map.Entry<String, List<String>>> hostAgentsSet =
                                                hostAgents.entrySet();
        for (Map.Entry<String, List<String>> hostAgentsEntry : hostAgentsSet)
            if (hostAgentsEntry.getValue().size() == 0)
                hostAgents.remove(hostAgentsEntry);

        // Then we need to make sure our agentHosts strings are accurate.

        // We want to keep the order the hosts were entered. So we use
        // a LinkedHashSet and insert every host in here.
        LinkedHashSet<String> agentHostSet = new LinkedHashSet<String>();
        for (String agentHost : agentHosts)
            agentHostSet.add(agentHost);

        Set<String> hostAgentsKeySet = hostAgents.keySet();

        // Now we remove all the hosts that are not used...
        agentHostSet.retainAll(hostAgentsKeySet);

        // and add what is used and not listed, if any.
        agentHostSet.addAll(hostAgentsKeySet);

        // Put it back into the array for later use.
        agentHosts = agentHostSet.toArray(new String[agentHostSet.size()]);

        // Recreate the string from the array.
        StringBuilder agentHostsBldr = new StringBuilder();
        for (int i = 0; i < agentHosts.length; i++) {
            if (i > 0)
                agentHostsBldr.append(' ');
            agentHostsBldr.append(agentHosts[i]);
        }

        try {
            // Save the new host back the list.
            params.setParameter("fa:runConfig/fa:hostConfig/fa:host",
                                                agentHostsBldr.toString());

            // Update the output directory to the one assigned by the harness.
            Element outputDirNode = (Element)                                      
                    params.getNode("fa:runConfig/fd:outputDir");

            if (outputDirNode == null)
                outputDirNode = params.addParameter("fa:runConfig",
                        "http://faban.sunsource.net/ns/fabandriver", null,
                        "outputDir");

            params.setParameter(outputDirNode, getOutDir());

            params.save();
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Exception updating " + getParamFile(), e);
            throw e;
        }
    }

    /**
     * This method is called to configure the specific benchmark run
     * Tasks done in this method include reading user parameters,
     * logging them and initializing various local variables.
     *
     * @throws Exception if any error occurred.
     */
    //public void configure() throws Exception {
        // No configuration needed.
    //}

    /**
     * This method is responsible for starting the benchmark run.
     *
     * @throws Exception if any error occurred.
     */
    @StartRun public void start() throws Exception {

        // Spread the drivers to the systems.
        // Start Agents. Other JVM Options like security policy and logging
        // properties are added by CmdAgent when starting the java command.
        Command c = null;

        HashMap<String, Integer> agentIds =
                                new HashMap<String, Integer>(agents.size());

        // Iterate through all the hosts before coming back for the next set
        // of agents for the first host. Once we're though all agent sets
        // for all hosts, the agentStarted flag will be false and we'll exit
        // the loop.
        boolean agentStarted = true;
        for (int i = 0; agentStarted; i++) {
            agentStarted = false;
            for (String hostName : agentHosts) {
                List<String> agentList = hostAgents.get(hostName);
                if (i >= agentList.size())
                    continue;
                String agentType = agentList.get(i);
                Integer oldAgentId = agentIds.get(agentType);
                int agentId;
                if (oldAgentId == null) {
                    agentIds.put(agentType, 0);
                    agentId = 0;
                } else {
                    agentId = oldAgentId.intValue() + 1;
                    agentIds.put(agentType, agentId);
                }
                logger.info("Starting " + agentType + "Agent[" + agentId +
                        "] on host " + hostName + '.');

                String masterIP = getMasterIP(hostName);
                if (masterIP == null) {
                    masterIP = getMasterIP();
                }

                Command agent = new Command("com.sun.faban.driver.engine." +
                        "AgentImpl", agentType, String.valueOf(agentId),
                        masterIP);

                List<String> env = agentEnv.get(agentType);
                if (env != null) {
                    String[] e = new String[env.size()];
                    e = env.toArray(e);
                    agent.setEnvironment(e);
                }

                agent.setSynchronous(false);
                java(hostName, agent);
                agentStarted = true;
                //Wait for the Agents to register
                try {
                    Thread.sleep(500);
                } catch(InterruptedException e) {
                    logger.severe("Exception Sleeping : " + e);
                    logger.log(Level.FINE, "Exception", e);
                }
            }
        }

        //Wait for all the Agents to register
        try {
            Thread.sleep(5000);
        } catch(InterruptedException e) {
            logger.severe("Exception Sleeping : " + e);
            logger.log(Level.FINE, "Exception", e);
        }

        // Start the master
        c = new Command("-Dbenchmark.config=" + getParamFile(),
                "-Dfaban.outputdir.unique=true",
                "com.sun.faban.driver.engine.MasterImpl");
        c.setSynchronous(false);

        masterHandle = java(c);

        // Wait until the master gets to rampup before we give back control.
        // This will ensure the tools start at correct times.
        java(new Command("com.sun.faban.driver.engine.PingMaster", "RAMPUP"));
        logger.info("Ramp up started");
    }

    /**
     * This method is responsible for waiting for all commands started and
     * run all postprocessing needed.
     *
     * @throws Exception if any error occurred.
     */
    @EndRun public void end() throws Exception {

        // Wait for the master to complete the run.
        masterHandle.waitFor();
        int exitValue = masterHandle.exitValue();
        if (exitValue != 0) {
            logger.severe("Master terminated with exit value " + exitValue);
            throw new Exception("Driver failed to complete benchmark run");
        }
    }

    /**
     * This method aborts the current benchmark run and is
     * called when a user asks for a run to be killed
     *
     * @throws Exception if any error occurred.
     */
    //public void kill() throws Exception {
        // We don't need to kill off anything here. All processes managed
        // by the run context are automatically terminated.
    //}
}