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
 * $Id: ParamRepository.java,v 1.1 2006/06/29 18:51:42 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.util.XMLReader;

import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;

public class ParamRepository {


    private XMLReader reader;

    /**
     * Constructor: Open specified repository
     * @param file Name of repository
     */
    public ParamRepository(String file) {
        reader = new XMLReader(file);
    }

    /**
     * Generic parameter access method.
     * @param xpath
     * @return value of the parameter
     */
    public String getParameter(String xpath) {
        return reader.getValue(xpath);
    }

    /**
     * Sets or replaces the parameter referenced by the XPath.
     * @param xpath The xpath referencing the parameter
     * @param newValue The new value to set
     */
    public void setParameter(String xpath, String newValue) {
        reader.setValue(xpath, newValue);
    }

    /**
     * Saves the parameter repository back to file if it has been modified.
     *
     * @throws Exception If there is an exception saving the repository.
     */
    public void save() throws Exception {
        reader.save(null);
    }

    /**
     * Generic parameter access method.
     * @param xpath
     * @return list containing all paramters with the xpath
     */
    public List getParameters(String xpath) {
        return reader.getValues(xpath);
    }

    /**
     * Gets the attribute values for the specified attribute of a certain XPath.
     *
     * @param elementPath The XPath of the element
     * @param attributeName The name of the attribute
     * @return A list of attribute values
     */
    public List getAttributeValues(String elementPath, String attributeName) {
        return reader.getAttributeValues(elementPath, attributeName);
    }

    /**
     * This returns tokenized values of parameters in a list.
     * Mainly used to get host(s)
     * @param xpath The xpath to the parameters
     * @return List of tokenized values
     */
    public List getTokenizedParameters(String xpath) {
        ArrayList params = new ArrayList();
        List l = reader.getValues(xpath);
        for(int i = 0; i < l.size(); i++) {
            StringTokenizer st = new StringTokenizer((String)l.get(i));
            String[] values = new String[st.countTokens()];
            int j = 0;
            while(st.hasMoreTokens())
                values[j++] = st.nextToken();
            params.add(values);
        }
        return params;
    }

    /**
     *
     * @param xpath XPath expression to get SPACE seperated values from a single
     * parameter. For Example sutConfig/host The values are seperated by SPACE
     * @return An array of hostnames.
     */
    public String[] getTokenizedValue(String xpath) {

        StringTokenizer st = new StringTokenizer(reader.getValue(xpath));
        String[] hosts = new String[st.countTokens()];
        int i = 0;
        while(st.hasMoreTokens())
            hosts[i++] = st.nextToken();

        return hosts;
    }

    /**
     *
     * @param xpath XPath expression to get  ',' and SPACE seperated 
     * values from a single parameter. For Example sutConfig/instances
     * The values are seperated by ',' and then by SPACE
     * @return an array of hostnames.
     */
    public List getTokenizedList(String xpath) {
        // Each value should be passed as , and SPACE seperated strings
        ArrayList list = new ArrayList();
        StringTokenizer st = new StringTokenizer(reader.getValue(xpath));
        while (st.hasMoreTokens()) {
            ArrayList l = new ArrayList();
            StringTokenizer st2 = new  StringTokenizer(st.nextToken(), ",");
            while (st2.hasMoreTokens())
                l.add(st2.nextToken());
            list.add(l.toArray(new String[1]));
        }
        return list;
    }

    /**
     * This method reads a value using the XPath and converts it to a boolean
     * @param xpath XPath expression to the value which is true or false
     * @return true or false
     */
    public boolean getBooleanValue(String xpath) {
        return  Boolean.valueOf(reader.getValue(xpath)).booleanValue();
    }


}

