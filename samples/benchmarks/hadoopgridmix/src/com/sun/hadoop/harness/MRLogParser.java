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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

/**
 *
 * @author Damien Cooke
 * The intention here is to read a line from the log and pop it into one of these objects then have the object
 * put into a container class for later reteieval.
 */
public class MRLogParser
{
    static Logger logger = Logger.getLogger(MRLogParser.class.getName());
    String runID = null;
    String hostname = null;

    /*
     * method processes a single directory by retrieving each logfile and disseminating the lines into descrete objects for processing by specialised classes by type.
     * @param baseDirectory, path to the base directory to locate the log files in.
     * @param runID, FABAN supplied run identifier so we put the results in the right place
     * @return collection of TimeCapture objects or null if there was an error
     */
    public Vector<TimeCapture> processDirctory(final String baseDirectory, final String runID)
    {
        Vector<TimeCapture> capturedStats = new Vector<TimeCapture>();
        Calendar startTime = Calendar.getInstance();
        String logLocation = null;
        String runName = null;

        if((baseDirectory != null)&&(baseDirectory.length() > 0))
        {
            StringTokenizer st = new StringTokenizer(baseDirectory);
            if(st.hasMoreTokens())
            {
                hostname = st.nextToken();
            }
            logLocation = baseDirectory;
        }else
        {
            logger.warning("MRLogParser.processDirctory() Bogus baseDirectory passed in" + baseDirectory);
            Vector<TimeCapture> returnTime = null;
            return returnTime;
        }

        if((runID != null)&&(runID.length() > 0))
        {
            runName = runID;
        }else
        {
            logger.warning("MRLogParser.processDirctory() Bogus runID passed in" + runID);
            Vector<TimeCapture> returnTime = null;
            return returnTime;
        }

        File logsDirectory = new File(baseDirectory);
        String[] taskIDList = null;
        if(logsDirectory.isDirectory())
        {
            //we now have a list of the tasks to be examined.
            taskIDList = logsDirectory.list();
        }else
        {
            logger.warning("MRLogParser.processDirctory() logsDirectory passed in is not a directory: "+ baseDirectory);
            Vector<TimeCapture> returnTime = null;
            return returnTime;
        }

        Arrays.sort(taskIDList);
        List<String> taskList = Arrays.asList(taskIDList);

        //now create two arrays one for the reducers and one for the mappers
        ArrayList<String> mapperList = new ArrayList<String>();
        ArrayList<String> reducerList = new ArrayList<String>();

        //only capture the files that are for this run.
        //taskList.trimToSize();

        String  lastDirectory;
        //if it is the first time it has been run since reboot we need to grab the first one
        //logger.warning("taskList size = "+ taskList.size());

        if(taskList.size() == 1)
        {
            lastDirectory = taskList.get(taskList.size());

        }else //else we get the last but 1
        {
            lastDirectory = taskList.get(taskList.size() -1);
        }

        String[] directoryPattern = lastDirectory.split("_");
        lastDirectory = directoryPattern[1];
        directoryPattern = null;
        //logger.warning("Directory patten found = "+directoryPattern[1]);

        String mapredTask;
        for(Iterator<String> sit = taskList.iterator(); sit.hasNext();)
        {
            while(sit.hasNext())
            {
                mapredTask = sit.next();

                if(mapredTask.contains(lastDirectory) == true )//only include items from the last run
                {                    
                    if(mapredTask.contains("_m_") == true )
                    {
                        //put the mappers in their own container
                        mapperList.add(mapredTask);

                     
                    }else if(mapredTask.contains("_r_") == true )
                    {
                        //put the reducers in their own container
                        reducerList.add(mapredTask);
                    }
                }
            }
        }

        taskList = null;
        mapredTask = null;

        logger.info("Mapper list contains "+mapperList.size());
        logger.info("Reducer List contains "+reducerList.size());

        TimeCapture timeConsumedinMap = calculateMRTime(mapperList, baseDirectory);
        TimeCapture timeConsumedinRed = calculateMRTime(reducerList, baseDirectory);
        TimeCapture timeConsumedinShuffle = calculateShuffleTime(reducerList, baseDirectory);
        TimeCapture timeConsumedinReduceWrite = calculateWriteTime(reducerList, baseDirectory);

        logger.info("Mappers combined time taken = "+timeConsumedinMap.getTotalAcumulatedTime()/1000 + " seconds Starting  @" +timeConsumedinMap.getFirstRecord().getTime().toString());
        logger.info("Reducers combined time taken = "+timeConsumedinRed.getTotalAcumulatedTime()/1000 + " seconds Starting  @" +timeConsumedinRed.getFirstRecord().getTime().toString());
        logger.info("Shuffle combined time taken = "+timeConsumedinShuffle.getTotalAcumulatedTime() + " milliseconds Starting  @" +timeConsumedinShuffle.getFirstRecord().getTime().toString());
        logger.info(" Reduce write combined time taken = "+timeConsumedinReduceWrite.getTotalAcumulatedTime() + "  miliseconds Starting  @" +timeConsumedinReduceWrite.getFirstRecord().getTime().toString());
        Calendar finishTime = Calendar.getInstance();
        logger.info("Time taken to process logs = " + (finishTime.getTimeInMillis() - startTime.getTimeInMillis()) + " milliseconds");

        //set the tags so we do not care for the order they are written into the vector
        timeConsumedinMap.setTag("MAP");
        timeConsumedinRed.setTag("REDUCE");
        timeConsumedinShuffle.setTag("SHUFFLE");
        timeConsumedinReduceWrite.setTag("WRITE");

        //copy the results into the vector for returning.
        capturedStats.add((TimeCapture)timeConsumedinMap);
        capturedStats.add((TimeCapture)timeConsumedinRed);
        capturedStats.add((TimeCapture)timeConsumedinShuffle);
        capturedStats.add((TimeCapture)timeConsumedinReduceWrite);

        return capturedStats;
    }

