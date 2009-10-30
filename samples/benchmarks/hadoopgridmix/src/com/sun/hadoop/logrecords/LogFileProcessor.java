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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 *
 * @author Damien Cooke
 * //
 */
public class LogFileProcessor {

    static Logger logger = Logger.getLogger(LogFileProcessor.class.getName());

    // Formatter is used to format dates with the status changes.
    private static SimpleDateFormat formatter = new SimpleDateFormat(
                              "yyyy'-'MM'-'dd kk':'mm':'ss','SSS ");

    /*
     * Takes a file name and returns a list of objects that contain a message type of a given type
     * Currently the two possibilities are JobRecord or a jobTrackerRecord
     */
    public ArrayList<AbstractLogRecord> processLogFile(final String filename, METRIC_TYPE logType)
    {
        ArrayList<AbstractLogRecord> returnList = new ArrayList<AbstractLogRecord>();
        logger.info("File passed in was "+filename);

        try
        {
            BufferedReader logReader = new BufferedReader(new FileReader(filename));
            String line = logReader.readLine();
            while(line != null)
            {

                AbstractLogRecord logRecord = getLogRecord(line);

                if(logRecord == null)
                {
                    logger.warning("getLogRecord(line) failed to return a valid object");
                    ArrayList<AbstractLogRecord> returnArray = null;
                    return returnArray;
                }


                String foundType = logRecord.getClass().toString();
                if((logType == METRIC_TYPE.JobRecord) && (foundType.contains("JobRecord")))
                {
                    //only store jobRecords
                    returnList.add(logRecord);

                }else if((logType == METRIC_TYPE.JobtrackerRecord) && (foundType.contains("JobtrackerRecord")))
                {
                    //only store jobTrackerRecords
                    returnList.add(logRecord);

                }else if((logType == METRIC_TYPE.ShuffleInputRecord)&&(foundType.contains("ShuffleInputRecord")))
                {
                    //only store ShuffleOutputRecords
                    returnList.add(logRecord);

                }else if((logType == METRIC_TYPE.ShuffleOutputRecord)&&(foundType.contains("ShuffleOutputRecord")))
                {
                    //only store ShuffleInputRecords
                    returnList.add(logRecord);
                }



                line = logReader.readLine();
            }
            logReader.close();
            line = null;
        }catch(IOException ioe)
        {
            logger.warning("Some file IO error in jobLogFileReader occured with file fileToRead : "+ioe.getMessage());
            ArrayList<AbstractLogRecord> returnArray = null;
            return returnArray;
        }


        return returnList;
    }

    /**
     * Purpose of this method is to parse a line then depending on the type call a specilised class to further the processing.
     * in development we used a modified Hadoop that produced a timestamp in the logrecords logfiles, in order to release this product we had to remove this feature
     * method extracts the datestamp (which does not exist) so it fills in a dummy value for this.
     * @param line is the string that will be processed
     * @return a specilisation extending AbstractLogParser
     *
     */
    public static AbstractLogRecord getLogRecord(final String line)
    {
        AbstractLogRecord record = null;

        if (line != null)
        {

          /*
           * 2009-05-21 17:39:15,204 mapred.job: counter=Map input bytes, group=Map-Reduce Framework, hostName=r3p4150-01, jobId=job_200905211738_0001, jobName=sorter, sessionId=, user=root, value=3.70063552E8
           * 2009-05-21 17:39:15,204 mapred.jobtracker: hostName=r3p4150-01, sessionId=, jobs_completed=0, jobs_submitted=1, maps_completed=11, maps_launched=13, reduces_completed=0, reduces_launched=0
           * Here we want to extract the date and metric type from the line
           */

          // Note: The date/time information is only available in some installations.
          int splitPoint = line.lastIndexOf(':');

          if (splitPoint <= 0)
          {
              logger.warning("line passed is not of type JobTracker or Job too short?:");
              logger.warning(line);
              return null;
          }
          String head = line.substring(0, splitPoint);
          String detail = line.substring(splitPoint + 1).trim();

          StringTokenizer st = new StringTokenizer(head, " ");
          String date = null;
          String time = null;
          if (st.hasMoreTokens())
          {
              date = st.nextToken();
          }
          if (st.hasMoreTokens())
          {
              time = st.nextToken();
          }


          Date timestamp = null;

          if (time != null) {
              try {
                  timestamp = formatter.parse(date + ' ' + time);
              } catch (ParseException e) {
                  // No time information in the record.
              }
          }


          if(head.endsWith("mapred.jobtracker"))
          {
              //we have a jobtracker metric
              record = new JobtrackerRecord();
              record.setTimestamp(timestamp);
              if(record.parse(detail) == true)
              {
                  return record;
              }

          }else if(head.endsWith("mapred.job"))
          {
              //we have a job metric
              record = new JobRecord();
              record.setTimestamp(timestamp);
              if(record.parse(detail) == true)
              {
                  return record;
              }
          }else if(head.endsWith("mapred.shuffleInput"))
          {
              //we have a shuffle output record
              record = new ShuffleInputRecord();
              record.setTimestamp(timestamp);
              if(record.parse(detail) == true)
              {
                  return record;
              }
          }else if(head.endsWith("mapred.shuffleOutput"))
          {
              //we have a shuffle output record
              record = new ShuffleOutputRecord();
              record.setTimestamp(timestamp);
              if(record.parse(detail) == true)
              {
                  return record;
              }
          }else if(head.endsWith("mapred.tasktracker"))
          {
              //we have a shuffle output record
              record = new TasktrackerRecord();
              record.setTimestamp(timestamp);
              if(record.parse(detail) == true)
              {
                  return record;
              }
          }else
          {
              logger.warning("line passed is not of type JobTracker, Job, shuffleInput, tasktracker or shuffleOutput:");
              logger.warning(line);
              return null;
          }
     }else
     {
        logger.warning("line passed in was null for MetricsLogParserBase.getParser(String line)");
        return null;
     }

     return null; //this never happens
  }
}
