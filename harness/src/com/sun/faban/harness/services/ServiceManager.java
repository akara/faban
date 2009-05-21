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
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.services;

import com.sun.faban.common.ParamReader;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.engine.DeployImageClassLoader;
import com.sun.faban.harness.tools.MasterToolContext;
import com.sun.faban.harness.tools.ToolDescription;
import com.sun.faban.harness.util.XMLReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Sheetal Patil
 */
public class ServiceManager {
    private static Logger logger =
            Logger.getLogger(ServiceContext.class.getName());
    private List<ServiceWrapper> loadedServicesList = 
            new ArrayList<ServiceWrapper>();
    private List<ServiceContext> ctxList = new ArrayList<ServiceContext>();

    private LinkedHashMap<String, ServiceDescription> serviceMap =
            new LinkedHashMap<String, ServiceDescription>();

    private LinkedHashMap<String, ToolDescription> toolMap =
            new LinkedHashMap<String, ToolDescription>();

    private LinkedHashMap<String, List<String>> toolSetsMap =
            new LinkedHashMap<String, List<String>>();

    ArrayList<MasterToolContext> toolList = new ArrayList<MasterToolContext>();

    private Run run;

    public ServiceManager(ParamRepository par, Run run)
            throws Exception{
        String benchmark = run.getBenchmarkName();
        this.run = run;
        // Get the service descriptions and tool descriptions
        parseAvailableServices(benchmark);

        // Obtain the active service list.
        parseRequestedServices(par);

        for (ServiceContext ctx : ctxList) {            
            DeployImageClassLoader loader = DeployImageClassLoader.getInstance(
                                ctx.desc.locationType, ctx.desc.location,
                                getClass().getClassLoader());
            ServiceWrapper wrapper = new ServiceWrapper(
                                loader.loadClass(ctx.desc.serviceClass), ctx);
            loadedServicesList.add(wrapper);

        }
        this.loadedServicesList = Collections.unmodifiableList(
                                                            loadedServicesList);
        this.ctxList = Collections.unmodifiableList(ctxList);
    }
  
    private void parseAvailableServices(String benchmark) {
        parseServicesAndTools("benchmarks", benchmark);
        parseToolSets("benchmarks", benchmark);
        File serviceDir = new File(Config.SERVICE_DIR);
        File[] serviceBundles;
        if (serviceDir.isDirectory()) {
            serviceBundles = serviceDir.listFiles();
            for (File serviceBundle : serviceBundles) {
                if (!serviceBundle.isDirectory())
                    continue;
                parseServicesAndTools("services", serviceBundle.getName());
                parseToolSets("services", serviceBundle.getName());
            }
        }      
    }

