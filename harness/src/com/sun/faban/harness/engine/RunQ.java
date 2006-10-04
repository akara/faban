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
 * $Id: RunQ.java,v 1.9 2006/10/04 23:55:06 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.util.FileHelper;
import com.sun.faban.harness.util.NameValuePair;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the Faban RunQ. It provides methods to add a run,
 * delete a run and to get a list of all the runs in the RunQ.
 *
 * @author Ramesh Ramachandran
 */

public class RunQ {

    String runqDir;
    RunDaemon runDaemon = null;
    RunQLock runqLock;
    Logger logger;

    private static RunQ runQ = null;

    static final int GENERIC = 0;
    static final int GENERATED = 1;
    static final int BENCH = 2;

    public static final int RUNID = 0;
    public static final int BENCHNAME = 1;
    public static final int DESCRIPTION = 2;

    /**
     * Constructor
     *
     */

    private RunQ() {
        runqLock = new RunQLock();
        logger = Logger.getLogger(this.getClass().getName());
        runDaemon = new RunDaemon(runqLock);
    }

    /**
     * Singleton initializer for runQ and runDaemon
     * @return runQ
     */

    public static RunQ getHandle() {
        if(runQ == null)
            runQ = new RunQ();

        return runQ;
    }

    /**
      * Adds a Run to the runq. Creates a directory for the run in the runq
      * directory and stores the parameter repository in it. It also notifies
      * the RunDaemon thread of the newly added run. All operations on the runq
      * directory are synchronized through the use of the LockFileMonitor class.
      * @param user The user name or id if logged in, or null
      * @param profile Profile name for this run
      * @param desc The description of the benchmark to run
      */
    public String addRun(String user, String profile, BenchmarkDescription desc)
            throws IOException {

        try {
            // Gets the lock for the runq directory.
            runqLock.grabLock();

            String runID = getRunID(desc.shortName);

            String runDir = Config.RUNQ_DIR + runID;
            // create Run Directory
            File dir = new File(runDir);
            if(dir.mkdirs())
                logger.fine("Created Run Directory " + runDir);

            // Create the META-INF and copy deployment info.
            File metaInf = new File(dir, "META-INF");
            metaInf.mkdirs();

            String benchMetaInf = Config.BENCHMARK_DIR + File.separator +
                    desc.shortName + File.separator + "META-INF" + File.separator;
            String runqMetaInf = metaInf.getAbsolutePath() + File.separator;
            String sourceBenchDesc = benchMetaInf + "benchmark.xml";
            String destBenchDesc = runqMetaInf + "benchmark.xml";
            String sourceFabanDesc = benchMetaInf + "faban.xml";
            String destFabanDesc = null;
            if (new File(sourceFabanDesc).exists())
                destFabanDesc = runqMetaInf + "faban.xml";
            else
                sourceFabanDesc = null;

            if (!(FileHelper.copyFile(sourceBenchDesc, destBenchDesc, false) &&
               (sourceFabanDesc == null ||
                FileHelper.copyFile(sourceFabanDesc, destFabanDesc, false)))) {
                String msg = "Error copying benchmark descriptors.";
                IOException e = new IOException(msg);
                logger.log(Level.SEVERE, "Error copying benchmark descriptors.",
                           e);
                throw e;
            }


            // Record the user
            if (Config.SECURITY_ENABLED) {
                // Set the submitter
                File submitter = new File(metaInf, "submitter");
                PrintStream p = new PrintStream(submitter);
                p.println(user);
                p.close();

                // Copy ACLs
                String[] aclFiles = { "view.acl", "write.acl" };
                for (int i = 0; i < aclFiles.length; i++) {
                    String benchAcl = Config.CONFIG_DIR + desc.shortName +
                                      File.separator + aclFiles[i];
                    String runAcl = null;
                    if (new File(benchAcl).exists())
                        runAcl = new File(metaInf, aclFiles[i]).getAbsolutePath();
                    else
                        benchAcl = null;

                    if (benchAcl != null &&
                            !FileHelper.copyFile(benchAcl, runAcl, false)) {
                        String msg = "Error copying " + benchAcl + " to " +
                                     runAcl + '.';
                        throw new IOException(msg);
                    }
                }
            }

            // copying the parameter repository file from the user's profile.
            String paramRepFileName =
                    runDir + File.separator + desc.configFileName;
            String paramSourceFileName = Config.PROFILES_DIR + profile +
                    File.separator + desc.configFileName + "." + desc.shortName;

            logger.fine("Copying " +
                    paramSourceFileName + " to " +
                    paramRepFileName);
            FileHelper.copyFile(paramSourceFileName, paramRepFileName, false);

            generateNextID(runID);

            return runID;
        } finally {
            runqLock.releaseLock();
        }
    }

