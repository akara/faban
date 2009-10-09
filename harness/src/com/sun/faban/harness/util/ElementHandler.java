/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.harness.util;

import org.w3c.dom.Node;

/**
 *
 * @author sp208304
 */
public interface ElementHandler {
    /**
     * Returns the xform code block for the given node.
     * @param eNode
     * @param doc
     * @return StringBuffer
     */
    public StringBuilder getBuffer(Node eNode, String id);

}
