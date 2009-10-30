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

package com.sun.hadoop.harness;

import java.util.Calendar;
import java.util.logging.Logger;

/**
 *
 * @author Damien Cooke
 *
 * This class manages a single line from a hadoop logfile.  This is used to calculate some of the statistics we can't get (or I can not see how to get) from anywhere else
 */
public class LogEntry
{
    static Logger logger = Logger.getLogger(LogEntry.class.getName());
    private String hostName;
    private String runID;
    private boolean start;
    private boolean end;
    private Calendar timestamp;
    private String logLevel;
    private String source;
    private String message;

    /*
     * Default class constructor, configures some pertinent instance variables
     */
    public void HadoopMRLogParser()
    {
        setHostName("N/A");
        setStart(false);
        setEnd(false);
        timestamp = Calendar.getInstance();
    }

    /*
     * Class constructor that configures the complete instance of this class
     * @param hostName, hostname of log reporting machine
     * @praam runID, system gemerated run identifier
     * @param start, conditional variable to assit in the determination of log sections
     * @praam end, conditional variable to assit in the determination of log sections
     * @param date, date field from the log entry
     * @praam logLevel, log message priority
     * @param source, type of message
     * @praam message, actual message
     * @return void
     */
    public void HadoopLogEntry(final String hostName, final String runID, final boolean start, final boolean end, final Calendar date, final String logLevel, final String source, final String message)
    {
        //construct a LogEntry from these details

        this.setHostName(hostName);
        this.setRunID(runID);
        this.setStart(start);
        this.setEnd(end);
        this.setDate(date);
        this.setLogLevel(logLevel);
        this.setSource(source);
        this.setMessage(message);        
    }

    /**
     * method returns the instance hostname
     * @return the hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * method sets the hostname for this instance
     * @param hostName the hostName to set
     */
    public void setHostName(final String hostName) {
        this.hostName = hostName;
    }

    /**
     * method returns the current value stored in this object for the FABAN supplied run identifier
     * @return the runID
     *
     */
    public String getRunID() {
        return runID;
    }

    /**
     *  method sets value for the FABAN supplied run identifier
     *  @param runID the runID to set
     */
    public void setRunID(final String runID) {
        this.runID = runID;
    }

    /**
     * method returnes the value for stat instance variable
     * @return the start
     */
    public boolean isStart() {
        return start;
    }

    /**
     *  methood sets the value for the instance variable start
     *  @param isStart the start to set
     */
    public void setStart(final boolean isStart) {
        this.start = isStart;
    }

    /**
     * method returns the value for the instance variable end
     * @return the end
     */
    public boolean isEnd() {
        return end;
    }

    /**
     * method sets the instance variable value for end
     * @param isEnd the end to set
     */
    public void setEnd(final boolean isEnd) {
        this.end = isEnd;
    }

    /**
     * method returns the value for the instance variable timestamp
     * @return the timestamp
     */
    public Calendar getDate() {
        return timestamp;
    }

    /**
     * method sets the current value for the timestamp instance variable
     * @param date the timestamp to set
     */
    public void setDate(final Calendar date) {
        this.timestamp = date;
    }

    /**
     * method returns the priority of this instance of the class
     * @return the logLevel
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * method sets the instance variable representing the priority of this message
     * @param logLevel the logLevel to set
     */
    public void setLogLevel(final String logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * method returnes the source of the message
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * method sets the source of the message for this instance
     * @param source the source to set
     */
    public void setSource(final String source) {
        this.source = source;
    }

    /**
     * method returns the value of the message instance variable
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * method sets the instance variable containing the message
     * @param message the message to set
     */
    public void setMessage(final String message) {
        this.message = message;
    }
}
