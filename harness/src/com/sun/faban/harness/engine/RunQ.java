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
 * $Id: RunQ.java,v 1.22 2008/01/15 08:02:52 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
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

    String runqDir;
    RunDaemon runDaemon = null;
    RunQLock runqLock;
    static Logger logger = Logger.getLogger(RunQ.class.getName());

    private static RunQ runQ = null;

    static final int GENERIC = 0;
    static final int GENERATED = 1;
    static final int BENCH = 2;

    public static final int RUNSEQ = 0;
    public static final int BENCHNAME = 1;
    public static final int DESCRIPTION = 2;

    /**
     * Constructor
     *
     */

    private RunQ() {
        runqLock = new RunQLock();
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
      * by the runDaemon thread
      *
      * @param runId the runId of the run to be deleted
      *
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
    public Run fetchNextRun(String runId) throws RunEntryException {
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
            String runSeqIntChar = null;
            try {
                runSeqIntChar = bufIn.readLine();
                bufIn.close();
            }
            catch (IOException ie) {
                logger.log(Level.SEVERE, "RunQ getValidPrevRun: " +
                        "could not read/close the sequence file", ie);
            }
            int colonPos = -1;
            if((runSeqIntChar != null) && ((colonPos = runSeqIntChar.indexOf(":")) != -1)) {
                String runSeqChar = runSeqIntChar.substring(colonPos + 1);
                int runSeqInt =
                    Integer.parseInt(runSeqIntChar.substring(0, colonPos));
            
                if ((runSeqChar.charAt(0) == 'A') && (runSeqInt == 1))
                    return null;
                    
                char runSeqCharZero = runSeqChar.charAt(0);
                if (runSeqCharZero == 'A') {
                    runSeqInt--;
                    runSeqChar = String.valueOf('z');
                }
                else {
                    if (runSeqCharZero == 'a') {
                        runSeqChar = String.valueOf('Z');
                    }
                    else {
                        runSeqChar = String.valueOf((char)((int) runSeqCharZero - 1));
                    }   
                }
                String runId = benchName + "." + String.valueOf(runSeqInt) + runSeqChar;
                BenchmarkDescription desc = BenchmarkDescription.getDescription(benchName);
                File checkIfExists =
                     new File(Config.RUNQ_DIR + runId + File.separator + desc.configFileName);
                if (checkIfExists.exists())
                    return runId;
                
                checkIfExists = new File(Config.OUT_DIR +
                    runId + File.separator + desc.configFileName);
                if (checkIfExists.exists())
                    return runId;
            }
        }
        return null;
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

