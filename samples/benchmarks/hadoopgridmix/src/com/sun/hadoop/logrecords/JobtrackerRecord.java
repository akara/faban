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
 * Container class to store a parsed JobtrackerRecord
 */
public class JobtrackerRecord extends AbstractLogRecord
{        
    private String sessionId;
    private String jobs_completed;
    private String jobs_submitted;
    private String maps_completed;
    private String maps_launched;
    private String reduces_completed;
    private String reduces_launched;
   

    public JobtrackerRecord()
    {
        sessionId = null;
        jobs_completed = null;
        jobs_submitted = null;
        maps_completed = null;
        maps_launched = null;
        reduces_completed = null;
        reduces_launched = null;
    }

   

    /**
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @return the jobs_completed
     */
    public String getJobs_completed() {
        return jobs_completed;
    }

    /**
     * @return the jobs_submitted
     */
    public String getJobs_submitted() {
        return jobs_submitted;
    }

    /**
     * @return the maps_completed
     */
    public String getMaps_completed() {
        return maps_completed;
    }

    /**
     * @return the maps_launched
     */
    public String getMaps_launched() {
        return maps_launched;
    }

    /**
     * @return the reduces_completed
     */
    public String getReduces_completed() {
        return reduces_completed;
    }

    /**
     * @return the reduces_launched
     */
    public String getReduces_launched() {
        return reduces_launched;
    }

    public boolean parse(final String detail)
    {
        //2009-05-21 17:39:15,204 mapred.jobtracker: hostName=r3p4150-01, sessionId=, jobs_completed=0, jobs_submitted=1, maps_completed=11, maps_launched=13, reduces_completed=0, reduces_launched=0
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
                String[] hostname = logMessages[0].split("=");
                setHostname(hostname[1]);

                String[] sessionId;
                try
                {
                    sessionId = logMessages[1].split("=");
                    this.sessionId = sessionId[1];
                }catch(ArrayIndexOutOfBoundsException aob)
                {
                    this.sessionId = "none";
                }

                String[] jobs_completed = logMessages[2].split("=");
                this.jobs_completed = jobs_completed[1];

                String[] jobs_submitted = logMessages[3].split("=");
                this.jobs_submitted = jobs_submitted[1];

                String[] maps_completed = logMessages[4].split("=");
                this.maps_completed = maps_completed[1];

                String[] maps_launched = logMessages[5].split("=");
                this.maps_launched = maps_launched[1];

                String[] reduces_completed = logMessages[6].split("=");
                this.reduces_completed = reduces_completed[1];

                String[] reduces_launched = logMessages[7].split("=");
                this.reduces_launched = reduces_launched[1];

            }else
            {
                //all bad
                logger.warning("Line passed into JobtrackerRecord.parse was not in the format expected noticed while retrieving the elements");
                return false;
            }


        }else
        {
            //all bad
            logger.warning("Line passed into JobtrackerRecord.parse was not in the format expected");
            return false;
        }

       return true;
    }

}

