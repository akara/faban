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
package com.sun.faban.harness.engine;

import com.sun.faban.common.NameValuePair;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.util.FileHelper;

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

    RunDaemon runDaemon = null;
    RunQLock runqLock;
    static Logger logger = Logger.getLogger(RunQ.class.getName());

    private static RunQ runQ = null;

    /** Run seqeunce field index in the run queue listing. */
    public static final int RUNSEQ = 0;

    /** Benchmark name field index in the run queue listing. */
    public static final int BENCHNAME = 1;

    /** Description field index in the run queue listing. */
    public static final int DESCRIPTION = 2;

    private RunQ() {
        runqLock = new RunQLock();
        runDaemon = new RunDaemon(runqLock);
    }

    /**
     * Singleton initializer for runQ and runDaemon.
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
     * @return The run id of the run just added
     * @throws IOException There was a problem accessing the run queue directory
     */
    public String addRun(String user, String profile, BenchmarkDescription desc)
            throws IOException {

        RunSequence seq = new RunSequence();
        try {
            // Gets the lock for the runq directory.
            runqLock.grabLock();

            String runId = desc.shortName + '.' + seq.get();

            String runDir = Config.RUNQ_DIR + runId;
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
            String sourceFabanDesc = benchMetaInf + "faban.xml";
            String destBenchDesc = null;
            String destFabanDesc = null;
            if (new File(sourceBenchDesc).exists())
                destBenchDesc = runqMetaInf + "benchmark.xml";
            else
                sourceBenchDesc = null;
            if (new File(sourceFabanDesc).exists())
                destFabanDesc = runqMetaInf + "faban.xml";
            else
                sourceFabanDesc = null;

            if (sourceBenchDesc == null && sourceFabanDesc == null) {
                String msg = "No benchmark descriptors found";
                IOException e = new IOException(msg);
                logger.log(Level.SEVERE, msg, e);
                throw e;
            }

            if (!((sourceBenchDesc == null ||
                 FileHelper.copyFile(sourceBenchDesc, destBenchDesc, false)) &
                  (sourceFabanDesc == null ||
                 FileHelper.copyFile(sourceFabanDesc, destFabanDesc, false)))) {
                String msg = "Error copying benchmark descriptors.";
                IOException e = new IOException(msg);
                logger.log(Level.SEVERE, msg, e);
                throw e;
            }


            // Record the user
            if (Config.SECURITY_ENABLED) {
                // Set the submitter
                File submitter = new File(metaInf, "submitter");
                FileHelper.writeStringToFile(user, submitter);

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

            // copying the tags file from the profile dir
            if (profile != null && !"".equals(profile)){
                String tagsForProfile = null;
                File tagsFile = new File(Config.PROFILES_DIR + profile + "/tags");
                if(tagsFile.exists() && tagsFile.length()>0){
                    tagsForProfile = FileHelper.readContentFromFile(tagsFile).trim();
                }
                if(tagsForProfile != null) {
                    String tagsRepFileName =
                        runDir + File.separator + "META-INF/tags";
                    String tagsSrcFileName =
                        Config.PROFILES_DIR + profile + "/tags";
                    FileHelper.copyFile(tagsSrcFileName, tagsRepFileName, false);
                }
            }
            seq.next();
            runqLock.signal();  // Signal a new run is submitted.
            return runId;
        } finally {
            if (seq != null)
                seq.cancel();
            runqLock.releaseLock();
        }
    }

    /**
     * Deletes the run with the specified runId from the runq. Does not take
     * any action if such a run is not found or is already being executed
     * by the runDaemon thread.
     *
     * @param runId the runId of the run to be deleted
     * @return Whether or not the run was successfully deleted from the queue
     */
    public boolean deleteRun(String runId)
    {
        try {
            //   File runqDirPath = new File(Config.RUNQ_DIR);
            logger.warning("Removing run directory " + runId);
            runqLock.grabLock();
            boolean retVal = FileHelper.recursiveDelete(
                    new File(Config.RUNQ_DIR), runId);
            runqLock.releaseLock();
            return retVal;
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "Could not delete the run directory "+
                    runId + '.', e);
        }
        return false;
    }


    /**
     * Returns a list of the runs currently in the runq. The list is an array
     * of run sequence, benchmark name, and description.
     * @return an array of RunInfo objects.
     *
     */
    public String[][] listRunQ() {
        //this causes unexpected nullpointer exception later
    	//String[][] data = null;
    	String [][] data = new String[0][0];

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
                    RunId runId = new RunId(list[i]);
                    String benchName = runId.getBenchName();
                    String runSeq = runId.getRunSeq();
                    data[i][RUNSEQ] = runSeq;
                    data[i][BENCHNAME] = benchName;
                    String paramFile = Config.RUNQ_DIR + list[i]
                            + File.separator + ((BenchmarkDescription)
                            benchMap.get(benchName)).configFileName;
                    String desc = null;
                    if (new File(paramFile).exists()) {
                        ParamRepository par =
                                new ParamRepository(paramFile, false);
                        desc = par.getParameter("fa:runConfig/fh:description");
                    }
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
     * Obtains the list of pending runs in the run queue. This is the list
     * of formatted runids. This is a convenience method and it internally
     * calls listRunQ.
     * @return The list of pending runs
     */
    public static String[] listPending() {
        String[][] pendingA = getHandle().listRunQ();
        String[]  pendingL = null;
        if (pendingA != null) {
            pendingL = new String[pendingA.length];
            for (int i = 0; i < pendingA.length; i++)
                pendingL[i] = pendingA[i][1] + '.' + pendingA[i][0];
        }
        return pendingL;
    }


    /**
     * Reports the status of the RunDaemonThread.
     * @return String representing the current status of the run daemon.
     */
    public String getRunDaemonStatus()
    {
        return "Run Daemon is " + runDaemon.getRunDaemonThreadStatus();
    }

    /**
     * Starts/restarts the run daemon.
     * @return Whether the run daemon is started
     */
    public boolean startRunDaemon()
    {
        return runDaemon.resumeRunDaemonThread();
    }

    /**
     * Stops/suspends the run daemon.
     * @return Whether the run daemon is successfully stopped
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
     * Method to stop the current benchamark run.
     * @param runId The run id to kill - this is for safety
     * @param user The current user name, or null if security is disabled
     * @return current run name.
     */
    public String killCurrentRun(String runId, String user) {
        return runDaemon.killCurrentRun(runId, user);
    }

    /**
     * Moves the run from the queue into the output directory. This
     * function is normally done by the run daemon with the exception
     * of a submission proxy where the run daemon is not run. The RunRetriever
     * servlet will call to fetch this run to run on a remote system instead.
     * If the given run id does not match the id of the next run, some
     * inconsistencies have happened. The method returns null in this case.
     * @param runId The id of the run in question.
     * @return The run object representing this run
     * @throws RunEntryException There is an error in the run queue entry
     */
    public Run fetchNextRun(String runId) throws RunEntryException, IOException, ClassNotFoundException {
        if (runId == null)
            throw new NullPointerException("Run name cannot be null!");
        return runDaemon.fetchNextRun(runId);
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
     * The RunSequence class assists in generating the run sequence.
     */
    static class RunSequence {

        RandomAccessFile seqRFile;
        String runSeq = null;

        // Gets the sequence for this run from the sequence file. Creates a new
        // sequence file if it does not already exist.
        String get() {

            String runSeqChar, runSeqIntChar;
            File seqFile = new File(Config.SEQUENCE_FILE);

            if (seqFile.exists()) {
                try {
                    seqRFile = new RandomAccessFile(seqFile, "rwd");
                    long size = seqRFile.length();
                    if (size > Integer.MAX_VALUE)
                        throw new IOException(Config.SEQUENCE_FILE +
                                " larger than 2GB not supported!");
                    byte[] buffer = new byte[(int) size];
                    seqRFile.readFully(buffer);
                    if (buffer[buffer.length - 1] == '\n')
                        --size;
                    runSeq = new String(buffer, 0, (int) size);

                    int colonPos = -1;
                    if(runSeq != null &&
                            (colonPos = runSeq.indexOf(":")) != -1) {
                        runSeqChar = runSeq.substring(colonPos + 1);
                        runSeqIntChar = runSeq.substring(0, colonPos);
                        runSeq = runSeqIntChar + runSeqChar;
                    } else {
                        logger.warning("RunQ getRunId: " +
                                "Invalid runSeq in sequence file");
                        runSeq = null;
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }

            // Could not find a valid runSeq
            if(runSeq == null) {
                runSeq = "1A";
            }
            return runSeq;
        }

        // Generate the sequence number for the next run and write it to
        // sequence file.
        void next() throws IOException {

            int length = runSeq.length();

            char seqChar = runSeq.charAt(length - 1);
            String seqIntStr = runSeq.substring(0, length - 1);
            int seqInt = Integer.parseInt(seqIntStr);
            switch (seqChar) {
                case 'z' : // old installs will still use lower case up to the
                           // next number switch. This fix is for win32
                           // compatibility. It is not case sensitive.
                case 'Z' : ++seqInt; seqChar = 'A'; break;
                default  : ++seqChar;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(seqInt).append(':').append(seqChar);

            try {
                if (seqRFile == null)
                    seqRFile = new RandomAccessFile(Config.SEQUENCE_FILE,"rwd");
                else
                    seqRFile.seek(0);
                byte[] buffer = sb.toString().getBytes();
                seqRFile.write(buffer);
                if (seqRFile.length() != buffer.length)
                    seqRFile.setLength(buffer.length);
                seqRFile.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE,
                        "Could not write to the sequence file", e);
                throw e;
            } finally {
                if (seqRFile != null) {
                    seqRFile.close();
                    seqRFile = null;
                }
            }
        }

        /**
         * Cancels any pending sequence operations.
         */
        public void cancel() {
            if (seqRFile != null) {
                try {
                    seqRFile.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING,
                                        "Closing sequence file failed!", e);
                }
                seqRFile = null;
            }
        }
    }

    /**
      * This class serves as the comparator to sort runs in the runq and pick
      * the run with the smallest run sequence.
      *
      */
    private class ComparatorImpl implements Comparator {

        public int compare(Object o1, Object o2) {
            RunId r1 = new RunId((String) o1);
            RunId r2 = new RunId((String) o2);
            return r1.getRunSeq().compareTo(r2.getRunSeq());
        }
    }
}