    // Gets the ID for this run from the sequence file. Creates a new
    // sequence file if it does not already exist.
    private String getRunID(String benchName)
    {

        String runID = null;
        String runIDChar, runIDIntChar;
        File seqFile = new File(Config.SEQUENCE_FILE);

        if (seqFile.exists()) {
            BufferedReader bufIn = null;
            try {
                bufIn = new BufferedReader(new FileReader(seqFile));
            }
            catch (FileNotFoundException fe) {
                logger.severe("RunQ getRunID: the sequence file does not exist");
            }
            runID = null;
            try {
                runID = bufIn.readLine();
                bufIn.close();
            }
            catch (IOException ie) {
                logger.severe("RunQ getRunID: could not read/close the sequence file");
            }
            int colonPos = -1;
            if((runID != null) && ((colonPos = runID.indexOf(":")) != -1)) {
                runIDChar = runID.substring(colonPos + 1);
                runIDIntChar = runID.substring(0, colonPos);
                runID = benchName + "." + runIDIntChar + runIDChar;
            }
            else {
                logger.warning("RunQ getRunID: Invalid runID in sequence file");
                seqFile.delete();
                runID = null;
            }
        }
        
        // Could not find a valid runID 
        if(runID == null) {
            try {
                seqFile.createNewFile();
            }
            catch (IOException ie) {
                logger.severe("Could not create the sequence File");
            }
            runID = benchName + ".1A";
        }
        return runID;
    }

