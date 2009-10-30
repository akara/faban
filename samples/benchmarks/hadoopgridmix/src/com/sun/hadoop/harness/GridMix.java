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

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;

import com.sun.faban.common.TextTable;
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.EndRun;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.PostRun;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.StartRun;
import com.sun.hadoop.logrecords.*;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.logging.Logger;
import java.io.FileNotFoundException;
import java.io.IOException;
import static com.sun.faban.harness.RunContext.*;
import java.util.concurrent.ConcurrentHashMap;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Harness hook for the hadoop hadoop benchmark.
 *
 * @author Damien Cooke
 */
//public class GridMix implements Benchmark {
public class GridMix{
    
    
    static Logger logger = Logger.getLogger(GridMix.class.getName());
    int totalRunningTimeInSecs = 0;    
    private String hadoopHome;
    private String gridmixHome;
    private String hadoopConfPath;
    private String hadoopVersion;
    private String benchmarkName;
    private Calendar startTime;
    private Calendar endTime;
    private CommandHandle benchHandle = null;
    private ArrayList<String> slaveList;
    private ArrayList<String> masterList;
    private String runDir;
    private String runID;
    private ParamRepository params;
    private String otherHadoopJob;
    private String javaHome;
    private String hadoopLogDir;
    
    
   

    /**
     * This method is called to configure the specific benchmark run
     * Tasks done in this method include reading user parameters,
     * logging them and initializing various local variables.
     *
     * @throws Exception If configuration was not successful
     **/

     @Configure public void configure() throws Exception{


        logger.info("Benchmark configure starting");
        BufferedWriter bf = null;
        runDir = getOutDir();
        runID = getRunId();
        params = getParamRepository();

        //Obtaining configuration parameters
        gridmixHome = params.getParameter("hadoopConfig/GridMixPath");
        hadoopHome = params.getParameter("hadoopConfig/hadoopHomeDir");
        hadoopVersion = params.getParameter("hadoopConfig/hadoopVersion");
        hadoopConfPath = hadoopHome + File.separator + "conf" + File.separator;
        benchmarkName = params.getParameter("hadoopConfig/benchmarkComboName");
        otherHadoopJob = params.getParameter("hadoopConfig/hadoopJob");

        //collect details for hadoop-env.sh
        javaHome = params.getParameter("fh:jvmConfig/fh:javaHome");
        hadoopLogDir = params.getParameter("hadoopConfig/hadoopLogDir");

        //collect the master(s) and slaves from the interface and place them in
        slaveList = new ArrayList<String>(Arrays.<String>asList(params.getParameter("slaveConfig/fa:hostConfig/fa:host").split(" ")));
        masterList = new ArrayList<String>(Arrays.<String>asList(params.getParameter("masterConfig/fa:hostConfig/fa:host").split(" ")));        
                
        // Create file "masters" and "slaves" in ConfPath
        try
        {            
            bf = new BufferedWriter(new FileWriter(hadoopHome + File.separator + "conf" + File.separator + "masters"));
            for(Iterator<String> it = masterList.iterator();it.hasNext();)
            {
                String master = it.next();
                bf.write(master, 0, master.length());
                bf.newLine();
            }            
            bf.close();
        } catch (Exception fe)
        {
            throw new Exception("Could not create masters file in " + hadoopConfPath, fe);
        }

        
        // Repeat for slaves        
        try
        {            
            bf = new BufferedWriter(new FileWriter(hadoopHome + File.separator + "conf" + File.separator + "slaves"));
            for(Iterator<String> it = slaveList.iterator();it.hasNext();)
            {
                String slave = it.next();
                bf.write(slave, 0, slave.length());
                bf.newLine();
            }            
            bf.close();
        } catch (Exception fe)
        {
            throw new Exception("Could not create slaves file in " + hadoopConfPath, fe);
        }

        //configure the logrecords
        if(this.generateMetricsFile(runDir) == true)
        {
            logger.info("Metrics generation config done.");
        }else
        {
            logger.warning("Metrics generation config not done.");
        }


        //alterHadoopConfigFile
        //configure the hadoop-env.xml file

        if(this.alterHadoopConfigFile(runDir) == true)
        {
            logger.info("Hadoop environment generation config done.");
        }else
        {
            logger.warning("Hadoop environment generation config not done.");
        }

                     
        //collect all advanced configs                        
        
        String key;
        String value;

        
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String,String> changesForCoreConfig = compareConfigs(getAdvancedCoreDefaultConfig(), gatherAdvancedCoreConfig());
        logger.info("changesForCoreConfig contains: "+changesForCoreConfig.size());
        for (Enumeration changesForCoreConfigEnumerator = changesForCoreConfig.keys(); changesForCoreConfigEnumerator.hasMoreElements();)
        {
            key = (String)changesForCoreConfigEnumerator.nextElement();
            value = (String)changesForCoreConfig.get(key);            
        }

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String,String> changesForHDFSConfig = compareConfigs(getAdvancedHDFSDefaultConfig(), gatherAdvancedHDFSConfig());
        logger.info("changesForHDFSConfig contains: "+changesForHDFSConfig.size());
        for (Enumeration changesForHDFSConfigEnumerator = changesForHDFSConfig.keys(); changesForHDFSConfigEnumerator.hasMoreElements();)
        {
            key = (String)changesForHDFSConfigEnumerator.nextElement();
            value = (String)changesForHDFSConfig.get(key);            
        }

        @SuppressWarnings("unchecked")
        ConcurrentHashMap <String,String>changesForMapredConfig = compareConfigs(getAdvancedMapredDefaultConfig(), gatherAdvancedMapredConfig());
        logger.info("changesForMapredConfig contains: "+changesForMapredConfig.size());
        for (Enumeration changesForMapredConfigEnumerator = changesForMapredConfig.keys(); changesForMapredConfigEnumerator.hasMoreElements();)
        {
            key = (String)changesForMapredConfigEnumerator.nextElement();
            value = (String)changesForMapredConfig.get(key);            
        }


        if(!writeConfigFiles(changesForCoreConfig,changesForHDFSConfig,changesForMapredConfig))
        {            
            logger.warning("writeConfigFiles has failed, writeConfigFiles failed");
        } 
        
        logger.info("Benchmark configure ending");
    }

    /*
     * This method uses collected data from the FABAN form and alters the the hadoop-env.sh file to ensure the java_home and log diectories are correct.
     * @param void
     * @return void
     */
    public boolean alterHadoopConfigFile(final String resultsLocation)
    {     
        String originalConfigFile = hadoopHome + "/conf/hadoop-env.sh";
        String configFile = resultsLocation + "com.sun.pae.hadoop.harness/hadoop-env.sh";

		ArrayList<String> config_values = new ArrayList<String>();

		try
		{
			BufferedReader hadoop_env = new BufferedReader(new FileReader(originalConfigFile));
			String line;

			while ( (line = hadoop_env.readLine()) != null)
			{
				config_values.add(line);
			}
            
            hadoop_env.close();

		}catch (IOException ioe)
		{
            logger.warning("IOException while reading the configuration file in alterHadoopConfigFile.  The message was "+ioe.getMessage());
            return false;
		}

		try
		{
			if(config_values.size() > 0)
			{
				BufferedWriter bf = new BufferedWriter(new FileWriter(configFile));
				String value;
				for(Iterator<String> itConfig = config_values.iterator();itConfig.hasNext();)
				{
					value = itConfig.next();
					if((value.startsWith("export JAVA_HOME=") == true) || (value.startsWith("# export JAVA_HOME=") == true))
					{
						value = "export JAVA_HOME=" + javaHome;
					}else if ((value.startsWith("export HADOOP_LOG_DIR=") == true) || (value.startsWith("# export HADOOP_LOG_DIR=") == true))
					{
						value = "export HADOOP_LOG_DIR=" + hadoopLogDir;
					}
					bf.write(value, 0, value.length());
					bf.newLine();
				}
				bf.close();

                //now copy them to all of the slaves
                ArrayList<String> hostList = new ArrayList<String>();

                //first copy the master and slaves hosts into the list
                hostList = getHostList();
                String host = null;

                //ensure the directory exists if not the logrecords have not been executed.  If not perhaps this run will be invalid.
                File pushDir = new File(resultsLocation + "com.sun.pae.hadoop.harness/");
                if(pushDir.isDirectory() == true)
                {
                    logger.info(pushDir.toString() + " already exists");

                    for(Iterator<String> it = hostList.iterator(); it.hasNext();)
                    {
                        host = it.next();
                        String configurationFile = pushDir.getPath() +"/hadoop-env.sh";
                        String destiniationFile = hadoopConfPath + "hadoop-env.sh";

                        if(RunContext.pushFile(configurationFile, host, destiniationFile) != true)
                        {
                            logger.warning("hadoop config file was not written to "+host);
                        }else
                        {
                            logger.info("Configuration file written to "+host);
                        }
                    }
                }else
                {
                    logger.warning("Directory com.sun.pae.hadoop.harness does not exist");
                    return false;
                }
			}
		}catch (IOException ioe)
		{
			logger.warning("IOException while writing the configuration file in alterHadoopConfigFile.  The message was "+ioe.getMessage());
            return false;
		}
        return true;
    }

    /*
     * This method collects all of the advanced HDF configurations from the configuration form.
     * @param void
     * @return hashMap of values and keys to configure hadoop with
     */
    public ConcurrentHashMap<String, String> getAdvancedHDFSDefaultConfig()
    {
        ConcurrentHashMap <String,String>advancedHDFSConfig = new ConcurrentHashMap<String,String>(46);

        advancedHDFSConfig.put("dfs.namenode.logging.level", "info");
        advancedHDFSConfig.put("dfs.secondary.http.address", "0.0.0.0:50090");
        advancedHDFSConfig.put("dfs.datanode.address", "0.0.0.0:50010");
        advancedHDFSConfig.put("dfs.datanode.http.address", "0.0.0.0:50075");
        advancedHDFSConfig.put("dfs.datanode.ipc.address", "0.0.0.0:50020");
        advancedHDFSConfig.put("dfs.datanode.handler.count", "3");
        advancedHDFSConfig.put("dfs.http.address", "0.0.0.0:50070");
        advancedHDFSConfig.put("dfs.https.enable", "false");
        advancedHDFSConfig.put("dfs.https.need.client.auth", "false");
        advancedHDFSConfig.put("dfs.https.server.keystore.resource", "ssl-server.xml");
        advancedHDFSConfig.put("dfs.https.client.keystore.resource", "ssl-client.xml");

        advancedHDFSConfig.put("dfs.datanode.https.address", "0.0.0.0:50475");
        advancedHDFSConfig.put("dfs.https.address", "0.0.0.0:50470");
        advancedHDFSConfig.put("dfs.datanode.dns.interface", "default");
        advancedHDFSConfig.put("dfs.datanode.dns.nameserver", "default");
        advancedHDFSConfig.put("dfs.replication.considerLoad", "true");
        advancedHDFSConfig.put("dfs.default.chunk.view.size", "32768");
        advancedHDFSConfig.put("dfs.datanode.du.reserved", "0");
        advancedHDFSConfig.put("dfs.name.dir", "${hadoop.tmp.dir}/dfs/name");
        advancedHDFSConfig.put("dfs.name.edits.dir", "${dfs.name.dir}");
        advancedHDFSConfig.put("dfs.web.ugi", "webuser,webgroup");

        advancedHDFSConfig.put("dfs.permissions", "true");
        advancedHDFSConfig.put("dfs.permissions.supergroup", "supergroup");
        advancedHDFSConfig.put("dfs.data.dir", "${hadoop.tmp.dir}/dfs/data");
        advancedHDFSConfig.put("dfs.replication", "3");
        advancedHDFSConfig.put("dfs.replication.max", "512");
        advancedHDFSConfig.put("dfs.replication.min", "1");
        advancedHDFSConfig.put("dfs.block.size", "67108864");
        advancedHDFSConfig.put("dfs.df.interval", "60000");
        advancedHDFSConfig.put("dfs.client.block.write.retries", "3");
        advancedHDFSConfig.put("dfs.blockreport.intervalMsec", "3600000");

        advancedHDFSConfig.put("dfs.blockreport.initialDelay", "0");
        advancedHDFSConfig.put("dfs.heartbeat.interval", "3");
        advancedHDFSConfig.put("dfs.namenode.handler.count", "10");
        advancedHDFSConfig.put("dfs.safemode.threshold.pct", "0.999f");
        advancedHDFSConfig.put("dfs.safemode.extension", "30000");
        advancedHDFSConfig.put("dfs.balance.bandwidthPerSec", "1048576");
        advancedHDFSConfig.put("dfs.hosts", "");
        advancedHDFSConfig.put("dfs.hosts.exclude", "");
        advancedHDFSConfig.put("dfs.max.objects", "0");
        advancedHDFSConfig.put("dfs.namenode.decommission.interval", "30");
        advancedHDFSConfig.put("dfs.namenode.decommission.nodes.per.interval", "5");
        advancedHDFSConfig.put("dfs.replication.interval", "3");
        advancedHDFSConfig.put("dfs.access.time.precision", "3600000");
        advancedHDFSConfig.put("dfs.support.append", "false");

        return advancedHDFSConfig;
        
    }


