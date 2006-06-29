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
 * $Id: FileAgent.java,v 1.1 2006/06/29 18:51:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.io.*;

/**
 * This is the interface that is implemented by the FileAgent.
 * The FileAgent is used to access any file remotely.
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.agent.FileService
 */
public interface FileAgent extends Remote {

    public static final int READ = 1;	/* Open for reading */
    public static final int WRITE = 2;	/* Open for writing */
    public static final int APPEND = 3;	/* Open for write in append mode */

    /**
     * Open a file for reading and/or writing
     * @param file filename
     * @param mode file open mode - READ, WRITE, APPEND
     */
    public FileService open(String file, int mode) throws RemoteException, FileServiceException;

    /**
     * Read a file from this machine and return the contents as a String
     * 
     * @param file name of the file
     * @return String contents of the file.
     *
     */
    public String readWholeFile(String file)
	throws RemoteException, IOException, FileNotFoundException;

    /**
     * Write the given file to this machine
     * 
     * @param file path
     * @param contents of the file.
     * @return boolean true if successful, false if not.
     *
     */
    public boolean writeWholeFile(String file, String contents)
	throws RemoteException;
    /**
     *
     * Remove a file.
     *
     * @param  fileName - The pathname for the file.
     *
     * @return boolean - true if successful, 
     *                   false if not successful or file does not exist.
     *
     */
     public boolean removeFile(String fileName) throws RemoteException;

    /**
     * Gets a property from a given file
     * @param configFile The config file name
     * @param propName The property key name
     * @return The property value
     * @throws java.io.IOException If there is an error accessing the config file
     */
    String getProperty(String configFile, String propName)
            throws IOException;
}