    public void parseServicesAndTools(String type, String dir) {
        HashMap<String, ServiceDescription> localServices =
                new HashMap<String, ServiceDescription>();

        String metaInf = Config.FABAN_HOME + File.separator + type +
                File.separator + dir + File.separator + "META-INF";
        File metaInfDir = new File(metaInf);
        if (!metaInfDir.isDirectory()) {
            return;
        }
        File serviceXml = new File(metaInf + File.separator +
                    "services-tools.xml");
        try {
            if (serviceXml.exists()) {
                XMLReader reader = new XMLReader(metaInf + File.separator +
                        "services-tools.xml");
                Element root = null;
                if (reader != null) {
                    root = reader.getRootNode();

                    // First, parse the services.
                    NodeList serviceNodes = reader.getNodes("service", root);
                    for (int i = 0; i < serviceNodes.getLength(); i++) {

                        Node serviceNode = serviceNodes.item(i);
                        if (serviceNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Element se = (Element) serviceNode;
                        String id = null;
                        NamedNodeMap attrList = serviceNode.getAttributes();
                        for (int j = 0; j < attrList.getLength(); j++) {
                            if (attrList.item(j).getNodeType() ==
                                    Node.ATTRIBUTE_NODE) {
                                if (attrList.item(j).getNodeName().equals("id")) {
                                    id = attrList.item(j).getNodeValue();
                                }
                            }
                        }
                        NodeList classNodes = serviceNode.getChildNodes();
                        String loadableClass = null;
                        for (int k = 0; k < classNodes.getLength(); k++) {
                            if (classNodes.item(k).getNodeType() ==
                                    Node.ELEMENT_NODE) {
                                loadableClass = reader.getValue("class", se);

                            }
                        }
                        if (id != null && loadableClass != null) {
                            if (serviceMap.containsKey(id)) {
                                logger.log(Level.WARNING,
                                        "Ignoring duplicate service " + id +
                                        " in " + type + File.separator + dir);
                            } else {
                                ServiceDescription desc = new ServiceDescription(id,
                                                                loadableClass, type, dir);
                                localServices.put(id, desc);
                                serviceMap.put(id, desc);
                            }
                        }
                    }

                    // Then parse the tools.
                    NodeList toolNodes = reader.getNodes("tool", root);
                    for (int i = 0; i < toolNodes.getLength(); i++) {
                        String id = null;
                        String serviceName = null;
                        String toolClass = null;
                        Node toolNode = toolNodes.item(i);
                        if (toolNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Element te = (Element) toolNode;
                        NamedNodeMap attrList = toolNode.getAttributes();
                        for (int j = 0; j < attrList.getLength(); j++) {
                            if (attrList.item(j).getNodeType() ==
                                    Node.ATTRIBUTE_NODE) {
                                if (attrList.item(j).getNodeName().equals("id")) {
                                    id = attrList.item(j).getNodeValue();
                                }
                                if (attrList.item(j).getNodeName().
                                        equals("service")) {
                                    serviceName = attrList.item(j).getNodeValue();
                                }
                            }
                        }
                        NodeList classNodes = toolNode.getChildNodes();
                        for (int k = 0; k < classNodes.getLength(); k++) {
                            if (classNodes.item(k).getNodeType() ==
                                    Node.ELEMENT_NODE) {
                                toolClass = reader.getValue("class", te);
                            }
                        }

                        ServiceDescription service = null;
                        if (serviceName != null) {
                            service = localServices.get(serviceName);
                            if (service == null) {
                                logger.warning("Tool " + id + " at " + dir +
                                        " references non-existent service " +
                                        serviceName);
                                continue;
                            }
                        }

                        if (id != null) {
                            String key = id;
                            if (service != null) {
                                key += '/' + service.id;
                                if (toolMap.containsKey(key)) {
                                    logger.log(Level.WARNING,
                                            "Ignoring duplicate tool" + id);
                                } else {
                                    toolMap.put(key, new ToolDescription(
                                            id, service, toolClass));
                                }
                            }
                        }
                    }
                }
            }
        } catch  (Exception e) {
            logger.log(Level.WARNING, "Error reading benchmark " +
                    "descriptor for " + dir, e);
        }
    }


    private void parseRequestedServices(ParamRepository par)
            throws XPathExpressionException, IOException {
        NodeList topLevelElements = par.getTopLevelElements();
        int topLevelSize = topLevelElements.getLength();
        for (int i = 0; i < topLevelSize; i++) {
            Node node = topLevelElements.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element ti = (Element) node;
            String ns = ti.getNamespaceURI();
            String topElement = ti.getNodeName();
            if (ParamReader.FABANURI.equals(ns) && "fa:runConfig".
                    equals(topElement))
                continue;
            if("http://faban.sunsource.net/ns/fabanharness".equals(ns) &&
                    "jvmConfig".equals(topElement))
                continue;

            // Get the hosts
            String hosts = par.getParameter("fa:hostConfig/fa:host", ti);
            ArrayList<String> hostList = new ArrayList<String>();
            StringTokenizer t = new StringTokenizer(hosts);
            while (t.hasMoreTokens())
                hostList.add(t.nextToken());

            // Get the services
            NodeList serviceNodes = par.getNodes("fh:service", ti);
            int serviceCount = serviceNodes.getLength();
            for (int j = 0; j < serviceCount; j++) {
                Element serviceElement = (Element) serviceNodes.item(j);
                String serviceName = par.getParameter("fh:name",
                                                        serviceElement);
                
                File runIdFile = new File(Config.SERVICE_DIR +
                                serviceName + File.separator +
                                "META-INF" + File.separator + "RunID");
                FileWriter wFile = new FileWriter(runIdFile);
                wFile.write(run.getRunId());
                wFile.close();
                Node configNode = par.getNode("fh:config", serviceElement);
                Properties properties = new Properties();
                NodeList propsList = configNode.getChildNodes();
                for (int k = 0; k < propsList.getLength(); k++) {
                    if (propsList.item(k).getNodeType() == Node.ELEMENT_NODE) {
                        Element propElement = (Element) propsList.item(k);
                        String key = propsList.item(k).getNodeName();
                        NodeList props = propElement.getChildNodes();
                        String value = ((Node) props.item(0)).getNodeValue().
                                        trim();
                        properties.setProperty(key, value);
                    }
                }
                ServiceDescription sd = serviceMap.get(serviceName);
                ServiceContext ctx =
                        new ServiceContext(sd, hostList, properties);
                ctxList.add(ctx);


                String toolCmds = par.getParameter("fh:tools", serviceElement);
                ArrayList<String> tools = new ArrayList<String>();
                if (toolCmds.toUpperCase().equals("NONE")) {
                }else if(toolCmds.length() != 0){
                    StringTokenizer st = new StringTokenizer(toolCmds, ";");
                    while (st.hasMoreTokens()) {
                        tools.add(st.nextToken().trim());
                    }
                }else if ("".equals(toolCmds) && toolCmds.length() == 0){
                    String key = "default" + '/' + serviceName;
                    ArrayList<String> toolset_tools = new ArrayList<String>();
                    if(toolSetsMap.containsKey(key)){
                            toolset_tools.addAll(toolSetsMap.get(key));
                            for (String t1 : toolset_tools){
                                StringTokenizer tt1 = new StringTokenizer(t1);
                                String toolId1 = tt1.nextToken();
                                String toolKey1 = toolId1 + '/' + serviceName;
                                ToolDescription toolDesc = toolMap.get(toolKey1);
                                MasterToolContext toolCtx = null;
                                if (toolDesc != null) {
                                    toolCtx = new MasterToolContext(t1, ctx, toolDesc);
                                } else {
                                    toolDesc = toolMap.get(toolId1);
                                    if (toolDesc != null) {
                                        toolCtx = new MasterToolContext(
                                                t1, ctx, toolDesc);
                                    } else {
                                        //logger.info("No Tool Description for tool: " + t1);
                                        logger.fine("No Tool Description for tool: "
                                                + t1 +" ,so it's a command line tool");
                                        toolCtx = new MasterToolContext(
                                                t1, ctx, null);
                                    }
                                }
                                if (toolCtx != null) {
                                    toolList.add(toolCtx);
                                }
                            }
                        }

                }
                if(tools != null){
                    for (String tool : tools) {
                        StringTokenizer tt = new StringTokenizer(tool);
                        String toolId = tt.nextToken();
                        String toolKey = toolId + '/' + serviceName;
                        ArrayList<String> toolset_tools = new ArrayList<String>();
                        if(toolSetsMap.containsKey(toolKey)){
                            toolset_tools.addAll(toolSetsMap.get(toolKey));
                            for (String t1 : toolset_tools){
                                StringTokenizer tt1 = new StringTokenizer(t1);
                                String toolId1 = tt1.nextToken();
                                String toolKey1 = toolId1 + '/' + serviceName;
                                ToolDescription toolDesc = toolMap.get(toolKey1);
                                MasterToolContext toolCtx = null;
                                if (toolDesc != null) {
                                    toolCtx = new MasterToolContext(t1, ctx, toolDesc);
                                } else {
                                    toolDesc = toolMap.get(toolId1);
                                    if (toolDesc != null) {
                                        toolCtx = new MasterToolContext(
                                                t1, ctx, toolDesc);
                                    } else {                                       
                                        //logger.info("No Tool Description for tool: " + t1);
                                        logger.fine("No Tool Description for tool: "
                                                + t1 +" ,so it's a command line tool");
                                        toolCtx = new MasterToolContext(
                                                t1, ctx, null);
                                    }
                                }
                                if (toolCtx != null) {
                                    toolList.add(toolCtx);
                                }
                            }
                        }else{
                            ToolDescription toolDesc = toolMap.get(toolKey);
                            MasterToolContext toolCtx = null;
                            if (toolDesc != null) {
                                toolCtx = new MasterToolContext(tool, ctx, toolDesc);
                            } else {
                                toolDesc = toolMap.get(toolId);
                                if (toolDesc != null) {
                                    toolCtx = new MasterToolContext(
                                            tool, ctx, toolDesc);
                                } else {
                                    //logger.info("No Tool Description for tool: " + tool);
                                    logger.fine("No Tool Description for tool: "
                                                + tool +" ,so it's a command line tool");
                                    toolCtx = new MasterToolContext(
                                                tool, ctx, null);
                                }
                            }
                            if (toolCtx != null) {
                                toolList.add(toolCtx);
                            }
                        }
                    }
                }
            }
        }
    }

    public void parseToolSets(String type, String dir) {
        String metaInf = Config.FABAN_HOME + File.separator + type +
                File.separator + dir + File.separator + "META-INF";
        File metaInfDir = new File(metaInf);
        if (!metaInfDir.isDirectory()) {
            return;
        }
        File toolsetsXml = new File(metaInf + File.separator +
                    "toolsets.xml");
        try {
            if(toolsetsXml.exists()){
                XMLReader reader = new XMLReader(metaInf + File.separator +
                        "toolsets.xml");
                Element root = null;
                if (reader != null) {
                    root = reader.getRootNode();

                    // First, parse the services.
                    NodeList toolsetsNodes = reader.getNodes("toolset", root);
                    for (int i = 0; i < toolsetsNodes.getLength(); i++) {

                        Node toolsetsNode = toolsetsNodes.item(i);
                        if (toolsetsNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Element tse = (Element) toolsetsNode;
                        ArrayList<String> toolsCmds = new ArrayList<String>();
                        String service = reader.getValue("service", tse);
                        String name = reader.getValue("name", tse);
                        String base = reader.getValue("base", tse);
                        List<String> toolIncludes = reader.getValues("includes", tse);
                        List<String> toolExcludes = reader.getValues("excludes", tse);
                        if(!"".equals(base)){
                            toolsCmds.addAll(toolSetsMap.get(base+"/"+service));
                        }
                        if(toolIncludes != null){
                            for (String tool : toolIncludes){
                                StringTokenizer st = new StringTokenizer(tool, ";");
                                while(st.hasMoreTokens())
                                    toolsCmds.add(st.nextToken().trim());
                            }
                        }
                        if(toolExcludes != null){
                            ArrayList<String> td = new ArrayList<String>();
                            for (String tool : toolExcludes){
                                StringTokenizer st = new StringTokenizer(tool, ";");
                                while(st.hasMoreTokens())
                                    td.add(st.nextToken().trim());
                            }
                            toolsCmds.removeAll(td);
                        }

                        if (!"".equals(service) && !"".equals(name) &&
                                (toolIncludes != null || base != null)) {
                            String key = name+"/"+service;
                            if (toolSetsMap.containsKey(key)) {
                                logger.log(Level.WARNING,
                                        "Ignoring duplicate toolset");
                            } else {
                                toolSetsMap.put(key, toolsCmds);
                            }
                        }
                    }
                }
            }
        } catch  (Exception e) {
            logger.log(Level.WARNING, "Error reading toolsets.xml " +
                    "for " + dir, e);
        }

        
    }

    public List<MasterToolContext> getTools() {
        return toolList;
    }

    public void clearLogs() throws Exception{
        for(ServiceWrapper sw : loadedServicesList){
            sw.clearLogs();
        }
    }

    public void configure() throws Exception {
        for(ServiceWrapper sw : loadedServicesList){
            sw.configure();
        }
    }

    public void getConfig() throws Exception {
        for(ServiceWrapper sw : loadedServicesList){
            sw.getConfig();
        }
    }

    public void getLogs() throws Exception {
        for(ServiceWrapper sw : loadedServicesList){
            sw.getLogs();
        }
    }

    public void startup() throws Exception {
        for(ServiceWrapper sw : loadedServicesList){
            sw.startup();
        }
    }

    public void shutdown() throws Exception {
        for (ServiceContext ctx : ctxList) {
            
            File runIdFile = new File(Config.SERVICE_DIR +
                                ctx.desc.id + File.separator +
                                "META-INF" + File.separator + "RunID");
            if (runIdFile.exists())
                runIdFile.delete();
        }
        for(ServiceWrapper sw : loadedServicesList){
            sw.shutdown();
        }
    }
}
