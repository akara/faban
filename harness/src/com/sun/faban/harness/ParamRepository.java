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
 * $Id: ParamRepository.java,v 1.6 2007/09/08 01:21:14 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness;

import com.sun.faban.harness.util.XMLReader;

import java.util.List;
import java.util.StringTokenizer;
import java.util.ArrayList;

/**
 * The ParamRepository is the programmatic representation of the
 * configuration file. It allows access to the xml file via xpath.
 * In addition, the ParamRepository also allows updating the configuration
 * file. Such updates should be made during the validation stage.
 */
public class ParamRepository {


    private XMLReader reader;

    /**
     * Constructor: Open specified repository
     * @param file Name of repository
     * @param warnDeprecated Log warning when config file is deprecated
     */
    public ParamRepository(String file, boolean warnDeprecated) {
        reader = new XMLReader(file, true, warnDeprecated);
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
    public List<String> getParameters(String xpath) {
        return reader.getValues(xpath);
    }

    /**
     * Gets the attribute values for the specified attribute of a certain XPath.
     *
     * @param elementPath The XPath of the element
     * @param attributeName The name of the attribute
     * @return A list of attribute values
     */
    public List<String> getAttributeValues(String elementPath, String attributeName) {
        return reader.getAttributeValues(elementPath, attributeName);
    }

    /**
     * This returns tokenized values of parameters in a list.
     * Mainly used to get host(s)
     * @param xpath The xpath to the parameters
     * @return List of tokenized values
     */
    public List<String[]> getTokenizedParameters(String xpath) {
        ArrayList<String[]> params = new ArrayList<String[]>();
        List<String> entries = reader.getValues(xpath);
        for (String entry : entries) {
            StringTokenizer st = new StringTokenizer(entry);
            String[] values = new String[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); i++)
                values[i] = st.nextToken();
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
        for (int i = 0; st.hasMoreTokens(); i++)
            hosts[i] = st.nextToken();
        return hosts;
    }

    /**
     *
     * @param xpath XPath expression to get  ',' and SPACE seperated 
     * values from a single parameter. For Example sutConfig/instances
     * The values are seperated by ',' and then by SPACE
     * @return List of arrays of hostnames.
     */
    public List<String[]> getTokenizedList(String xpath) {
        // Each value should be passed as , and SPACE seperated strings
        ArrayList<String[]> list = new ArrayList<String[]>();
        StringTokenizer st = new StringTokenizer(reader.getValue(xpath));
        while (st.hasMoreTokens()) {
            ArrayList<String> l = new ArrayList<String>();
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

