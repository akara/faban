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
 */
package com.sun.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.NameValuePair;
import com.sun.faban.common.TextTable;

import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.Context;
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;
import com.sun.faban.harness.tools.ToolContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
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
 * modified by Sheetal Patil
 * 
 */
public class MemcacheStats {

    private static StatsClient cache = null;
    private static Logger logger = Logger.getLogger(MemcacheStats.class.getName());
    TextTable outputTextTable = null;
    int interval;
    private SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

    /** Injected tool context. */
    @Context public ToolContext ctx;
    Command cmd;
    CommandHandle processRef;
    ArrayList<String> toolCmd;
    String toolName;
    private Timer timer;
    private PrintWriter out;
    String[] memCacheServers;
    /**
	 * This constructor creates a memcache client with a pool of servers.
     * This is for standalone invocation from main.
     * 
     * @param servers Name of servers running memcached
	 * @param interval Interval in secs for stats collection
     * @throws IOException Error communicating with memcached
     */
    public MemcacheStats(String servers[], int interval) throws IOException {
        this.interval = interval;
        cache = new StatsClient(servers);
    }

    /**
     * Constructs the memcache stats from the tool agent. A no-arg constructor
     * is required in this case. Arguments are passed via calling the configure
     * method.
     */
    public MemcacheStats() {

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
		double dblval = 0;
		DecimalFormat decval = new DecimalFormat("0.00");
		FieldPosition fld = new FieldPosition(DecimalFormat.INTEGER_FIELD);

        Map memcacheStats = cache.stats();
        //cache.stats() will return a Map whose key  is the name of the memcache server
        //and whose value is a Map with the memcache statistics

        Set<Map.Entry> serverEntries = memcacheStats.entrySet();

        //set counter to allow to set number of columns to output
        int row = 0;

        //reset the iterator
        for (Map.Entry serverEntry : serverEntries) {
            String key = (String) serverEntry.getKey();
            if (key == null)
                break;
            Map statsMap = (Map) serverEntry.getValue();
            // get rid of the ":<port>"
            String srv = key.substring(0, key.indexOf(":"));
            if (srv == null)
				break;

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
                outputTextTable.setHeader(SETS, "sets/s");
                outputTextTable.setHeader(GETS, "gets/s");
                outputTextTable.setHeader(GET_HITS, "get_hits/s");
                outputTextTable.setHeader(GET_MISSES, "get_misses/s");
                outputTextTable.setHeader(EVICTS, "evicts/s");
                outputTextTable.setHeader(BYTES_R, "rB/s");
                outputTextTable.setHeader(BYTES_W, "wB/s");
                
                previousStats = new long[serverEntries.size()][NUM_COLS+1];
            }
            outputTextTable.setField(row, 0, key);

            //get this value's iterator
            Set<Map.Entry> statsMapEntries = statsMap.entrySet();

            // Populate the rest of the table.
            for (Map.Entry statsMapEntry : statsMapEntries) {
		        StringBuffer str = new StringBuffer();
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
                if (fldKey.equals("time")) {
                    longval = Long.parseLong(fldValue) * 1000; // sec to ms
                    outputTextTable.setField(row, CURTIME,
                                                df.format(new Date(longval)));
                } else if (fldKey.equals("curr_items")) {
                    intval = Integer.parseInt(fldValue);
                    outputTextTable.setField(row, CUR_ITMS, intval.toString());
                } else if (fldKey.equals("bytes")) {
				// bytes can be large, so store in long - convert to MBs
                    longval = Long.parseLong(fldValue);
					dblval = (double)longval / 1000000;
					decval.format(dblval, str, fld);
                    outputTextTable.setField(row, BYTES, str);
                } else if (fldKey.equals("curr_connections")) {
                    intval = Integer.parseInt(fldValue);
                    outputTextTable.setField(row, CUR_CONNS, intval.toString());
                } else if (fldKey.equals("cmd_set")) {
                    longval = Long.parseLong(fldValue);
                    dblval = (double)(longval - previousStats[row][SETS])/interval;
					decval.format(dblval, str, fld);
                    outputTextTable.setField(row, SETS, str);
                    previousStats[row][SETS] = longval;
                } else if (fldKey.equals("cmd_get")) {
                    longval = Long.parseLong(fldValue);
                    dblval = (double)(longval - previousStats[row][GETS])/interval;
					decval.format(dblval, str, fld);
                    outputTextTable.setField(row, GETS, str);
                    previousStats[row][GETS] = longval;
                } else if (fldKey.equals("get_hits")) {
                    longval = Long.parseLong(fldValue);
                    dblval = (double)(longval - previousStats[row][GET_HITS])/interval;
					decval.format(dblval, str, fld);
                    outputTextTable.setField(row, GET_HITS, str);
                    previousStats[row][GET_HITS] = longval;
                } else if (fldKey.equals("get_misses")) {
                    longval = Long.parseLong(fldValue);
                    dblval = (double)(longval - previousStats[row][GET_MISSES])/interval;
					decval.format(dblval, str, fld);
                    outputTextTable.setField(row, GET_MISSES, str);
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
                // Some version of memcached do not have evicts.
                if (outputTextTable.getField(row, EVICTS) == null)
                    outputTextTable.setField(row, EVICTS, "-");
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

        /*logger.fine("Starting memcache stats");

        MemcacheStats memcacheStats = new MemcacheStats(memCacheServers, intervalTime/1000);

        try {
            Timer timer = new Timer();
            MemCacheTask task = new MemCacheTask(memcacheStats);
            timer.scheduleAtFixedRate(task, 0, intervalTime);

        } catch (Exception ex) {
            logger.severe("Exception in setting up timer " + ex);
            logger.log(Level.FINE, "Exception", ex);
            return;
        }*/
    }

    /**
     * Configures this MemcacheStats tool.
     */
    @Configure public void configure() throws ConfigurationException {
        LinkedHashSet<String> serverSet = new LinkedHashSet<String>();
        List<String> toolArgs = ctx.getToolArgs();
        if(toolArgs == null){
            throw new ConfigurationException("MemcacheStats toolArgs is not provided");
        }
        List<NameValuePair<Integer>> myHostPorts =
                ctx.getServiceContext().getUniqueHostPorts();
        if(myHostPorts == null){
            throw new ConfigurationException("Memcached host:port is not provided");
        }
        for(NameValuePair<Integer> myHostPort : myHostPorts){
            if(myHostPort.value == null)
                myHostPort.value = 11211;
            serverSet.add(myHostPort.name +":"+myHostPort.value);
        }
        String[] args = new String[toolArgs.size()];
        for(int i=0; i < toolArgs.size(); i++){
            args[i] = toolArgs.get(i);
        }
        int intervalTime = 10000; // in msecs


        if (args==null || args.length < 1) {
            logger.log(Level.INFO, "MemcacheStats [-i interval]");
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
            } 
        }
        //finished processing all of the args.  populate server list
        memCacheServers = new String[serverSet.size()];
        memCacheServers = serverSet.toArray(memCacheServers);
        interval = intervalTime/1000;
        // Trim host:ports down to the ones pertaining to this host.
        // Parse interval from the mcstat command from the tool context.
        // store interval/1000 and host ports in field interval and ports.
    }

    /**
     * Starts monitoring the memcached instances.
     * @throws IOException Error communicating with memcached
     */
    @Start public void start() throws IOException {
        logger.fine("Starting memcache stats");
        // MemcacheStats memcacheStats = new MemcacheStats(memCacheServers, intervalTime/1000);
        cache = new StatsClient(memCacheServers);

        try {
            timer = new Timer();
            out = new PrintWriter(ctx.getOutputFile());
            MemCacheTask task = new MemCacheTask(this, out);
            timer.scheduleAtFixedRate(task, 0, interval);

        } catch (Exception ex) {
            logger.severe("Exception in setting up timer " + ex);
            logger.log(Level.FINE, "Exception", ex);
            return;
        }
    }

    /**
     * Stops monitoring the memcached instances.
     */
    @Stop public void stop() {
        timer.cancel();
        out.flush();
        out.close();
    }

    /* class for TimerTask */
    private static class MemCacheTask extends TimerTask {

        private MemcacheStats memcacheStats;
        private PrintWriter out;
        public MemCacheTask(MemcacheStats memcacheStats, PrintWriter out) {
            this.memcacheStats = memcacheStats;
            this.out = out;
        }

        public void run() {
            out.println(memcacheStats.getStats());
        }
    }

    /**
     * The client code to interface with all memcached servers.
     */
    private static class StatsClient {

        ArrayList<StatsConnection> connections;
        Map<String, Map<String, String>> stats;

        /**
         * Constructs the client for all given servers.
         * @param servers host:port pairs for the server
         * @throws IOException Error communicating with memcached
         */
        public StatsClient(String[] servers) throws IOException {
            connections = new ArrayList<StatsConnection>(servers.length);
            stats = new LinkedHashMap<String, Map<String, String>>(servers.length);
            for (String server : servers)
                try {
                    StatsConnection conn = new StatsConnection(server);
                    connections.add(conn);
                    stats.put(server, conn.result);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Cannot connect to " + server +
                                ".", e);
                }
            if (connections.size() == 0)
                throw new IOException("No host available.");
        }

        /**
         * Constructs the client for the given servers on the local system.
         * @param ports The list of ports
         * @throws IOException Error communicating to memcached
         */
        public StatsClient(ArrayList<Integer> ports) throws IOException {
            connections = new ArrayList<StatsConnection>(ports.size());
            stats = new LinkedHashMap<String, Map<String, String>>(ports.size());
            for (int port : ports)
                try {
                    StatsConnection conn = new StatsConnection("localhost", port);
                    connections.add(conn);
                    stats.put("localhost:" + port, conn.result);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Cannot connect to " + port +
                                ".", e);
                }
            if (connections.size() == 0)
                throw new IOException("No host available.");
        }

