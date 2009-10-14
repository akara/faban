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

import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Handles the generation of the input form elements.
 * Delegates to specific element handlers.
 * @author Sheetal Patil1
 */
public class XformsHandler {
    private ElementHandler handleElement; 
    Node eNode;
    Document doc;
    String id;

    /**
     * Constructs and prepares the handler.
     * @param eNode The node to be handled
     * @param id The element identifier
     */
    public XformsHandler(Node eNode, String id) {
       this.eNode = eNode;
       this.id = id;

       
       String handlerName = getHandlerName(eNode.getLocalName());
       try {
           this.handleElement = Class.forName(
                   "com.sun.faban.harness.formsgen." + handlerName).
                   asSubclass(ElementHandler.class).newInstance();
       } catch (Exception e) {
           this.handleElement = new GenericElement();
       }
    }

    /**
     * Executes the specific element handler.
     * @return The buffer containing the xforms output for the element.
     */
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
        return s.length() > 0 ?
                Character.toUpperCase(s.charAt(0)) + s.substring(1) :s;
    }

}
