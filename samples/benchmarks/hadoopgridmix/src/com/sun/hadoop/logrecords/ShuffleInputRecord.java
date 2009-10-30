/*
* The contents of this file are subject to the terms
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
* Copyright 2009 Sun Microsystems Inc. All Rights Reserved
*/

package com.sun.hadoop.logrecords;


/**
 *
 * @author Damien Cooke
 * Container class to store a parsed ShuffleInputRecord
 */
public class ShuffleInputRecord extends AbstractLogRecord
{
    private String jobId;
    private String jobName;
    private String sessionId;
    private String taskId;
    private String user;
    private String shuffle_failed_fetches;
    private String shuffle_fetchers_busy_percent;
    private String shuffle_input_bytes;
    private String shuffle_success_fetches;

    /**
     * @return the jobId
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @return the taskId
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the shuffle_failed_fetches
     */
    public String getShuffle_failed_fetches() {
        return shuffle_failed_fetches;
    }

    /**
     * @return the shuffle_fetchers_busy_percent
     */
    public String getShuffle_fetchers_busy_percent() {
        return shuffle_fetchers_busy_percent;
    }

    /**
     * @return the shuffle_input_bytes
     */
    public String getShuffle_input_bytes() {
        return shuffle_input_bytes;
    }

    /**
     * @return the shuffle_success_fetches
     */
    public String getShuffle_success_fetches() {
        return shuffle_success_fetches;
    }

    /*
     * method is a specilised parse method to parse a detail from a log of ShuffleInputRecord type
     * @param detail, detail of log file to parse
     * @return success or failure of the parse process
     */
    public boolean parse(final String detail)
    {
        /*
         * 2009-06-05 17:05:13,034 mapred.shuffleInput: hostName=pae2250-9h, jobId=job_200906051703_0001, jobName=sorter, sessionId=, taskId=attempt_200906051703_0001_r_000001_0, user=root, shuffle_failed_fetches=0, shuffle_fetchers_busy_percent=0.0, shuffle_
input_bytes=271327602, shuffle_success_fetches=16
         */

        // we get the detail section, after the last ':'
        if(detail != null && detail.length() > 0)
        {
            //all is good
            String[] logMessages = detail.split(",");

            String[] hostname = logMessages[0].split("=");
            setHostname(hostname[1]);

            String[] jobId = logMessages[1].split("=");
            this.jobId = jobId[1];

            String[] jobName = logMessages[2].split("=");
            this.jobName = jobName[1];

            String[] sessionId;
            try
            {
                sessionId = logMessages[3].split("=");
                this.sessionId = sessionId[1];
            }catch(ArrayIndexOutOfBoundsException aob)
            {
                this.sessionId = "none";
            }

            String[] taskId = logMessages[4].split("=");
            this.taskId = taskId[1];

            String[] user = logMessages[5].split("=");
            this.user = user[1];

            String[] shuffle_failed_fetches = logMessages[6].split("=");
            this.shuffle_failed_fetches = shuffle_failed_fetches[1];

            String[] shuffle_fetchers_busy_percent = logMessages[7].split("=");
            this.shuffle_fetchers_busy_percent = shuffle_fetchers_busy_percent[1];

            String[] shuffle_input_bytes = logMessages[8].split("=");
            this.shuffle_input_bytes = shuffle_input_bytes[1];

            String[] shuffle_success_fetches = logMessages[9].split("=");
            this.shuffle_success_fetches = shuffle_success_fetches[1];

        }else
        {
            //all bad
            logger.warning("Line passed into ShuffleInputRecord.parse was not in the format expected noticed while retrieving the elements");
            return false;
        }
        return true;
    }

}
