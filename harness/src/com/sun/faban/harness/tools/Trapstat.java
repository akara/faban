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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Trapstat.java,v 1.1 2006/06/29 18:51:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.harness.agent.CmdAgent;

import java.util.List;


/**
 * Trapstat is a wrapper for the lockstat utility.
 * <ul>
 * <li> Implements the Tool interface by extending GenericTool
 * </ul>
 *
 * @author Ramesh Ramachandran
 * @see GenericTool
 * @see Tool
 */
public class Trapstat extends GenericTool {

    /**
     * Config method appends the toolName with 'sleep 30' 
     * as this tool can run only for a limited amount of time
     * and calls GenericTool with the arguments to configure
     */
    public void configure(String toolName, List argList, String path,
                          String outDir, String host, String masterhost,
                          CmdAgent cmdAgent) {
        
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
            
        super.configure(toolName, argList, path, outDir, host, masterhost,
                cmdAgent);
        // execute the command as root using fastsu
   }
    // All other methods are inherited from GenericTool
    //@todo trapstat and lockstat needs to be run in sequence
}
    