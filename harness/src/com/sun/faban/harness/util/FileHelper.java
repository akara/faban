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
 * $Id: FileHelper.java,v 1.6 2006/10/05 23:42:20 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.util;

import com.sun.faban.harness.agent.CmdAgentImpl;
import com.sun.faban.harness.agent.FileAgent;
import com.sun.faban.harness.agent.FileService;
import com.sun.faban.harness.agent.FileServiceException;
import com.sun.faban.harness.common.Config;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileHelper {
    
    private static Logger logger = Logger.getLogger(FileHelper.class.getName());
    /**
      * This method copies a file
      * @param srcFile  - the full pathname of the source file
      * @param destFile  - the full pathname of the destination file
      * @param append - should destination file be appended with source file
      */    
    public static boolean copyFile(String srcFile, String destFile, boolean append) {
        try {
            FileChannel src = (new FileInputStream(srcFile)).getChannel();
            FileChannel dest = (new FileOutputStream(destFile)).getChannel();
            if (append)
                dest.position(dest.size());
            src.transferTo(0, src.size(), dest);
            dest.close();
            src.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not copy " + srcFile + " to " +
                                     destFile, e);
            return false;
        }
        return true;
    }

    /**
      * This method opens, traverses through the file and 
      * finds the properties and replaces the values 
      * This updates only the first occurrence of the prop
      * in the file to eliminate cases where it changes
      * props defined like PROP=$PROP:MYPROP
      * We expect only one prop per line in the file
      * @param fileName  - the full pathname of the file
      * @param prop - property names and their new values
      */    
    public static boolean editPropFile(String fileName, Properties prop, String backupFileName) {

        String tmpFile = Config.TMP_DIR + ".FileHelper";
        //copy the file
        if(backupFileName == null)
            backupFileName = tmpFile;
            
        if(!FileHelper.copyFile(fileName, backupFileName, false))
            return false; // Failed to backup the file

        // Do not modify the passed prop as it may be used by the caller
        // Call by reference pitfalls (:-)
        Properties props = new Properties(prop);

        try {
            BufferedReader in = new BufferedReader(new FileReader(backupFileName));
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
            String line;
            while((line = in.readLine()) != null) {
                line = line.trim();
                if((line.length() > 3) && (Character.isLetterOrDigit(line.charAt(0)))) {
                    Enumeration e = props.propertyNames();
                    while (e.hasMoreElements()) {
                        String propName = (String) e.nextElement();
                        if((line.indexOf(propName) != -1) &&
                           (line.charAt(line.indexOf(propName) + propName.length()) == '=')) {
                            StringBuffer sb = new StringBuffer(propName);
                            sb.append("=").append(props.getProperty(propName));
                            line = sb.toString();
                            // Will update only the first instance of the prop
                            props.remove(propName);
                            // We expect only one prop per line
                            break;
                        }
                    }
                }
                // Write the line to file
                out.write(line, 0, line.length());
                out.newLine();
            }
            in.close();
            out.close();           
            
            // Delete the temp file
            if(tmpFile == backupFileName) 
                (new File(tmpFile)).delete();
        }
        catch (Exception e) {
            logger.severe("Failed with " + e);
            logger.log(Level.FINE, "Exception", e);
            return false;
        }
        return true;
    }       
    
    /**
      * This method opens, traverses through the file and
      * finds the token and replaces it with new value
      * This method updates only the first occurrence of
      * the token in the file to eliminate cases where it
      * changes props defined like PROP=$PROP:MYPROP
      * @param fileName  - the full pathname of the file
      * @param token - to find
      * @param replacement - the replacement string
      * @param backupFileName - if needed pass a backup file name
      */
    public static boolean tokenReplace(String fileName, String token, String replacement, String backupFileName) {

        String tmpFile = Config.TMP_DIR + ".FileHelper";

        //copy the file
        if(backupFileName == null)
            backupFileName = tmpFile;

        if(!FileHelper.copyFile(fileName, backupFileName, false))
            return false; // Failed to backup the file

        System.out.println("Token : " + token);
        System.out.println("Replacement : " + replacement);

        try {
            BufferedReader in = new BufferedReader(new FileReader(backupFileName));
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
            String line;
            boolean replaced = false;
            while((line = in.readLine()) != null) {
                // replace only the first occurrence of the token
                if((!replaced) && (line.indexOf(token) != -1)) {
                    System.err.println("FileHelper.tokenReplace : replacing " + token + " with " + replacement);
                    StringBuffer sb = new StringBuffer();
                    // to escape special chars in token
                    for(int i = 0; i < token.length(); i++) {
                        if(Character.isLetterOrDigit(token.charAt(i)))
                            sb.append(token.charAt(i));
                        else
                            sb.append("\\").append(token.charAt(i));
                    }

                    // to escape special chars in replacement string
                    token = sb.toString();
                    sb = new StringBuffer();
                    for(int i = 0; i < replacement.length(); i++) {
                        if(Character.isLetterOrDigit(replacement.charAt(i)))
                            sb.append(replacement.charAt(i));
                        else
                            sb.append("\\").append(replacement.charAt(i));
                    }
                    replacement = sb.toString();

                    line = line.replaceAll(token, replacement);
                    replaced = true;
                }
                // Write the line to file
                out.write(line, 0, line.length());
                out.newLine();
            }
            in.close();
            out.close();

            // Delete the temp file
            if(tmpFile == backupFileName)
                (new File(tmpFile)).delete();
        }
        catch (Exception e) {
            logger.severe("Failed with " + e);
            logger.log(Level.FINE, "Exception", e);
            return false;
        }
        return true;
    }

    /**
      * This method opens, traverses through the file and
      * finds the token, it will avoid comments when searching
      * @param fileName  - the full pathname of the file
      * @param token - token to serch for
      */
    public static boolean isInFile(String fileName, String token) {
        boolean found = false;

        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String line;
            while((line = in.readLine()) != null) {
                // Avoid comments
                if(line.length() > token.length() &&
                   Character.isLetterOrDigit(line.charAt(0)) && line.indexOf(token) != -1) {
                    found = true;
                    break;
                }
            }
            in.close();
        }
        catch (Exception e) {
            logger.severe("Failed with " + e);
            logger.log(Level.FINE, "Exception", e);
            return false;
        }
        return found;
    }

    /**
     * This  method is used to delete a directory and 
     * recursively delete files and subdirectories within it.
     *
     * @param file The file or directory to delete
     */
    public static boolean recursiveDelete(File file) {
        // Does a post-order traversal of the directory hierarchy and deletes
        // all the nodes in its path.
	
        boolean success = true;
 
	    try {
            if (file.isDirectory()) {
                File[] list = file.listFiles();
                for (int i = 0; i < list.length; i++)
                    recursiveDelete(list[i]);
            } 
            if (!file.delete()) {
                logger.severe("Delete failed for file " + file.getPath());
                success = false;
            }
        } catch(Exception e) {
            logger.severe("Failed with " + e);
            logger.log(Level.FINE, "Exception", e);
            success = false;
        }
        return success;
    }

    /**
     * This method is used to delete a directory and
     * recursively delete files and subdirectories within it.
     *
     * @param parentDir : the file object corresponding to the parent directory
     * @param name : name of the directory to be deleted
     *
     */
    public static boolean recursiveDelete(File parentDir, String name) {
        return recursiveDelete(new File(parentDir, name));
    }

    /**
     * Copies a file from source to dest. If src is a directory, the whole
     * directory tree is copied. The dest file must not exist.
     * @param src The source file
     * @param dest The dest file, must not exist before calling method
     * @return true if copy succeeded, false afterwise
     */
    public static boolean recursiveCopy(File src, File dest) {
        boolean success = true;
        if (dest.exists())
            success = false;
        else if (src.isDirectory()) {
            dest.mkdir();
            File[] files = src.listFiles();
            for (File s : files) {
                File d = new File(dest, s.getName());
                if (!recursiveCopy(s, d))
                    success = false;
            }
        } else {
            success = copyFile(src.getAbsolutePath(), dest.getAbsolutePath(),
                            false);
        }
        return success;
    }

    /**
     * Transfers a file from the current host to the Faban master.
     * @param inFile The input file name on the current host
     * @param outFile The output file name on the Faban master
     * @param move Whether to remove the original file or not
     * @return True if the transfer is complete, false otherwise.
     */
    public static boolean xferFile(String inFile, String outFile, boolean move) {
        File f = new File(inFile);
        if(!f.exists())
            return false ;

        // Use FileAgent on master machine to copy log
        FileAgent fa = null;
         try {
            String s = Config.FILE_AGENT;
            fa = (FileAgent) CmdAgentImpl.getRegistry().getService(s);
         } catch (Exception e) {
            logger.severe("Unable to get File Service");
             return false;
         }

        logger.fine("Input File = " + inFile + " Output File = "+ outFile);

        try {
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
            byte[] buf = new byte[8192];
            int i = in.read(buf);

            if(i > 0) {
                FileService outfp = fa.open(outFile, FileAgent.WRITE);
                while (i > 0) {
                    outfp.writeBytes(buf, 0, i);
                    i = in.read(buf);
                }
                outfp.close();
            }
            in.close();
        } catch (FileServiceException fe) {
            logger.severe("Error in creating file " + outFile);
            logger.log(Level.FINE, "Exception", fe);
        } catch (IOException e) {
            logger.severe("Error in reading file " + inFile);
            logger.log(Level.FINE, "Exception", e);
        } catch (Exception e) {
            logger.severe("Error Xfering file " + e);
            logger.log(Level.FINE, "Exception", e);
            if(move)
                f.delete();
        }
        return true;
    }

    /**
     * Obtains the whole content of a local file in a byte array.
     * @param file The file name
     * @return The byte content of the file
     * @throws IOException If the file cannot be read.
     */
    public static byte[] getContent(String file) throws IOException {
        FileChannel channel = (new FileInputStream(file)).getChannel();
        long channelSize = channel.size();
        if (channelSize >= Integer.MAX_VALUE)
            throw new IOException("Cannot handle file size >= 2GB");
        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY,
                0, channelSize);
        byte[] content = new byte[(int) channelSize];
        buffer.get(content);
        channel.close();
        return content;
    }


    // Unit test the functionality
    public static void main(String[] args) {
        if (args.length < 3) {
//            System.out.println("Usage: java FileHelper File Key Value");
            System.out.println("Usage: java FileHelper File token replacement");
            return;
        }
        Properties prop = new Properties();
        prop.setProperty(args[1], args[2]);
        
        //FileHelper.editPropFile(args[0], prop, null);
        // FileHelper.tokenReplace(args[0], args[1], args[2], null);
        //FileHelper.tokenReplace("/tmp/t", "\"$JAVA_HOME\"/bin/java", "profcmd= \n\\$profcmd \"\\$JAVA_HOME\"/bin/java", null);
        FileHelper.tokenReplace("/tmp/t", "\"$JAVA_HOME\"/bin/java", "profcmd= \n$profcmd \"$JAVA_HOME\"/bin/java", null);
    }
}
/*
// This code will replace properties which is specified anywhere in the line
// But will messup cases where there are blanks in it.
                        if((line.indexOf(propName)) != -1) {
                            StringBuffer sb = new StringBuffer();
                            StringTokenizer st = new StringTokenizer(line);
                            while (st.hasMoreTokens()) {
                                String tk = st.nextToken();
                                if(tk.indexOf(propName) != -1)
                                    sb.append(propName).append("=")
                                      .append(props.getProperty(propName)).append(" ");
                                else
                                    sb.append(tk).append(" ");
                            }
                            line = sb.toString();
                        }

*/                        
