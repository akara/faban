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

package com.sun.hadoop.services;

import com.sun.faban.common.Command;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.services.ServiceContext;
import com.sun.faban.harness.Context;

import com.sun.faban.harness.Configure;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;
import com.sun.faban.harness.ParamRepository;
import java.io.File;
import java.io.IOException;

import java.util.logging.Logger;

/**
 *
 * @author Damien Cooke
 * This class manages the Hadoop service management
 * 
 */
public class HadoopService
{
    @Context public ServiceContext ctx;
    Logger logger = Logger.getLogger(HadoopService.class.getName());
    String hadoopServers[]; //this will be the master server
    private String runID;
    private ParamRepository params;
    private String hadoopBinPath;
    private String hadoopHome;


    @Configure public void configureHadoopService()
    {
        logger.info("ConfigureHadoopService starting");

        params = RunContext.getParamRepository();
        runID = RunContext.getRunId();
        hadoopHome = params.getParameter("hadoopConfig/hadoopHomeDir");
        hadoopBinPath = hadoopHome + File.separator + "bin" + File.separator;

        hadoopServers = new String[ctx.getHosts().length]; //determine how many we need (should only be 1)
        hadoopServers = ctx.getHosts();

        logger.info("ConfigureHadoopService done");
    }
    

    @Start public void startupHadoopService()
    {
        logger.info("startupHadoopService starting");

        
        try
        {
            logger.info("starting DFS");
            Command bench = new Command(hadoopBinPath + "start-dfs.sh");
            bench.setStreamHandling(Command.STDERR, Command.TRICKLE_LOG);
            //ctx.exec(bench);
            RunContext.exec(bench);

            logger.info("starting MAPRED");
            bench = new Command(hadoopBinPath + "start-mapred.sh");
            bench.setStreamHandling(Command.STDERR, Command.TRICKLE_LOG);
            //ctx.exec(bench);
            RunContext.exec(bench);
            logger.info("startupHadoopService done");

        }catch(InterruptedException ie)
        {
            logger.warning("startupHadoopService not starting: " +ie.getMessage());

        }catch(IOException ioe)
        {
            logger.warning("startupHadoopService not starting: " +ioe.getMessage());
        }

        
    }

    @Stop public void shutdownHadoopService()
    {
        try
        {
            logger.info("shutdownHadoopService starting");
            logger.info("Stopping RIG if it is running");
            Command bench = new Command(hadoopBinPath + "stop-all.sh");
            bench.setStreamHandling(Command.STDERR, Command.TRICKLE_LOG);
            //ctx.exec(bench);
            RunContext.exec(bench);
            logger.info("shutdownHadoopService done");
            
        }catch(InterruptedException ie)
        {
            logger.warning("shutdownHadoopService not stopping: " +ie.getMessage());

        }catch(IOException ioe)
        {
            logger.warning("shutdownHadoopService not stopping: " +ioe.getMessage() );
        }
        
    }

}
