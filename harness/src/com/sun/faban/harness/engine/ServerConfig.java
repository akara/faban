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
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.util.*;
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
     * @return Whether the system configuration was successfully obtained.
     */
    public boolean get() {
        // Generate name of system log file - system.log
        String syslogfile = run.getOutDir() + "sysinfo.";
        String userCmds[];
        String[][] serverMachines;
        boolean success = true;

        List<String[]> enabledHosts;
        try {
            enabledHosts = par.getEnabledHosts();
        } catch (ConfigurationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }
        List<String> cmdList =
                        par.getParameters("fa:hostConfig/fh:userCommands");
        ArrayList<String> commands = new ArrayList<String>();

        if(enabledHosts.size() != cmdList.size()) {
            logger.severe("Number of hosts does not match Number of " +
                    "userCommands");
            return false;
        }
        int idx = 0;
        for (Iterator<String[]> iter = enabledHosts.iterator(); iter.hasNext();) {
            String[] hosts = iter.next();
            if (hosts.length > 0)
                commands.add(cmdList.get(idx++));
            else
                iter.remove();
        }

        serverMachines = enabledHosts.toArray(new String[1][1]);
        // Each category of hosts may have a user command to be executed.
        userCmds = commands.toArray((new String[1]));


        for(int j = 0; j < serverMachines.length; j++) {
            for (int i = 0; i < serverMachines[j].length; i++) {
                String machine = serverMachines[j][i];
                String machineName = cmds.getHostName(machine);
                try {
                    File f = new File(syslogfile + machineName + ".html");

                    // In case we have multiple interfaces, the file may
                    // already exist. We don't want to spend the time doing
                    // the same thing over and over again.
                    if (f.exists())
                        continue;

                    // Get system info
                    PrintStream syslog =
                                    new PrintStream(new FileOutputStream(f));
                    Command sysinfo = new Command("sysinfo");
                    sysinfo.setStreamHandling(Command.STDOUT, Command.CAPTURE);
                    CommandHandle handle = cmds.execute(machine, sysinfo, null);
                    byte[] info = handle.fetchOutput(Command.STDOUT);

                    // Write header and info to file.
                    syslog.println("<html><head><title>System Info for Server "
                            + machineName + "</title></head><body>");

                    syslog.write(info);

                    // Get User Commands output if specified
                    if (userCmds[j] != null && userCmds[j].trim().length() > 0) {
                        String[] cmdStrings = userCmds[j].split(";");
                        for (String cmdString : cmdStrings) {
                            Command c = new Command(cmdString);
                            c.setStreamHandling(Command.STDOUT, Command.CAPTURE);
                            handle = cmds.execute(machine, c, null);
                            info = handle.fetchOutput(Command.STDOUT);
                            if (info != null) {
                                syslog.println(linesep);
                                syslog.println("<h3>" + userCmds[j] +
                                        " on server " + machineName + "</h3>");
                                syslog.println("<pre>\n");
                                syslog.write(info);
                                syslog.println("\n</pre>");
                            }
                        }
                    }
                    syslog.println("</body></html>");
                    syslog.close();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to collect system info " +
                                            "for host " + machineName + '.', e);
                    success = false;
                }
            }
        }
        return success;
    }

    /**
     * Set system configuration.
     * Currently the only thing we set is the number of cpus.
     * @param cmds object to use
     * @return true/false depending on whether we were successful or not
     */
    public boolean set(CmdService cmds) {

        List<String[]> enabledHosts;
        try {
            enabledHosts = par.getEnabledHosts();
        } catch (ConfigurationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }

        List<String[]> CPUs  =
                            par.getTokenizedParameters("fa:hostConfig/fh:cpus");
        ArrayList<String[]> cpuVector = new ArrayList<String[]>();
        String[][] serverMachines = null;
        String[][] numCpus = null;

        if(enabledHosts.size() != CPUs.size()) {
            logger.severe("Number of hosts does not match Number of cpus");
            return false;
        }
        int idx = 0;
        for (Iterator<String[]> iter = enabledHosts.iterator(); iter.hasNext();) {
            String[] hosts = iter.next();
            if (hosts.length > 0)
                cpuVector.add(CPUs.get(idx++));
            else
                iter.remove();
        }

        serverMachines = enabledHosts.toArray(new String[1][1]);
        numCpus = cpuVector.toArray(new String[1][1]);

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
                if ((cpus == null) || ((cpus.length == 1) &&
                        ((cpus[0].trim().equals("")) ||
                        (cpus[0].trim().equals("0")))))
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
                    Command cmd = new Command(Config.BIN_DIR + "fastsu", "/usr/sbin/psradm", "-a", "-n");
                    logger.config("Turning on all cpus on " + machines[j]);
                    cmds.execute(machines[j], cmd, null);

                    cmd = new Command("/usr/sbin/psrinfo");
                    cmd.setStreamHandling(Command.STDOUT, Command.CAPTURE);
                    logger.fine("Getting cpus");
                    CommandHandle handle = cmds.execute(machines[j], cmd, null);
                    byte[] buffer = handle.fetchOutput(Command.STDOUT);

                    if (buffer != null) {
                        StringTokenizer t =
                                new StringTokenizer(new String(buffer), "\n");

                        ArrayList<Integer> cpuList = new ArrayList<Integer>();
                        boolean isMultiCore = false;

                        while (t.hasMoreTokens()) {
                            String line = t.nextToken();
                            // build list of cpus
                            Integer cpuId = Integer.valueOf((
                                    new StringTokenizer(line)).nextToken().
                                    trim());
                            if((isMultiCore == false) &&
                                    (cpuId.intValue() > 511))
                                isMultiCore = true;
                            cpuList.add(cpuId);
                        }
                        logger.info("Total number of CPUs is " +
                                cpuList.size()/(isMultiCore ? 2 : 1));

                        // The index gets changed when you remove elements
                        // Remove number of CPUs configured to be used for this
                        // server
                        for(int k = 0; k < numCPUs; k++) {
                            if(isMultiCore) {
                                Integer cpuId = new Integer(
                                        (cpuList.get(0)).intValue() + 512);
                                cpuList.remove(cpuId);
                            }
                            cpuList.remove(0);
                        }

                        logger.info("Number of CPUs turned off is " +
                                cpuList.size()/(isMultiCore ? 2 : 1));

                        // The remaining CPUs in the list have to be turned off.

                        if (cpuList.size() > 0) {
                            ArrayList<String> offlineCmd = new ArrayList<String>();
                            offlineCmd.add(Config.BIN_DIR + "fastsu");
                            offlineCmd.add("/usr/sbin/psradm");
                            offlineCmd.add("-f");

                            for(int k = 0; k < cpuList.size(); k++)
                                offlineCmd.add(cpuList.get(k).toString());

                            logger.info("Off-lining CPUs with command: " +
                                    offlineCmd);
                            cmd = new Command(offlineCmd);
                            cmds.execute(machines[j], cmd, null);
                        }
                    }
                    else {
                        logger.severe("Could not set CPUs on server " +
                                serverMachines[i]);
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
     * Get system logs for benchmark duration.
     * This method captures the relevant portion of var/adm/messages
     * to the system report file for the benchmark run.

     * @param startTime of benchmark run
     * @param endTime of benchmark run
     */
    public void report(long startTime, long endTime) {

        List<String[]> enabledHosts;
        try {
            enabledHosts = par.getEnabledHosts();
        } catch (ConfigurationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return;
        }

        // Remove empty entries identifying disabled host roles.
        for (Iterator<String[]> iter = enabledHosts.iterator(); iter.hasNext();) {
            String[] hosts = iter.next();
            if (hosts.length == 0)
                iter.remove();
        }
        
        String sysfile = run.getOutDir() + "system.report";
        PrintStream syslog = null;
        DateFormat df = DateFormat.getDateTimeInstance(
                            DateFormat.MEDIUM, DateFormat.LONG, Locale.US);
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
        CommandHandle handle;
        Command c = new Command("messages", "\"" +
                startMon + " " + startDay + " " + stime + "\"",
                "\""  + endMon + " " + endDay + " " + etime + "\"");
        c.setStreamHandling(Command.STDOUT, Command.CAPTURE);
        logger.fine("Getting system messages");

        for (String[] machines : enabledHosts)
            for(String machine : machines) {
                File f = new File(sysfile + "." + machine);
                f.delete();
                try {
                    syslog = new PrintStream(new FileOutputStream(f));
                    handle = cmds.execute(machine, c, null);
                    byte[] messages = handle.fetchOutput(Command.STDOUT);
                    syslog.println(linesep);
                    syslog.println("System messages during run from server " +
                            machine);
                    syslog.println("\n");
                    if (messages != null) // Null if no messages.
                        syslog.write(messages);
                    syslog.println("\n");
                } catch (RemoteException e) {
                    Throwable cause = e.getCause();
                    while (cause != null)
                        cause = cause.getCause();
                    String message = "Error processing system messages for " +
                                                                    machine;
                    // A remote IOException usually means the messages script
                    // is not available for the target OS. We want to log
                    // at a lower level.
                    if (cause instanceof IOException)
                        logger.log(Level.FINE, message, cause);
                    else
                        logger.log(Level.WARNING, message, cause);
                } catch (Exception e) {
                    logger.log(Level.WARNING,
                            "Error collecting system messages from " + machine,
                            e);
                }
            }
    }
}
