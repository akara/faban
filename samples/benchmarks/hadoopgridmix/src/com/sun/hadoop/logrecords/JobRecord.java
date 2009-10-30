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
 * Container class to store a parsed JobRecord
 */
public class JobRecord extends AbstractLogRecord
{
    private String counter;
    private String group;    
    private String jobId;
    private String jobName;
    private String sessionId;
    private String user;
    private String value;

    public JobRecord()
    {
        counter = null;
        group = null;
        jobId = null;
        jobName = null;
        sessionId = null;
        user = null;
        value = null;
    }

    /**
     * @return the counter
     */
    public String getCounter() {
        return counter;
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

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
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    public boolean parse(final String detail)
    {
        //2009-05-21 17:39:25,203 mapred.job: counter=Combine output records, group=Map-Reduce Framework, hostName=r3p4150-01, jobId=job_200905211738_0001, jobName=sorter, sessionId=, user=root, value=0.0
        //so first remove the date as we do not need it



        // we get the detail section, after the last ':'
        if(detail != null && detail.length() > 0)
        {
            //all good
            String[] logMessages = detail.split(",");
            //we now should have 8 Strings in the Array
            if(logMessages[7] != null)
            {
                //all good
                String[] counter = logMessages[0].split("=");
                this.counter = counter[1];
                /*if(jobRecord.getCounter().compareTo("Local bytes written") == 0)
                {
                    logger.warning("Local bytes written metric found");
                }*/


                String[] group = logMessages[1].split("=");
                this.group = group[1];

                String[] hostname = logMessages[2].split("=");
                setHostname(hostname[1]);

                String[] jobId = logMessages[3].split("=");
                this.jobId = jobId[1];

                String[] jobName =  logMessages[4].split("=");
                this.jobName = jobName[1];

                String[] sessionId;
                try
                {
                    sessionId = logMessages[5].split("=");
                    this.sessionId = sessionId[1];
                }catch(ArrayIndexOutOfBoundsException aob)
                {
                    this.sessionId = "none";
                }

                String[] user = logMessages[6].split("=");
                this.user = user[1];

                String[] value = logMessages[7].split("=");
                this.value = value[1];

            }else
            {
                //all bad
                logger.warning("Line passed into JobRecord.parse was not in the format expected, noticed while retrieving the elements");
                return false;
            }


        }else
        {
            //all bad
            logger.warning("Line passed into MapRedJobRecord.parse was not in the format expected");
            return false;
        }

       return true;
    }
}
