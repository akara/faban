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
 * Container class to store a parsed TasktrackerRecord
 */
public class TasktrackerRecord extends AbstractLogRecord
{
    //2009-06-06 03:35:34,704 mapred.tasktracker: hostName=pae2250-9h, sessionId=, mapTaskSlots=5, maps_running=0, reduceTaskSlots=5, reduces_running=1, tasks_completed=0, tasks_failed_ping=0, tasks_failed_timeout=0
    private String sessionId;
    private String mapTaskSlots;
    private String maps_running;
    private String reduceTaskSlots;
    private String reduces_running;
    private String tasks_completed;
    private String tasks_failed_ping;
    private String tasks_failed_timeout;

    /**
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * @return the mapTaskSlots
     */
    public String getMapTaskSlots() {
        return mapTaskSlots;
    }

    /**
     * @return the maps_running
     */
    public String getMaps_running() {
        return maps_running;
    }

    /**
     * @return the reduceTaskSlots
     */
    public String getReduceTaskSlots() {
        return reduceTaskSlots;
    }

    /**
     * @return the reduces_running
     */
    public String getReduces_running() {
        return reduces_running;
    }

    /**
     * @return the tasks_completed
     */
    public String getTasks_completed() {
        return tasks_completed;
    }

    /**
     * @return the tasks_failed_ping
     */
    public String getTasks_failed_ping() {
        return tasks_failed_ping;
    }

    /**
     * @return the tasks_failed_timeout
     */
    public String getTasks_failed_timeout() {
        return tasks_failed_timeout;
    }

    /*
     * method is a specilised parse method to parse a detail from a log of TasktrackerRecord type
     * @param detail, detail of log file to parse
     * @return success or failure of the parse process
     */
    public boolean parse(String detail)
    {
        //2009-06-06 03:35:34,704 mapred.tasktracker: hostName=pae2250-9h, sessionId=, mapTaskSlots=5, maps_running=0, reduceTaskSlots=5, reduces_running=1, tasks_completed=0, tasks_failed_ping=0, tasks_failed_timeout=0

        // we get the detail section, after the last ':'
        if(detail != null && detail.length() > 0)
        {
            //all good
            String[] logMessages = detail.split(",");

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

            String[] mapTaskSlots = logMessages[2].split("=");
            this.mapTaskSlots = mapTaskSlots[1];

            String[] maps_running = logMessages[3].split("=");
            this.maps_running = maps_running[1];

            String[] reduceTaskSlots = logMessages[4].split("=");
            this.reduceTaskSlots = reduceTaskSlots[1];

            String[] reduces_running = logMessages[5].split("=");
            this.reduces_running = reduces_running[1];

            String[] tasks_completed = logMessages[6].split("=");
            this.tasks_completed = tasks_completed[1];

            String[] tasks_failed_ping = logMessages[7].split("=");
            this.tasks_failed_ping = tasks_failed_ping[1];

            String[] tasks_failed_timeout = logMessages[8].split("=");
            this.tasks_failed_timeout = tasks_failed_timeout[1];

        }else
        {
            //all bad
            logger.warning("Line passed into TasktrackerRecord.parse was not in the format expected noticed while retrieving the elements");
            return false;
        }

        return true;
    }

}
