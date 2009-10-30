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
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.Context;
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;

import com.sun.faban.harness.tools.ToolContext;
import java.util.logging.Logger;
import java.io.IOException;

/**
 *
 * @author Damien Cooke
 * This class manages the Hadoop tools management
 * 
 */
public class HadoopStats
{
    private static Logger logger = Logger.getLogger(HadoopStats.class.getName());

    @Context public ToolContext ctx;

    @Configure public void configureHadoopStats()
    {
        logger.warning("configureHadoopStats starting");
        logger.warning("configureHadoopStats done");
    }

    @Start public void startHadoopStats() throws IOException, InterruptedException
    {
        logger.warning("startHadoopStats starting");
        logger.warning("startHadoopStats done");
    }

    @Stop public void stopHadoopStats()
    {
        logger.warning("stopHadoopStats starting");
        logger.warning("stopHadoopStats done");
    }

}
