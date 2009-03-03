/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.faban.harness.webclient;

import java.util.Set;
import junit.framework.TestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 *
 * @author Sheetal Patil
 */
public class TagEngineTest extends TestCase {

    TagEngine tagEngine;
    public TagEngineTest() {
    }

    @BeforeClass
    public void setUpClass() throws Exception {
        super.setUp();
        System.out.println("* TagEngineTest: setUp() method");
    }

    public void testEqual() {
        System.out.println("equal");
        int a = 5;
        int b = 5;
        assertEquals(a, b);
        //String tag = "Hadoop";
        //boolean result = tag.equalsIgnoreCase("hadoop");
        System.out.println("* TagEngineTest: testEqual() method");
    }

    public void testAdd() throws Exception {
        String[] tagsadd1 = {"Hadoop/fs/gridmix", "Hadoop"};
        String[] tagsadd2 = {"Solaris/9", "Oracle/10", "gridmix"};
        String[] tagsadd3 = {"Solaris/10"};
        String[] tagsadd4 = {"web20/workload/sample", "web20"};
        String[] tagsadd5 = {"web20", "workload"};
        String[] tagsadd6 = {"Solaris/9", "Oracle/10"};
        try {
            tagEngine = TagEngine.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Set<String> answer;
        tagEngine.add("HadoopGridMix.5G",tagsadd1);
        tagEngine.add("HadoopGridMix.5F",tagsadd2);
        tagEngine.add("HadoopGridMix.5E",tagsadd3);
        tagEngine.add("web101.5D",tagsadd4);
        tagEngine.add("web101.5C",tagsadd5);
        System.out.println("------------------------------------------------");
        System.out.println("Testing Intersection multiple tags");
        System.out.println("------------------------------------------------");
        answer = tagEngine.search("Solaris Solaris/10 Oracle/10");
        for(String ans : answer){
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
        System.out.println("Searching for solaris, single tag");
        System.out.println("------------------------------------------------");
        answer = tagEngine.search("solaris");
        for(String ans : answer){
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
        System.out.println("Searching for solaris with te.removeRunId(ans)");
        System.out.println("------------------------------------------------");
        answer = tagEngine.search("solaris");
        int i= 0;
        for(String ans : answer){
            if(i==1){
                tagEngine.removeRun(ans);
                break;
            }
            i++;
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
        System.out.println("Searching for solaris after te.removeRunId(ans)");
        System.out.println("------------------------------------------------");
        answer = tagEngine.search("solaris");
        for(String ans : answer){
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
        System.out.println("Searching for gridmix, before removeEntry");
        System.out.println("------------------------------------------------");
        answer = tagEngine.search("gridmix");
        for(String ans : answer){
            System.out.println(ans);
        }
        tagEngine.add("HadoopGridMix.5F",tagsadd6);
        System.out.println("------------------------------------------------");
        System.out.println("Searching for gridmix, after removeEntry");
        System.out.println("------------------------------------------------");
        answer = tagEngine.search("gridmix");
        for(String ans : answer){
            System.out.println(ans);
        }
        System.out.println("------------------------------------------------");
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        super.tearDown();
        System.out.println("* TagEngineTest: tearDown() method");
    }

}