    /*
     * using the info from the config form and generates a list of configuration items.  These are tested and compared
     * to the default values.  If the values are different from default they are written to the resultant ConcurrentHashMap.
     * @return advancedCoreConfig, ConcurrentHashMap of config entries that can be empty
     */
    public ConcurrentHashMap<String, String> getAdvancedCoreDefaultConfig()
    {
        ConcurrentHashMap <String,String>advancedCoreDefaultConfig = new ConcurrentHashMap<String,String>(52);

        //collect the values from the forms        

        advancedCoreDefaultConfig.put("hadoop.tmp.dir", "/tmp/hadoop-${user.name}");
        advancedCoreDefaultConfig.put("hadoop.native.lib", "true");
        advancedCoreDefaultConfig.put("hadoop.http.filter.initializers", "");
        advancedCoreDefaultConfig.put("hadoop.security.authorization", "false");
        advancedCoreDefaultConfig.put("hadoop.logfile.size", "10000000");
        advancedCoreDefaultConfig.put("hadoop.logfile.count", "10");
        advancedCoreDefaultConfig.put("io.file.buffer.size", "4096");
        advancedCoreDefaultConfig.put("io.bytes.per.checksum", "512");
        advancedCoreDefaultConfig.put("io.skip.checksum.errors", "false");
        advancedCoreDefaultConfig.put("io.compression.codecs", "org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.BZip2Codec");

        advancedCoreDefaultConfig.put("io.serializations", "org.apache.hadoop.io.serializer.WritableSerialization");
        advancedCoreDefaultConfig.put("fs.default.name", "file:///");
        advancedCoreDefaultConfig.put("fs.trash.interval", "0");
        advancedCoreDefaultConfig.put("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
        advancedCoreDefaultConfig.put("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        advancedCoreDefaultConfig.put("fs.s3.impl", "org.apache.hadoop.fs.s3.S3FileSystem");
        advancedCoreDefaultConfig.put("fs.s3n.impl", "org.apache.hadoop.fs.s3native.NativeS3FileSystem");
        advancedCoreDefaultConfig.put("fs.kfs.impl", "org.apache.hadoop.fs.kfs.KosmosFileSystem");
        advancedCoreDefaultConfig.put("fs.hftp.impl", "org.apache.hadoop.hdfs.HftpFileSystem");
        advancedCoreDefaultConfig.put("fs.hsftp.impl", "org.apache.hadoop.hdfs.HsftpFileSystem");

        advancedCoreDefaultConfig.put("fs.ftp.impl", "org.apache.hadoop.fs.ftp.FTPFileSystem");
        advancedCoreDefaultConfig.put("fs.ramfs.impl", "org.apache.hadoop.fs.InMemoryFileSystem");
        advancedCoreDefaultConfig.put("fs.har.impl", "org.apache.hadoop.fs.HarFileSystem");
        advancedCoreDefaultConfig.put("fs.checkpoint.dir", "${hadoop.tmp.dir}/dfs/namesecondary");
        advancedCoreDefaultConfig.put("fs.checkpoint.edits.dir", "${fs.checkpoint.dir}");
        advancedCoreDefaultConfig.put("fs.checkpoint.period", "3600");
        advancedCoreDefaultConfig.put("fs.checkpoint.size", "67108864");
        advancedCoreDefaultConfig.put("fs.s3.block.size", "67108864");
        advancedCoreDefaultConfig.put("fs.s3.buffer.dir", "${hadoop.tmp.dir}/s3");
        advancedCoreDefaultConfig.put("fs.s3.maxRetries", "4");
        advancedCoreDefaultConfig.put("fs.s3.sleepTimeSeconds", "10");
        advancedCoreDefaultConfig.put("local.cache.size", "10737418240");
        advancedCoreDefaultConfig.put("io.seqfile.compress.blocksize", "1000000");
        advancedCoreDefaultConfig.put("io.seqfile.lazydecompress", "true");
        advancedCoreDefaultConfig.put("io.seqfile.sorter.recordlimit", "1000000");

        advancedCoreDefaultConfig.put("io.mapfile.bloom.size", "1048576");
        advancedCoreDefaultConfig.put("io.mapfile.bloom.error.rate", "0.005");
        advancedCoreDefaultConfig.put("hadoop.util.hash.type", "murmur");
        advancedCoreDefaultConfig.put("ipc.client.idlethreshold", "4000");
        advancedCoreDefaultConfig.put("ipc.client.kill.max", "10");
        advancedCoreDefaultConfig.put("ipc.client.connection.maxidletime", "10000");
        advancedCoreDefaultConfig.put("ipc.client.connect.max.retries", "10");
        advancedCoreDefaultConfig.put("ipc.server.listen.queue.size", "128");
        advancedCoreDefaultConfig.put("ipc.server.tcpnodelay", "false");
        advancedCoreDefaultConfig.put("ipc.client.tcpnodelay", "false");
        advancedCoreDefaultConfig.put("webinterface.private.actions", "false");
        advancedCoreDefaultConfig.put("hadoop.rpc.socket.factory.class.default", "org.apache.hadoop.net.StandardSocketFactory");
        advancedCoreDefaultConfig.put("hadoop.rpc.socket.factory.class.ClientProtocol", "");
        advancedCoreDefaultConfig.put("hadoop.socks.server", "");
        advancedCoreDefaultConfig.put("topology.node.switch.mapping.impl", "org.apache.hadoop.net.ScriptBasedMapping");
        advancedCoreDefaultConfig.put("topology.script.file.name", "");
        advancedCoreDefaultConfig.put("topology.script.number.args", "100");

        return advancedCoreDefaultConfig;
    }

    /*
     * This method collects all of the advanced Mapred configurations from the configuration form.
     * @param void
     * @return hashMap of values and keys to configure hadoop with
     *
     */
    public ConcurrentHashMap<String, String> getAdvancedMapredDefaultConfig()
    {
        ConcurrentHashMap <String,String>advancedMapredDefaultConfig = new ConcurrentHashMap<String,String>(98);


        advancedMapredDefaultConfig.put("hadoop.job.history.location", "");
        advancedMapredDefaultConfig.put("hadoop.job.history.user.location", "");
        advancedMapredDefaultConfig.put("io.sort.factor", "10");
        advancedMapredDefaultConfig.put("io.sort.mb", "100");
        advancedMapredDefaultConfig.put("io.sort.record.percent", "0.05");
        advancedMapredDefaultConfig.put("io.sort.spill.percent", "0.80");
        advancedMapredDefaultConfig.put("io.map.index.skip", "0");
        advancedMapredDefaultConfig.put("mapred.job.tracker", "local");
        advancedMapredDefaultConfig.put("mapred.job.tracker.http.address", "0.0.0.0:50030");
        advancedMapredDefaultConfig.put("mapred.job.tracker.handler.count", "10");

        advancedMapredDefaultConfig.put("mapred.task.tracker.report.address", "127.0.0.1:0");
        advancedMapredDefaultConfig.put("mapred.local.dir", "${hadoop.tmp.dir}/mapred/local");
        advancedMapredDefaultConfig.put("mapred.system.dir", "${hadoop.tmp.dir}/mapred/system");
        advancedMapredDefaultConfig.put("mapred.temp.dir", "${hadoop.tmp.dir}/mapred/temp");
        advancedMapredDefaultConfig.put("mapred.local.dir.minspacestart", "0");
        advancedMapredDefaultConfig.put("mapred.local.dir.minspacekil", "0");
        advancedMapredDefaultConfig.put("mapred.tasktracker.expiry.interval", "600000");
        advancedMapredDefaultConfig.put("mapred.tasktracker.instrumentation", "org.apache.hadoop.mapred.TaskTrackerMetricsInst");
        advancedMapredDefaultConfig.put("mapred.tasktracker.vmem.reserved", "-1");
        advancedMapredDefaultConfig.put("mapred.tasktracker.pmem.reserved", "-1");

        advancedMapredDefaultConfig.put("mapred.task.default.maxvmem", "-1");
        advancedMapredDefaultConfig.put("mapred.task.limit.maxvmem", "-1");
        advancedMapredDefaultConfig.put("mapred.task.maxvmem", "-1");
        advancedMapredDefaultConfig.put("mapred.task.maxpmem", "-1");
        advancedMapredDefaultConfig.put("mapred.tasktracker.memory_calculator_plugin", "");
        advancedMapredDefaultConfig.put("mapred.tasktracker.taskmemorymanager.monitoring-interval", "5000");
        advancedMapredDefaultConfig.put("mapred.tasktracker.procfsbasedprocesstree.sleeptime-before-sigkill", "5000");
        advancedMapredDefaultConfig.put("mapred.map.tasks", "2");
        advancedMapredDefaultConfig.put("mapred.reduce.tasks", "1");
        advancedMapredDefaultConfig.put("mapred.jobtracker.restart.recover", "false");

        advancedMapredDefaultConfig.put("mapred.jobtracker.job.history.block.size", "3145728");
        advancedMapredDefaultConfig.put("mapred.jobtracker.taskScheduler", "org.apache.hadoop.mapred.JobQueueTaskScheduler	");
        advancedMapredDefaultConfig.put("mapred.jobtracker.taskScheduler.maxRunningTasksPerJob", "");
        advancedMapredDefaultConfig.put("mapred.map.max.attempts", "4");
        advancedMapredDefaultConfig.put("mapred.reduce.max.attempts", "4");
        advancedMapredDefaultConfig.put("mapred.reduce.parallel.copies", "5");
        advancedMapredDefaultConfig.put("mapred.reduce.copy.backoff", "300");
        advancedMapredDefaultConfig.put("mapred.task.timeout", "600000");
        advancedMapredDefaultConfig.put("mapred.tasktracker.map.tasks.maximum", "2");
        advancedMapredDefaultConfig.put("mapred.tasktracker.reduce.tasks.maximum", "2");

        advancedMapredDefaultConfig.put("mapred.jobtracker.completeuserjobs.maximum", "100");
        advancedMapredDefaultConfig.put("mapred.jobtracker.instrumentation", "org.apache.hadoop.mapred.JobTrackerMetricsInst");
        advancedMapredDefaultConfig.put("mapred.child.java.opts", "-Xmx200m");
        advancedMapredDefaultConfig.put("mapred.child.ulimit", "");
        advancedMapredDefaultConfig.put("mapred.child.tmp", "./tmp");
        advancedMapredDefaultConfig.put("mapred.inmem.merge.threshold", "1000");
        advancedMapredDefaultConfig.put("mapred.job.shuffle.merge.percent", "0.66");
        advancedMapredDefaultConfig.put("mapred.job.shuffle.input.buffer.percent", "0.70");
        advancedMapredDefaultConfig.put("mapred.job.reduce.input.buffer.percent", "0.0");
        advancedMapredDefaultConfig.put("mapred.map.tasks.speculative.execution", "true");

        advancedMapredDefaultConfig.put("mapred.reduce.tasks.speculative.execution", "true");
        advancedMapredDefaultConfig.put("mapred.job.reuse.jvm.num.tasks", "1");
        advancedMapredDefaultConfig.put("mapred.min.split.size", "0");
        advancedMapredDefaultConfig.put("mapred.jobtracker.maxtasks.per.job", "-1");
        advancedMapredDefaultConfig.put("mapred.submit.replication", "10");
        advancedMapredDefaultConfig.put("mapred.tasktracker.dns.interface", "default");
        advancedMapredDefaultConfig.put("mapred.tasktracker.dns.nameserver", "default");
        advancedMapredDefaultConfig.put("tasktracker.http.threads", "40");
        advancedMapredDefaultConfig.put("mapred.task.tracker.http.address", "0.0.0.0:50060");
        advancedMapredDefaultConfig.put("keep.failed.task.files", "false");

        advancedMapredDefaultConfig.put("mapred.output.compress", "false");
        advancedMapredDefaultConfig.put("mapred.output.compression.type", "RECORD");
        advancedMapredDefaultConfig.put("mapred.output.compression.codec", "org.apache.hadoop.io.compress.DefaultCodec");
        advancedMapredDefaultConfig.put("mapred.compress.map.output", "false");
        advancedMapredDefaultConfig.put("mapred.map.output.compression.codec", "org.apache.hadoop.io.compress.DefaultCodec");
        advancedMapredDefaultConfig.put("map.sort.class", "org.apache.hadoop.util.QuickSort");
        advancedMapredDefaultConfig.put("mapred.userlog.limit.kb", "0");
        advancedMapredDefaultConfig.put("mapred.userlog.retain.hours", "24");
        advancedMapredDefaultConfig.put("mapred.hosts", "");
        advancedMapredDefaultConfig.put("mapred.hosts.exclude", "");

        advancedMapredDefaultConfig.put("mapred.max.tracker.blacklists", "4");
        advancedMapredDefaultConfig.put("mapred.max.tracker.failures", "4");
        advancedMapredDefaultConfig.put("jobclient.output.filter", "FAILED");
        advancedMapredDefaultConfig.put("mapred.job.tracker.persist.jobstatus.active", "false");
        advancedMapredDefaultConfig.put("mapred.job.tracker.persist.jobstatus.hours", "0");
        advancedMapredDefaultConfig.put("mapred.job.tracker.persist.jobstatus.dir", "/jobtracker/jobsInfo");
        advancedMapredDefaultConfig.put("mapred.task.profile", "false");
        advancedMapredDefaultConfig.put("mapred.task.profile.maps", "0-2");
        advancedMapredDefaultConfig.put("mapred.task.profile.reduces", "0-2");
        advancedMapredDefaultConfig.put("mapred.line.input.format.linespermap", "1");

        advancedMapredDefaultConfig.put("mapred.skip.attempts.to.start.skipping", "2");
        advancedMapredDefaultConfig.put("mapred.skip.map.auto.incr.proc.count", "true");
        advancedMapredDefaultConfig.put("mapred.skip.reduce.auto.incr.proc.count", "true");
        advancedMapredDefaultConfig.put("mapred.skip.out.dir", "");
        advancedMapredDefaultConfig.put("mapred.skip.map.max.skip.records", "0");
        advancedMapredDefaultConfig.put("mapred.skip.reduce.max.skip.groups", "0");
        advancedMapredDefaultConfig.put("job.end.retry.attempts", "0");
        advancedMapredDefaultConfig.put("job.end.retry.interval", "30000");
        advancedMapredDefaultConfig.put("hadoop.rpc.socket.factory.class.JobSubmissionProtocol", "");
        advancedMapredDefaultConfig.put("mapred.task.cache.levels", "2");

        advancedMapredDefaultConfig.put("mapred.queue.names", "default");
        advancedMapredDefaultConfig.put("mapred.acls.enabled", "false");
        advancedMapredDefaultConfig.put("mapred.queue.default.acl-submit-job", "*");
        advancedMapredDefaultConfig.put("mapred.queue.default.acl-administer-jobs", "*");
        advancedMapredDefaultConfig.put("mapred.job.queue.name", "default");
        advancedMapredDefaultConfig.put("mapred.tasktracker.indexcache.mb", "10");
        advancedMapredDefaultConfig.put("mapred.merge.recordsBeforeProgress", "10000");
        advancedMapredDefaultConfig.put("mapred.reduce.slowstart.completed.maps", "0.05");

        return advancedMapredDefaultConfig;
    }

    /**
     * This method is responsible for starting the benchmark run
     * @throws java.lang.Exception
     */
    @StartRun public void process() throws Exception {
    //public void process() throws Exception {
        logger.info("Starting benchmark run");
        
        /*  TODO:
         * All the below code is executed directly on the current system.
         * This should be okay, but to be more robust we could get the hostname
         * of the hadoop master system and execute on that
         */
        
         if(benchmarkName.compareTo("ExternalHadoopJob") == 0)
         {                                                                 
             logger.warning("The external job to be executed is: "+otherHadoopJob);
             logger.info("starting Foreign Job");
             startTime = Calendar.getInstance();             

             Command bench = new Command(hadoopHome +File.separator+ "bin" +File.separator+ "hadoop "+otherHadoopJob);
             bench.setOutputFile(Command.STDOUT, runDir + "/hadoop.output");
             bench.setOutputFile(Command.STDERR, runDir + "/hadoop.err");
             bench.setSynchronous(false);
             bench.setWorkingDirectory(runDir);
             benchHandle = RunContext.exec(bench);
             
         }else if(benchmarkName.compareTo("JavaSort-Medium") == 0)         
         {
             startTime = Calendar.getInstance();             

             logger.info("start java Sorting");
             Command bench = new Command(gridmixHome + File.separator + "javasort" + File.separator + "text-sort.medium");
             bench.setOutputFile(Command.STDOUT, runDir + "/hadoop.output");
             bench.setOutputFile(Command.STDERR, runDir + "/hadoop.err");             
             bench.setSynchronous(false);             
             bench.setWorkingDirectory(runDir);
             benchHandle = RunContext.exec(bench);
         }else         
         {
            logger.warning("Hadoop not starting.");            
         }  
    }

    /*
     * This method tars up all of the logfiles on each of the hosts and then collects them in the run directry ready for processing.
     * @param list of hosts to process
     * @return void
     *
     */
    public void produceRemoteLogFiles(final ArrayList<String> hostList)
    {
        String host;
        String[] names;
        String[] nodeList = new String[hostList.size()];

        for(int i = 0; i < hostList.size();i++)
        {
            host = hostList.get(i);
            names = host.split("\\.");
            nodeList[i] = names[0];
            names = null;
            host = null;
        }

       
        Command tarCommand = new Command("tar -cf "+hadoopLogDir+"/userlogs.tar "+hadoopLogDir+"/userlogs");
        Command renameCommand;
        try
        {
            for(int i = 0; i < hostList.size();i++)
            {
                logger.info("Host to get logs from = "+nodeList[i]);
                RunContext.exec(nodeList[i],tarCommand);
            }
            
        }catch(InterruptedException ie)
        {
            logger.warning("InterruptedException while running tar command on "+ ie.getMessage());
            
        }catch(IOException ioe)
        {
            logger.warning("IOException while running tar command on "+ ioe.getMessage());
        }

        //now lets copy these into the run directory
        String currentNode;
        for(int i = 0; i < hostList.size();i++)
        {
            currentNode = hostList.get(i);            
            if(RunContext.getFile(currentNode, hadoopLogDir+"/userlogs.tar", RunContext.getOutDir()+"userlogs-"+currentNode+".tar") != true)
            {
                logger.warning("Logs not retrieved from "+currentNode);
            }

            //now uncompress the directories and rename them
            tarCommand = new Command("tar -xf "+RunContext.getOutDir()+"userlogs-"+currentNode+".tar");
            renameCommand = new Command("mv "+hadoopLogDir+"/userlogs "+RunContext.getOutDir()+"userlogs-"+currentNode);
            try
            {
                RunContext.exec(tarCommand);
                RunContext.exec(renameCommand);

            }catch(InterruptedException ie)
            {
                logger.warning("InterruptedException while running copy methods for host"+currentNode+" "+ ie.getMessage());

            }catch(IOException ioe)
            {
                logger.warning("IOException while running copy methods for host"+currentNode+" "+ ioe.getMessage());
            }
        }
    }

    /*
     * Collect logrecords output files from the hosts.
     * @param list of hosts (including master which may not have logrecords logs and in this case will issue a warning in the run log
     * @return void
     * TODO modify to use hadoopLogDir
     *
    */
    public void collectMetricsFiles(final ArrayList<String> hostList)
    {
        String host;
        String[] nodeName;

        for(Iterator<String> it = hostList.iterator(); it.hasNext();)
        {
            host = it.next();
            nodeName = host.split("\\.");
            if(RunContext.getFile(host, "/tmp/hadoop_mapred_metrics-"+nodeName[0]+"-"+runID, "hadoop_mapred_metrics-"+nodeName[0]+"-"+runID) == true)
            {
                logger.info("metrics file from "+nodeName[0]+" has been copied to the master in "+RunContext.getOutDir());
            }else
            {
                logger.warning("metrics file from "+nodeName[0]+" has not been copied to the master");
            }
            host = null;
            nodeName = null;
        }        
    }

    
    /*
     * generate summary file in html format rather than using the xstl and xpath
     * facilities while we get some features added to FABAN
     */
    //this is temporary until FABAN has some add new features to support my summary    
    public boolean doSummaryHTML(final String resultsLocation)
    {
        BufferedWriter bfw_summary = null;
        BufferedReader bfr_error = null;
        BufferedReader bfr_output = null;
        ArrayList<String> error_file = new ArrayList<String>();
        ArrayList<String> output_file = new ArrayList<String>();
        String timeTaken = "";
        String jobID;
        String counters;
        String hdfsBytesRead = "";
        String hdfsBytesWritten = "";
        String localBytesRead = "";
        String localBytesWritten = "";
        String launchedReduceTasks = "";
        String rackLocalMapTasks = "0";
        String launchedMapTasks = "";
        String dataLocalMapTasks = "";

        String reduceInputGroups = "";
        String combineOutputRecords = "";
        String mapInputRecords = "";
        String reduceOutputRecords = "";
        String mapOutputBytes = "";
        String mapInputBytes = "";
        String combineInputRecords = "";
        String mapOutputRecords = "";
        String reduceInputRecords = "";
        String reduceInputBytes = ""; 

        long mapper = 0;
        long reducer = 0;
        long shuffle = 0;
        long writer = 0;

        ArrayList<String> hostList = new ArrayList<String>();
        hostList = slaveList;

        //copy the logfiles from the slaves to the master.
        produceRemoteLogFiles(hostList);

        //copy the logrecords files to the Hadoop master
        hostList = getHostList();
        collectMetricsFiles(hostList);

        Vector<ArrayList<AbstractLogRecord>> metricsVector = processAllMetrics(hostList);
        if(metricsVector == null)
        {
            logger.info("metricsVector is empty in doSummaryHTML");
        }

        Vector<ArrayList<TimeCapture>> logVector = processAllLogs(hostList);
        if((logVector == null) || (logVector.isEmpty()))
        {
            logger.warning("logVector is empty in doSummaryHTML");
        }else
        {
            //check to see if any of them are empty
            for(Enumeration enumer = logVector.elements(); enumer.hasMoreElements();)
            {
                @SuppressWarnings("unchecked")
                ArrayList<TimeCapture> arrayList = (ArrayList<TimeCapture>)enumer.nextElement();
                if(arrayList.size() > 0)
                {
                    mapper = mapper + arrayList.get(0).getTotalAcumulatedTime();
                    reducer = reducer + arrayList.get(1).getTotalAcumulatedTime();
                    shuffle = shuffle + arrayList.get(2).getTotalAcumulatedTime();
                    writer = writer + arrayList.get(3).getTotalAcumulatedTime();

                }else
                {
                    logger.warning("Array of hadoop statistics is empty");
                }
            }
        }

        //process the task logs
        //MRLogParser logParser = new MRLogParser();

        if(resultsLocation.length() != 0)
        {
            logger.info("Output location = "+resultsLocation);
            try
            {
                bfr_error = new BufferedReader(new FileReader(resultsLocation + File.separator + "hadoop.err"));
                int error_line_counter = 0;
                String line;
                while( (line = bfr_error.readLine()) != null)
                {
                    error_file.add(line);
                    error_line_counter++;
                }
                bfr_error.close();

                //now copy the output file into an ArrayList
                bfr_output = new BufferedReader(new FileReader(resultsLocation + File.separator + "hadoop.output"));
                int output_line_counter = 0;
                while( (line = bfr_output.readLine()) != null)
                {
                    output_file.add(line);
                    output_line_counter++;
                }
                bfr_output.close();

            }catch(FileNotFoundException fnf)
            {
                logger.info("hadoop.err file could not be found, no summary will be produced");
            }catch(IOException ioe)
            {
                logger.info(ioe.getMessage());
                logger.info("hadoop.err file IO Error, no summary will be produced");
            }
            //read the err file into an array of strings

            for(Iterator<String> itOutput = output_file.iterator();itOutput.hasNext();)
            {
                String line = itOutput.next();
                if(line.startsWith("The job took ") == true)
                {
                    try
                    {
                        int location = line.indexOf(" seconds.");
                        if(location > 0)
                        {
                            timeTaken = line.substring(13, location);
                            logger.info("Job Has Taken "+timeTaken+" Seconds");
                        }else
                        {
                            logger.warning("Job failed as we can not get the time");
                        }
                    }catch(StringIndexOutOfBoundsException oob)
                    {
                        logger.warning("Job failed as we got an exception and can not get the time" +oob.getMessage());
                    }
                }
            }

            for(Iterator<String> itError = error_file.iterator();itError.hasNext();)
            {
                String line = itError.next();
                int locationOfSub = line.indexOf("Running job:");
                if(locationOfSub >0 )
                {
                    jobID = line.substring(locationOfSub +13);
                }

                locationOfSub = line.indexOf("Counters:");
                if(locationOfSub >0 )
                {
                    counters = line.substring(locationOfSub +10);
                }

                locationOfSub = line.indexOf("HDFS_BYTES_READ=");
                if(locationOfSub >0 )
                {
                    hdfsBytesRead = line.substring(locationOfSub +16);
                }

                locationOfSub = line.indexOf("HDFS_BYTES_WRITTEN=");
                if(locationOfSub >0 )
                {
                    hdfsBytesWritten = line.substring(locationOfSub +19);
                }

                locationOfSub = line.indexOf("FILE_BYTES_READ=");
                if(locationOfSub >0 )
                {
                    localBytesRead = line.substring(locationOfSub +17);
                }

                locationOfSub = line.indexOf("FILE_BYTES_WRITTEN=");
                if(locationOfSub >0 )
                {
                    localBytesWritten = line.substring(locationOfSub +20);
                }

                locationOfSub = line.indexOf("Launched reduce tasks=");
                if(locationOfSub >0 )
                {
                    launchedReduceTasks = line.substring(locationOfSub +22);
                }

                locationOfSub = line.indexOf("Rack-local map tasks=");
                if(locationOfSub >0 )
                {
                    rackLocalMapTasks = line.substring(locationOfSub +21);
                }

                locationOfSub = line.indexOf("Launched map tasks=");
                if(locationOfSub >0 )
                {
                    launchedMapTasks = line.substring(locationOfSub +19);
                }

                locationOfSub = line.indexOf("Data-local map tasks=");
                if(locationOfSub >0 )
                {
                    dataLocalMapTasks = line.substring(locationOfSub +21);
                }

                locationOfSub = line.indexOf("Reduce input groups=");
                if(locationOfSub >0 )
                {
                    reduceInputGroups = line.substring(locationOfSub +20);
                }

                locationOfSub = line.indexOf("Combine output records=");
                if(locationOfSub >0 )
                {
                    combineOutputRecords = line.substring(locationOfSub +23);
                }

                locationOfSub = line.indexOf("Map input records=");
                if(locationOfSub >0 )
                {
                    mapInputRecords = line.substring(locationOfSub +18);
                }

                locationOfSub = line.indexOf("Reduce output records=");
                if(locationOfSub >0 )
                {
                    reduceOutputRecords = line.substring(locationOfSub +22);
                }

                locationOfSub = line.indexOf("Map output bytes=");
                if(locationOfSub >0 )
                {
                    mapOutputBytes = line.substring(locationOfSub +17);
                }

                locationOfSub = line.indexOf("Map input bytes=");
                if(locationOfSub >0 )
                {
                    mapInputBytes = line.substring(locationOfSub +16);
                }

                locationOfSub = line.indexOf("Combine input records=");
                if(locationOfSub >0 )
                {
                    combineInputRecords = line.substring(locationOfSub +22);
                    
                }

                locationOfSub = line.indexOf("Map output records=");
                if(locationOfSub >0 )
                {
                    mapOutputRecords = line.substring(locationOfSub +19);
                }

                locationOfSub = line.indexOf("Reduce input records=");
                if(locationOfSub >0 )
                {
                    reduceInputRecords = line.substring(locationOfSub +21);
                }


                if((timeTaken == null) || (timeTaken.length() == 0))
                {
                    timeTaken = "0";
                }
                
                Float nodeCount = new Float(slaveList.size());
                Float secnode = new Float((float)(new Float(timeTaken)/new Float(slaveList.size())));
                
                ArrayList<String> summary_file_data = new ArrayList<String>();
                summary_file_data.add("");

                summary_file_data.add("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\"http://www.w3.org/TR/html4/loose.dtd\">");
                summary_file_data.add("<html lang=\"en\"><head><title>GridMix Run Summary for "+runID+"</title><meta http-equiv=\"Content-type\" content=\"text/html; charset=iso-8859-1\">");

                summary_file_data.add("<center><h1>GridMix Workload on Hadoop version "+hadoopVersion+"</h1><h1>Summary Report</h1></center>");


                summary_file_data.add("<table summary=\"Vital Statistics\" width=\"30%\" border=\"0\" cellspacing=\"2\" cellpadding=\"1\"><tr align=\"left\" valign=\"top\">");
                summary_file_data.add("<td>Start Time:</td><td><b>"+startTime.getTime().toString()+"</b></td></tr>");
                summary_file_data.add("<td>End Time:</td><td><b>"+endTime.getTime().toString()+"</b></td></tr>");
                summary_file_data.add("<td>Total number of seconds:</td><td><b>"+timeTaken+"</b></td></tr>");
                summary_file_data.add("<td>seconds/slave:</td><td><b>"+new DecimalFormat("0.##").format(secnode)+"</b></td></tr>");
                summary_file_data.add("<td>Run ID:</td><td><b>"+runID+"</b></td></tr>");
                summary_file_data.add("<td>Active slaves :</td><td><b>"+nodeCount+"</b></td></tr>");
                summary_file_data.add("</table><hr>");

                summary_file_data.add("<center><h1><i>Hadoop System Counters</i></h1></center><hr>");

                summary_file_data.add("<center><table width=\"40%\" border=\"0\" cellspacing=\"2\" cellpadding=\"1\"><tr align=\"left\" valign=\"top\"><th><b>Description</b></th><th><b>Result</b></th></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>HDFS bytes read</td><td>"+hdfsBytesRead+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>HDFS bytes written</td><td>"+hdfsBytesWritten+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Local bytes read</td><td>"+localBytesRead+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Local bytes written</td><td>"+localBytesWritten+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Launched reduce tasks</td><td>"+launchedReduceTasks+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Rack-local map tasks</td><td>"+rackLocalMapTasks+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Launched map tasks</td><td>"+launchedMapTasks+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Data-local map tasks</td><td>"+dataLocalMapTasks+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Reduce input groups</td><td>"+reduceInputGroups+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Combine output records</td><td>"+combineOutputRecords+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Map input records</td><td>"+mapInputRecords+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Reduce output records</td><td>"+reduceOutputRecords+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Map output bytes</td><td>"+mapOutputBytes+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Map input bytes</td><td>"+mapInputBytes+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Combine input records</td><td>"+combineInputRecords+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Map output records</td><td>"+mapOutputRecords+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Reduce input records</td><td>"+reduceInputRecords+"</td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td></td><td></td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Total time in Map</td><td>"+mapper/1000+" seconds </td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Total time in Reduce</td><td>"+reducer/1000+" seconds </td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Total time in shuffle</td><td>"+shuffle/1000+" seconds </td></tr>");
                summary_file_data.add("<tr align=\"left\" valign=\"top\"><td>Total time in reduce write</td><td>"+writer/1000+" seconds </td></tr>");
                summary_file_data.add("</table></center><hr>");

                summary_file_data.add("</body></html>");


                try
                    {
                        bfw_summary = new BufferedWriter(new FileWriter(resultsLocation + File.separator + "summary.html"));
                        for(Iterator<String> it = summary_file_data.iterator();it.hasNext();)
                        {
                            String line_to_write = (String)it.next();
                            bfw_summary.write(line_to_write, 0, line_to_write.length());
                            bfw_summary.newLine();
                        }
                        bfw_summary.close();
                        
                    } catch (Exception fe)
                    {
                       logger.warning("Could not create summary.html file in " + resultsLocation + fe.getMessage());
                    }
                }
            }
        return true;

    }


    /*
     * generate summary file to show details of the run just completed
     * @param resultsLocation location of the run output directory
     */
    public void doSummary(final String resultsLocation)
    {
        BufferedWriter bfw_summary = null;
        BufferedReader bfr_error = null;
        BufferedReader bfr_output = null;
        ArrayList<String> error_file = new ArrayList<String>();
        ArrayList<String> output_file = new ArrayList<String>();
        String timeTaken = "";
        String jobID;
        String counters;
        String hdfsBytesRead = "";
        String hdfsBytesWritten = "";
        String localBytesRead = "";
        String localBytesWritten = "";
        String launchedReduceTasks = "";
        String rackLocalMapTasks = "";
        String launchedMapTasks = "";
        String dataLocalMapTasks = "";
        
        String reduceInputGroups = "";
        String combineOutputRecords = "";
        String mapInputRecords = "";
        String reduceOutputRecords = "";
        String mapOutputBytes = "";
        String mapInputBytes = "";
        String combineInputRecords = "";
        String mapOutputRecords = "";
        String reduceInputRecords = "";
        
        
        
        if(resultsLocation.length() != 0)
        {
            
            try
            {
                bfr_error = new BufferedReader(new FileReader(resultsLocation + File.separator + "hadoop.err"));                
                String line;
                while( (line = bfr_error.readLine()) != null)
                {                    
                    error_file.add(line);
                }
                bfr_error.close();
                
                //now copy the output file into an ArrayList
                bfr_output = new BufferedReader(new FileReader(resultsLocation + File.separator + "hadoop.output"));
                while( (line = bfr_output.readLine()) != null)
                {
                    output_file.add(line);
                }
                bfr_output.close();
                
            }catch(FileNotFoundException fnf)
            {                
                logger.info("hadoop.err file could not be found, no summary will be produced");
            }catch(IOException ioe)
            {
                logger.info("hadoop.err file IO Error, no summary will be produced");
            }
            //read the err file into an array of strings 
            
            for(Iterator<String> itOutput = output_file.iterator();itOutput.hasNext();)
            {
                String line = itOutput.next();
                if(line.startsWith("The job took ") == true)
                {
                    try
                    {
                        int location = line.indexOf(" seconds.");
                        if(location > 0)
                        {
                            timeTaken = line.substring(13, location);                            
                        }else
                        {
                            logger.warning("Job failed as we can not get the time");
                        }
                    }catch(StringIndexOutOfBoundsException oob)
                    {
                        logger.warning("Job failed as we got an exception and can not get the time");
                    }                                                           
                }
            }
            
            for(Iterator<String> itError = error_file.iterator();itError.hasNext();)
            {
                String line = itError.next();
                int locationOfSub = line.indexOf("Running job:");
                if(locationOfSub >0 )
                {                    
                    jobID = line.substring(locationOfSub +13);                   
                }
                
                locationOfSub = line.indexOf("Counters:");
                if(locationOfSub >0 )
                {                    
                    counters = line.substring(locationOfSub +10);                    
                }
                
                locationOfSub = line.indexOf("HDFS bytes read=");
                if(locationOfSub >0 )
                {
                    hdfsBytesRead = line.substring(locationOfSub +16);
                }
                
                locationOfSub = line.indexOf("HDFS bytes written=");
                if(locationOfSub >0 )
                {
                    hdfsBytesWritten = line.substring(locationOfSub +19);
                }
                
                locationOfSub = line.indexOf("Local bytes read=");
                if(locationOfSub >0 )
                {
                    localBytesRead = line.substring(locationOfSub +17);
                }
                
                locationOfSub = line.indexOf("Local bytes written=");
                if(locationOfSub >0 )
                {
                    localBytesWritten = line.substring(locationOfSub +20);
                }
                
                locationOfSub = line.indexOf("Launched reduce tasks=");
                if(locationOfSub >0 )
                {
                    launchedReduceTasks = line.substring(locationOfSub +22);
                }
                
                locationOfSub = line.indexOf("Rack-local map tasks=");
                if(locationOfSub >0 )
                {
                    rackLocalMapTasks = line.substring(locationOfSub +21);
                }
                
                locationOfSub = line.indexOf("Launched map tasks=");
                if(locationOfSub >0 )
                {
                    launchedMapTasks = line.substring(locationOfSub +19);
                }
                
                locationOfSub = line.indexOf("Data-local map tasks=");
                if(locationOfSub >0 )
                {
                    dataLocalMapTasks = line.substring(locationOfSub +21);
                }
                
                locationOfSub = line.indexOf("Reduce input groups=");
                if(locationOfSub >0 )
                {
                    reduceInputGroups = line.substring(locationOfSub +20);
                }
                
                locationOfSub = line.indexOf("Combine output records=");
                if(locationOfSub >0 )
                {
                    combineOutputRecords = line.substring(locationOfSub +23);
                }
                
                locationOfSub = line.indexOf("Map input records=");
                if(locationOfSub >0 )
                {
                    mapInputRecords = line.substring(locationOfSub +18);
                }
                
                locationOfSub = line.indexOf("Reduce output records=");
                if(locationOfSub >0 )
                {
                    reduceOutputRecords = line.substring(locationOfSub +22);
                }
                
                locationOfSub = line.indexOf("Map output bytes=");
                if(locationOfSub >0 )
                {
                    mapOutputBytes = line.substring(locationOfSub +17);
                }
                
                locationOfSub = line.indexOf("Map input bytes=");
                if(locationOfSub >0 )
                {
                    mapInputBytes = line.substring(locationOfSub +16);
                }
                
                locationOfSub = line.indexOf("Combine input records=");
                if(locationOfSub >0 )
                {
                    combineInputRecords = line.substring(locationOfSub +22);
                }
                
                locationOfSub = line.indexOf("Map output records=");
                if(locationOfSub >0 )
                {
                    mapOutputRecords = line.substring(locationOfSub +19);
                }
                
                locationOfSub = line.indexOf("Reduce input records=");
                if(locationOfSub >0 )
                {
                    reduceInputRecords = line.substring(locationOfSub +21);
                }

                                
                ArrayList<String> summary_file_data = new ArrayList<String>();
                summary_file_data.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                summary_file_data.add("<?xml-stylesheet type=\"text/xsl\" href=\"../../xslt/summary_report.xsl\"?>");
                summary_file_data.add("<benchResults>");
                
                summary_file_data.add("\t<benchSummary name=\"GridMix Workload\" version= \""+hadoopVersion+"\">");
                summary_file_data.add("\t\t<runId>"+runID+"</runId>");
                summary_file_data.add("\t\t<startTime>"+startTime.getTime().toString()+"</startTime>");
                summary_file_data.add("\t\t<endTime>"+endTime.getTime().toString()+"</endTime>");
                
                
                if((timeTaken == null) || (timeTaken.length() == 0))
                {
                    timeTaken = "0";
                }
                
                Float secnode = new Float((float)(new Float(timeTaken)/new Float(slaveList.size())));
                               
                summary_file_data.add("\t\t<metric unit=\"sec/node\">"+new DecimalFormat("0.##").format(secnode)+"</metric>");
                summary_file_data.add("\t\t<passed>true</passed>");
                summary_file_data.add("\t\t<hdfsBytesRead>hdfsBytesRead = "+hdfsBytesRead+"</hdfsBytesRead>");
                
                summary_file_data.add("\t</benchSummary>");                                                
                summary_file_data.add("<driverSummary name=\"Hadoop System Counters\">");
                summary_file_data.add("<metric unit=\"sec/node\">"+new DecimalFormat("0.##").format(secnode)+"</metric>");
                summary_file_data.add("<startTime>"+startTime.getTime().toString()+"</startTime>");
                summary_file_data.add("<endTime>"+endTime.getTime().toString()+"</endTime>");
                summary_file_data.add("<totalOps unit=\"seconds\">"+timeTaken+"</totalOps>");
                summary_file_data.add("<users>1</users>");
                summary_file_data.add("<rtXtps>1</rtXtps>");
                summary_file_data.add("<passed>true</passed>");                                                                
                summary_file_data.add("\t<miscStats>");
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>HDFS bytes read</description>");
                summary_file_data.add("\t\t\t<result>"+hdfsBytesRead+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>HDFS bytes written</description>");
                summary_file_data.add("\t\t\t<result>"+hdfsBytesWritten+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Local bytes read</description>");
                summary_file_data.add("\t\t\t<result>"+localBytesRead+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Local bytes written</description>");
                summary_file_data.add("\t\t\t<result>"+localBytesWritten+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Launched reduce tasks</description>");
                summary_file_data.add("\t\t\t<result>"+launchedReduceTasks+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Rack-local map tasks</description>");
                summary_file_data.add("\t\t\t<result>"+rackLocalMapTasks+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Launched map tasks</description>");
                summary_file_data.add("\t\t\t<result>"+launchedMapTasks+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Data-local map tasks</description>");
                summary_file_data.add("\t\t\t<result>"+dataLocalMapTasks+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Reduce input groups</description>");
                summary_file_data.add("\t\t\t<result>"+reduceInputGroups+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Combine output records</description>");
                summary_file_data.add("\t\t\t<result>"+combineOutputRecords+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Map input records</description>");
                summary_file_data.add("\t\t\t<result>"+mapInputRecords+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Reduce output records</description>");
                summary_file_data.add("\t\t\t<result>"+reduceOutputRecords+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Map output bytes</description>");
                summary_file_data.add("\t\t\t<result>"+mapOutputBytes+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Map input bytes</description>");
                summary_file_data.add("\t\t\t<result>"+mapInputBytes+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Combine input records</description>");
                summary_file_data.add("\t\t\t<result>"+combineInputRecords+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Map output records</description>");
                summary_file_data.add("\t\t\t<result>"+mapOutputRecords+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t\t<stat>");
                summary_file_data.add("\t\t\t<description>Reduce input records</description>");
                summary_file_data.add("\t\t\t<result>"+reduceInputRecords+"</result>");
                summary_file_data.add("\t\t\t<passed>true</passed>");
                summary_file_data.add("\t\t</stat>");
                
                summary_file_data.add("\t</miscStats>");
                summary_file_data.add("</driverSummary>");
                summary_file_data.add("</benchResults>");
                              
                try
                {            
                    bfw_summary = new BufferedWriter(new FileWriter(resultsLocation + File.separator + "summary.xml"));                              
                    for(Iterator<String> it = summary_file_data.iterator();it.hasNext();)
                    {
                        String line_to_write = it.next();
                        bfw_summary.write(line_to_write, 0, line_to_write.length());
                        bfw_summary.newLine();
                    }                    
                    bfw_summary.close();                    
                } catch (Exception fe)
                {
                   logger.warning("Could not create summary.xml file in " + resultsLocation + fe.getMessage());
                }
            }                                   
        }
    }

    /*
     * This method removes the old logrecords configuration files from each of the hosts
     * @param list of hosts to process
     * @return void
     * TODO modify to use hadoopLogDir
     */
    public void prepareMetricsFacilities(final ArrayList<String> hostList)
    {
        //for each of the hosts we clear all of the logrecords ready to pick up the new
        //ones prepared in this run.
        if(hostList != null)
        {
            String host;            
            File metricsDir = new File("/tmp");
            for(Iterator<String> it = hostList.iterator(); it.hasNext();)
            {
                host = it.next();                
                if(RunContext.deleteFiles(host, metricsDir.getPath(), new MetricsFileFilter()) == false)
                {
                    logger.warning("Metric files were not deleted from " +host+"/:"+ metricsDir.getPath());
                }
            }
        }else
        {
            logger.warning("hostList was empty in GridMix.prepareMetricsFacilities()");
        }
    }

    /*
     * This method returns an arrayList used in secveral places to process logs and other common tasks to be performed on each host of the cluster
     * @param void
     * @return arrayList containing a list of all the nodenames for the cluster
     */
    public ArrayList<String> getHostList()
    {
        ArrayList<String> hostList = new ArrayList<String>();
        //first copy the master host into the list
        for(Iterator<String> it = masterList.iterator(); it.hasNext();)
        {
            hostList.add(it.next());
        }

        //now copy the slave hosts into the list
        for(Iterator<String> it = slaveList.iterator(); it.hasNext();)
        {
            hostList.add(it.next());
        }

        return hostList;
    }



    /*
     *
     */
    public void prepareLoggingFacilities(final ArrayList<String> hostList)
    {
        //in each of the hosts we clear all of the logs ready to pick up the new
        //ones prepared in this run.
        if(hostList != null)
        {

            String host;
            logger.info("hostList still has "+hostList.size() + " elements");
            File logDir = new File(hadoopLogDir + "/userlogs");
            for(Iterator<String> it = hostList.iterator(); it.hasNext();)
            {                
                host = it.next();
                logger.info("Removing prevoius logfiles from "+host);
                if(RunContext.deleteFiles(host, logDir.getPath(), new LogFileFilter()) == false)
                {
                    logger.warning("Logging files were not deleted from " +host+"/:"+ logDir.getPath());
                }
                //now remove the tar files
                logger.info("Removing prevoius tar logfiles from "+host);
                if(RunContext.deleteFiles(host, logDir.getPath(), new LogTarFileFilter()) == false)
                {
                    logger.warning("Logging tar files were not deleted from " +host+"/:"+ logDir.getPath());
                }
            }
        }else
        {
            logger.warning("hostList was empty in GridMix.prepareLoggingFacilities()");
        }
    }
    
    /*
     * using the info from the config form and generates a list of configuration items.  These are tested and compared
     * to the default values.  If the values are different from default they are written to the resultant ConcurrentHashMap.
     * @return advancedHDFSConfig, ConcurrentHashMap of config entries that can be empty
     */
    public ConcurrentHashMap gatherAdvancedMapredConfig()
    {
        ConcurrentHashMap <String,String>advancedMapredConfig = new ConcurrentHashMap<String,String>(52);


        advancedMapredConfig.put("hadoop.job.history.location", params.getParameter("advancedMapredSiteConfig/hadoopJobHistoryLocation"));
        advancedMapredConfig.put("hadoop.job.history.user.location", params.getParameter("advancedMapredSiteConfig/hadoopJobHistoryUserLocation"));
        advancedMapredConfig.put("io.sort.factor", params.getParameter("advancedMapredSiteConfig/ioSortFactor"));
        advancedMapredConfig.put("io.sort.mb", params.getParameter("advancedMapredSiteConfig/ioSortMb"));
        advancedMapredConfig.put("io.sort.record.percent", params.getParameter("advancedMapredSiteConfig/ioSortRecordPercent"));
        advancedMapredConfig.put("io.sort.spill.percent", params.getParameter("advancedMapredSiteConfig/ioSortSpillPercent"));
        advancedMapredConfig.put("io.map.index.skip", params.getParameter("advancedMapredSiteConfig/ioMapIndexSkip"));
        advancedMapredConfig.put("mapred.job.tracker", params.getParameter("newBasicHadoopSiteConfig/newMapredJobTracker"));
        advancedMapredConfig.put("mapred.job.tracker.http.address", params.getParameter("advancedMapredSiteConfig/mapredJobTrackerHttpAddress"));
        advancedMapredConfig.put("mapred.job.tracker.handler.count", params.getParameter("advancedMapredSiteConfig/mapredJobTrackerHandlerCount"));
        advancedMapredConfig.put("mapred.task.tracker.report.address", params.getParameter("advancedMapredSiteConfig/mapredTaskTrackerReportAddress"));
        advancedMapredConfig.put("mapred.local.dir", params.getParameter("advancedMapredSiteConfig/mapredLocalDir"));
        advancedMapredConfig.put("mapred.system.dir", params.getParameter("advancedMapredSiteConfig/mapredSystemDir"));
        advancedMapredConfig.put("mapred.temp.dir", params.getParameter("advancedMapredSiteConfig/mapredTempDir"));
        advancedMapredConfig.put("mapred.local.dir.minspacestart", params.getParameter("advancedMapredSiteConfig/mapredLocalDirMinspacestart"));
        advancedMapredConfig.put("mapred.local.dir.minspacekil", params.getParameter("advancedMapredSiteConfig/mapredLocalDirMinspacekil"));
        advancedMapredConfig.put("mapred.tasktracker.expiry.interval", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerExpiryInterval"));
        advancedMapredConfig.put("mapred.tasktracker.instrumentation", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerInstrumentation"));
        advancedMapredConfig.put("mapred.tasktracker.vmem.reserved", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerVmemReserved"));
        advancedMapredConfig.put("mapred.tasktracker.pmem.reserved", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerPmemReserved"));
        advancedMapredConfig.put("mapred.task.default.maxvmem", params.getParameter("advancedMapredSiteConfig/mapredTaskDefaultMaxvmem"));
        advancedMapredConfig.put("mapred.task.limit.maxvmem", params.getParameter("advancedMapredSiteConfig/mapredTaskLimitMaxvmem"));
        advancedMapredConfig.put("mapred.task.maxvmem", params.getParameter("advancedMapredSiteConfig/mapredTaskMaxvmem"));
        advancedMapredConfig.put("mapred.task.maxpmem", params.getParameter("advancedMapredSiteConfig/mapredTaskMaxpmem"));
        advancedMapredConfig.put("mapred.tasktracker.memory_calculator_plugin", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerMemoryCalculatorPlugin"));
        advancedMapredConfig.put("mapred.tasktracker.taskmemorymanager.monitoring-interval", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerTaskmemorymanagerMonitoringInterval"));
        advancedMapredConfig.put("mapred.tasktracker.procfsbasedprocesstree.sleeptime-before-sigkill", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerProcfsbasedprocesstreeSleeptimeBeforeSigkill"));
        advancedMapredConfig.put("mapred.map.tasks", params.getParameter("advancedMapredSiteConfig/mapredMapTasks"));
        advancedMapredConfig.put("mapred.reduce.tasks", params.getParameter("advancedMapredSiteConfig/mapredReduceTasks"));
        advancedMapredConfig.put("mapred.jobtracker.restart.recover", params.getParameter("advancedMapredSiteConfig/mapredJobtrackerRestartRecover"));

        advancedMapredConfig.put("mapred.jobtracker.job.history.block.size", params.getParameter("advancedMapredSiteConfig/mapredJobtrackerJobHistoryBlockSize"));
        advancedMapredConfig.put("mapred.jobtracker.taskScheduler", params.getParameter("advancedMapredSiteConfig/mapredJobtrackerTaskScheduler"));
        advancedMapredConfig.put("mapred.jobtracker.taskScheduler.maxRunningTasksPerJob", params.getParameter("advancedMapredSiteConfig/mapredJobtrackerTaskSchedulerMaxRunningTasksPerJob"));
        advancedMapredConfig.put("mapred.map.max.attempts", params.getParameter("advancedMapredSiteConfig/mapredMapMaxAttempts"));
        advancedMapredConfig.put("mapred.reduce.max.attempts", params.getParameter("advancedMapredSiteConfig/mapredReduceMaxAttempts"));
        advancedMapredConfig.put("mapred.reduce.parallel.copies", params.getParameter("advancedMapredSiteConfig/mapredReduceParallelCopies"));
        advancedMapredConfig.put("mapred.reduce.copy.backoff", params.getParameter("advancedMapredSiteConfig/mapredReduceCopyBackoff"));
        advancedMapredConfig.put("mapred.task.timeout", params.getParameter("advancedMapredSiteConfig/mapredTaskTimeout"));
        advancedMapredConfig.put("mapred.tasktracker.map.tasks.maximum", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerMapTasksMaximum"));
        advancedMapredConfig.put("mapred.tasktracker.reduce.tasks.maximum", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerReduceTasksMaximum"));
        
        advancedMapredConfig.put("mapred.jobtracker.completeuserjobs.maximum", params.getParameter("advancedMapredSiteConfig/mapredJobtrackerCompleteuserjobsMaximum"));
        advancedMapredConfig.put("mapred.jobtracker.instrumentation", params.getParameter("advancedMapredSiteConfig/mapredJobtrackerInstrumentation"));
        advancedMapredConfig.put("mapred.child.java.opts", params.getParameter("advancedMapredSiteConfig/mapredChildJavaOpts"));
        advancedMapredConfig.put("mapred.child.ulimit", params.getParameter("advancedMapredSiteConfig/mapredChildUlimit"));
        advancedMapredConfig.put("mapred.child.tmp", params.getParameter("advancedMapredSiteConfig/mapredChildTmp"));
        advancedMapredConfig.put("mapred.inmem.merge.threshold", params.getParameter("advancedMapredSiteConfig/mapredInmemergeThreshold"));
        advancedMapredConfig.put("mapred.job.shuffle.merge.percent", params.getParameter("advancedMapredSiteConfig/mapredJobShuffleMergePercent"));
        advancedMapredConfig.put("mapred.job.shuffle.input.buffer.percent", params.getParameter("advancedMapredSiteConfig/mapredJobShuffleInputBufferPercent"));
        advancedMapredConfig.put("mapred.job.reduce.input.buffer.percent", params.getParameter("advancedMapredSiteConfig/mapredJobReduceInputBufferPercent"));
        advancedMapredConfig.put("mapred.map.tasks.speculative.execution", params.getParameter("advancedMapredSiteConfig/mapredMapTasksSpeculativeExecution"));

        advancedMapredConfig.put("mapred.reduce.tasks.speculative.execution", params.getParameter("advancedMapredSiteConfig/mapredReduceTasksSpeculativeExecution"));
        advancedMapredConfig.put("mapred.job.reuse.jvm.num.tasks", params.getParameter("advancedMapredSiteConfig/mapredJobReuseJvmNumTasks"));
        advancedMapredConfig.put("mapred.min.split.size", params.getParameter("advancedMapredSiteConfig/mapredMinSplitSize"));
        advancedMapredConfig.put("mapred.jobtracker.maxtasks.per.job", params.getParameter("advancedMapredSiteConfig/mapredJobtrackerMaxtasksPerJob"));
        advancedMapredConfig.put("mapred.submit.replication", params.getParameter("advancedMapredSiteConfig/mapredSubmitReplication"));
        advancedMapredConfig.put("mapred.tasktracker.dns.interface", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerDnsInterface"));
        advancedMapredConfig.put("mapred.tasktracker.dns.nameserver", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerDnsNameserver"));
        advancedMapredConfig.put("tasktracker.http.threads", params.getParameter("advancedMapredSiteConfig/tasktrackerHttpThreads"));
        advancedMapredConfig.put("mapred.task.tracker.http.address", params.getParameter("advancedMapredSiteConfig/mapredTaskTrackerHttpAddress"));
        advancedMapredConfig.put("keep.failed.task.files", params.getParameter("advancedMapredSiteConfig/keepFailedTaskFiles"));

        advancedMapredConfig.put("mapred.output.compress", params.getParameter("advancedMapredSiteConfig/mapredOutputCompress"));
        advancedMapredConfig.put("mapred.output.compression.type", params.getParameter("advancedMapredSiteConfig/mapredOutputCompressionType"));
        advancedMapredConfig.put("mapred.output.compression.codec", params.getParameter("advancedMapredSiteConfig/mapredOutputCompressionCodec"));
        advancedMapredConfig.put("mapred.compress.map.output", params.getParameter("advancedMapredSiteConfig/mapredCompressMapOutput"));
        advancedMapredConfig.put("mapred.map.output.compression.codec", params.getParameter("advancedMapredSiteConfig/mapredMapOutputCompressionCodec"));
        advancedMapredConfig.put("map.sort.class", params.getParameter("advancedMapredSiteConfig/mapSortClass"));
        advancedMapredConfig.put("mapred.userlog.limit.kb", params.getParameter("advancedMapredSiteConfig/mapredUserlogLimitKb"));
        advancedMapredConfig.put("mapred.userlog.retain.hours", params.getParameter("advancedMapredSiteConfig/mapredUserlogRetainHours"));
        advancedMapredConfig.put("mapred.hosts", params.getParameter("advancedMapredSiteConfig/mapredHosts"));
        advancedMapredConfig.put("mapred.hosts.exclude", params.getParameter("advancedMapredSiteConfig/mapredHostsExclude"));

        advancedMapredConfig.put("mapred.max.tracker.blacklists", params.getParameter("advancedMapredSiteConfig/mapredMaxTrackerBlacklists"));
        advancedMapredConfig.put("mapred.max.tracker.failures", params.getParameter("advancedMapredSiteConfig/mapredMaxTrackerFailures"));
        advancedMapredConfig.put("jobclient.output.filter", params.getParameter("advancedMapredSiteConfig/jobclientOutputFilter"));
        advancedMapredConfig.put("mapred.job.tracker.persist.jobstatus.active", params.getParameter("advancedMapredSiteConfig/mapredJobTrackerPersistJobstatusActive"));
        advancedMapredConfig.put("mapred.job.tracker.persist.jobstatus.hours", params.getParameter("advancedMapredSiteConfig/mapredJobTrackerPersistJobstatusHours"));
        advancedMapredConfig.put("mapred.job.tracker.persist.jobstatus.dir", params.getParameter("advancedMapredSiteConfig/mapredJobTrackerPersistJobstatusDir"));
        advancedMapredConfig.put("mapred.task.profile", params.getParameter("advancedMapredSiteConfig/mapredTaskProfile"));
        advancedMapredConfig.put("mapred.task.profile.maps", params.getParameter("advancedMapredSiteConfig/mapredTaskProfileMaps"));
        advancedMapredConfig.put("mapred.task.profile.reduces", params.getParameter("advancedMapredSiteConfig/mapredTaskProfileReduces"));
        advancedMapredConfig.put("mapred.line.input.format.linespermap", params.getParameter("advancedMapredSiteConfig/mapredLineInputFormatLinespermap"));

        advancedMapredConfig.put("mapred.skip.attempts.to.start.skipping", params.getParameter("advancedMapredSiteConfig/mapredSkipAttemptsToStartSkipping"));
        advancedMapredConfig.put("mapred.skip.map.auto.incr.proc.count", params.getParameter("advancedMapredSiteConfig/mapredSkipMapAutoIncrProcCount"));
        advancedMapredConfig.put("mapred.skip.reduce.auto.incr.proc.count", params.getParameter("advancedMapredSiteConfig/mapredSkipReduceAutoIncrProcCount"));
        advancedMapredConfig.put("mapred.skip.out.dir", params.getParameter("advancedMapredSiteConfig/mapredSkipOutDir"));
        advancedMapredConfig.put("mapred.skip.map.max.skip.records", params.getParameter("advancedMapredSiteConfig/mapredSkipMapMaxSkipRecords"));
        advancedMapredConfig.put("mapred.skip.reduce.max.skip.groups", params.getParameter("advancedMapredSiteConfig/mapredSkipReduceMaxSkipGroups"));
        advancedMapredConfig.put("job.end.retry.attempts", params.getParameter("advancedMapredSiteConfig/jobEndRetryAttempts"));
        advancedMapredConfig.put("job.end.retry.interval", params.getParameter("advancedMapredSiteConfig/jobEndRetryInterval"));
        advancedMapredConfig.put("hadoop.rpc.socket.factory.class.JobSubmissionProtocol", params.getParameter("advancedMapredSiteConfig/hadoopRpcSocketFactoryClassJobSubmissionProtocol"));
        advancedMapredConfig.put("mapred.task.cache.levels", params.getParameter("advancedMapredSiteConfig/mapredTaskCacheLevels"));

        advancedMapredConfig.put("mapred.queue.names", params.getParameter("advancedMapredSiteConfig/mapredQueueNames"));
        advancedMapredConfig.put("mapred.acls.enabled", params.getParameter("advancedMapredSiteConfig/mapredAclsEnabled"));
        advancedMapredConfig.put("mapred.queue.default.acl-submit-job", params.getParameter("advancedMapredSiteConfig/mapredQueueDefaultAclSubmitJob"));
        advancedMapredConfig.put("mapred.queue.default.acl-administer-jobs", params.getParameter("advancedMapredSiteConfig/mapredQueueDefaultAclAdministerJobs"));
        advancedMapredConfig.put("mapred.job.queue.name", params.getParameter("advancedMapredSiteConfig/mapredJobQueueName"));
        advancedMapredConfig.put("mapred.tasktracker.indexcache.mb", params.getParameter("advancedMapredSiteConfig/mapredTasktrackerIndexcacheMb"));
        advancedMapredConfig.put("mapred.merge.recordsBeforeProgress", params.getParameter("advancedMapredSiteConfig/mapredMergeRecordsBeforeProgress"));
        advancedMapredConfig.put("mapred.reduce.slowstart.completed.maps", params.getParameter("advancedMapredSiteConfig/mapredReduceSlowstartCompletedMaps"));
        
        String setMapredFinalFlag = params.getParameter("advancedMapredSiteConfig/setCoreFinalFlag");
        return advancedMapredConfig;
        
    }
    



    /*
     * using the info from the config form and generates a list of configuration items.  These are tested and compared
     * to the default values.  If the values are different from default they are written to the resultant ConcurrentHashMap.
     * @return advancedHDFSConfig, ConcurrentHashMap of config entries that can be empty
     */
    public ConcurrentHashMap gatherAdvancedHDFSConfig()
    {
        ConcurrentHashMap <String,String>advancedHDFSConfig = new ConcurrentHashMap<String,String>(52);

        advancedHDFSConfig.put("dfs.namenode.logging.level", params.getParameter("advancedHDFSSiteConfig/dfsNamenodeLoggingLevel"));
        advancedHDFSConfig.put("dfs.secondary.http.address", params.getParameter("advancedHDFSSiteConfig/dfsSecondaryHttpAddress"));
        advancedHDFSConfig.put("dfs.datanode.address", params.getParameter("advancedHDFSSiteConfig/dfsDatanodeAddress"));
        advancedHDFSConfig.put("dfs.datanode.http.address", params.getParameter("advancedHDFSSiteConfig/dfsDatanodeHttpAddress"));
        advancedHDFSConfig.put("dfs.datanode.ipc.address", params.getParameter("advancedHDFSSiteConfig/dfsDatanodeIpcAddress"));
        advancedHDFSConfig.put("dfs.datanode.handler.count", params.getParameter("advancedHDFSSiteConfig/dfsDatanodeHandlerCount"));
        advancedHDFSConfig.put("dfs.http.address", params.getParameter("advancedHDFSSiteConfig/dfsHttpAddress"));
        advancedHDFSConfig.put("dfs.https.enable", params.getParameter("advancedHDFSSiteConfig/dfsHttpsEnable"));
        advancedHDFSConfig.put("dfs.https.need.client.auth", params.getParameter("advancedHDFSSiteConfig/dfsHttpsNeedClientAuth"));
        advancedHDFSConfig.put("dfs.https.server.keystore.resource", params.getParameter("advancedHDFSSiteConfig/dfsHttpsServerKeystoreResource"));
        advancedHDFSConfig.put("dfs.https.client.keystore.resource", params.getParameter("advancedHDFSSiteConfig/dfsHttpsClientKeystoreResource"));

        advancedHDFSConfig.put("dfs.datanode.https.address", params.getParameter("advancedHDFSSiteConfig/dfsDatanodeHttpsAddress"));
        advancedHDFSConfig.put("dfs.https.address", params.getParameter("advancedHDFSSiteConfig/dfsHttpsAddress"));
        advancedHDFSConfig.put("dfs.datanode.dns.interface", params.getParameter("advancedHDFSSiteConfig/dfsDatanodeDnsInterface"));
        advancedHDFSConfig.put("dfs.datanode.dns.nameserver", params.getParameter("advancedHDFSSiteConfig/dfsDatanodeDnsNameserver"));
        advancedHDFSConfig.put("dfs.replication.considerLoad", params.getParameter("advancedHDFSSiteConfig/dfsReplicationConsiderLoad"));
        advancedHDFSConfig.put("dfs.default.chunk.view.size", params.getParameter("advancedHDFSSiteConfig/dfsDefaultChunkViewSize"));
        advancedHDFSConfig.put("dfs.datanode.du.reserved", params.getParameter("advancedHDFSSiteConfig/dfsDatanodeDuReserved"));
        advancedHDFSConfig.put("dfs.name.dir", params.getParameter("advancedHDFSSiteConfig/dfsNameDir"));
        advancedHDFSConfig.put("dfs.name.edits.dir", params.getParameter("advancedHDFSSiteConfig/dfsNameEditsDir"));
        advancedHDFSConfig.put("dfs.web.ugi", params.getParameter("advancedHDFSSiteConfig/dfsWebUgi"));

        advancedHDFSConfig.put("dfs.permissions", params.getParameter("advancedHDFSSiteConfig/dfsPermissions"));
        advancedHDFSConfig.put("dfs.permissions.supergroup", params.getParameter("advancedHDFSSiteConfig/dfsPermissionsSupergroup"));
        advancedHDFSConfig.put("dfs.data.dir", params.getParameter("advancedHDFSSiteConfig/dfsDataDir"));
        advancedHDFSConfig.put("dfs.replication", params.getParameter("advancedHDFSSiteConfig/dfsReplication"));
        advancedHDFSConfig.put("dfs.replication.max", params.getParameter("advancedHDFSSiteConfig/dfsReplicationMax"));
        advancedHDFSConfig.put("dfs.replication.min", params.getParameter("advancedHDFSSiteConfig/dfsReplicationMin"));
        advancedHDFSConfig.put("dfs.block.size", params.getParameter("advancedHDFSSiteConfig/dfsBlockSize"));
        advancedHDFSConfig.put("dfs.df.interval", params.getParameter("advancedHDFSSiteConfig/dfsDfInterval"));
        advancedHDFSConfig.put("dfs.client.block.write.retries", params.getParameter("advancedHDFSSiteConfig/dfsClientBlockWriteRetries"));
        advancedHDFSConfig.put("dfs.blockreport.intervalMsec", params.getParameter("advancedHDFSSiteConfig/dfsBlockreportIntervalMsec"));

        advancedHDFSConfig.put("dfs.blockreport.initialDelay", params.getParameter("advancedHDFSSiteConfig/dfsBlockreportInitialDelay"));
        advancedHDFSConfig.put("dfs.heartbeat.interval", params.getParameter("advancedHDFSSiteConfig/dfsHeartbeatInterval"));
        advancedHDFSConfig.put("dfs.namenode.handler.count", params.getParameter("advancedHDFSSiteConfig/dfsNamenodeHandlerCount"));
        advancedHDFSConfig.put("dfs.safemode.threshold.pct", params.getParameter("advancedHDFSSiteConfig/dfsSafemodeThresholdPct"));
        advancedHDFSConfig.put("dfs.safemode.extension", params.getParameter("advancedHDFSSiteConfig/dfsSafemodeExtension"));
        advancedHDFSConfig.put("dfs.balance.bandwidthPerSec", params.getParameter("advancedHDFSSiteConfig/dfsBalanceBandwidthPerSec"));
        advancedHDFSConfig.put("dfs.hosts", params.getParameter("advancedHDFSSiteConfig/dfsHosts"));
        advancedHDFSConfig.put("dfs.hosts.exclude", params.getParameter("advancedHDFSSiteConfig/dfsHostsExclude"));
        advancedHDFSConfig.put("dfs.max.objects", params.getParameter("advancedHDFSSiteConfig/dfsMaxObjects"));
        advancedHDFSConfig.put("dfs.namenode.decommission.interval", params.getParameter("advancedHDFSSiteConfig/dfsDamenodeDecommissionInterval"));
        advancedHDFSConfig.put("dfs.namenode.decommission.nodes.per.interval", params.getParameter("advancedHDFSSiteConfig/dfsNamenodeDecommissionNodesPerInterval"));
        advancedHDFSConfig.put("dfs.replication.interval", params.getParameter("advancedHDFSSiteConfig/dfsReplicationInterval"));
        advancedHDFSConfig.put("dfs.access.time.precision", params.getParameter("advancedHDFSSiteConfig/dfsAccessTimePrecision"));
        advancedHDFSConfig.put("dfs.support.append", params.getParameter("advancedHDFSSiteConfig/dfsSupportAppend"));
        
        String setHDFSFinalFlag = params.getParameter("advancedHDFSSiteConfig/setCoreFinalFlag");
        return advancedHDFSConfig;
    }


    /*
     * using the info from the config form and generates a list of configuration items.  These are tested and compared
     * to the default values.  If the values are different from default they are written to the resultant ConcurrentHashMap.
     * @return advancedCoreConfig, ConcurrentHashMap of config entries that can be empty
     */
    public ConcurrentHashMap gatherAdvancedCoreConfig()
    {
        ConcurrentHashMap <String,String>advancedCoreConfig = new ConcurrentHashMap<String,String>(52);

        //collect the values from the forms               
        advancedCoreConfig.put("hadoop.tmp.dir", params.getParameter("newBasicHadoopSiteConfig/newHadoopTmpDir"));
        
        advancedCoreConfig.put("hadoop.native.lib", params.getParameter("advancedCoreSiteConfig/hadoopNativeLib"));
        advancedCoreConfig.put("hadoop.http.filter.initializers", params.getParameter("advancedCoreSiteConfig/hadoopHttpFilterInitializers"));
        advancedCoreConfig.put("hadoop.security.authorization", params.getParameter("advancedCoreSiteConfig/hadoopSecurityAuthorization"));
        advancedCoreConfig.put("hadoop.logfile.size", params.getParameter("advancedCoreSiteConfig/hadoopLogfileSize"));
        advancedCoreConfig.put("hadoop.logfile.count", params.getParameter("advancedCoreSiteConfig/hadoopLogfileCount"));
        advancedCoreConfig.put("io.file.buffer.size", params.getParameter("advancedCoreSiteConfig/ioFileBufferSize"));
        advancedCoreConfig.put("io.bytes.per.checksum", params.getParameter("advancedCoreSiteConfig/ioBytesPerChecksum"));
        advancedCoreConfig.put("io.skip.checksum.errors", params.getParameter("advancedCoreSiteConfig/ioSkipChecksumErrors"));        
        advancedCoreConfig.put("io.compression.codecs", params.getParameter("advancedCoreSiteConfig/ioCompressionCodecs"));
        
        advancedCoreConfig.put("io.serializations", params.getParameter("advancedCoreSiteConfig/ioSerializations"));        
        advancedCoreConfig.put("fs.default.name", params.getParameter("newBasicHadoopSiteConfig/newFsDefaultName"));

        advancedCoreConfig.put("fs.trash.interval", params.getParameter("advancedCoreSiteConfig/fsTrashInterval"));
        advancedCoreConfig.put("fs.file.impl", params.getParameter("advancedCoreSiteConfig/fsFileImpl"));
        advancedCoreConfig.put("fs.hdfs.impl", params.getParameter("advancedCoreSiteConfig/fsHdfsImpl"));
        advancedCoreConfig.put("fs.s3.impl", params.getParameter("advancedCoreSiteConfig/fsS3Impl"));
        advancedCoreConfig.put("fs.s3n.impl", params.getParameter("advancedCoreSiteConfig/fsS3nImpl"));
        advancedCoreConfig.put("fs.kfs.impl", params.getParameter("advancedCoreSiteConfig/fsKfsImpl"));
        advancedCoreConfig.put("fs.hftp.impl", params.getParameter("advancedCoreSiteConfig/fsHftpImpl"));
        advancedCoreConfig.put("fs.hsftp.impl", params.getParameter("advancedCoreSiteConfig/fsHsftpImpl"));

        advancedCoreConfig.put("fs.ftp.impl", params.getParameter("advancedCoreSiteConfig/fsFtpImpl"));
        advancedCoreConfig.put("fs.ramfs.impl", params.getParameter("advancedCoreSiteConfig/fsRamfsImpl"));
        advancedCoreConfig.put("fs.har.impl", params.getParameter("advancedCoreSiteConfig/fsHarImpl"));
        advancedCoreConfig.put("fs.checkpoint.dir", params.getParameter("advancedCoreSiteConfig/fsCheckpointDir"));
        advancedCoreConfig.put("fs.checkpoint.edits.dir", params.getParameter("advancedCoreSiteConfig/fsCheckpointEditsDir"));
        advancedCoreConfig.put("fs.checkpoint.period", params.getParameter("advancedCoreSiteConfig/fsCheckpointPeriod"));
        advancedCoreConfig.put("fs.checkpoint.size", params.getParameter("advancedCoreSiteConfig/fsCheckpointSize"));
        advancedCoreConfig.put("fs.s3.block.size", params.getParameter("advancedCoreSiteConfig/fsS3BlockSize"));
        advancedCoreConfig.put("fs.s3.buffer.dir", params.getParameter("advancedCoreSiteConfig/fsS3BufferDir"));
        advancedCoreConfig.put("fs.s3.maxRetries", params.getParameter("advancedCoreSiteConfig/fsS3MaxRetries"));
        advancedCoreConfig.put("fs.s3.sleepTimeSeconds", params.getParameter("advancedCoreSiteConfig/fsS3SleepTimeSeconds"));
        advancedCoreConfig.put("local.cache.size", params.getParameter("advancedCoreSiteConfig/localCacheSize"));
        advancedCoreConfig.put("io.seqfile.compress.blocksize", params.getParameter("advancedCoreSiteConfig/ioSeqfileCompressBlocksize"));
        advancedCoreConfig.put("io.seqfile.lazydecompress", params.getParameter("advancedCoreSiteConfig/ioSeqfileLazydecompress"));
        advancedCoreConfig.put("io.seqfile.sorter.recordlimit", params.getParameter("advancedCoreSiteConfig/ioSeqfileSorterRecordlimit"));

        advancedCoreConfig.put("io.mapfile.bloom.size", params.getParameter("advancedCoreSiteConfig/ioMapfileBloomSize"));
        advancedCoreConfig.put("io.mapfile.bloom.error.rate", params.getParameter("advancedCoreSiteConfig/ioMapfileBloomErrorRate"));
        advancedCoreConfig.put("hadoop.util.hash.type", params.getParameter("advancedCoreSiteConfig/hadoopUtilHashType"));
        advancedCoreConfig.put("ipc.client.idlethreshold", params.getParameter("advancedCoreSiteConfig/ipcClientIdlethreshold"));
        advancedCoreConfig.put("ipc.client.kill.max", params.getParameter("advancedCoreSiteConfig/ipcClientKillMax"));
        advancedCoreConfig.put("ipc.client.connection.maxidletime", params.getParameter("advancedCoreSiteConfig/ipcClientConnectionMaxidletime"));
        advancedCoreConfig.put("ipc.client.connect.max.retries", params.getParameter("advancedCoreSiteConfig/ipcClientConnectMaxRetries"));
        advancedCoreConfig.put("ipc.server.listen.queue.size", params.getParameter("advancedCoreSiteConfig/ipcServerListenQueueSize"));
        advancedCoreConfig.put("ipc.server.tcpnodelay", params.getParameter("advancedCoreSiteConfig/ipcServerTcpnodelay"));
        advancedCoreConfig.put("ipc.client.tcpnodelay", params.getParameter("advancedCoreSiteConfig/ipcClientTcpnodelay"));
        advancedCoreConfig.put("webinterface.private.actions", params.getParameter("advancedCoreSiteConfig/webinterfacePrivateActions"));
        advancedCoreConfig.put("hadoop.rpc.socket.factory.class.default", params.getParameter("advancedCoreSiteConfig/hadoopRpcSocketFactoryClassDefault"));
        advancedCoreConfig.put("hadoop.rpc.socket.factory.class.ClientProtocol", params.getParameter("advancedCoreSiteConfig/hadoopRpcSocketFactoryClassClientProtocol"));
        advancedCoreConfig.put("hadoop.socks.server", params.getParameter("advancedCoreSiteConfig/hadoopSocksServer"));
        advancedCoreConfig.put("topology.node.switch.mapping.impl", params.getParameter("advancedCoreSiteConfig/topologyNodeSwitchMappingImpl"));
        advancedCoreConfig.put("topology.script.file.name", params.getParameter("advancedCoreSiteConfig/topologyScriptFileName"));
        advancedCoreConfig.put("topology.script.number.args", params.getParameter("advancedCoreSiteConfig/topologyScriptNumberArgs"));

        String setCoreFinalFlag = params.getParameter("advancedCoreSiteConfig/setCoreFinalFlag");
        return advancedCoreConfig;

    }

    /*
     * take the list of masters and slaves and generate logrecords files to be
     * placed in their config directories by the framework by placing them in
     * the pushDir location
     * @param resultsLocation is the location of the pushDir
     * TODO modify to use hadoopLogDir
     */

    public boolean generateMetricsFile(final String resultsLocation)
    {
        //take the list of masters and slaves and generate files to be
        //placed in their config directories
        ArrayList<String> hostList = new ArrayList<String>();
        

        //first copy the master and slaves hosts into the list
        hostList = getHostList();
       
        //clear the previous runs from the logs and logrecords directories
        prepareLoggingFacilities(hostList);
        prepareMetricsFacilities(hostList);

        //configure the client push directory
        File pushDir = new File(runDir + "com.sun.pae.hadoop.harness/");
        if(pushDir.mkdir() == false)
        {
            logger.warning("mkdir failed in GridMix.generateMetricsFile(String resultsLocation)");
        }
        logger.info(pushDir.getPath());

        BufferedWriter bfw_metrics = null;
        String nextHost = null;

        for(Iterator<String> it = hostList.iterator(); it.hasNext();)
        {
            nextHost = it.next();            
            String[] host = nextHost.split("\\.");            

            ArrayList<String> metrics_file_data = new ArrayList<String>();

            metrics_file_data.add("# Generated by PAE's FABAN harness");
            metrics_file_data.add("# Configuration of the dfs context for file");
            metrics_file_data.add("dfs.class=org.apache.hadoop.metrics.file.FileContext");
            metrics_file_data.add("dfs.period=10");
            metrics_file_data.add("dfs.fileName=/tmp/hadoop_fs_metrics-"+host[0]+"-"+runID);
            metrics_file_data.add("# Configuration of the mapred context for file");
            metrics_file_data.add("mapred.class=org.apache.hadoop.metrics.file.FileContext");
            metrics_file_data.add("mapred.period=10");
            metrics_file_data.add("mapred.fileName=/tmp/hadoop_mapred_metrics-"+host[0]+"-"+runID);
            metrics_file_data.add("# Configuration of the jvm context for file");
            metrics_file_data.add("jvm.class=org.apache.hadoop.metrics.file.FileContext");
            metrics_file_data.add("jvm.period=10");
            metrics_file_data.add("jvm.fileName=/tmp/hadoop_jvm_metrics-"+host[0]+"-"+runID);

            try
            {
                String line_to_write = null;                
                bfw_metrics = new BufferedWriter(new FileWriter(pushDir + "/hadoop-metrics.properties,"+host[0]));
                for(Iterator<String> sit = metrics_file_data.iterator();sit.hasNext();)
                {
                    line_to_write = sit.next();
                    bfw_metrics.write(line_to_write, 0, line_to_write.length());
                    bfw_metrics.newLine();
                }
                bfw_metrics.close();
                bfw_metrics = null;
                line_to_write = null;

            } catch (Exception fe)
            {
               logger.warning("Could not create metrics config file" + fe.getMessage());
            }

            //make sure we are sending valid data
            String propertiesFile = pushDir.getPath() +"/hadoop-metrics.properties,"+host[0];
            String destiniationFile = hadoopConfPath + "hadoop-metrics.properties";

            if(RunContext.pushFile(propertiesFile, nextHost, destiniationFile) != true)
            {
                logger.warning("logrecords config file was not written to "+nextHost);
            }
        }        
        return true;
    }


    @EndRun public void end() throws Exception {
        benchHandle.waitFor();
    }

    /* override DefaultBenchmark's end method to collect log file
     * and stop the hadoop processes.
     * This method is responsible for waiting for all commands started and
     * run all postprocessing needed.
  
     */
    @PostRun public void postProcess () throws Exception {        
        logger.info("PostProcessing has commenced");
        endTime = Calendar.getInstance();        
                     
        // configure the commnad to stop hadoop
        //logger.info("Stopping hadoop servers");
        //Command c = new Command(hadoopBinPath + "stop-all.sh");
        //RunContext.exec(c);

        if(this.doSummaryHTML(runDir) == true)
        {
            logger.info("doSummaryHTML completed");
        }else
        {
            logger.info("doSummaryHTML not completed");
        }
        this.doSummary(runDir);
        //stop hadoop
        //RunContext.exec(c);
     
    }

    /*
     * Compare two hashmaps and return the items that are different in another
     * @param defaultList list of Hadoop default configuration options
     * @param gatheredList list of gathered configuration options
     * @return ConcurrentHashMap list of options that differ between the lists.
     */
    public ConcurrentHashMap<String, String> compareConfigs(final ConcurrentHashMap defaultList, final ConcurrentHashMap gatheredList)
    {
        ConcurrentHashMap <String,String>changesList = new ConcurrentHashMap<String,String>(0);

        if(((defaultList != null) && (!defaultList.isEmpty())) && ((gatheredList != null) && (!gatheredList.isEmpty())))
        {
            //both lists contain something
            Enumeration defaultKeyListEnumerator = defaultList.keys();
            Enumeration gatheredKeyListEnumerator = gatheredList.keys();

            String defaultValue;
            String gatheredValue;
            String gatheredKey;
            String defaultKey;

            for (; defaultKeyListEnumerator.hasMoreElements() && gatheredKeyListEnumerator.hasMoreElements();)
            {
                defaultKey = (String)defaultKeyListEnumerator.nextElement();
                gatheredKey = (String)gatheredKeyListEnumerator.nextElement();
                defaultValue =  (String)defaultList.get(defaultKey);
                gatheredValue = (String)gatheredList.get(gatheredKey);

                if(gatheredKey.compareTo("hadoop.tmp.dir") == 0)
                {
                    changesList.put(gatheredKey, gatheredValue);

                }else if(gatheredKey.compareTo("fs.default.name") == 0)
                {              
                    changesList.put(gatheredKey, gatheredValue);

                }else if(gatheredKey.compareTo("mapred.job.tracker") == 0)
                {                    
                    changesList.put(gatheredKey, gatheredValue);
                }else if(defaultValue.compareTo(gatheredValue) != 0)
                {
                    //write the key and value to the new list                    
                    changesList.put(gatheredKey, gatheredValue);
                }              
            }

        }else
        {
            changesList = null;
            return changesList;
        }

        return changesList;
    }

    /*
     * Process all logfiles
     * @param hostList - list of all hosts for which these files should exist
     * @return Vector of Arraylists of logentries
     */
    public Vector<ArrayList<TimeCapture>> processAllLogs(final ArrayList<String> hostList)
    {

        Vector<ArrayList<TimeCapture>> hostVector = new Vector<ArrayList<TimeCapture>>();
        MRLogParser logParser = new MRLogParser();
        Vector<TimeCapture> capturedStats = new Vector<TimeCapture>();
        SimpleDateFormat newdateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd kk':'mm':'ss','SSS ");
        String host;
        for(int i = 0; i < hostList.size();i++) 
        {

            ArrayList<TimeCapture> eachHostArray = new ArrayList<TimeCapture>();

            TimeCapture mapper = new TimeCapture();
            TimeCapture reducer = new TimeCapture();
            TimeCapture shuffle = new TimeCapture();
            TimeCapture writer = new TimeCapture();

            host = hostList.get(i);

            capturedStats = logParser.processDirctory(RunContext.getOutDir()+"userlogs-"+host, runID);

            if(capturedStats != null)
            {
                for (Enumeration<TimeCapture> enumer = capturedStats.elements(); enumer.hasMoreElements();)
                {
                    TimeCapture stats = enumer.nextElement();
                    if(stats.getTag().compareTo("MAP")== 0)
                    {
                        mapper = stats;
                    }else if(stats.getTag().compareTo("REDUCE")== 0)
                    {
                        reducer = stats;
                    }else if(stats.getTag().compareTo("SHUFFLE")== 0)
                    {
                        shuffle = stats;
                    }else if(stats.getTag().compareTo("WRITE")== 0)
                    {
                        writer = stats;
                        logger.info("Write time = " + writer.getTotalAcumulatedTime());
                        logger.info("Write started at "+writer.getFirstRecord().getTime().toString());
                    }
                }
                //write each type into the arraylist for this host
                eachHostArray.add(mapper);
                eachHostArray.add(reducer);
                eachHostArray.add(shuffle);
                eachHostArray.add(writer);
                //now wite this into the vector
                hostVector.add(eachHostArray);
            }else
            {
                 hostVector.add(new ArrayList<TimeCapture>());
            }
        }
        //return new Vector<ArrayList<LogEntry>>();
        return hostVector;

        /*capturedStats = logParser.processDirctory(RunContext.getOutDir()+"userlogs-rep4450-02.sfbay", runID);
        logger.warning("size of capturedStats = "+capturedStats.size());

        TimeCapture mapper = new TimeCapture();
        TimeCapture reducer = new TimeCapture();
        TimeCapture shuffle = new TimeCapture();
        TimeCapture writer = new TimeCapture();

        //SimpleDateFormat newdateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd kk':'mm':'ss','SSS ");
        String mapperTime = newdateFormat.format(mapper.getFirstRecord().getTime());
        String reducerTime = newdateFormat.format(reducer.getFirstRecord().getTime());
        String shuffleTime = newdateFormat.format(shuffle.getFirstRecord().getTime());
        String writerTime = newdateFormat.format(writer.getFirstRecord().getTime());

        for (Enumeration<TimeCapture> enumer = capturedStats.elements(); enumer.hasMoreElements();)
        {
            TimeCapture stats = enumer.nextElement();
            if(stats.getTag().compareTo("MAP")== 0)
            {
                mapper = stats;
            }else if(stats.getTag().compareTo("REDUCE")== 0)
            {
                reducer = stats;
            }else if(stats.getTag().compareTo("SHUFFLE")== 0)
            {
                shuffle = stats;
            }else if(stats.getTag().compareTo("WRITE")== 0)
            {
                writer = stats;
                logger.info("Write time = " + writer.getTotalAcumulatedTime());
                logger.info("Write started at "+writer.getFirstRecord().getTime().toString());
            }
        }*/
   
    }

    

    /*
     * Process all logrecords files
     * @param hostList - list of all hosts for which these files should exist
     * @return Vector of Arraylists of metric log entries
     * TODO modify to use hadoopLogDir
     */

    public Vector<ArrayList<AbstractLogRecord>> processAllMetrics(final ArrayList<String> hostList)
    {

        ArrayList<AbstractLogRecord> jobTrackerRecords = new ArrayList<AbstractLogRecord>();
        ArrayList<AbstractLogRecord> jobRecords = new ArrayList<AbstractLogRecord>();
        ArrayList<AbstractLogRecord> shuffleInputRecords = new ArrayList<AbstractLogRecord>();
        ArrayList<AbstractLogRecord> shuffleOutputRecords = new ArrayList<AbstractLogRecord>();

        ArrayList<String> filesToProcess;

        //check input
        if((hostList == null) || (hostList.size() == 0))
        {
            logger.info("hostList passed to processAllMetrics was null or empty");
            return new Vector<ArrayList<AbstractLogRecord>>();
        }

        File runOutput = new File(RunContext.getOutDir());

        if(runOutput.isDirectory() == true)//ok this is the directoy
        {
            filesToProcess = new ArrayList<String>(Arrays.<String>asList(runOutput.list(new MapredMetricsFileFilter())));

            logger.info("Number of logrecords files to be processed = "+filesToProcess.size());

            String[] fileNameData;
            String host = null;
            String fileName = null;
            for(Iterator<String> it = filesToProcess.iterator(); it.hasNext();)
            {
                fileName = it.next();
                fileNameData = fileName.split("-");
                host = fileNameData[1] + "-"+fileNameData[2];


                LogFileProcessor logProcessor = new LogFileProcessor();
                jobRecords = logProcessor.processLogFile(RunContext.getOutDir() + fileName, METRIC_TYPE.JobRecord);
                if(jobRecords != null)
                {
                    logger.info("GridMix.processAllMetrics Number of JobRecord logrecords entries found = " +jobRecords.size());
                }else
                {
                    logger.warning("No JobRecord logrecords entries found");
                }

                jobTrackerRecords = logProcessor.processLogFile(RunContext.getOutDir() + fileName, METRIC_TYPE.JobtrackerRecord);
                if(jobTrackerRecords != null)
                {

                    logger.info("GridMix.processAllMetrics Number of JobtrackerRecord logrecords entries found = " +jobTrackerRecords.size());
                }else
                {
                    logger.warning("No JobtrackerRecord logrecords entries found");
                }

                shuffleInputRecords = logProcessor.processLogFile(RunContext.getOutDir() + fileName, METRIC_TYPE.ShuffleInputRecord);
                if(shuffleInputRecords != null)
                {
                    logger.info("GridMix.processAllMetrics Number of ShuffleInputRecord logrecords entries found = " +shuffleInputRecords.size());
                }else
                {
                  logger.warning("No ShuffleInputRecord logrecords entries found");
                }

                shuffleOutputRecords = logProcessor.processLogFile(RunContext.getOutDir() + fileName, METRIC_TYPE.ShuffleOutputRecord);
                if(shuffleOutputRecords != null)
                {
                    logger.info("GridMix.processAllMetrics Number of ShuffleOutputRecord logrecords entries found = " +shuffleOutputRecords.size());
                }else
                {
                  logger.warning("No shuffleOutputRecords logrecords entries found");
                }             

                //now write these logrecords to a file
                if(generateTableFromMetrics(host,jobRecords ,jobTrackerRecords, shuffleInputRecords, shuffleOutputRecords) == true)
                {
                    logger.info("Metrics output files from "+fileName+" written.");
                }else
                {
                    logger.warning("Metrics output files from "+fileName+" not written.");
                }

            }

        }else
        {
            logger.warning("Fatal error in GridMix.processAllMetrics()");
        }



        //String metricsFileName = "hadoop-mapred-logrecords."+


        return new Vector<ArrayList<AbstractLogRecord>>();
    }


    /*
     * This method takes a hadoop number in the format of 3.4444444E7 and converts it to 34444444.  Sometimes these
     * values look like this 3.5E7 so we need to add some 0s to make it make sense.  Also sometimes it looks like this
     * 4.55667788E3 so we need to drop some chars.
     * @param inputValue takes a string to convert
     * @return string with the integer value altered.
     */
    public String formatHadoopStringNumber(final String inputValue)
    {

        String extensionRemoved[] = inputValue.split("E");

        String decimalRemoved[] = extensionRemoved[0].split("\\.");
        String returnString = decimalRemoved[0] + decimalRemoved[1];

        //case when too much precision
        if((extensionRemoved[1] != null) && (returnString.length() > new Integer(extensionRemoved[1]).intValue()))
        {
            returnString = returnString.substring(0, (new Integer(extensionRemoved[1]).intValue() ));

        }else if(returnString.length() < new Integer(extensionRemoved[1]).intValue())
        {
            for(int padder = returnString.length(); padder < new Integer(extensionRemoved[1]).intValue(); padder++)
            {
                returnString = returnString + "0";
            }
        }        
        return returnString;
    }


    /*
     * This method subtracts 2 hadoop supplied integers in the form of a string like 1.9993564E7 or normal integer like 32
     * @param firstNumber is a String representing a integer to be subtracted from
     * @paran secondNumber is a String representing the integer to be subtracted
     * @return a String representing the product of the subtracted integers
     */
    public String stringSubtract(final String firstNumber, final String secondNumber)
    {
        //make sure the inputs are what we are execting
        if((firstNumber != null) &&  (secondNumber != null))
        {

            try
            {

            if((firstNumber.contains("E") == true) && (secondNumber.contains("E") == true))
            {
                if(firstNumber.compareTo("0")== 0)
                {
                    return firstNumber;
                }else
                {
                    long first = new Long(formatHadoopStringNumber(firstNumber));
                    long second = new Long(formatHadoopStringNumber(secondNumber));

                    return new String(""+ (first - second));
                }
            }else
            {
                //this means we have a normal integer
                long first = new Long(firstNumber);
                long second = new Long(secondNumber);
                
                return new String(""+ (first - second));
            }
            }catch (NumberFormatException nfe)
            {
                logger.warning("Error found in stringSubtract argumens were" +firstNumber +", "+secondNumber +".  error was: "+nfe.getMessage());
                return new String("-1");
            }
                                    
        }
        return new String("-1");
    }

    /*
     * This method uses the formatted hadoop integer represented as a String and
     * divides this number by the second number.
     * @param firstNumber formatted hadoop integer representad as a string
     * @param secondNumber integer as s String
     * @return product of the division
     */
    public String stringDivide(final String firstNumber, final String secondNumber)
    {
        if((firstNumber != null) && (secondNumber != null))
        {
            //the values are not = 0
            long lFirstNumber = new Long(firstNumber).longValue();
            long lSecondsPassed = new Long(secondNumber).longValue();

            if((lFirstNumber > 0) && (lSecondsPassed > 0))
            {
                return new String(""+(lFirstNumber/lSecondsPassed));
            }else
            {
                return new String("0");
            }
        }
        return new String();
    }

    /*
     * This method takes 3 arguments one list of options for each file to be written and
     * these files are written only to the master.
     */
    public boolean writeConfigFiles(final ConcurrentHashMap coreConfig, final ConcurrentHashMap hdfsConfig, final ConcurrentHashMap mapredConfig)
    {
        BufferedWriter coreSiteFile = null;
        BufferedWriter hdfsSiteFile = null;
        BufferedWriter mapredSiteFile = null;

        if(((coreConfig != null))&&((hdfsConfig != null))&&((mapredConfig != null)))
        {
            try
            {
                String startProperty = "\t<property>";
                String endProperty = "\t</property>";
                String startName = "\t\t<name>";
                String endName = "</name>";
                String startValue = "\t\t<value>";
                String endValue = "</value>";

                ArrayList <String>coreData = new ArrayList<String>();                                

                coreData.add("<?xml version=\"1.0\"?>");
                coreData.add("<?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?>");
                coreData.add("<configuration>");
                
                if(!coreConfig.isEmpty())
                {
                    String key;
                    String value;

                    for(Enumeration coreConfigEnumerator = coreConfig.keys();coreConfigEnumerator.hasMoreElements();)
                    {
                        key = (String)coreConfigEnumerator.nextElement();
                        value = (String)coreConfig.get(key);
                        coreData.add(startProperty);
                        coreData.add(startName + key + endName);
                        coreData.add(startValue + value + endValue);
                        coreData.add(endProperty);
                    }
                }

                coreData.add("</configuration>");
                coreSiteFile = new BufferedWriter(new FileWriter(hadoopHome + File.separator + "conf" + File.separator + "core-site.xml"));
                for(Iterator<String> it = coreData.iterator();it.hasNext();)
                {
                    String line = it.next();
                    coreSiteFile.write(line, 0, line.length());
                    coreSiteFile.newLine();
                }
                coreSiteFile.close();

                ArrayList <String>hdfsData = new ArrayList<String>();
                hdfsData.add("<?xml version=\"1.0\"?>");
                hdfsData.add("<?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?>");
                hdfsData.add("<configuration>");
                
                if(!hdfsConfig.isEmpty())
                {
                    String key;
                    String value;

                    for(Enumeration hdfsConfigEnumerator = hdfsConfig.keys();hdfsConfigEnumerator.hasMoreElements();)
                    {
                        key = (String)hdfsConfigEnumerator.nextElement();
                        value = (String)hdfsConfig.get(key);
                        hdfsData.add(startProperty);
                        hdfsData.add(startName + key + endName);
                        hdfsData.add(startValue + value + endValue);
                        hdfsData.add(endProperty);
                    }
                }

                hdfsData.add("</configuration>");
                hdfsSiteFile = new BufferedWriter(new FileWriter(hadoopHome + File.separator + "conf" + File.separator + "hdfs-site.xml"));
                for(Iterator<String> it = hdfsData.iterator();it.hasNext();)
                {
                    String line = it.next();
                    hdfsSiteFile.write(line, 0, line.length());
                    hdfsSiteFile.newLine();
                }
                hdfsSiteFile.close();

                ArrayList <String>mapredData = new ArrayList<String>();
                mapredData.add("<?xml version=\"1.0\"?>");
                mapredData.add("<?xml-stylesheet type=\"text/xsl\" href=\"configuration.xsl\"?>");
                mapredData.add("<configuration>");

                if(!mapredConfig.isEmpty())
                {
                    String key;
                    String value;

                    for(Enumeration mapredConfigEnumerator = mapredConfig.keys();mapredConfigEnumerator.hasMoreElements();)
                    {
                        key = (String)mapredConfigEnumerator.nextElement();
                        value = (String)mapredConfig.get(key);
                        mapredData.add(startProperty);
                        mapredData.add(startName + key + endName);
                        mapredData.add(startValue + value + endValue);
                        mapredData.add(endProperty);
                    }
                }

                mapredData.add("</configuration>");
                mapredSiteFile = new BufferedWriter(new FileWriter(hadoopHome + File.separator + "conf" + File.separator + "mapred-site.xml"));
                for(Iterator<String> it = mapredData.iterator();it.hasNext();)
                {
                    String line = it.next();
                    mapredSiteFile.write(line, 0, line.length());
                    mapredSiteFile.newLine();
                }
                mapredSiteFile.close();

            }catch (Exception fe)
            {
                logger.warning("Could not create one or more of the site.xml files " + fe);
                return false;
            }
        }else
        {
            return false;
        }

        return true;
    }


    /*
     * This method generates the tables needed to graph the logrecords results from the logrecords log outputs
     * If new logrecords are needed we need to add 2 classes per metric
     * we need a record class that extends AbstractLogRecord
     * we need a parser class that extends AbstractLogParser
     * We also need to add an if then else clause to processAllMetrics to
     * process the new logrecords along with a new section for this method
     * @param jobArray Array of JobRecord
     * @param jobTrackerArray Array of JobtrackerRecord
     * @param shuffleInputArray Array of ShuffleInputRecord
     * @prarm shuffleOutputArray Array of ShuffleOutputRecord
     */
    public boolean generateTableFromMetrics(final String host, final ArrayList<AbstractLogRecord> jobArray ,
            ArrayList<AbstractLogRecord> jobTrackerArray,
            ArrayList<AbstractLogRecord> shuffleInputArray,
            ArrayList<AbstractLogRecord> shuffleOutputArray)
    {
        SimpleDateFormat newdateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd kk':'mm':'ss','SSS ");

        //convert the arrays
            ArrayList<JobRecord> jobRecordArray = new ArrayList<JobRecord>(jobArray.size());
            ArrayList<JobtrackerRecord> jobRecordTrackerArray = new ArrayList<JobtrackerRecord>(jobTrackerArray.size());
            ArrayList<ShuffleInputRecord> shuffleInputRecordArray = new ArrayList<ShuffleInputRecord>(shuffleInputArray.size());
            ArrayList<ShuffleOutputRecord> shuffleOutputRecordArray = new ArrayList<ShuffleOutputRecord>(shuffleOutputArray.size());

            if((jobArray != null)&&(jobArray.size() > 0))
            {
                //we create a container for each of the known types we will see here.  This is so we can dictate the order of the info
                //produced.  This is because hadoop does not produce these logs in any aparticular order.
                ArrayList<JobRecord> dataLocalMapTasks = new ArrayList<JobRecord>();
                ArrayList<JobRecord> mapInputRecords = new ArrayList<JobRecord>();
                ArrayList<JobRecord> combineOutputRecords = new ArrayList<JobRecord>();
                ArrayList<JobRecord> outputBytes = new ArrayList<JobRecord>();
                ArrayList<JobRecord> inputBytes = new ArrayList<JobRecord>();
                ArrayList<JobRecord> hdfsRead = new ArrayList<JobRecord>();
                ArrayList<JobRecord> launchedTasks = new ArrayList<JobRecord>();
                ArrayList<JobRecord> combineInputRecords = new ArrayList<JobRecord>();
                ArrayList<JobRecord> localBytesWritten = new ArrayList<JobRecord>();
                ArrayList<JobRecord> mapOutputRecords = new ArrayList<JobRecord>();
                
                for(Iterator<AbstractLogRecord> it = jobArray.iterator(); it.hasNext();)
                {
                    JobRecord jr = (JobRecord)it.next();

                    if(jr.getCounter().startsWith("Data-local map tasks") == true)
                    {
                        dataLocalMapTasks.add(jr);
                    }else if(jr.getCounter().startsWith("Map input records") == true)
                    {
                        mapInputRecords.add(jr);
                    }else if(jr.getCounter().startsWith("Combine output records") == true)
                    {
                        combineOutputRecords.add(jr);
                    }else if(jr.getCounter().startsWith("Map output bytes") == true)
                    {
                        outputBytes.add(jr);
                    }else if(jr.getCounter().startsWith("Map input bytes") == true)
                    {
                        inputBytes.add(jr);
                    }else if(jr.getCounter().startsWith("HDFS bytes read") == true)
                    {
                        hdfsRead.add(jr);
                    }else if(jr.getCounter().startsWith("Launched map tasks") == true)
                    {
                        launchedTasks.add(jr);
                    }else if(jr.getCounter().startsWith("Combine input records") == true)
                    {
                        combineInputRecords.add(jr);
                    }else if(jr.getCounter().compareTo("Local bytes written") == 0)
                    {                        
                        localBytesWritten.add(jr);
                    }else if(jr.getCounter().startsWith("Map output records") == true)
                    {
                        mapOutputRecords.add(jr);
                    }
                    
                }

                //we need to know what is the smallest list else we will get bogus or or part records
                //we set the variable smallest to one of the values and check the others keeping the smallest value
                //this prevents us attempting a row we can not complete

                int smallestList = dataLocalMapTasks.size();
                if(mapInputRecords.size() < smallestList)
                {
                    smallestList = mapInputRecords.size();
                }
                if(combineOutputRecords.size() < smallestList)
                {
                    smallestList = combineOutputRecords.size();
                }
                if(outputBytes.size() < smallestList)
                {
                    smallestList = outputBytes.size();
                }
                if(inputBytes.size() < smallestList)
                {
                    smallestList = inputBytes.size();
                }
                if(hdfsRead.size() < smallestList)
                {
                    smallestList = hdfsRead.size();
                }
                if(launchedTasks.size() < smallestList)
                {
                    smallestList = launchedTasks.size();
                }
                if(combineInputRecords.size() < smallestList)
                {
                    smallestList = combineInputRecords.size();
                }
                if(localBytesWritten.size() < smallestList)
                {
                    smallestList = localBytesWritten.size();
                }
                if(mapOutputRecords.size() < smallestList)
                {
                    smallestList = mapOutputRecords.size();
                }
              
                //first we will develop the job files
                TextTable jobTable = new TextTable(smallestList, 11);
                
                //set the headers
                jobTable.setHeader(0, "Time");
                jobTable.setHeader(1, "Data local map tasks ");
                jobTable.setHeader(2, "Map input records ");
                jobTable.setHeader(3, "Combine output records");
                jobTable.setHeader(4, "Output bytes");
                jobTable.setHeader(5, "Input bytes");
                jobTable.setHeader(6, "HDFS bytes read");
                jobTable.setHeader(7, "Launched map tasks");
                jobTable.setHeader(8, "Combine input records");
                jobTable.setHeader(9, "Local bytes written");
                jobTable.setHeader(10, "Map output records");


                TextTable outputTime = new TextTable(smallestList, 2);
                outputTime.setHeader(0, "Seconds");
                outputTime.setHeader(1, "Map Output bytes/sec");

                TextTable inputTime = new TextTable(smallestList, 2);
                inputTime.setHeader(0, "Seconds");
                inputTime.setHeader(1, "Map Input bytes/sec");

                TextTable outputPeak = new TextTable(1,3);
                outputPeak.setHeader(0, "Timestamp");
                outputPeak.setHeader(1, "Peak Map Output Bytes rate");
                outputPeak.setHeader(2, "Sample point (seconds)");

                TextTable inputPeak = new TextTable(1,3);
                inputPeak.setHeader(0, "Timestamp");
                inputPeak.setHeader(1, "Peak Map Input Bytes rate");
                inputPeak.setHeader(2, "Sample point (seconds)");



                int row = 0;
                int counter = jobRecordArray.size();
                int peakInputByteRate = 0;
                int peakOutputByteRate = 0;
                int runPeakOutputByteRate = 0;
                int runPeakInputByteRate = 0;
                int peakInputSamplePoint = 0;
                int peakOutputSamplePoint = 0;
                String peakInputTimestamp = null;
                String peakOutputTimestamp = null;

                //all of these are placed outside of the loop for efficiency
                String currentOutputBytes = null;
                String previousOutputBytes = null;
                String currentInputBytes = null;
                String previousInputBytes = null;
                String currentOutputRate = null;
                String currentInputRate = null;
                
                for(row = 0; row < smallestList;row++)
                {                    
                    jobTable.setField(row, 0, new String(""+(row +1)*10));//newdateFormat.format(dataLocalMapTasks.get(row).getTimestamp().getTime()));
                    jobTable.setField(row, 1, dataLocalMapTasks.get(row).getValue());
                    jobTable.setField(row, 2, mapInputRecords.get(row).getValue());
                    jobTable.setField(row, 3, combineOutputRecords.get(row).getValue());
                    jobTable.setField(row, 4, outputBytes.get(row).getValue());
                    jobTable.setField(row, 5, inputBytes.get(row).getValue());
                    jobTable.setField(row, 6, hdfsRead.get(row).getValue());
                    jobTable.setField(row, 7, launchedTasks.get(row).getValue());
                    jobTable.setField(row, 8, combineInputRecords.get(row).getValue());
                    jobTable.setField(row, 9, localBytesWritten.get(row).getValue());
                    jobTable.setField(row, 10, mapOutputRecords.get(row).getValue());

                    //set the relative time we know the samples are taken at 10 second intervals
                    outputTime.setField(row, 0, new String(""+(row +1)*10));
                    inputTime.setField(row, 0, new String(""+(row +1)*10));
                    

                    if(row > 0)
                    {
                        //we do not want to divide by zero or wast time formatting or subtracting the string
                        if((outputBytes.get(row -1).getValue().compareTo("0") != 0) && (outputBytes.get(row).getValue().compareTo("0") != 0))
                        {
                            currentOutputBytes = outputBytes.get(row).getValue();
                            previousOutputBytes = outputBytes.get(row -1).getValue();
                            currentInputBytes = inputBytes.get(row).getValue();
                            previousInputBytes = inputBytes.get(row -1).getValue();
                            //this is a little confusing but I am dividing the result of the stringSubtraction by 10 which needs to be a String to use
                            currentOutputRate = stringDivide(stringSubtract(currentOutputBytes, previousOutputBytes), new String(""+10));
                            currentInputRate = stringDivide(stringSubtract(currentInputBytes, previousInputBytes), new String(""+10));
                            
                            outputTime.setField(row, 1,currentOutputRate);
                            inputTime.setField(row, 1, currentInputRate);

                            if(new Integer(currentOutputRate).intValue() > peakOutputByteRate)
                            {
                                peakOutputByteRate = new Integer(currentOutputRate).intValue();
                                Date timestamp = dataLocalMapTasks.get(row).getTimestamp();
                                if (timestamp != null)
                                    peakOutputTimestamp = newdateFormat.format(timestamp);
                                peakOutputSamplePoint = (row +1)*10;
                            }
                            
                            if(new Integer(currentInputRate).intValue() > peakInputByteRate)
                            {
                                peakInputByteRate = new Integer(currentInputRate).intValue();
                                Date timestamp = dataLocalMapTasks.get(row).getTimestamp();
                                if (timestamp != null)
                                    peakInputTimestamp = newdateFormat.format(timestamp);
                                peakInputSamplePoint = (row +1)*10;
                            }
                        }
                        
                    }else
                    {
                        outputTime.setField(row, 1, stringDivide(formatHadoopStringNumber(outputBytes.get(row).getValue()),new String(""+10)));
                        inputTime.setField(row, 1, stringDivide(formatHadoopStringNumber(inputBytes.get(row).getValue()), new String(""+10)));
                    }
                }

                //now configure the peak values tables
                inputPeak.setField(0, 0, "N/A");
                inputPeak.setField(0, 1, new String("" +peakInputByteRate));
                inputPeak.setField(0, 2, new String("" +peakInputSamplePoint));

                outputPeak.setField(0, 0, "N/A");
                outputPeak.setField(0, 1, new String(""+peakOutputByteRate));
                outputPeak.setField(0, 2, new String(""+peakOutputSamplePoint));

                //write the file
                try
                {
                    //BufferedWriter bfw_metrics = new BufferedWriter(new FileWriter(hadoopConfPath + File.separator + "hadoop-logrecords.properties-"+host));
                    BufferedWriter bfw_metrics = new BufferedWriter(new FileWriter(RunContext.getOutDir() + "hadoop_metrics_jobRecords.xan."+host));

                    StringBuffer pageBuffer = new StringBuffer();
                    StringBuffer timeInputBuffer = new StringBuffer();
                    StringBuffer timeOutputBuffer = new StringBuffer();
                    StringBuffer peakInputBuffer= new StringBuffer();
                    StringBuffer peakOutputBuffer = new StringBuffer();

                    StringBuffer inputBuffer = new StringBuffer();
                    StringBuffer outputBuffer = new StringBuffer();

                    StringBuffer heading = new StringBuffer();

                    heading.append("Title:" + "Hadoop JobRecord Metric Results");
                    heading.append("\n");

                    pageBuffer.append("\n");
                    pageBuffer.append("\n");
                    pageBuffer.append("Section:" + "Hadoop JobRecord Metric");
                    pageBuffer.append("\n");

                    inputBuffer.append("\n");
                    inputBuffer.append("\n");
                    inputBuffer.append("Section: Map Input bytes/sec\n");
                    inputBuffer.append("Display: Line\n");

                    outputBuffer.append("\n");
                    outputBuffer.append("\n");
                    outputBuffer.append("Section: Map Output bytes/sec\n");
                    outputBuffer.append("Display: Line\n");


                    peakInputBuffer.append("\n");
                    peakInputBuffer.append("\n");
                    peakInputBuffer.append("Section: Peak Map Input Bytes rate\n");
                    //peakInputBuffer.append("Display: Line\n");

                    peakOutputBuffer.append("\n");
                    peakOutputBuffer.append("\n");
                    peakOutputBuffer.append("Section: Peak Map Output Bytes rate\n");
                    //peakOutputBuffer.append("Display: Line\n");

                   
                    bfw_metrics.write(heading.toString());
                    bfw_metrics.write(inputBuffer.toString());
                    bfw_metrics.write(inputTime.format(timeInputBuffer).toString());
                    bfw_metrics.write(outputBuffer.toString());
                    bfw_metrics.write(outputTime.format(timeOutputBuffer).toString());

                    
                    bfw_metrics.write(inputPeak.format(peakInputBuffer).toString());
                    
                    bfw_metrics.write(outputPeak.format(peakOutputBuffer).toString());

                    bfw_metrics.write(jobTable.format(pageBuffer).toString());

                    bfw_metrics.close();
                    bfw_metrics = null;
                    logger.info("File written " + RunContext.getOutDir() + "hadoop_metrics_jobRecords." +host);

                }catch(IOException ioe)
                {
                    logger.warning("Could not create logrecords output file (hadoop_metrics_jobRecords) in generateTableFromMetrics" + ioe.getMessage());
                }

            }else
            {
                logger.warning("jobArray was empty or null in GridMix.generateTableFromMetrics()");
            }

            if((jobTrackerArray != null)&&(jobTrackerArray.size() > 0))
            {
                for(Iterator<AbstractLogRecord> it = jobTrackerArray.iterator(); it.hasNext();)
                {
                    JobtrackerRecord jr = (JobtrackerRecord)it.next();
                    jobRecordTrackerArray.add(jr);
                }

                //next we will develop the jobTracker file
                TextTable jobTrackerTable = new TextTable(jobRecordTrackerArray.size(), 5);

                //set the headers
                jobTrackerTable.setHeader(0, "Time");
                jobTrackerTable.setHeader(1, "Maps launched");
                jobTrackerTable.setHeader(2, "Maps finished");
                jobTrackerTable.setHeader(3, "Reducers launched");
                jobTrackerTable.setHeader(4, "Reducers finished");


                TextTable mapLaunched = new TextTable(jobRecordTrackerArray.size(),2);
                mapLaunched.setHeader(0, "Relative Time");
                mapLaunched.setHeader(1, "Maps Launched/sec");

                TextTable mapFinished = new TextTable(jobRecordTrackerArray.size(),2);
                mapFinished.setHeader(0, "Relative Time");
                mapFinished.setHeader(1, "Maps Finished/sec");
                         
                for(int row = 0; row < jobRecordTrackerArray.size();row++)
                {                    
                    jobTrackerTable.setField(row, 0, "N/A");
                    jobTrackerTable.setField(row, 1, jobRecordTrackerArray.get(row).getMaps_launched());
                    jobTrackerTable.setField(row, 2, jobRecordTrackerArray.get(row).getMaps_completed());
                    jobTrackerTable.setField(row, 3, jobRecordTrackerArray.get(row).getReduces_launched());
                    jobTrackerTable.setField(row, 4, jobRecordTrackerArray.get(row).getReduces_completed());

                    //set the relative time we know the samples are taken at 10 second intervals
                    mapLaunched.setField(row, 0, new String(""+((row +1)*10)));
                    mapFinished.setField(row,0, new String(""+((row +1)*10)));
                    if(row > 0)
                    {
                        //outputTime.setField(row, 1,stringSubtract(outputBytes.get(row).getValue(), outputBytes.get(row -1).getValue()) );
                        mapLaunched.setField(row, 1, stringSubtract(jobRecordTrackerArray.get(row).getMaps_launched(), jobRecordTrackerArray.get(row -1).getMaps_launched()));
                        mapFinished.setField(row, 1, stringSubtract(jobRecordTrackerArray.get(row).getMaps_completed(), jobRecordTrackerArray.get(row -1).getMaps_completed()));
                        
                        //mapFinished.setField(row, 1, jobTracker.getMaps_completed());
                    }else
                    {
                        mapLaunched.setField(row, 1, jobRecordTrackerArray.get(row).getMaps_launched());
                        mapFinished.setField(row, 1, jobRecordTrackerArray.get(row).getMaps_completed());
                    }

                    
                }

                //write the file
                try
                {
                    BufferedWriter bfw_metrics = new BufferedWriter(new FileWriter(RunContext.getOutDir() + "hadoop_metrics_jobTrackerRecords.xan."+host));
                    StringBuffer sb = new StringBuffer();
                    StringBuffer sbl = new StringBuffer();
                    StringBuffer sbf = new StringBuffer();

                    StringBuffer launched = new StringBuffer();
                    StringBuffer finished = new StringBuffer();

                    StringBuffer heading = new StringBuffer();

                    heading.append("Title:" + "Hadoop JobTrackerRecord Metric Results");

                    sb.append("\n");
                    sb.append("\n");
                    sb.append("Section: JobTrackerRecord Metrics\n");


                    launched.append("\n");
                    launched.append("\n");
                    launched.append("Section: Launched Map Metrics\n");
                    launched.append("Display: Line\n");

                    finished.append("\n");
                    finished.append("\n");
                    finished.append("Section: Finished Map Metrics\n");
                    finished.append("Display: Line\n");

                    bfw_metrics.write(heading.toString());
                    bfw_metrics.write(launched.toString());
                    bfw_metrics.write(mapLaunched.format(sbl).toString());
                    bfw_metrics.write(finished.toString());
                    bfw_metrics.write(mapFinished.format(sbf).toString());
                    bfw_metrics.write(jobTrackerTable.format(sb).toString());
                    bfw_metrics.close();
                    bfw_metrics = null;
                    logger.info("File written " + RunContext.getOutDir() + "hadoop_metrics_jobTrackerRecords." +host);

                }catch(IOException ioe)
                {
                    logger.warning("Could not create logrecords output file (hadoop_metrics_jobTrackerRecords) in generateTableFromMetrics" + ioe.getMessage());
                }
            }else
            {
                logger.warning("jobTrackerArray was empty or null in GridMix.generateTableFromMetrics()");
            }
            
            if((shuffleInputArray != null) && (shuffleInputArray.size() > 0))
            {
                for(Iterator<AbstractLogRecord> it = shuffleInputArray.iterator(); it.hasNext();)
                {
                    ShuffleInputRecord jr = (ShuffleInputRecord)it.next();
                    shuffleInputRecordArray.add(jr);
                }

                //next we will develop the jobTracker file
                TextTable shuffleInputTable = new TextTable(shuffleInputArray.size(), 5);
                //set the headers
                shuffleInputTable.setHeader(0, "Relative Time");                
                shuffleInputTable.setHeader(1, "Shuffle failed fetches");
                shuffleInputTable.setHeader(2, "Shuffle fetchers busy percent");
                shuffleInputTable.setHeader(3, "Shuffle input bytes");
                shuffleInputTable.setHeader(4, "Shuffle success fetches");


                TextTable failed = new TextTable(shuffleInputArray.size(), 2);
                failed.setHeader(0, "Relative Time");
                failed.setHeader(1, "Failed fetches");

                TextTable busy = new TextTable(shuffleInputArray.size(), 2);
                busy.setHeader(0, "Relative Time");
                busy.setHeader(1, "Fetches % Busy");

                TextTable input = new TextTable(shuffleInputArray.size(), 2);
                input.setHeader(0, "Relative Time");
                input.setHeader(1, "Input Bytes/sec");

                TextTable success = new TextTable(shuffleInputArray.size(), 2);
                success.setHeader(0, "Relative Time");
                success.setHeader(1, "Success fetches");
                                
                String currentValue;
                String previousValue;

                for(int row = 0; row < shuffleInputRecordArray.size();row++)
                {                    
                    shuffleInputTable.setField(row, 0, new String(""+((row +1)*10)));
                    shuffleInputTable.setField(row, 1, shuffleInputRecordArray.get(row).getShuffle_failed_fetches());
                    shuffleInputTable.setField(row, 2, shuffleInputRecordArray.get(row).getShuffle_fetchers_busy_percent());
                    shuffleInputTable.setField(row, 3, shuffleInputRecordArray.get(row).getShuffle_input_bytes());
                    shuffleInputTable.setField(row, 4, shuffleInputRecordArray.get(row).getShuffle_success_fetches());


                    failed.setField(row, 0, new String(""+((row +1)*10)));
                    failed.setField(row, 1, shuffleInputRecordArray.get(row).getShuffle_failed_fetches());

                    busy.setField(row, 0, new String(""+((row +1)*10)));
                    busy.setField(row, 1, shuffleInputRecordArray.get(row).getShuffle_fetchers_busy_percent());

                    input.setField(row, 0, new String(""+((row +1)*10)));
                    if(row != 0)
                    {
                        currentValue = shuffleInputRecordArray.get(row).getShuffle_input_bytes();
                        previousValue = shuffleInputRecordArray.get(row - 1).getShuffle_input_bytes();
                        input.setField(row, 1, stringDivide(stringSubtract(currentValue, previousValue), new String(""+10)));
                    }else
                    {
                        input.setField(row, 1, stringDivide(shuffleInputRecordArray.get(row).getShuffle_input_bytes(), new String(""+10)));
                    }                    

                    success.setField(row, 0, new String(""+((row +1)*10)));
                    success.setField(row, 1, shuffleInputRecordArray.get(row).getShuffle_success_fetches());

                    currentValue = null;
                    previousValue = null;
                    
                }

                //write the file
                try
                {
                    BufferedWriter bfw_metrics = new BufferedWriter(new FileWriter(RunContext.getOutDir() + "hadoop_metrics_shuffleInputRecords.xan."+host));
                    StringBuffer sb = new StringBuffer();
                    StringBuffer sbf = new StringBuffer();
                    StringBuffer sbb = new StringBuffer();
                    StringBuffer sbi = new StringBuffer();
                    StringBuffer sbs = new StringBuffer();

                    StringBuffer failedsb = new StringBuffer();
                    StringBuffer busysb = new StringBuffer();
                    StringBuffer inputsb = new StringBuffer();
                    StringBuffer successdb = new StringBuffer();
                    StringBuffer heading = new StringBuffer();



                    heading.append("Title:" + "Hadoop ShuffleInput Metric Results");                                        

                    sb.append("\n");
                    sb.append("\n");
                    sb.append("Section: Hadoop ShuffleInput Metric Data\n");

                    failedsb.append("\n");
                    failedsb.append("\n");
                    failedsb.append("Section: Hadoop ShuffleInput Failed Data\n");
                    failedsb.append("Display: Line\n");

                    busysb.append("\n");
                    busysb.append("\n");
                    busysb.append("Section: Hadoop ShuffleInput Busy Data\n");
                    busysb.append("Display: Line\n");

                    inputsb.append("\n");
                    inputsb.append("\n");
                    inputsb.append("Section: Hadoop ShuffleInput Input Data\n");
                    inputsb.append("Display: Line\n");

                    successdb.append("\n");
                    successdb.append("\n");
                    successdb.append("Section: Hadoop ShuffleInput Successful Fetches Data\n");
                    successdb.append("Display: Line\n");

                    bfw_metrics.write(heading.toString());
                    bfw_metrics.write(failedsb.toString());
                    bfw_metrics.write(failed.format(sbf).toString());

                    bfw_metrics.write(inputsb.toString());
                    bfw_metrics.write(input.format(sbi).toString());

                    bfw_metrics.write(busysb.toString());
                    bfw_metrics.write(busy.format(sbb).toString());
                 
                    bfw_metrics.write(successdb.toString());
                    bfw_metrics.write(success.format(sbs).toString());

                    bfw_metrics.write(shuffleInputTable.format(sb).toString());
                    
                    bfw_metrics.close();
                    bfw_metrics = null;
                    logger.info("File written " + RunContext.getOutDir() + "hadoop_metrics_shuffleInputRecords." +host);

                }catch(IOException ioe)
                {
                    logger.warning("Could not create logrecords output file (hadoop_metrics_shuffleInputRecords) in generateTableFromMetrics" + ioe.getMessage());
                }

            }else
            {
                logger.warning("shuffleInputArray was empty or null in GridMix.generateTableFromMetrics()");
            }

            if((shuffleOutputArray != null) && (shuffleOutputArray.size() > 0))
            {
                for(Iterator<AbstractLogRecord> it = shuffleOutputArray.iterator(); it.hasNext();)
                {
                    ShuffleOutputRecord jr = (ShuffleOutputRecord)it.next();
                    shuffleOutputRecordArray.add(jr);
                }

                //next we will develop the jobTracker file
                TextTable shufflePutputTable = new TextTable(shuffleOutputArray.size(), 5);
                //set the headers
                shufflePutputTable.setHeader(0, "Relative Time");
                shufflePutputTable.setHeader(1, "Shuffle failed outputs");
                shufflePutputTable.setHeader(2, "Shuffle handler busy percent");
                shufflePutputTable.setHeader(3, "Shuffle output bytes");
                shufflePutputTable.setHeader(4, "Shuffle_success_outputs");

                TextTable failed = new TextTable(shuffleOutputArray.size(), 2);
                failed.setHeader(0, "Relative Time");
                failed.setHeader(1, "Shuffle failed outputs");

                TextTable busy = new TextTable(shuffleOutputArray.size(), 2);
                busy.setHeader(0, "Relative Time");
                busy.setHeader(1, "Shuffle handler % busy");

                TextTable output = new TextTable(shuffleOutputArray.size(), 2);
                output.setHeader(0, "Relative Time");
                output.setHeader(1, "Shuffle output bytes/sec");

                TextTable success = new TextTable(shuffleOutputArray.size(), 2);
                success.setHeader(0, "Relative Time");
                success.setHeader(1, "Shuffle_success_outputs");

                for(int row = 0; row < shuffleOutputRecordArray.size(); row++)
                {                    
                    shufflePutputTable.setField(row, 0, new String(""+((row +1)*10)));
                    shufflePutputTable.setField(row, 1, shuffleOutputRecordArray.get(row).getShuffle_failed_outputs());
                    shufflePutputTable.setField(row, 2, shuffleOutputRecordArray.get(row).getShuffle_handler_busy_percent());
                    shufflePutputTable.setField(row, 3, shuffleOutputRecordArray.get(row).getShuffle_output_bytes());
                    shufflePutputTable.setField(row, 4, shuffleOutputRecordArray.get(row).getShuffle_success_outputs());

                    failed.setField(row, 0, new String(""+((row +1)*10)));
                    busy.setField(row, 0, new String(""+((row +1)*10)));
                    output.setField(row, 0, new String(""+((row +1)*10)));
                    success.setField(row, 0, new String(""+((row +1)*10)));

                    failed.setField(row, 1, shuffleOutputRecordArray.get(row).getShuffle_failed_outputs());
                    busy.setField(row, 1, shuffleOutputRecordArray.get(row).getShuffle_handler_busy_percent());
                    if(row != 0)
                    {
                        output.setField(row, 1, shuffleOutputRecordArray.get(row).getShuffle_output_bytes());
                    }else
                    {
                        output.setField(row, 1, shuffleOutputRecordArray.get(row).getShuffle_output_bytes());
                    }
                    
                    success.setField(row, 1, shuffleOutputRecordArray.get(row).getShuffle_success_outputs());
                    
                }

                //write the file
                try
                {
                    BufferedWriter bfw_metrics = new BufferedWriter(new FileWriter(RunContext.getOutDir() + "hadoop_metrics_shuffleOutputRecords.xan."+host));
                    StringBuffer sb = new StringBuffer();
                    StringBuffer sbf = new StringBuffer();
                    StringBuffer sbb = new StringBuffer();
                    StringBuffer sbo = new StringBuffer();
                    StringBuffer sbs = new StringBuffer();

                    StringBuffer failed_sb = new StringBuffer();
                    StringBuffer busy_sb = new StringBuffer();
                    StringBuffer output_sb = new StringBuffer();
                    StringBuffer success_sb = new StringBuffer();

                    StringBuffer heading = new StringBuffer();

                    heading.append("Title:" + "Hadoop ShuffleOutput Metric Results");

                    sb.append("\n");
                    sb.append("\n");
                    sb.append("Section: ShuffleOutput Metrics\n");                    

                    failed_sb.append("\n");
                    failed_sb.append("\n");
                    failed_sb.append("Section: ShuffleOutput Failed Metrics\n");
                    failed_sb.append("Display: Line\n");

                    busy_sb.append("\n");
                    busy_sb.append("\n");
                    busy_sb.append("Section: ShuffleOutput % Busy Metrics\n");
                    busy_sb.append("Display: Line\n");

                    output_sb.append("\n");
                    output_sb.append("\n");
                    output_sb.append("Section: ShuffleOutput Output Metrics\n");
                    output_sb.append("Display: Line\n");

                    success_sb.append("\n");
                    success_sb.append("\n");
                    success_sb.append("Section: ShuffleOutput Successful Shuffle Metrics\n");
                    success_sb.append("Display: Line\n");


                    bfw_metrics.write(heading.toString());
                    bfw_metrics.write(failed_sb.toString());
                    bfw_metrics.write(failed.format(sbf).toString());

                    bfw_metrics.write(busy_sb.toString());
                    bfw_metrics.write(busy.format(sbb).toString());

                    bfw_metrics.write(output_sb.toString());
                    bfw_metrics.write(output.format(sbo).toString());

                    bfw_metrics.write(success_sb.toString());
                    bfw_metrics.write(success.format(sbs).toString());


                    bfw_metrics.write(shufflePutputTable.format(sb).toString());
                    bfw_metrics.close();
                    bfw_metrics = null;
                    logger.info("File written " + RunContext.getOutDir() + "hadoop_metrics_shuffleOutputRecords." +host);

                }catch(IOException ioe)
                {
                    logger.warning("Could not create logrecords output file (hadoop_metrics_shuffleOutputRecords) in generateTableFromMetrics" + ioe.getMessage());
                }
                
            }else
            {
                logger.warning("shuffleOutputArray was empty or null in GridMix.generateTableFromMetrics()");
            }
        
        return true;
    }

    /* Override DefaultBenchmark's kill method to stop the servers.
     */
    public void kill() throws Exception {
       // We'll worry about this later
           // hadoopService.kill();       
    }
}
