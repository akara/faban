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
 * $Id: MemcacheStats.java,v 1.2 2008/04/10 20:46:47 shanti_s Exp $
 */
package com.sun.faban.harness.tools;

import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;
import com.sun.faban.common.TextTable;

import com.sun.faban.harness.RemoteCallable;
import com.sun.faban.harness.RunContext;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MemcacheStats.java
 * This program implements a tool to collect memcache stats from
 * a list of servers. The stats are collected and displayed at the
 * specified interval. Most of the stats are on a per. second basis
 * allowing easy tabulation and comparison with other tools.
 *
 * @author Shanti Subramanyam based on work by Kim LiChong
 * 
 */
public class MemcacheStats {

    private static MemCachedClient cache = null;
    static Logger logger = Logger.getLogger(
            MemcacheStats.class.getName());
    TextTable outputTextTable = null;
    int interval;
    private SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

    /**
	 * This constructor creates a memcache client with a pool of servers
     * 
     * @param servers -  name of servers running memcached
	 * @param interval - interval in secs for stats collection
     */
    public MemcacheStats(String servers[], int interval) {
        this.interval = interval;

        logger.info("Connecting to " + servers[0]);
        SockIOPool pool = SockIOPool.getInstance("statsPool");
        pool.setServers(servers);
        pool.initialize();

        cache = new MemCachedClient();
        cache.setPoolName("statsPool");
    }
    
    public static GregorianCalendar getGregorianCalendar(String hostName)
            throws Exception {
        return RunContext.exec(hostName, new RemoteCallable<GregorianCalendar>() {
            public GregorianCalendar call() {
                return new GregorianCalendar();
            }
        });
    }


    /*
     * Columns in which the various stats fields will appear
     */
    private static final int CURTIME = 1;
    private static final int CUR_ITMS = 2;
    private static final int BYTES = 3;
    private static final int CUR_CONNS = 4;
    private static final int SETS = 5;
    private static final int GETS = 6;
    private static final int GET_HITS = 7;
    private static final int GET_MISSES = 8;
    private static final int EVICTS = 9;
    private static final int BYTES_R = 10;
    private static final int BYTES_W = 11;

    private static final int NUM_COLS = 11;
    long[][] previousStats;
    
    /**
	 * This method is used for dynamic memcache stats gathering.
     * We only gather and print out the following stats in a single row:
     * cur_itms bytes cur_conns sets gets get_hits get_misses evicts bytes_r, bytes_w
     * Since memcached returns cumulative stats, we do the subtraction to get
     * the stats for this interval and then figure out stats/sec
     * @return TextTable - a single row of stats
     */
    public TextTable getStats() {
        Integer intval = 0;
		long longval = 0;
        String curTime = null;

        Map memcacheStats = cache.stats();
        //cache.stats() will return a Map whose key  is the name of the memcache server
        //and whose value is a Map with the memcache statistics

        Set<Map.Entry> serverEntries = memcacheStats.entrySet();

        //set counter to allow to set number of columns to output
        int row = 0;

		boolean ignoreTime = false;
		GregorianCalendar cal = null;

        //reset the iterator
        for (Map.Entry serverEntry : serverEntries) {
            int column = 0;
            String key = (String) serverEntry.getKey();
            if (key == null)
                break;
            Map statsMap = (Map) serverEntry.getValue();
            // get rid of the ":<port>"
            String srv = key.substring(0, key.indexOf(":"));
            if (srv == null)
				break;
			/*
			 * Note the following requires a CmdAgent on the server machine
			 * So this will only work in the context of a benchmark run
			*/
			try {
			    cal = getGregorianCalendar(srv);
				ignoreTime = false;
            } catch (Exception ex) {
				logger.log(Level.WARNING, "Couldn't get calendar on host " + srv, ex);
                ignoreTime = true;
            }
			//cal = new GregorianCalendar();

			if ( !ignoreTime)
                curTime = df.format(cal.getTime());
            if (outputTextTable == null) {
                // the number of rows is #of servers (for each interval)
                // One extra column for server name
                outputTextTable = new TextTable(serverEntries.size(), NUM_COLS + 1);

                // the number of columns is the stats that we gather
                //set Header
                outputTextTable.setHeader(0, "Server");
                outputTextTable.setHeader(CURTIME, "Time");
                outputTextTable.setHeader(CUR_ITMS, "items");
                outputTextTable.setHeader(BYTES, "cache_MB");
                outputTextTable.setHeader(CUR_CONNS, "conns");
                outputTextTable.setHeader(SETS, "sets");
                outputTextTable.setHeader(GETS, "gets");
                outputTextTable.setHeader(GET_HITS, "get_hits");
                outputTextTable.setHeader(GET_MISSES, "get_misses");
                outputTextTable.setHeader(EVICTS, "evicts");
                outputTextTable.setHeader(BYTES_R, "rB");
                outputTextTable.setHeader(BYTES_W, "wB");
                
                previousStats = new long[serverEntries.size()][NUM_COLS+1];
            }
            outputTextTable.setField(row, CURTIME, curTime);
            outputTextTable.setField(row, 0, key);

            //get this value's iterator
            Set<Map.Entry> statsMapEntries = statsMap.entrySet();

            // Populate the rest of the table.
            for (Map.Entry statsMapEntry : statsMapEntries) {
                String fldKey = statsMapEntry.getKey().toString();
                String fldValue = ((CharSequence)(statsMapEntry.getValue())).toString();
                logger.fine("key = " + fldKey + ", value = " + fldValue);

				/*
                 * NOTE: Although it seems that we're doing a portion of the same 
                 * code (setting val, curVal) in all of the if statements, we can't
                 * take it out in a common way. This is because other stats that
                 * we're not looking at are not integers.
				 */
				 /* We do absolute stats for CUR_ITMS,BYTES and CUR_CONNS */
                if (fldKey.equals("curr_items")) {
                    intval = Integer.parseInt(fldValue);
                    outputTextTable.setField(row, CUR_ITMS, intval.toString());
                } else if (fldKey.equals("bytes")) {
				// bytes can be large, so store in long - convert to MBs
                    longval = Long.parseLong(fldValue);
					intval = (int)(longval / 1000000);
                    outputTextTable.setField(row, BYTES, intval.toString());
                } else if (fldKey.equals("curr_connections")) {
                    intval = Integer.parseInt(fldValue);
                    outputTextTable.setField(row, CUR_CONNS, intval.toString());
                } else if (fldKey.equals("cmd_set")) {
                    longval = Long.parseLong(fldValue);
                    intval = (int)((longval - previousStats[row][SETS])/interval);
                    outputTextTable.setField(row, SETS, intval.toString());
                    previousStats[row][SETS] = longval;
                } else if (fldKey.equals("cmd_get")) {
                    longval = Long.parseLong(fldValue);
                    intval = (int)((longval - previousStats[row][GETS])/interval);
                    outputTextTable.setField(row, GETS, intval.toString());
                    previousStats[row][GETS] = longval;
                } else if (fldKey.equals("get_hits")) {
                    longval = Long.parseLong(fldValue);
                    intval = (int)((longval - previousStats[row][GET_HITS])/interval);
                    outputTextTable.setField(row, GET_HITS, intval.toString());
                    previousStats[row][GET_HITS] = longval;
                } else if (fldKey.equals("get_misses")) {
                    longval = Long.parseLong(fldValue);
                    intval = (int)((longval - previousStats[row][GET_MISSES])/interval);
                    outputTextTable.setField(row, GET_MISSES, intval.toString());
                    previousStats[row][GET_MISSES] = longval;
                } else if (fldKey.equals("evictions")) {
                    longval = Long.parseLong(fldValue);
                    intval = (int)((longval - previousStats[row][EVICTS])/interval);
                    outputTextTable.setField(row, EVICTS, intval.toString());
                    previousStats[row][EVICTS] = longval;
                } else if (fldKey.equals("bytes_read")) {
                    longval = Long.parseLong(fldValue);
                    intval = (int)((longval - previousStats[row][BYTES_R])/interval);
                    outputTextTable.setField(row, BYTES_R, intval.toString());
                    previousStats[row][BYTES_R] = longval;
                } else if (fldKey.equals("bytes_written")) {
                    longval = Long.parseLong(fldValue);
                    intval = (int)((longval - previousStats[row][BYTES_W])/interval);
                    outputTextTable.setField(row, BYTES_W, intval.toString());
                    previousStats[row][BYTES_W] = longval;
                }                
            }
            row++;
        }
        return outputTextTable;
    }

