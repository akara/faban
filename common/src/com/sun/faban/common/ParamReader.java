/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://faban.dev.java.net/public/CDDLv1.0.html or
 * install_dir/license.txt
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
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import org.w3c.dom.*;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ParamReader reads the run parameter file (usually run.xml)
 * and provides the caller with a document node. It is making sure that
 * older parameter file formats get converted to the latest format.
 * This conversion happens in the DOM tree. All access to the parameter
 * file should go through the ParamReader to ensure backwards compatibility.
 *
 * @author Akara Sucharitakul
 */
public class ParamReader {

    static Logger logger = Logger.getLogger(ParamReader.class.getName());

    /** The base Faban namespace URI. */ 
    public static final String FABANURI = "http://faban.sunsource.net/ns/faban";

    /** List of xpaths and their prefixes. */
    private static final String[][] PREFIX_TABLE = {
        { "jvmConfig", "fh" },
        { "jvmConfig/javaHome", "fh" },
        { "jvmConfig/jvmOptions", "fh" },
        { "runConfig", "fa" },
        { "runConfig/description", "fh" },
        { "//hostConfig", "fa" },
        { "//hostConfig/host", "fa" },
        { "//hostConfig/enabled", "fh" },
        { "//hostConfig/cpus", "fh" },
        { "//hostConfig/tools", "fh" },
        { "//hostConfig/userCommands", "fh" },
        { "runConfig/scale", "fa" },
        { "runConfig/runControl", "fa" },
        { "runConfig/runControl/rampUp", "fa" },
        { "runConfig/runControl/steadyState", "fa" },
        { "runConfig/runControl/rampDown", "fa" },
        { "runConfig/benchmarkDefinition", "fd" },
        { "runConfig/benchmarkDefinition/name", "fd" },
        { "runConfig/benchmarkDefinition/version", "fd" },
        { "runConfig/benchmarkDefinition/metric", "fd" },
        { "runConfig/benchmarkDefinition/scaleName", "fd" },
        { "runConfig/benchmarkDefinition/scaleUnit", "fd" },
        { "runConfig/outputDir", "fd" },
        { "runConfig/audit", "fd" },
        { "runConfig/threadStart", "fd" },
        { "runConfig/threadStart/delay", "fd" },
        { "runConfig/threadStart/simultaneous", "fd" },
        { "runConfig/threadStart/parallel", "fd" },
        { "runConfig/stats", "fd" },
        { "runConfig/stats/maxRunTime", "fd" },
        { "runConfig/stats/interval", "fd" },
        { "runConfig/runtimeStats", "fd" },
        { "runConfig/runtimeStats/interval", "fd" },
        { "runConfig/driverConfig", "fd" },
        { "runConfig/driverConfig/agents", "fd" },
        { "runConfig/driverConfig/stats", "fd" },
        { "runConfig/driverConfig/stats/interval", "fd" },
        { "runConfig/driverConfig/runtimeStats", "fd" },
        { "runConfig/driverConfig/metric", "fd"},
        { "//requestLagTime", "fd" },
        { "//requestLagTime/FixedTime", "fd" },
        { "//requestLagTime/FixedTime/cycleType", "fd" },
        { "//requestLagTime/FixedTime/cycleTime", "fd" },
        { "//requestLagTime/FixedTime/cycleDeviation", "fd" },
        { "//requestLagTime/Uniform", "fd" },
        { "//requestLagTime/Uniform/cycleType", "fd" },
        { "//requestLagTime/Uniform/cycleMin", "fd" },
        { "//requestLagTime/Uniform/cycleMax", "fd" },
        { "//requestLagTime/Uniform/cycleDeviation", "fd" },
        { "//requestLagTime/NegativeExponential", "fd" },
        { "//requestLagTime/NegativeExponential/cycleType", "fd" },
        { "//requestLagTime/NegativeExponential/cycleMean", "fd" },
        { "//requestLagTime/NegativeExponential/cycleMax", "fd" },
        { "//requestLagTime/NegativeExponential/cycleDeviation", "fd" },
        { "runConfig/driverConfig/operation", "fd"},
        { "runConfig/driverConfig/operation/name", "fd"},
        { "runConfig/driverConfig/operation/url", "fd"},
        { "runConfig/driverConfig/operation/get", "fd"},
        { "runConfig/driverConfig/operation/post", "fd"},
        { "runConfig/driverConfig/operation/max90th", "fd"},
        { "runConfig/driverConfig/operationMix", "fd" },
        { "runConfig/driverConfig/operationMix/name", "fd" },
        { "runConfig/driverConfig/operationMix/r", "fd" },
        { "runConfig/driverConfig/properties", "fd" },
        { "runConfig/driverConfig/properties/property", "fd" },
        { "runConfig/driverConfig/properties/property/name", "fd" },
        { "runConfig/driverConfig/properties/property/value", "fd" },
    };

