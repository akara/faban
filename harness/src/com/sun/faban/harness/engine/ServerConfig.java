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
 * $Id: ServerConfig.java,v 1.3 2006/07/27 22:34:35 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;

import java.io.*;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class: ServerConfig.java
 * This class manages the configuration of the server machines.
 * It provides methods to capture system configuration, change
 * the number of processors, get system log messages during a
 * benchmark run, etc.
 * WARNING: It is specific to Solaris and non-portable.
 *
 * @see GenericBenchmark
 * @author Ramesh Ramachandran
 */
class ServerConfig {
    Run run;
    ParamRepository par;
    static String linesep = "<hr>";
    String master;
    CmdService cmds;
    Logger logger;

    public ServerConfig(Run r, ParamRepository par) {
        this.par = par;
        run = r;
        cmds = CmdService.getHandle();
        logger = Logger.getLogger(this.getClass().getName());
        master = CmdService.getHandle().getMaster();
    }

    /**
     * Get system configuration
     * This method retrieves the various system parameters from the
     * ParamRepository.
     * It gather /etc/system, prtdiag, psrinfo, uname, ps , vxprint
     * info from the server machines and logs them to the system log.
     */
    public boolean get() {
        // Generate name of system log file - system.log
        String syslogfile = run.getOutDir() + "sysinfo.";
        String userCmds[];
        String[][] serverMachines;
        try {
            ArrayList enabledHosts = new ArrayList();
            List hosts = par.getTokenizedParameters("hostConfig/host");
            List enabled = par.getParameters("hostConfig/enabled");
            List cmdList = par.getParameters("hostConfig/userCommands");
            ArrayList commands = new ArrayList();
            if(hosts.size() != enabled.size()) {
                logger.severe("Number of hosts does not match Number of " +
                        "enabled node");
                return false;
            }
            else {
                if(hosts.size() != cmdList.size()) {
                    logger.severe("Number of hosts does not match Number of " +
                            "userCommands");
                    return false;
                }
                for(int i = 0; i < hosts.size(); i++) {
                    if(Boolean.valueOf((String)enabled.get(i)).booleanValue()) {
                        enabledHosts.add(hosts.get(i));
                        commands.add(cmdList.get(i));
                    }
                }
                serverMachines = (String[][]) enabledHosts.toArray(new String[1][1]);
                // Each category of hosts may have a user command to be executed.
                userCmds = (String[]) commands.toArray((new String[1]));
            }


            for(int j = 0; j < serverMachines.length; j++) {
                for (int i = 0; i < serverMachines[j].length; i++) {
                    String machine = serverMachines[j][i];
                    String machineName = cmds.getHostName(machine);
                    File f = new File(syslogfile + machineName + ".html");

                    // In case we have multiple interfaces, the file may
                    // already exist. We don't want to spend the time doing
                    // the same thing over and over again.
                    if (f.exists())
                        continue;

                    // Get system info
                    PrintStream syslog = new PrintStream(new FileOutputStream(f));
                    Command sysinfo = new Command("sysinfo");
                    sysinfo.setStreamHandling(Command.STDOUT, Command.CAPTURE);
                    CommandHandle handle = cmds.execute(machine, sysinfo);
                    byte[] info = handle.fetchOutput(Command.STDOUT);

                    // Write header and info to file.
                    syslog.println("<html><head><title>System Info for Server "
                                   + machineName + "</title></head><body>");

                    syslog.write(info);

                    // Get User Commands output if specified
                    if (userCmds[j] != null && userCmds[j].trim().length() > 0) {
                        Command c = new Command(userCmds[j]);
                        c.setStreamHandling(Command.STDOUT, Command.CAPTURE);
                        handle = cmds.execute(machine, c);
                        info = handle.fetchOutput(Command.STDOUT);
                        syslog.println(linesep);
                        syslog.println("<h3>" + userCmds[j] + " on server " +
                                       machineName + "</h3>");
                        syslog.println("<pre>\n");
                        syslog.write(info);
                        syslog.println("\n</pre>");
                    }
                    syslog.println("</body></html>");
                    syslog.close();
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to collect system info.", e);
            return(false);
        }
        return(true);
    }

    /**
     * Set system configuration
     * Currently the only thing we set is the number of cpus
     * @param cmds object to use
     * @return true/false depending on whether we were successful or not
     */
    public boolean set(CmdService cmds) {
        List CPUs  = par.getTokenizedParameters("hostConfig/cpus");
        ArrayList enabledHosts = new ArrayList();
        List hosts = par.getTokenizedParameters("hostConfig/host");
        List enabled = par.getParameters("hostConfig/enabled");
        ArrayList cpuVector = new ArrayList();
        String[][] serverMachines = null;
        String[][] numCpus = null;

        if(hosts.size() != enabled.size()) {
            logger.severe("Number of hosts does not match Number of enabled node");
            return false;
        }
        else {
            if(hosts.size() != CPUs.size()) {
                logger.severe("Number of hosts does not match Number of cpus");
                return false;
            }
            for(int i = 0; i < hosts.size(); i++) {
                if(Boolean.valueOf((String)enabled.get(i)).booleanValue()) {
                    enabledHosts.add(hosts.get(i));
                    cpuVector.add(CPUs.get(i));
                }
                serverMachines = (String[][]) enabledHosts.toArray(new String[1][1]);
                numCpus = (String[][]) cpuVector.toArray(new String[1][1]);

            }
        }

        if(serverMachines.length != numCpus.length) {
            logger.severe("serverMachines.length != numCPUs.length");
            return false;
        }

        for(int i = 0; i < serverMachines.length; i++) {
            String[] machines = serverMachines[i];
            String[] cpus = numCpus[i];
            int numCPUs = 0;

            for(int j = 0; j < machines.length; j++) {
                // If the CPU is set to null, Null String or 0 don't do anything.
                if((cpus == null) || ((cpus.length == 1) && ((cpus[0].trim().equals("")) || (cpus[0].trim().equals("0")))))
                    break;
                else {
                    if(cpus.length == 1)
                        numCPUs = Integer.parseInt(cpus[0]);
                    else
                        numCPUs = Integer.parseInt(cpus[j]);
                }

                // User don't want to reconfigure this system.
                if(numCPUs == 0)
                    continue;

                try {
                    // We first turn on all cpus, then turn off enough of them
                    // to get the required number
                    String buf = Config.BIN_DIR + "fastsu /usr/sbin/psradm -a -n";
                    logger.config("Turning on all cpus on " + machines[j]);
                    cmds.start(machines[j], buf, CmdService.SEQUENTIAL, Config.DEFAULT_PRIORITY);

                    buf = "/usr/sbin/psrinfo > /tmp/sys.out";
                    logger.fine("Getting cpus");
                    cmds.start(machines[j], buf, CmdService.SEQUENTIAL, Config.DEFAULT_PRIORITY);

                    if (cmds.copy(machines[j], master, "/tmp/sys.out", "/tmp/sys.out", false)) {
                        BufferedReader in = new BufferedReader(new FileReader("/tmp/sys.out"));

                        ArrayList cpuList = new ArrayList();
                        boolean isMultiCore = false;

                        while ((buf = in.readLine()) != null) {
                            // build list of cpus
                            Integer cpuId = Integer.valueOf((new StringTokenizer(buf)).nextToken().trim());
                            if((isMultiCore == false) && (cpuId.intValue() > 511))
                                isMultiCore = true;
                            cpuList.add(cpuId);
                        }
                        logger.info("Total number of CPUs is " + cpuList.size()/(isMultiCore ? 2 : 1));

                        // The index gets changed when you remove elements
                        // Remove number of CPUs configured to be used for this server
                        for(int k = 0; k < numCPUs; k++) {
                            if(isMultiCore) {
                                Integer cpuId = new Integer(((Integer)cpuList.get(0)).intValue() + 512);
                                cpuList.remove(cpuId);
                            }
                            cpuList.remove(0);
                        }

                        logger.info("Number of CPUs turned off is " + cpuList.size()/(isMultiCore ? 2 : 1));

                        // The remaining CPUs in the list have to be turned off.
                        if (cpuList.size() > 0) {
                            StringBuffer offline = new StringBuffer(" ");
                            for(int k = 0; k < cpuList.size(); k++)
                                offline.append(cpuList.get(k).toString()).append(" ");

                            logger.info("Off-lining CPUs " + offline);
                            String cmd = Config.BIN_DIR + "fastsu /usr/sbin/psradm -f " + offline;
                            cmds.start(machines[j], cmd,
                                    CmdService.SEQUENTIAL, Config.HIGHER_PRIORITY);
                        }
                    }
                    else {
                        logger.severe("Could not set CPUs on server " + serverMachines[i]);
                    }
                } catch (Exception ie) {
                    logger.log(Level.SEVERE, "Failed to set Server Config.", ie);
                    return(false);
                }
            }
        }
        return(true);
    }

    /**
     * Get system logs for benchmark duration
     * This method captures the relevant portion of var/adm/messages
     * to the system report file for the benchmark run.

     * @param startTime of benchmark run
     * @param endTime of benchmark run
     */
    public void report(long startTime, long endTime) {
        String[][] serverMachines = null;
        ArrayList enabledHosts = new ArrayList();
        List hosts = par.getTokenizedParameters("hostConfig/host");
        List enabled = par.getParameters("hostConfig/enabled");
        if(hosts.size() != enabled.size()) {
            logger.severe("Number of hosts does not match Number of enabled node");
            return;
        }
        else {
            for(int i = 0; i < hosts.size(); i++) {
                if(Boolean.valueOf((String)enabled.get(i)).booleanValue()) {
                    enabledHosts.add(hosts.get(i));
                }
            }
            serverMachines = (String[][]) enabledHosts.toArray(new String[1][1]);
        }

        String sysfile = run.getOutDir() + "system.report";
        PrintStream syslog = null;
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG);
        String start = df.format(new Date(startTime));
        String end = df.format(new Date(endTime));

        String startMon = start.substring(0, 3);
        int ind = start.indexOf(',');
        String startDay = start.substring(4, ind);
        ind = ind + 7; // skip over ' 1999 ' to get to time
        String stime = start.substring(ind, start.indexOf(' ', ind));
        logger.fine("Run started Month = " + startMon +
                " Day = " + startDay + " Time = " + stime);

        String endMon = end.substring(0, 3);
        ind = end.indexOf(',');
        String endDay = end.substring(4, ind);
        ind = ind + 7; // skip over ' 1999 ' to get to time
        String etime = end.substring(ind, end.indexOf(' ', ind));
        logger.fine("Run ended Month = " + endMon
                + " Day = " + endDay + " Time = " + etime);

        // Now, get /var/adm/messages and look for messages between
        // start and end
        try {
            CommandHandle handle;
            Command c = new Command("messages \"" +
                    startMon + " " + startDay + " " + stime + "\" \""  +
                    endMon + " " + endDay + " " + etime + "\"");
            c.setStreamHandling(Command.STDOUT, Command.CAPTURE);
            logger.fine("Getting system messages");

            for (int i = 0; i < serverMachines.length; i++)
                for(int j = 0; j < serverMachines[i].length; j++) {
                    String machine = serverMachines[i][j];
                    File f = new File(sysfile + "." + machine);
                    f.delete();
                    syslog = new PrintStream(new FileOutputStream(f));
                    handle = cmds.execute(machine, c);
                    byte[] messages = handle.fetchOutput(Command.STDOUT);
                    syslog.println(linesep);
                    syslog.println("System messages during run from server " + machine);
                    syslog.println("\n");
                    if (messages != null) // Null if no messages.
                        syslog.write(messages);
                    syslog.println("\n");
                }
        } catch (Exception ie) {
            logger.log(Level.SEVERE, "Reporting failed!", ie);
        }
    }
}
