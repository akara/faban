/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.harness.formsgen;

import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author sp208304
 */
public class XformsHandler {
    private ElementHandler handleElement; 
    Node eNode;
    Document doc;
    String id;

    // Constructor
    public XformsHandler(Node eNode, String id) {
       this.eNode = eNode;
       this.id = id;
       /*
       if("driverConfig".equals(eNode.getLocalName())){
           driverConfig = new DriverConfigElement();
           this.handleElement = driverConfig;
       }else if("runConfig".equals(eNode.getLocalName())){
           runConfig = new RunConfigElement();
           this.handleElement = runConfig;
       }else if("hostConfig".equals(eNode.getLocalName())){
           hostConfig = new HostConfigElement();
           this.handleElement = hostConfig;
       }else if("runControl".equals(eNode.getLocalName())){
           runControl = new RunControlElement();
           this.handleElement = runControl;
       }else if("service".equals(eNode.getLocalName())){
           service = new ServiceElement();
           this.handleElement = service;
       }else {
           generic = new GenericElement();
           this.handleElement = generic;
       }
       */

       
       String handlerName = getHandlerName(eNode.getLocalName());
       try {
           //this.handleElement = Class.forName(handlerName).asSubclass(ElementHandler.class).newInstance();
           this.handleElement = (ElementHandler) Class.forName("com.sun.faban.harness.util."+handlerName, true, this.getClass().getClassLoader()).newInstance();
       } catch (Exception e) {
           this.handleElement = new GenericElement();
       }
    }

    public StringBuilder executeElement() {
        return handleElement.getBuffer(eNode, id);
    }

    private String getHandlerName(String s) {
         int cnt = 0;
        ArrayList str = new ArrayList();
        for (int i = 0; i < s.length(); i++) {
            for (char c = 'A'; c <= 'Z'; c++) {
                if (s.charAt(i) == c) {
                    str.add(s.substring(cnt,i));
                    cnt = i;
                }
            }
        }
        str.add(s.substring(cnt,s.length()));
        String newStr = (String) str.get(0);
        if(!str.isEmpty()){
            for(int i = 1; i < str.size(); i++){
                newStr = newStr + "" + str.get(i);
            }
            s = newStr + "Element";
        }
        return (s.length()>0)? Character.toUpperCase(s.charAt(0))+s.substring(1) :s;
    }

}
