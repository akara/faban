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


import java.io.File;
import com.sun.faban.harness.FileFilter;
import java.util.logging.Logger;



/**
 *
 * @author Damien Cooke
 * This class extends FilFilter and is used for the removal/collection of hadoop logrecords logs
 */
public class MetricsFileFilter implements FileFilter{

    
    static Logger logger = Logger.getLogger(MetricsFileFilter.class.getName());
    /*
     * decide if the current file meets the criteria to be a logrecords file
     * @param pathname, path of the file/directory we are interested in
     * @return true or false representing that the file is one we are interested in
     */
    public boolean accept(final File pathname)
    {        
        if(pathname.getParent().compareTo("/tmp") == 0)
        {
            //we are inthe correct dir
            if(pathname.getName().toLowerCase().startsWith("hadoop_mapred_metrics"))
            {
                return true;
            }else if(pathname.getName().toLowerCase().startsWith("hadoop_jvm_metrics"))
            {
                return true;
            }else
            {
                logger.fine("File did not meet the criteria in MetricsFileFilter.accept()");
                return false;
            }
        }else
        {
            logger.fine("File did not meet the criteria in MetricsFileFilter.accept() as the parent was incorrect");
            return false;
        }
    }

}