    private boolean docChecked = false;
    private boolean docUpgraded = false;
    private boolean warnDeprecated = true;
    private Document doc;
    private Element root;
    private XPath xPath;
    private NamespaceContext nsCtx;


    private void checkAndUpgrade() throws XPathExpressionException {

        Node runConfig = null;
        String rootTag = root.getTagName();
        if ("runConfig".equals(rootTag) || (rootTag.endsWith(":runConfig") &&
                FABANURI.equals(root.getNamespaceURI())))
            runConfig = root;
        else
            runConfig = (Node) xPath.evaluate(
                                "fa:runConfig", root, XPathConstants.NODE);
        if (runConfig == null) {
            runConfig = (Node) xPath.evaluate(
                    "runConfig", root, XPathConstants.NODE);
            if (runConfig == null) {
                logger.severe("Cannot find runConfig element!");
                return;
            }
        }
        if (!FABANURI.equals(runConfig.getNamespaceURI())) {
            if (warnDeprecated)
                logger.warning("The parameter or configuration file is in a " +
                        "deprecated format.\n Please upgrade your " +
                        "configuration file. You may want to use the \n" +
                        "ParamReader to help the transition:\n" +
                        "java " + ParamReader.class.getName() +
                        " inputXML outputXML");

            for (int i = PREFIX_TABLE.length - 1; i >= 0; i--) {
                String xPathExp = PREFIX_TABLE[i][0];
                String prefix = PREFIX_TABLE[i][1];
                NodeList nodes = null;
                try {
                    nodes = (NodeList) xPath.evaluate(xPathExp, root,
                                                        XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
                if (nodes == null)
                    continue;
                int count = nodes.getLength();
                for (int j = 0; j < count; j++) {
                    Node node = nodes.item(j);
                    short nodeType = node.getNodeType();
                    if (nodeType == Node.ELEMENT_NODE ||
                            nodeType == Node.ATTRIBUTE_NODE) {
                        doc.renameNode(node, nsCtx.getNamespaceURI(prefix),
                                                            node.getNodeName());
                        node.setPrefix(prefix);
                    }
                }
            }
            docUpgraded = true;
        }
        docChecked = true;
    }

    private void saveDoc(String fileName)
            throws TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        DocumentType docType = doc.getDoctype();
        if (docType != null){
            transformer.setOutputProperty(
                    OutputKeys.DOCTYPE_PUBLIC, docType.getPublicId());
            transformer.setOutputProperty(
                    OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
        }

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(fileName));
        transformer.transform(source, result);
    }

    /**
     * Constructs a ParamReader for the given configuration file.
     * @param fileName The configuration file
     * @param warnDeprecated Log warnings if config file is deprecated format
     * @throws Exception If reading this file does not succeed
     */
    public ParamReader(String fileName, boolean warnDeprecated)
            throws Exception {
        this.warnDeprecated = warnDeprecated;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser = factory.newDocumentBuilder();
        xPath = XPathFactory.newInstance().newXPath();
        nsCtx = new FabanNamespaceContext();
        xPath.setNamespaceContext(nsCtx);
        doc = parser.parse(new File(fileName));
        root = doc.getDocumentElement();
    }

    /**
     * Obtains the Document object for this param file.
     * @return The Document element of the DOM tree.
     */
    public Document getDocument() {
        if (!docChecked)
            try {
                checkAndUpgrade();
            } catch (XPathExpressionException e) {
                logger.log(Level.SEVERE, e.getMessage() , e);
            }
        return doc;
    }

    /**
     * Obtains the preconfigured XPath object.
     * @return The XPath object.
     */
    public XPath getXPath() {
        return xPath;
    }

    private void printXPath(String xPathExp) {
        try {
            String v = xPath.evaluate(xPathExp, root);
            System.out.println(xPathExp + '=' + v);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    private void basicTest() {
        // Try printing common param file elements
        printXPath("fh:jvmConfig/fh:javaHome");
        printXPath("fh:jvmConfig/fh:jvmOptions");
        printXPath("fa:runConfig/fa:runControl/fa:steadyState");
        printXPath("fa:runConfig/@definition");
    }

    /**
     * Invokes the ParamReader in conversion mode and actually saves the
     * conversion output to a file.
     * @param args The input and output file
     * @throws Exception If anything goes wrong with the conversion
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java " + ParamReader.class.getName() +
                                                    " inputFile outputFile");
            System.exit(1);
        }
        ParamReader params = new ParamReader(args[0], false);
        params.checkAndUpgrade();

        if (params.docUpgraded) {
            params.saveDoc(args[1]);
            System.err.println("Faban made the best attempt to convert the " +
                    "document from an old format to\na new format. The " +
                    "resulting document may not be accurate for non-Faban\n" +
                    "elements and may not be clean. Please check your" +
                    " results and edit as\nappropriate/necessary.");
        } else {
            System.err.println("The parameter file " + args[0] +
                                                " appears to be up-to-date.");
        }
        // params.basicTest();
    }
}