        /**
         * Obtains the stats from all servers.
         * @return the stats from all servers.
         */
        public Map<String, Map<String, String>> stats() {
            for (StatsConnection connection : connections) {
                try {
                    connection.stats();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error obtaining stats from " +
                            connection.server + ".", e);
                }
            }
            return stats;
        }
    }

    private static class StatsConnection {

        String server;
        Socket socket;
        BufferedReader reader;
        OutputStream out;
        HashMap<String, String> result = new HashMap<String, String>();

        static final byte[] cmd = "stats\r\n".getBytes();

        StatsConnection(String server) throws IOException {
            this.server = server;
            int colIdx = server.indexOf(':');
            String host = server.substring(0, colIdx);
            int port = Integer.parseInt(server.substring(colIdx + 1));
            init(host, port);
        }

        StatsConnection(String host, int port) throws IOException {
            server = host + ':' + port;
            init(host, port);
        }

        private void init(String host, int port) throws IOException {
            socket = new Socket(host, port);
            out = socket.getOutputStream();
            reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
        }

        Map<String, String> stats() throws IOException {
            out.write(cmd);
            for (;;) {
                String line = reader.readLine();
                if ("END".equals(line))
                    break;
                StringTokenizer t = new StringTokenizer(line);
                String statStr = t.nextToken();
                if (!"STAT".equals(statStr))
                    throw new IOException("Expecting STAT, got " + statStr);
                String key = t.nextToken();
                String value = t.nextToken();
                result.put(key, value);
            }
            return result;
        }
    }
}
