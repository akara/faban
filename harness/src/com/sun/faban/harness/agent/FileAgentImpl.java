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
 * $Id: FileAgentImpl.java,v 1.1 2006/06/29 18:51:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import java.io.*;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;
import java.util.Properties;


/**
 * This is the class that reads/writes files for remote objects
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.agent.FileAgent
 */
public class FileAgentImpl extends UnicastRemoteObject
        implements FileAgent {

    private Logger logger;

    /**
     *
     * Constructor
     *
     */
    public FileAgentImpl() throws RemoteException {
        super();
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
     *
     * This method creates a new FileServiceImpl object and returns a 
     * reference to its interface, FileService. The caller can then use it
     * to read or write from/to the file.
     *
     * @param file - The pathname for the file.
     * @param mode - specifies whether the file is opened for reading or
     *                   for writing. The mode is specified by the FileAgent
     *                   interface class variables - READ, WRITE, APPEND.
     *
     * @return FileService - reference to the FileService interface.
     *
     */
    public FileService open(String file, int mode) throws RemoteException, FileServiceException {
        return(new FileServiceImpl(file, mode));
    }

    /**
     *
     * Read the contents of a file. It maybe called by the benchmark specific 
     * code or the RunQ to copy files.
     *
     * @param file - The pathname for the file.
     * 
     * @return String - Contents of the file as a String object.
     *
     */
    public String readWholeFile(String file)
            throws RemoteException, IOException, FileNotFoundException {

        BufferedReader br;
        char[] buf = new char[65535];
        StringBuffer strBuf = new StringBuffer();
        int cnt;

        logger.fine("FileAgentImpl:readWholefile() reading " + file);
        try {
            br = new BufferedReader(new FileReader(file));

            while (true) {
                cnt = br.read(buf, 0, 65535);
                if (cnt == -1)
                    break;
                if (cnt > 0) {
                    strBuf.append(buf, 0, cnt);
                }
            }
        }
        catch (FileNotFoundException e1)
        {
            FileNotFoundException e2 =
                    new FileNotFoundException("FileAgentImpl:readWholeFile " +
                    e1.getMessage());
            throw e2;
        }
        catch (IOException e1)
        {
            IOException e2 = new IOException("FileAgentImpl:readWholeFile " +
                    e1.getMessage());
            throw e2;
        }
        String s = strBuf.toString();
        return s;
    }


    /**
     * Gets a property from a given file
     * @param configFile The config file name
     * @param propName The property key name
     * @return The property value
     * @throws IOException If there is an error accessing the config file
     */
    public String getProperty(String configFile, String propName) 
            throws IOException {
        Properties p = new Properties();
        FileInputStream cfgStream = new FileInputStream(configFile);
        p.load(cfgStream);
        cfgStream.close();
        return p.getProperty(propName);
    }


    /**
     *
     * Write contents to a file. It maybe called by the benchmark specific 
     * code or the RunQ to copy files.
     *
     * @param fileName - The pathname for the file.
     * @param contents - Contents of the file as a String object.
     *
     * @return boolean - true if successful, false if not.
     *
     */
    public boolean writeWholeFile(String fileName, String contents) throws RemoteException {

        PrintWriter pr = null;
        try {
            pr = new PrintWriter(new FileWriter("/tmp/fa.log"));
        }
        catch (IOException ie) {
            return false;
        }
        try {
            pr.println("In writeWholeFile");
            pr.flush();
            pr.println("filename = " + fileName);
            pr.flush();
            pr.println("\ncontents = \n" + contents);
            pr.flush();
            logger.fine("FileAgentImpl:writeWholeFile() writing " + fileName);
            PrintWriter out = null;

            try {
                out = new PrintWriter(new FileWriter(fileName));
                pr.println("dasdasd\n");
                pr.flush();
                if (out == null) {
                    return false;
                }
                out.println(contents);
                out.flush();
            }  catch (IOException ioe) {}

            return (!out.checkError());
        }
        catch (NullPointerException npe) {
            return false;
        }
    }

    /**
     *
     * Remove a file.
     *
     * @param fileName - The pathname for the file.
     *
     * @return boolean - true if successful, 
     *                   false if not successful or file does not exist.
     *
     */
    public boolean removeFile(String fileName) throws RemoteException {

        File file = new File(fileName);

        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    /**
     * Registration for RMI serving - used only for stand-alone testing
     */

    public static void main(String [] argv) {

        //		LocateRegistry.createRegistry();
        System.setSecurityManager (new RMISecurityManager());

        try {
            FileAgentImpl log = new FileAgentImpl();
            System.out.println("FileAgentImpl object created");
            String host = (InetAddress.getLocalHost()).getHostName();
            String s = "//" + host  + "/FileAgent";
            System.out.println("Calling rebind on " + s);
            Naming.rebind(s , log);
            System.out.println("FileAgent started on machine " + host);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
