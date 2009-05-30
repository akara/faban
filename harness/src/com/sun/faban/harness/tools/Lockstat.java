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
 * $Id: Lockstat.java,v 1.4 2009/05/30 04:48:50 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;


import com.sun.faban.harness.agent.CmdAgentImpl;

import java.util.List;
import java.util.concurrent.CountDownLatch;


/**
 * Lockstat is a wrapper for the lockstat utility.
 * <ul>
 * <li> It implements the Tool interface by extending GenericTool
 * </ul>
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.tools.GenericTool
 * @see Tool
 * @deprecated
 */
@Deprecated public class Lockstat extends GenericTool {

    /**
     * Config method appends the toolName with 'sleep 30' 
     * as this tool can run only for a limited amount of time
     * and calls GenericTool with the arguments to configure
     */
    public void configure(String toolName, List<String> argList, String path,
                          String outDir, String host, String masterhost,
                          CmdAgentImpl cmdAgent, CountDownLatch latch) {
        
        // If there is no sleep for trapstat call
        // insert a sleep for 30 sec.
        String[] a = (String[]) argList.toArray(new String[1]);
        boolean isSleeping = false;
        for(int i = 0; i < a.length; i++)
            if(a[i].indexOf("sleep") != -1)
                isSleeping = true;
       if(!isSleeping) {
           argList.add("sleep");
           argList.add("30");
       }

        //@todo we need to execute trapstat and lockstat
        super.configure(toolName, argList, path, outDir, host, masterhost,
                        cmdAgent, latch);

   }
   
    // All other methods are inherited from GenericTool    
    
}
    