    /*
     * Reades a file of log entries into a vector of LogEntry objects
     * @param  fileToRead file whoese contents need to read into the Vector
     * @return collection of type LogEntry for further processing
     */
    public Vector<LogEntry> jobLogFileReader(final String fileToRead)
    {
        Vector<LogEntry> logEntries = new Vector<LogEntry>();
        try
        {
            BufferedReader logReader = new BufferedReader(new FileReader(fileToRead));
            String line = logReader.readLine();
            LogEntry entry;
            while((line != null)&&(line.length() != 0))
            {
                entry = parseLine(line);
                if(entry != null)
                {
                    logEntries.add((LogEntry)entry);
                }
                
                line = logReader.readLine();
            }
            logReader.close();
            line = null;
        }catch(IOException ioe)
        {
            logger.warning("Some file IO error in jobLogFileReader occured with file fileToRead : "+ioe.getMessage());
            Vector<LogEntry>returnVector = null;
            return returnVector;
        }
        return (Vector<LogEntry>)logEntries;
    }

    /*
     * Calculates the time spent writing in the reduce phase of the process
     * @param  reduceList an ArrayList of directories holdiing files to process
     * @param  baseDirectory holds the location of the base directory for these directories
     * @return TimeCapture object encapsulating processing time data for further processing
     *
     */
    public TimeCapture calculateWriteTime(final ArrayList<String> reduceList, final String baseDirectory)
    {
        boolean isFirst = true;
        TimeCapture timeConsumed = new TimeCapture();

        for(Iterator<String> it = reduceList.iterator();it.hasNext();)
        {
            Vector<LogEntry> reduceLogEntries = new Vector<LogEntry>();
            reduceLogEntries = (Vector<LogEntry>)jobLogFileReader(baseDirectory+"/"+it.next()+"/syslog");
            if(reduceLogEntries == null) //something failed here
            {
                logger.warning("taskLogEntries artificially empty in calculateWriteTime");
                TimeCapture returnTime = null;
                return returnTime;
            }else
            {
                Calendar start = null;
                Calendar end = null;                
                for(Iterator<LogEntry> itle = reduceLogEntries.iterator();itle.hasNext();)
                {
                    LogEntry logentry = itle.next();
                    if(logentry.getDate() == null)
                    {
                        logger.info("date is null");
                        TimeCapture returnTime = null;
                        return returnTime;

                    }else
                    {
                        //logger.info(logentry.getMessage());
                        if(logentry.getMessage().contains("commiting") == true)
                        {

                            start = logentry.getDate();
                            if(isFirst)
                            {
                                timeConsumed.setFirstRecord(start);
                                isFirst = false;
                            }

                            //(logentry.getSource().compareTo("org.apache.hadoop.mapred.FileOutputCommitter") == 0) &&
                        }else if(logentry.getMessage().contains("Saved output of task") == true)
                        {                            
                            end = logentry.getDate();

                            timeConsumed.setTotalAcumulatedTime((end.getTimeInMillis() - start.getTimeInMillis() ));                        
                            end = null;
                            start = null;
                        }
                    }

                }
            }
        }
        return timeConsumed;
    }

