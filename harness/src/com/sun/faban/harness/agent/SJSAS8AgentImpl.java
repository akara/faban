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
 * $Id: SJSAS8AgentImpl.java,v 1.4 2007/04/27 21:33:26 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.util.FileHelper;
import com.sun.faban.harness.util.XMLEditor;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This is SJSAS8Agent class to performance the SJSAS8Service remotely on
 * the server machine(s)
 *
 * @author Ramesh Ramachandran
 */
public class SJSAS8AgentImpl extends UnicastRemoteObject implements SJSAS8Agent, Unreferenced {

    private static final String[] SJSAS8_CONFIG_FILES = {"config/domain.xml"};
    private static final String SJSAS8_START = "bin/startserv";
    private static final String SJSAS8_STOP = "bin/stopserv";
    private static final String SSL_PASSWD_FILE = Config.TMP_DIR + "sslPasswd";


    private String host;
    private String outDir;
    private CmdAgent cmdAgent;

    private String[] myInstances = null;
    private String[] myLogs = null;
    private boolean profileRun = false;
    private String runId = null;
    private boolean  logGC = false;

    private boolean sslRun = false;

    private Logger logger;

    public SJSAS8AgentImpl() throws RemoteException {
        super();
        logger = Logger.getLogger(this.getClass().getName());
        host = CmdAgentImpl.getHost();
    }


