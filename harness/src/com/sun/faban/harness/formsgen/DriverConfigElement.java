/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.harness.formsgen;

import java.util.ArrayList;
import org.w3c.dom.Node;

/**
 *
 * @author sp208304
 */
public class DriverConfigElement implements ElementHandler{
   StringBuilder casesBuffer = null;
   ArrayList<String> ignoreNodesStack = new ArrayList<String>();

   private void loadIgnoreStack() {
        ignoreNodesStack.add("runtimeStats");
        ignoreNodesStack.add("name");
        ignoreNodesStack.add("value");
        ignoreNodesStack.add("operationMix");
        ignoreNodesStack.add("r");
    }

    public StringBuilder getBuffer(Node eNode, String id) {
        loadIgnoreStack();
        XformsUtil xu = new XformsUtil();
        xu.buildPropertyLabelsStack(eNode);
        casesBuffer = new StringBuilder(xu.buildXformsCases(eNode, 0, id, ignoreNodesStack));
        return casesBuffer;
    }
}