    /**
     * This main method is used to gather dynamic statistics on memcache server instances.
	 * The primary arguments are the names of the memcached servers 
     *  It accepts the following optional argument:
     * -i interval   the snapshot period to collect the stats, in seconds (default 10)
     *  Usage:  java com.sun.faban.harnes.tools.MemcacheStats host[:port]... [-i interval]
	 * It creates an instance of MemcacheStats and sets up a timer task
	 * at the specified interval to gather the stats.
     *  @param args String []
     *
     */
    public static void main(String[] args) {
        int intervalTime = 10000; // in msecs
        LinkedHashSet<String> serverSet = new LinkedHashSet<String>();

        if (args==null || args.length < 1) {
            System.err.println("Usage: java com.sun.faban.harness.tools.MemcacheStats host[:port]... [-i interval]");
                System.exit(1);
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-i")) {
                if (args[i].length() > 2) // -iarg
                {
                    intervalTime =
                            Integer.parseInt(args[i].substring(2)) * 1000;
                } else // -i arg
                {
                    intervalTime = Integer.parseInt(args[++i]) * 1000;
                }
            } else if (args[i].contains(":")) {// host:port pair
                serverSet.add(args[i]);
            } else { // host only. Append default port 11211.
                serverSet.add(args[i] + ":11211");
            }
        }
        //finished processing all of the args.  populate server list
        String memCacheServers[] = new String[serverSet.size()];
        memCacheServers = serverSet.toArray(memCacheServers);

        logger.info("Starting memcache stats");

        MemcacheStats memcacheStats = new MemcacheStats(memCacheServers, intervalTime);

        try {
            Timer timer = new Timer();
            MemCacheTask task = new MemCacheTask(memcacheStats);
            timer.scheduleAtFixedRate(task, 0, intervalTime);

        } catch (Exception ex) {
            logger.severe("Exception in setting up timer " + ex);
            logger.log(Level.FINE, "Exception", ex);
            return;
        }
    }

    /* class for TimerTask */
    private static class MemCacheTask extends TimerTask {

        private MemcacheStats memcacheStats;
        public MemCacheTask(MemcacheStats memcacheStats) {
            this.memcacheStats = memcacheStats;
        }

        public void run() {
            System.out.println(memcacheStats.getStats());
        }
    }
}