    // Generate the sequence number for the next run and write it to
    // sequence file.
    private void generateNextID(String runID) throws IOException {

        File seqFile = new File(Config.SEQUENCE_FILE);
        int index = runID.lastIndexOf(".");
        int length = runID.length();

        char runIDChar = runID.charAt(length - 1);
        String runIDIntStr = runID.substring(index + 1, length - 1);
        int runIDInt = Integer.parseInt(runIDIntStr);
        if (runIDChar == 'z') {
            runIDInt++;
            runIDChar = 'A';
        }
        else {
            runIDChar = (runIDChar == 'Z') ? 'a' :
                    ((char)((int) runIDChar + 1));
        }

        StringBuffer sb = new StringBuffer();
        sb.append(runIDInt).append(':').append(runIDChar);

        try {
            BufferedWriter bufOut =
                    new BufferedWriter(new FileWriter(seqFile));
            bufOut.write(sb.toString());
            bufOut.close();
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "Could not write to the sequence file", e);
            throw e;
        }
    }

    /**
      * Deletes the run with the specified runID from the runq. Does not take
      * any action if such a run is not found or is already being executed
      * by the runDaemon thread
      *
      * @param runID the runID of the run to be deleted
      *
      */
    public boolean deleteRun(String runID)
    {
        try {
            //   File runqDirPath = new File(Config.RUNQ_DIR);
            logger.warning("Removing run directory " + runID);
            runqLock.grabLock();
            boolean retVal = FileHelper.recursiveDelete(
                    new File(Config.RUNQ_DIR), runID);
            runqLock.releaseLock();
            return retVal;
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Could not delete the run directory "+
                    runID + '.', e);
        }
        return false;
    }


    /** Returns a list of the runs currently in the runq
      *
      * @return an array of RunInfo objects.
      *
      **/

    public String[][] listRunQ() {
        String[][] data = null;

        try {
            File runqDirPath = new File(Config.RUNQ_DIR);
            String[] list = runqDirPath.list();

            if((list != null) && (list.length > 0)) {
                Arrays.sort(list, new ComparatorImpl());
                data = new String[list.length][3];

                // We do not want to check for new deployments in listRunQ,
                // pass false as getBenchDirMap argument.
                Map<String, BenchmarkDescription> benchMap =
                        BenchmarkDescription.getBenchDirMap(false);
                for (int i = 0; i < list.length; i++) {
                    int dotPos = list[i].lastIndexOf(".");
                    String tmpBenchName = list[i].substring(0, dotPos);
                    String tmpRunID = list[i].substring(dotPos + 1);
                    data[i][RUNID] = tmpRunID;
                    data[i][BENCHNAME] = tmpBenchName;
                    String paramFile = Config.RUNQ_DIR + list[i]
                            + File.separator + ((BenchmarkDescription)
                            benchMap.get(tmpBenchName)).configFileName;
                    ParamRepository par = new ParamRepository(paramFile);
                    String desc = par.getParameter("runConfig/description");
                    if((desc == null) || (desc.length() == 0))
                        data[i][DESCRIPTION] = "UNAVAILABLE";
                    else
                        data[i][DESCRIPTION] = desc;
                }
            }
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Could not list the runQ.", e);
        }
        return data;
    }


    /**
      * Reports the status of the RunDaemonThread.
      *
      */
    public String getRunDaemonStatus()
    {
        return "Run Daemon is " + runDaemon.getRunDaemonThreadStatus();
    }

    /**
      * Not sure if this method will be used
      *
      */
    public boolean startRunDaemon()
    {
        return runDaemon.resumeRunDaemonThread();
    }

    /**
      * Not sure if this method will be used
      *
      */
    public boolean stopRunDaemon()
    {
        return runDaemon.suspendRunDaemonThread();
    }

    /**
     * Obtains the run id of the current run.
     * @return The run id of the current run,
     *         or null if there is no ccurrent run
     */
    public String getCurrentRunId() {
        return runDaemon.getCurrentRunId();
    }

    /**
     * Obtains the short name of the current benchmark run.
     * @return The benchmark's short name
     */         
    public String getCurrentBenchmark() {
        return runDaemon.getCurrentRunBenchmark();
    }

    /**
     * method to stop the current benchamark run
     * @param runId The run id to kill - this is for safety
     * @return current run id.
     */
    public String killCurrentRun(String runId, String user) {
        return runDaemon.killCurrentRun(runId, user);
    }

    /**
     * Moves the run from the queue into the output directory. This
     * function is normally done by the run daemon with the exception
     * of a submission proxy where the run daemon is not run. The RunRetriever
     * servlet will call to fetch this run to run on a remote system instead.
     * @return The run object representing this run
     * @throws RunEntryException There is an error in the run queue entry
     */
    public Run fetchNextRun() throws RunEntryException {
        return runDaemon.fetchNextRun();
    }

    /**
     * Obtains the name and age of the next run, in milliseconds
     * since submitted, if the age is more than minAge.
     * @param minAge The minimum run age to return.
     * @return The age of the next run, or null if there is no next run or the
     *         next run is younger than the given age
     */
    public NameValuePair<Long> nextRunAge(long minAge) {
        return runDaemon.nextRunAge(minAge);
    }

    /**
     * Method to stop the run daemon before unloading
     * Faban Engine servlet.
     */
    public void exit() {
        runDaemon.exit();
    }

    /**
      * This method returns a previous run with a parameter repository
      * from the runq or the output directory.
      *
      * It is called when loading parameters from some previous run
      * and to create paramchanges file too.
      *
      */
    public String getValidPrevRun(String benchName)
    {
        File seqFile = new File(Config.SEQUENCE_FILE);
        if (seqFile.exists()) {
            BufferedReader bufIn = null;
            try {
                bufIn = new BufferedReader(new FileReader(seqFile));
            }
            catch (FileNotFoundException fe) {
                logger.severe("RunQ getValidPrevRun: the sequence file does not exist");
                logger.log(Level.FINE, "Exception", fe);
            }
            String runIDIntChar = null;
            try {
                runIDIntChar = bufIn.readLine();
                bufIn.close();
            }
            catch (IOException ie) {
                logger.log(Level.SEVERE, "RunQ getValidPrevRun: " +
                        "could not read/close the sequence file", ie);
            }
            int colonPos = -1;
            if((runIDIntChar != null) && ((colonPos = runIDIntChar.indexOf(":")) != -1)) {            
                String runIDChar = runIDIntChar.substring(colonPos + 1);
                int runIDInt =
                    Integer.parseInt(runIDIntChar.substring(0, colonPos));
            
                if ((runIDChar.charAt(0) == 'A') && (runIDInt == 1))
                    return null;
                    
                char runIDCharZero = runIDChar.charAt(0);
                if (runIDCharZero == 'A') {
                    runIDInt--;
                    runIDChar = String.valueOf('z');
                }
                else {
                    if (runIDCharZero == 'a') {
                        runIDChar = String.valueOf('Z');
                    }
                    else {
                        runIDChar = String.valueOf((char)((int) runIDCharZero - 1));
                    }   
                }
                String runID = benchName + "." + String.valueOf(runIDInt) + runIDChar;
                BenchmarkDescription desc = BenchmarkDescription.getDescription(benchName);
                File checkIfExists =
                     new File(Config.RUNQ_DIR + runID + File.separator + desc.configFileName);
                if (checkIfExists.exists())
                    return runID;
                
                checkIfExists = new File(Config.OUT_DIR +
                    runID + File.separator + desc.configFileName);
                if (checkIfExists.exists())
                    return runID;
            }
        }
        return null;
    }

    /**
      * This class serves as the comparator to sort runs in the runq and pick
      * the run with the smallest runid.
      *
      */
    private class ComparatorImpl implements Comparator
    {

        public int compare(Object o1, Object o2)
        {

            String s1 = (String) o1;
            String s2 = (String) o2;
            String sub1 = s1.substring(s1.lastIndexOf(".") + 1);
            String sub2 = s2.substring(s2.lastIndexOf(".") + 1);
            return (sub1.compareTo(sub2));

        }
    }


}

