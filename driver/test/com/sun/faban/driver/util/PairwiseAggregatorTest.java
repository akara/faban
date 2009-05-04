/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.driver.util;

import java.util.logging.Logger;
import junit.framework.TestCase;
import static org.junit.Assert.*;

/**
 * Test class for the PairwiseAggregator.
 * @author akara
 */
public class PairwiseAggregatorTest extends TestCase {

    static Logger logger =
            Logger.getLogger(PairwiseAggregatorTest.class.getName());

    public void testCollectStats() {
        for (int count = 1; count <= 50000; count++) {
            if (count % 1000 == 0)
                logger.info("Now testing " + count + " items.");
            ElementProvider provider = new ElementProvider();
            PairwiseAggregator<Element> aggregator =
                    new PairwiseAggregator<Element>(count, provider);
            Element el = aggregator.collectStats();
            assertEquals(count, el.value);
        }
    }

    private class Element implements PairwiseAggregator.Aggregable<Element> {

        public int value;

        public Element(int value) {
            this.value = value;
        }

        public void add(Element other) {
            this.value += other.value;
        }
    }

    private class ElementProvider
            implements PairwiseAggregator.Provider<Element> {

        public Element getMutableMetrics(int idx) {
            return new Element(1);
        }

        public void add(Element instance, int idx) {
            ++instance.value;
        }

        public Class getComponentClass() {
            return Element.class;
        }

        public void recycle(Element e) {
        }
    }
}