    /*
     * Calculates the time spent shuffling in the reduce phase of the process
     * @param  reduceList an ArrayList of directories holdiing files to process
     * @param  baseDirectory holds the location of the base directory for these directories
     * @return TimeCapture object encapsulating processing time data for further processing
     *
     */
    public TimeCapture calculateShuffleTime(final ArrayList<String> reduceList, final String baseDirectory)
    {
        TimeCapture timeConsumed = new TimeCapture();
        boolean isFirst = true;

        for(Iterator<String> it = reduceList.iterator();it.hasNext();)
        {
            Vector<LogEntry> reduceLogEntries = new Vector<LogEntry>();
            reduceLogEntries = (Vector<LogEntry>)jobLogFileReader(baseDirectory+"/"+it.next()+"/syslog");
            if(reduceLogEntries == null) //something failed here
            {
                logger.warning("reduceLogEntries artificially empty in calculateShuffleTime");
                TimeCapture returnTime = null;
                return returnTime;
            }else
            {
                Calendar start = null;
                Calendar end = null;
                for(Iterator<LogEntry> itle = reduceLogEntries.iterator();itle.hasNext();)
                {                                       
                    LogEntry logentry = itle.next();
                    if(logentry.getDate() == null)
                    {
                        logger.info("date is null");
                        TimeCapture returnTime = null;
                        return returnTime;
                    }else
                    {
                        if(logentry.getMessage().contains("Shuffling") == true)
                        {
                            start = logentry.getDate();
                            if(isFirst)
                            {
                                timeConsumed.setFirstRecord(start);
                                isFirst = false;
                            }
                            

                        }else if(logentry.getMessage().contains("Rec #") == true)
                        {
                            end = logentry.getDate();

                            if((start != null) && (end != null))
                            {
                                timeConsumed.setTotalAcumulatedTime((end.getTimeInMillis() - start.getTimeInMillis() ));
                            }else
                            {
                                //logger.info("either start or end == null");
                                timeConsumed.setTotalAcumulatedTime(0);
                            }

                            
                            //timeConsumed = timeConsumed + (end.getTimeInMillis() - start.getTimeInMillis() );
                            end = null;
                            start = null;
                        }
                    }
                }
            }
        }
        return timeConsumed;
    }