    /**
      * This method set the configure parameters of the SJSAS8 instance on this
      * machine identified by the serverID
      * @param instanceHomes - the full pathname of the instances
      * @param sunParams - Parameters passed as properties
      */
    public void setConfig(String[] instanceHomes, Properties sunParams)
            throws RemoteException {
        String [] instances = null;
        String [] logs = null;

        if(instanceHomes != null) {
            instances = instanceHomes;
            logs = new String[instanceHomes.length];
            for(int i = 0; i < instanceHomes.length; i++) {
                for(int j = 0; i < myInstances.length; j++) {
                    if(instanceHomes[i].equals(myInstances[j])) {
                        logs[i] = myLogs[j];
                        break;
                    }
                }
            }
        }
        else { // ALL instances in the setup.
            instances = myInstances;
            logs = myLogs;
        }

        for(int i = 0; i < instances.length; i++) {
            try
            {
                String configXML = instances[i] + File.separator +
                                   SJSAS8_CONFIG_FILES[0];

                // Modify the config.xml of instances[i] using sunParams
                XMLEditor editor = new XMLEditor();
                editor.open(configXML);
                logger.config("Opening Config " + configXML);

                // delete -Xruncollector if any
                editor.delete(null, "\t", "\tdomain\tconfigs\tconfig\tjava-config\tjvm-options\t-Xruncollector");

                Enumeration en = sunParams.propertyNames();
                while (en.hasMoreElements()) {
                    String param = (String) en.nextElement();
                    String element = null;
                    if(param.equals("java-home") || (param.equals( "jvm-options")))
                            element = "\tdomain\tconfigs\tconfig\tjava-config\t" + param;
                    else        // Not a property in Config XML
                            element = param;

                    // Check if the -Xloggc is specified  then append it with a log file name
                    if(sunParams.getProperty(param).indexOf("Xloggc") != -1) {
                        // The log file will be named Config.TMP_DIR + gc.log.<instance>
                        logGC = true;
                        String gcLogFile = Config.TMP_DIR + "gc.log." + new File(instances[i]).getName();
                        String value = sunParams.getProperty(param);
                        value = value.replaceAll("Xloggc", "Xloggc" + ":" + gcLogFile);
                        sunParams.setProperty(param, value);
                    }

                    if(element != null)
                        editor.replace(null, "\t",  "\t" + element + "\t" + sunParams.getProperty(param));

                    logger.config("Updating Config : \t" + element + "\t" + sunParams.getProperty(param));

                    if(sunParams.getProperty(param).indexOf("Xruncollector") != -1)
                        profileRun = true;

                    String sslPasswd = sunParams.getProperty("ssl-passwd");
                    if (sslPasswd != null) {
                        sslRun = true;
                        try {
                            BufferedWriter out = new BufferedWriter(new FileWriter(SSL_PASSWD_FILE));
                            out.write(sslPasswd + "\n");
                            out.flush();
                            out.close();
                        } catch (Exception e) {
                            logger.severe("Failed to write SSL passwd file " + e);
                            logger.log(Level.FINE, "Exception", e);
                        }
                    }

                }
                //if(editor.save(Config.TMP_DIR + "server.xml.bak."+ runId)) {
                // we are no longer need to backup the file
                if(editor.save(null)) {
                    logger.config("Updated  " + configXML);
                }
                else {
                    logger.config("All Configs are up to date for " + configXML);
                }
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "SetConfig failed.", e);
            }
            return;
        }
    }

    /**
      * start a web server instance
      */
    public void start(String[] instanceHomes, boolean force) throws RemoteException
    {
        String [] instances = null;
        String [] logs = null;

        // We need to restart the server for profile runs to specify the collector
        // output directory.
        if(this.profileRun)
            force = true;

        if(instanceHomes != null) {
            instances = instanceHomes;
            logs = new String[instanceHomes.length];
            for(int i = 0; i < instanceHomes.length; i++) {
                for(int j = 0; i < myInstances.length; j++) {
                    if(instanceHomes[i].equals(myInstances[j])) {
                        logs[i] = myLogs[j];
                        break;
                    }
                }
            }
        }
        else { // ALL instances in the setup.
            instances = myInstances;
            logs = myLogs;
        }

        // Check if there is a need to restart if force is false by comparing
        // admch file and config file time stamps.
        if(!force) {
            boolean restart = false;
            for(int i = 0; i < instances.length; i++) {
                String configXML = instances[i] + File.separator +
                                   SJSAS8_CONFIG_FILES[0];
                File config = new File(configXML);
                File admch = new File((new File(logs[i])).getParent() + File.separator
                           + "admch");

                if(!admch.exists() || (admch.lastModified() < config.lastModified())) {
                    restart = true;
                    break;
                }
            }
            // No need to restart
            if(!restart) {
                logger.info("No need to restart SJSAS8 instance, skipping restart");
                return;
            }
        }

        try {
            stop(instances);
        }
        catch (Exception e) {
            logger.warning("failed to stop");
            logger.warning("instance/s may not be running");
        }

        for(int i = 0; i < instances.length; i++) {
            String cmd = "cd " + instances[i] + ";" + SJSAS8_START;

            if (sslRun) {
                cmd += " < " + SSL_PASSWD_FILE;
            }

            Properties profilerCmd = new Properties();
            profilerCmd.setProperty("PROFILER_COMMAND", " ");

            if(profileRun) {
                File f = new File(instances[i]);
                String tmpOut = Config.TMP_DIR + runId + File.separator + f.getName();

                f = new File(tmpOut);
                f.mkdirs();
                profilerCmd.setProperty("PROFILER_COMMAND",
                                        "\"" + Config.FABAN_HOME+ "perftool/bin/collect " +
                                        "-L unlimited -F on -j on -A copy -y PROF -d "  + tmpOut + " \"");
            }
            String scripts[] = {instances[i] + File.separator + SJSAS8_START,
                                instances[i] + File.separator + SJSAS8_STOP};

            // Update the PROFILER_COMMAND for start and stop script
            for(int j = 0; j < scripts.length; j++)  {
                if(FileHelper.isInFile(scripts[j], "PROFILER_COMMAND")) {
                    FileHelper.editPropFile(scripts[j], profilerCmd, null);
                    logger.config("Updating PROFILER_COMMAND in script");
                }
                // Edit the script to include the PROFILER_COMMAND (First time)
                else {
                    FileHelper.tokenReplace(scripts[j],
                                            "\"$JAVA_HOME\"/bin/java",
                                            "PROFILER_COMMAND=" + profilerCmd.getProperty("PROFILER_COMMAND") + "\n" +
                                            "$PROFILER_COMMAND \"$JAVA_HOME\"/bin/java", null);
                    logger.config("Inserting script with PROFILER_COMMAND");
                }
            }
            logger.info("Starting instance " + instances[i]);

            try {
                boolean retVal = false;

                // Run the command in the background and wait for the start
                retVal = cmdAgent.start(cmd, null, Config.DEFAULT_PRIORITY);

                if (retVal) {
                    retVal = false;
                    // Read the log file to make sure the server has started.
                    String msg = "Application onReady complete";

                    FileInputStream is = new FileInputStream(logs[i]);
                    BufferedReader bufR = new BufferedReader(new InputStreamReader(is));

                    // Just to make sure we don't wait for ever.
                    // We try to read the msg 120 times before we give up
                    // Sleep 1 sec between each try. So wait for about 2 min
                    int attempts = 120;
                    while(attempts > 0) {
                        // make sure we don't block
                        if(bufR.ready()) {
                            String s = bufR.readLine();
                            if((s !=  null) && (s.indexOf(msg) != -1)) {
                                retVal = true;
                                break;
                            }
                        }
                        else {
                            // Sleep for some time
                            try {
                                Thread.sleep(1000);
                                attempts --;
                            }
                            catch(Exception e){ break; }
                        }
                    }
                    bufR.close();
                    if(retVal) {
                        logger.info("Completed SJSAS8 instance startup command successfully");                    }
                    else {
                        logger.warning("Could not find expected message in server log");
                    }
                }
                else {
                    logger.warning("Could not complete start command for SJSAS8 instance");
                }
            }
            catch (Exception ee) {
               logger.log(Level.WARNING, "Starting SJSAS8 failed.", ee);
            }
        }
        // The cmdAgent will return only after the startup.
    }

    /**
      * stop Server
      */
    public void stop(String[] instanceHomes) throws RemoteException
    {

        String [] instances = null;

        if(instanceHomes != null) {
            instances = instanceHomes;
        }
        else { // ALL instances in the setup.
            instances = myInstances;
        }

        for(int i = 0; i < instances.length; i++) {

            String cmd = "cd " + instances[i] + ";" + SJSAS8_STOP;

            logger.info("Stopping instance " + instances[i]);

            try {
                boolean retVal = cmdAgent.start(cmd, Config.DEFAULT_PRIORITY);
                if (retVal) {

                    // SJSAS8 shutdown command exits when the sutdown is complete
                    logger.fine("Stop command completed successfully for " + instances[i]);
                }
                else {
                    logger.warning("Could not complete stop command for " + instances[i]);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Stopping SJSAS8 failed.", e);
            }
        }

        // Now clear the logs Just in case
        // This is to make sure that the logs read by start is the latest
        try {
            this.clearLogs(instances);
            logger.info("Cleared log files of insatnce/s");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not clear Logs.", e);
        }
    }

    // Transfer the logfiles
    public void xferLogs(String[] instanceHomes)  throws RemoteException {
        String[] instances = null;
        String [] logs = null;

        if(instanceHomes != null) {
            instances = instanceHomes;
            logs = new String[instanceHomes.length];
            for(int i = 0; i < instanceHomes.length; i++) {
                for(int j = 0; i < myInstances.length; j++) {
                    if(instanceHomes[i].equals(myInstances[j])) {
                        logs[i] = myLogs[j];
                        break;
                    }
                }
            }
        }
        else { // ALL instances in the setup.
            instances = myInstances;
            logs = myLogs;
        }

        // If profile run jar up the profile data files and transfer it
        // we need to do this only once even if there are multiple instances running on the machine.
        if(this.profileRun) {
            String jarFile = Config.TMP_DIR + runId + ".profile.jar";
            String cmd = "cd " + Config.TMP_DIR + runId +";/usr/java/bin/jar -cf " + jarFile + " *";
            try {
                if(!cmdAgent.start(cmd, Config.DEFAULT_PRIORITY))
                    logger.severe("Failed to create profile jar file ");
            } catch(Exception e) {
                logger.severe("Failed to create profile jar file " + e);
                logger.log(Level.FINE, "Exception", e);
            }
            String outFile = outDir + "/profile.jar." + host;
            // Move the profile a jar file
            FileHelper.xferFile(jarFile, outFile, true);
        }

        for(int i = 0; i < logs.length; i++) {
            String instance = new File(instances[i]).getName();
            String log = new File(logs[i]).getName();

            StringBuffer sb = new StringBuffer(outDir);
            sb.append("/");
            sb.append(log);
            sb.append(".");
            //append the instance name
            sb.append(instance);
            sb.append(".");
            //append hostname
            sb.append(host);
            // Copy log file
            FileHelper.xferFile(logs[i], sb.toString(), false);

            // Copy config files like server.xml
            for (int j = 0; j < SJSAS8_CONFIG_FILES.length; j++) {
                String config = instances[i] + File.separator + SJSAS8_CONFIG_FILES[j];
                sb = new StringBuffer(outDir);
                sb.append("/");
                sb.append(new File(config).getName());
                sb.append(".");
                sb.append(instance);
                sb.append(".");
                sb.append(host);
                FileHelper.xferFile(config, sb.toString(), false);
            }

            // if using Xloggc:<filename>
            if(logGC) {
                String gcLogFile = Config.TMP_DIR + "gc.log." + instance;
                sb = new StringBuffer(outDir);
                sb.append("/gc.log.");
                sb.append(instance);
                sb.append(".");
                sb.append(host);
                // Move GC log file
                FileHelper.xferFile(gcLogFile, sb.toString(), true);
            }

            // remove only the test dirs created by collector as
            // stopserv needs the instance dir/s
            if(profileRun) {
                // Remove the profile directory created for this run
                File file = new File(Config.TMP_DIR + runId + "/" + instance );
                String [] list = file.list();
                for(int j = 0; j < list.length; j++)
                    FileHelper.recursiveDelete(file, list[j]);
            }
        }

    }

    /**
      * clear app server log files
      */
    public void clearLogs(String[] instanceHomes) throws RemoteException
    {
        String [] logs = null;

        if(instanceHomes != null) {
            logs = new String[instanceHomes.length];
            for(int i = 0; i < instanceHomes.length; i++) {
                for(int j = 0; i < myInstances.length; j++) {
                    if(instanceHomes[i].equals(myInstances[j])) {
                        logs[i] = myLogs[j];
                        break;
                    }
                }
            }
        }
        else { // ALL instances in the setup.
            logs = myLogs;
        }

        for(int i = 0; i < logs.length; i++) {
            String logFile =  logs[i];
            // Get the log dir
            File f = new File(logFile).getParentFile();
            File [] list = f.listFiles();
            for (int j = 0; j < list.length; j ++) {
                if (list[j].isDirectory())
                   continue;
                list[j].delete();
                try {
                    list[j].createNewFile();
                } catch (IOException e) {
                    logger.severe("Failed for  " + logs[i] + " : " + e);
                    logger.log(Level.FINE, "Exception", e);
                }
            }
            // remove files with benchmark name or instance name from Config.TMP_DIR
            list = new File(Config.TMP_DIR).listFiles();
            String instance = f.getParent();
            String bm = new RunId(runId).getBenchName();
            for (int j = 0; j < list.length; j ++) {
                if(list[j].isDirectory())
                    continue;

                String name = list[j].getName();
                if((name.indexOf(bm) != -1) || (name.indexOf(instance) != -1))
                    list[j].delete();
            }
        }
    }

    public void setup(Run run, String serverHome,
                String[] instanceHomess, String[] instanceLogs) throws RemoteException {
        myInstances = instanceHomess;
        myLogs = instanceLogs;
        runId = run.getRunId();

        cmdAgent = CmdAgentImpl.getHandle();
        outDir =  run.getOutDir();
        logger.info("Setup completed");
    }

    public synchronized void kill() throws RemoteException {
        logger.fine("Killed");
    }

    /**
      * When this instance is unreferenced the application must exit.
      *
      * @see         java.rmi.server.Unreferenced
      *
      */
    public void unreferenced() {
        try {
            kill();
        }
        catch (Exception e) {}
    }
}
