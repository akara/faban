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
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.formsgen;

import com.sun.faban.harness.util.FileHelper;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/**
 * This class is responsible for generating the xform form the run.xml file.
 *
 * @author Sheetal Patil
 */

public class XformsGenerator {

    static Document doc;    
    static StringBuilder xformsBindBuffer;
    static StringBuilder xformsLabelsBuffer;
    static StringBuilder xformsTriggersBuffer;
    static StringBuilder xformsCasesBuffer;

    /**
     * Generates the xform file.
     * @param infile The input configuration file
     * @param outfile The generated output
     * @param templateFile The template file used for generation
     */
    public static void generate(File infile, File outfile, File templateFile) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            doc = docBuilder.parse(infile);
            doc.getDocumentElement().normalize ();
            startDocument();
            FileHelper.copyFile(templateFile.getAbsolutePath(), outfile.getAbsolutePath(),false);
            endDocument(outfile);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void startDocument() {
        XformsUtil xu = new XformsUtil();
        xu.buildXforms();           
    }

    private static void endDocument(File outfile) {
        FileHelper.tokenReplace(outfile.getAbsolutePath(), "@binds@", xformsBindBuffer.toString(), null);
        FileHelper.tokenReplace(outfile.getAbsolutePath(), "@labels@", xformsLabelsBuffer.toString(), null);
        FileHelper.tokenReplace(outfile.getAbsolutePath(), "@triggers@", xformsTriggersBuffer.toString(), null);
        FileHelper.tokenReplace(outfile.getAbsolutePath(), "@cases@", xformsCasesBuffer.toString(), null);
        xformsBindBuffer = null;
        xformsLabelsBuffer = null;
        xformsTriggersBuffer = null;
        xformsCasesBuffer = null;
    }

    /**
     * Tests/runs the XForms generator.
     * @param args The command line argument
     */
    public static void main(String[] args){
        File infile = new File(args[0]);
        File outfile = new File(args[1]);
        File templateFile = new File(args[2] + "/resources/config-template.xhtml");
        generate(infile,outfile,templateFile);
    }
}