    /*
     * Calculates the map or reduce time depending on the list of files supplied
     * @param  mrList an ArrayList of directories holdiing files to process
     * @param  baseDirectory holds the location of the base directory for these directories
     * @return TimeCapture object encapsulating processing time data for further processing
     *
     */
    public TimeCapture calculateMRTime(final ArrayList<String> mrList, final String baseDirectory)
    {
        boolean isFirst = true;
        TimeCapture timeConsumed = new TimeCapture();

        for(Iterator<String> it = mrList.iterator();it.hasNext();)
        {
            Vector<LogEntry> taskLogEntries = new Vector<LogEntry>();

            String currentLogDir =  it.next();


            taskLogEntries = (Vector<LogEntry>) jobLogFileReader(baseDirectory+"/"+currentLogDir+"/syslog");

            if(taskLogEntries == null) //something failed here
            {
                logger.warning("taskLogEntries artificially empty in calculateMRTime");
                //TimeCapture returnTime = null;
                //return returnTime;
            }else
            {
                for(Iterator<LogEntry> itle = taskLogEntries.iterator();itle.hasNext();)
                {

                    LogEntry logentry = itle.next();
                    if(logentry.getDate() == null)
                    {
                        logger.info("date is null");
                        TimeCapture returnTime = null;
                        return returnTime;
                    }
                }

                if(taskLogEntries.isEmpty())
                {
                    logger.warning("Log file is empty: "+baseDirectory+"/"+currentLogDir+"/syslog");
                    
                }else
                {
                    Calendar start =  ((LogEntry)taskLogEntries.firstElement()).getDate();
                    if(isFirst)
                    {
                        timeConsumed.setFirstRecord(start);
                        isFirst = false;
                    }

                    Calendar end = ((LogEntry)taskLogEntries.lastElement()).getDate();
                    timeConsumed.setTotalAcumulatedTime((end.getTimeInMillis() - start.getTimeInMillis() ));

                }
            }
        }
        return timeConsumed;
    }


    /*
     * Parse a line passed to the method looking for significant log entries
     * @param line Line to parse.
     * @return LogEntry for further processing
     */
    public LogEntry parseLine(final String line) throws IOException
    {
        LogEntry result = null;

        //logger.warning("Line found = "+line);

        if( (line != null)&&(line.length() != 0))
        {          
          String date;
          String time;
          String loglevel;
          String classname;
          String message;
          String[] processedLine = line.split(" ");
          String[] parsedForMessages = line.split(":");

          try
          {
              if(processedLine[3] == null)
              {
                  logger.warning("processedLine[3] == null");
                  logger.warning("error located Line passed in was: "+line);
              }
          }catch(ArrayIndexOutOfBoundsException aiob)
          {
                            
              result = null;
              return result;
          }

          date = processedLine[0];
          time = processedLine[1];
          loglevel = processedLine[2];
          classname = processedLine[3];
          //had to do a second split to get the messages.


          if(parsedForMessages.length > 3)
          {
              StringBuffer sb = new StringBuffer();
              for(int i = 3; i < parsedForMessages.length; i++)
              {
                  sb.append(parsedForMessages[i]);
                  sb.append(":");
              }
              message = sb.toString();
          }else
          {
              logger.warning("There is a problem with your hadoop system!");
              logger.warning("We have detected log entries that are not supposed to be there.");
              logger.warning("We could just ignore these but it is better that you know.");
              logger.warning("exception Line passed in was: "+line);
              logger.warning("Line examined = "+line);
              message = parsedForMessages[3];
          }
          
          //date needs further processing
          String[] dateString = date.split("-");
          Calendar timeStamp = Calendar.getInstance();
          timeStamp.set(Calendar.YEAR,Integer.parseInt(dateString[0]));
          timeStamp.set(Calendar.MONTH, Integer.parseInt(dateString[1]));
          timeStamp.set(Calendar.DATE, Integer.parseInt(dateString[2]));

          String[] sec_milisecString = time.split(",");
          String[] timeString = sec_milisecString[0].split(":");


          timeStamp.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeString[0]));
          timeStamp.set(Calendar.MINUTE, Integer.parseInt(timeString[1]));
          timeStamp.set(Calendar.SECOND, Integer.parseInt(timeString[2]));
          timeStamp.set(Calendar.MILLISECOND, Integer.parseInt(sec_milisecString[1]));

          //now create a new LogEntry to store the entry
          result = new LogEntry();

          //we do not have the hosname or runid info at this point.
          result.setDate(timeStamp);
          result.setLogLevel(loglevel);
          result.setSource(classname);
          result.setMessage(message);

          if(result.getDate() == null)
          {
              logger.warning("Error in getParser date is null");
          }
        }else
        {
            result = null;
            return result;
        }


    return result;
  }
  
}
