/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.harness.util;

import java.util.ArrayList;
import org.w3c.dom.Node;

/**
 *
 * @author sp208304
 */
public class HostConfigElement implements ElementHandler{
    private StringBuilder casesBuffer = null;
    ArrayList<String> ignoreNodesStack = new ArrayList<String>();

   private void loadIgnoreStack() {
        ignoreNodesStack.add("cpus");
    }

    public StringBuilder getBuffer(Node eNode, String id) {
        loadIgnoreStack();
        XformsUtil xu = new XformsUtil();
        casesBuffer = new StringBuilder(xu.buildXformsCases(eNode, 0, id, ignoreNodesStack));
        return casesBuffer;
    }
}
