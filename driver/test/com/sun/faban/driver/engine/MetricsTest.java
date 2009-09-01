/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.driver.engine;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author akara
 */
public class MetricsTest {

    /**
     * Test of addSumSquare method, of class Metrics.
     */
    @Test
    public void testAddSumSquare() {
        double s = Metrics.addSumSquare(0.0, 0, 0.0, 0.0, 0, 0.0);
        assertTrue(!Double.isNaN(s));
    }
}