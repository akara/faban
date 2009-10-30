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
import java.io.FilenameFilter;
import java.util.logging.Logger;

/**
 *
 * @author Damien Cooke
 * This class extends FilenameFilter and is used for the removal/collection of hadoop logrecords logs
 */
public class MapredMetricsFileFilter implements FilenameFilter
{
    
    static Logger logger = Logger.getLogger(MapredMetricsFileFilter.class.getName());

    /*
     * method given a directory will examine each file to determine if we are interested in that file or not
     * @param name, path of the file we are interested in
     * @param dir, path of the directory we are interested in
     * @return true or false representing that the file is one we are interested in
     */
    public boolean accept(final File dir, final String name)
    {
        if(name != null)
        {
            //we are inthe correct dir
            if(name.startsWith("hadoop_mapred_metrics"))
            {
                return true;
            }else
            {
                logger.fine("File did not meet the criteria in MapredMetricsFileFilter.accept()");
                return false;
            }
        }else
        {
            logger.fine("File did not meet the criteria in MapredMetricsFileFilter.accept() as the parent was incorrect");
            return false;
        }
    }
}
