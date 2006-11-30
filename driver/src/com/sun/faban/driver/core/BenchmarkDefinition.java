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
 * $Id: BenchmarkDefinition.java,v 1.5 2006/11/30 23:58:38 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

import com.sun.faban.driver.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

/**
 * Implements the basic benchmark, driver, and operation definitions.
 *
 * @author Akara Sucharitakul
 */
public class BenchmarkDefinition implements Serializable, Cloneable {

    String name;
    String version;
    Driver[] drivers;
    RunControl runControl;
    String metric;
    String scaleName;
    String scaleUnit;
    boolean configPrecedence;

    static BenchmarkDefinition read(String defClassName)
            throws DefinitionException, ConfigurationException {
        BenchmarkDefinition def = new BenchmarkDefinition();
        String className = def.getClass().getName();
        Logger logger = Logger.getLogger(className);

        Class<?> defClass = null;
        try {
            defClass = Class.forName(defClassName);
        } catch (ClassNotFoundException e) {
            
            //Did not find the class in the default classloader, 
            //look first in the faban.tmpdir then in java.io.tmpdir
            //for the generated class
            String tempDir = System.getProperty("faban.tmpdir");
            
            if(tempDir==null){
                tempDir = System.getProperty("java.io.tmpdir");
            }
            
            File classFile = new File(tempDir);
            
            URL url[]= new URL[1];
            
            try {
                url[0] = classFile.toURI().toURL();
            } catch (MalformedURLException ex) {
                logger.log(Level.SEVERE, "Bad file URL for generated java class!");
                throw new ConfigurationException(ex);
            }

            URLClassLoader loader = new URLClassLoader(url, BenchmarkDefinition.class.getClassLoader());
            
            try{
                
                defClass=loader.loadClass(defClassName);
                
            }catch(ClassNotFoundException cnfex){
                ConfigurationException ce = new ConfigurationException(e);
                logger.log(Level.SEVERE, e.getMessage(), ce);
                throw ce;

            }
            
        }

        if (!defClass.isAnnotationPresent(
                com.sun.faban.driver.BenchmarkDefinition.class)) {
            String msg = "Class " + defClassName +
                    " is not a benchmark definition.";
            logger.severe(msg);
            throw new ConfigurationException(msg);
        }

        com.sun.faban.driver.BenchmarkDefinition benchDefAnnotation = defClass.
                getAnnotation(com.sun.faban.driver.BenchmarkDefinition.class);

        def.name = benchDefAnnotation.name();
        def.version = benchDefAnnotation.version();
        def.runControl = benchDefAnnotation.runControl();
        def.metric = benchDefAnnotation.metric();
        def.scaleName = benchDefAnnotation.scaleName();
        def.scaleUnit = benchDefAnnotation.scaleUnit();
        def.configPrecedence = benchDefAnnotation.configPrecedence();

        ArrayList<Class<?>> driverClassList = new ArrayList<Class<?>>();

        // Get all the driver classes
        for (Class<?> driverClass : benchDefAnnotation.drivers())
            if (driverClass != Object.class && driverClass.isAnnotationPresent(
                    BenchmarkDriver.class))
                driverClassList.add(driverClass);

        // If defClass is not in list and is a driver, prepend
        if (driverClassList.indexOf(defClass) < 0 &&
                defClass.isAnnotationPresent(BenchmarkDriver.class))
            driverClassList.add(0, defClass);

        // Check that we have at least one driver
        if (driverClassList.size() <= 0) {
            String msg = "No driver classes found";
            logger.severe(msg);
            throw new DefinitionException(msg);
        }

        // Transfer the classes to an array
        Class<?>[] driverClasses = new Class<?>[driverClassList.size()];
        driverClasses = driverClassList.toArray(driverClasses);

        def.drivers = new Driver[driverClasses.length];

        // Obtain all driver and driver class names
        for (int i = 0; i < driverClasses.length; i++) {
            BenchmarkDriver benchDriver = driverClasses[i].getAnnotation(
                    BenchmarkDriver.class);
            def.drivers[i] = new Driver();
            def.drivers[i].name = benchDriver.name();
            def.drivers[i].metric = benchDriver.metric();
            def.drivers[i].opsUnit = benchDriver.opsUnit();
            def.drivers[i].threadPerScale = benchDriver.threadPerScale();
            def.drivers[i].className = driverClasses[i].getName();
            getBackground(driverClasses[i], def.drivers[i]);
            def.drivers[i].mix[0] = Mix.getMix(driverClasses[i]);
            def.drivers[i].initialDelay[0] = getInitialDelay(driverClasses[i]);
            int totalOps = def.drivers[i].mix[0].operations.length;
            if (def.drivers[i].mix[1] != null)
                totalOps += def.drivers[i].mix[1].operations.length;

            // Copy operation references into a flat array.
            def.drivers[i].operations = 
                    new BenchmarkDefinition.Operation[totalOps];
            for (int j = 0; j < def.drivers[i].mix[0].operations.length; j++)
                def.drivers[i].operations[j] =
                        def.drivers[i].mix[0].operations[j];
            if (def.drivers[i].mix[1] != null)
            for (int j = 0; j < def.drivers[i].mix[1].operations.length; j++)
                def.drivers[i].operations[j + def.drivers[i].mix[0].operations.
                        length] = def.drivers[i].mix[1].operations[j];

            def.drivers[i].driverClass = driverClasses[i];
        }
        return def;
    }


    /**
     * Reads the Faban definition annotations and prints a DD to file.
     * @param defClassName The defining class name
     */
    public static void printFabanDD(String defClassName) {
        Logger logger = Logger.getLogger(BenchmarkDefinition.class.getName());
        Class<?> defClass = null;
        try {
            defClass = Class.forName(defClassName);
        } catch (ClassNotFoundException e) {
            ConfigurationException ce = new ConfigurationException(e);
            logger.log(Level.SEVERE, e.getMessage(), ce);
            return;
        }

        if (!defClass.isAnnotationPresent(
                com.sun.faban.driver.BenchmarkDefinition.class)) {
            String msg = "Class " + defClassName +
                    " is not a benchmark definition.";
            logger.severe(msg);
            return;
        }

        com.sun.faban.driver.BenchmarkDefinition benchDefAnnotation = defClass.
                getAnnotation(com.sun.faban.driver.BenchmarkDefinition.class);

        StringBuilder b = new StringBuilder(2048);
        b.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        b.append("<!-- Generated Faban Driver Framework DD, please do not " +
                 "modify -->\n");
        b.append("<fabanDriver>\n");
        b.append("    <name>" + benchDefAnnotation.name() + "</name>\n");
        b.append("    <version>" + benchDefAnnotation.version() +
                 "</version>\n");
        b.append("    <runControl>" + benchDefAnnotation.runControl() +
                 "</runControl>\n");
        b.append("    <metric>" + benchDefAnnotation.metric() + "</metric>\n");
        b.append("    <scaleName>" + benchDefAnnotation.scaleName() +
                 "</scaleName>\n");
        b.append("    <scaleUnit>" + benchDefAnnotation.scaleUnit() +
                 "</scaleUnit>\n");
        b.append("    <configPrecedence>" + benchDefAnnotation.
                 configPrecedence() + "</configPrecedence>\n");
        b.append("</fabanDriver>\n");

        String outputFile = System.getProperty("benchmark.ddfile");
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            out.write(b.toString().getBytes());
            out.flush();
            out.close();
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

    private static void getBackground(Class<?> driverClass, Driver driver)
            throws DefinitionException {
        if (!driverClass.isAnnotationPresent(Background.class)) {
            driver.mix[1] = null;
            driver.initialDelay[1] = null;
            return;
        }
        Background background = driverClass.getAnnotation(Background.class);
        String[] ops = background.operations();
        FixedSequence mix = new FixedSequence();
        mix.deviation = 2d;
        mix.operations = getOperationsNoCycles(driverClass, ops);
        com.sun.faban.driver.FixedTime[] timings = background.timings();
        if (timings.length == 0) { // No bg timing, use driver or method timing
            Cycle.setCycles(mix.operations, driverClass);
        } else if (timings.length == 1) { // Apply to all the background ops
            FixedTime fixedTime = new FixedTime();
            fixedTime.init(timings[0]);
            for (Operation op : mix.operations)
                op.cycle = fixedTime;
        } else if (timings.length > 1) { // Appy timing to each background op
            if (timings.length != mix.operations.length)
                throw new DefinitionException("No of background ops must " +
                        "match the no of timings, currently " +
                        mix.operations.length + " vs " + timings.length);
            for (int i = 0; i < timings.length; i++) {
                if (mix.operations[i].cycle != null)
                    throw new DefinitionException("Duplicate operations " +
                            "entry in @Background");
                mix.operations[i].cycle = new FixedTime();
                mix.operations[i].cycle.init(timings[i]);
            }
        }
        driver.mix[1] = mix;
        driver.initialDelay[1] =
                getInitialDelay(background.initialDelay().max());
    }

    private static Cycle getInitialDelay(Class<?> driverClass) {
        InitialDelay initDelay = driverClass.getAnnotation(
                InitialDelay.class);
        int max;
        if (initDelay == null)
            max = 0;
        else
            max = initDelay.max();
        return getInitialDelay(max);
    }

    private static Cycle getInitialDelay(int max) {
        Cycle delay;
        if (max <= 0) {
            FixedTime fixed = new FixedTime();
            fixed.cycleTime = 0;
            delay = fixed;
        } else {
            Uniform uniform = new Uniform();
            uniform.cycleMin = 0;
            uniform.cycleMax = max;
            delay = uniform;
        }
        delay.cycleType = CycleType.CYCLETIME;
        delay.cycleDeviation = 0d;
        return delay;
    }

    private BenchmarkDefinition() {
    }

    static Operation[] getOperations(Class<?> driverClass, String[] names)
            throws DefinitionException {
        Operation[] ops = getOperationsNoCycles(driverClass, names);
        Cycle.setCycles(ops, driverClass);
        return ops;
    }

    private static Operation[] getOperationsNoCycles(Class<?> driverClass,
                                                     String[] names)
            throws DefinitionException {
        HashMap<String, Operation> operationMap =
                new HashMap<String, Operation>();

        // First we read all the operations that have the annotation
        Method[] methods = driverClass.getMethods();
        for (Method m : methods)
            if (m.isAnnotationPresent(BenchmarkOperation.class)) {
                BenchmarkOperation benchOp = m.getAnnotation(
                        BenchmarkOperation.class);
                Operation op = new Operation();
                op.name = benchOp.name();
                op.max90th = benchOp.max90th();
                op.timing = benchOp.timing();
                op.m = m;
                operationMap.put(op.name, op);
            }
        Operation[] ops = new Operation[names.length];

        // Then we list them according to the name
        for (int i = 0; i < names.length; i++) {
            ops[i] = operationMap.get(names[i]);
            if (ops[i] == null)
                throw new DefinitionException("Operation \"" + names[i] +
                        "\" listed in mix not found");
        }
        return ops;
    }

    static Operation[] getOperations(Class<?> driverClass)
            throws DefinitionException {
        ArrayList<Operation> operationList = new ArrayList<Operation>();
        Method[] methods = driverClass.getMethods();
        for (Method m : methods)
            if (m.isAnnotationPresent(BenchmarkOperation.class)) {
                BenchmarkOperation benchOp = m.getAnnotation(
                        BenchmarkOperation.class);
                Operation op = new Operation();
                op.name = benchOp.name();
                op.max90th = benchOp.max90th();
                op.timing = benchOp.timing();
                op.m = m;
                operationList.add(op);
            }

        Operation[] ops = new Operation[operationList.size()];
        ops = operationList.toArray(ops);
        Cycle.setCycles(ops, driverClass);
        return ops;
    }

    /**
     * The refullOperations method re-establishes the non-serializable
     * parts of the operations array.
     */
    static void refillOperations(Class<?> driverClass, Operation[] operations) {

        {  // Use a separate code block as the vars in here are not used later
            int i;

            // Find a method in the list that is null
            for (i = 0; i < operations.length; i++ )
                if (operations[i].m == null)
                    break;

            // If none found, we do not need to do anything else.
            if (i == operations.length)
                return;
        }

        HashMap<String, Method> methodMap =
                new HashMap<String, Method>();

        // First we read all the operations that have the annotation
        Method[] methods = driverClass.getMethods();
        for (Method m : methods)
            if (m.isAnnotationPresent(BenchmarkOperation.class)) {
                BenchmarkOperation benchOp = m.getAnnotation(
                        BenchmarkOperation.class);
                methodMap.put(benchOp.name(), m);
            }

        // Then we check each operation and get the method from the map.
        for (Operation o : operations)
            if (o.m == null)
                o.m = methodMap.get(o.name);
    }

    /**
     * Creates and returns a copy of this object.  The precise meaning
     * of "copy" may depend on the class of the object. The general
     * intent is that, for any object <tt>x</tt>, the expression:
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * will be true, and that the expression:
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * will be <tt>true</tt>, but these are not absolute requirements.
     * While it is typically the case that:
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * will be <tt>true</tt>, this is not an absolute requirement.
     * <p/>
     * By convention, the returned object should be obtained by calling
     * <tt>super.clone</tt>.  If a class and all of its superclasses (except
     * <tt>Object</tt>) obey this convention, it will be the case that
     * <tt>x.clone().getClass() == x.getClass()</tt>.
     * <p/>
     * By convention, the object returned by this method should be independent
     * of this object (which is being cloned).  To achieve this independence,
     * it may be necessary to modify one or more fields of the object returned
     * by <tt>super.clone</tt> before returning it.  Typically, this means
     * copying any mutable objects that comprise the internal "deep structure"
     * of the object being cloned and replacing the references to these
     * objects with references to the copies.  If a class contains only
     * primitive fields or references to immutable objects, then it is usually
     * the case that no fields in the object returned by <tt>super.clone</tt>
     * need to be modified.
     * <p/>
     * The method <tt>clone</tt> for class <tt>Object</tt> performs a
     * specific cloning operation. First, if the class of this object does
     * not implement the interface <tt>Cloneable</tt>, then a
     * <tt>CloneNotSupportedException</tt> is thrown. Note that all arrays
     * are considered to implement the interface <tt>Cloneable</tt>.
     * Otherwise, this method creates a new instance of the class of this
     * object and initializes all its fields with exactly the contents of
     * the corresponding fields of this object, as if by assignment; the
     * contents of the fields are not themselves cloned. Thus, this method
     * performs a "shallow copy" of this object, not a "deep copy" operation.
     * <p/>
     * The class <tt>Object</tt> does not itself implement the interface
     * <tt>Cloneable</tt>, so calling the <tt>clone</tt> method on an object
     * whose class is <tt>Object</tt> will result in throwing an
     * exception at run time.
     *
     * @return a clone of this instance.
     * @throws CloneNotSupportedException if the object's class does not
     *                                    support the <code>Cloneable</code> interface. Subclasses
     *                                    that override the <code>clone</code> method can also
     *                                    throw this exception to indicate that an instance cannot
     *                                    be cloned.
     * @see Cloneable
     */
    public Object clone() throws CloneNotSupportedException {
        // Shallow copy for primitives and immutables
        BenchmarkDefinition clone = (BenchmarkDefinition) super.clone();

        // Then deep copy for the arrays and mutables.
        clone.drivers = new Driver[drivers.length];
        for (int i = 0; i < drivers.length; i++)
            clone.drivers[i] = (Driver) drivers[i].clone();

        return clone;
    }

    static class Driver implements Serializable, Cloneable {
        String name;
        String metric;
        String opsUnit;
        int threadPerScale;
        Mix[] mix = new Mix[2]; // Foreground (0) and background (1) mix.
        Cycle[] initialDelay = new Cycle[2]; // Foreground and background
        BenchmarkDefinition.Operation[] operations;
        String className;

        // We try to send the whole driver class over to the agents so that all
        // agents will run consistent drivers. In case this is slow or does not
        //  work due to dependencies, we will need to set this field transient.
        // The receiving end must check this value for null and re-load the
        // class in such cases.
        Class driverClass;

        /**
         * Creates and returns a copy of this object.  The precise meaning
         * of "copy" may depend on the class of the object. The general
         * intent is that, for any object <tt>x</tt>, the expression:
         * <blockquote>
         * <pre>
         * x.clone() != x</pre></blockquote>
         * will be true, and that the expression:
         * <blockquote>
         * <pre>
         * x.clone().getClass() == x.getClass()</pre></blockquote>
         * will be <tt>true</tt>, but these are not absolute requirements.
         * While it is typically the case that:
         * <blockquote>
         * <pre>
         * x.clone().equals(x)</pre></blockquote>
         * will be <tt>true</tt>, this is not an absolute requirement.
         * <p/>
         * By convention, the returned object should be obtained by calling
         * <tt>super.clone</tt>.  If a class and all of its superclasses (except
         * <tt>Object</tt>) obey this convention, it will be the case that
         * <tt>x.clone().getClass() == x.getClass()</tt>.
         * <p/>
         * By convention, the object returned by this method should be independent
         * of this object (which is being cloned).  To achieve this independence,
         * it may be necessary to modify one or more fields of the object returned
         * by <tt>super.clone</tt> before returning it.  Typically, this means
         * copying any mutable objects that comprise the internal "deep structure"
         * of the object being cloned and replacing the references to these
         * objects with references to the copies.  If a class contains only
         * primitive fields or references to immutable objects, then it is usually
         * the case that no fields in the object returned by <tt>super.clone</tt>
         * need to be modified.
         * <p/>
         * The method <tt>clone</tt> for class <tt>Object</tt> performs a
         * specific cloning operation. First, if the class of this object does
         * not implement the interface <tt>Cloneable</tt>, then a
         * <tt>CloneNotSupportedException</tt> is thrown. Note that all arrays
         * are considered to implement the interface <tt>Cloneable</tt>.
         * Otherwise, this method creates a new instance of the class of this
         * object and initializes all its fields with exactly the contents of
         * the corresponding fields of this object, as if by assignment; the
         * contents of the fields are not themselves cloned. Thus, this method
         * performs a "shallow copy" of this object, not a "deep copy" operation.
         * <p/>
         * The class <tt>Object</tt> does not itself implement the interface
         * <tt>Cloneable</tt>, so calling the <tt>clone</tt> method on an object
         * whose class is <tt>Object</tt> will result in throwing an
         * exception at run time.
         *
         * @return a clone of this instance.
         * @throws CloneNotSupportedException if the object's class does not
         *                                    support the <code>Cloneable</code> interface. Subclasses
         *                                    that override the <code>clone</code> method can also
         *                                    throw this exception to indicate that an instance cannot
         *                                    be cloned.
         * @see Cloneable
         */
        public Object clone() throws CloneNotSupportedException {
            Driver clone = (Driver) super.clone();
            clone.mix[0] = (Mix) mix[0].clone();
            if (mix[1] != null)
                clone.mix[1] = (Mix) mix[1].clone();

            clone.initialDelay[0] = (Uniform) initialDelay[0].clone();
            if (initialDelay[1] != null)
                clone.initialDelay[1] = (Uniform) initialDelay[1].clone();

            // Copy operation references into a flat array.
            int totalOps = operations.length;
            clone.operations = new BenchmarkDefinition.Operation[totalOps];
            for (int j = 0; j < mix[0].operations.length; j++)
                clone.operations[j] = clone.mix[0].operations[j];
            for (int j = 0; j < mix[1].operations.length; j++)
                clone.operations[j + mix[0].operations.length] = 
                        clone.mix[1].operations[j];

            return clone;
        }
        
        /**
         * Obtains the global operation index based on the mix identifier and
         * operation identifier inside the mix.
         * 
         * @param mixId 0 for foreground and 1 for background mix
         * @param opId the id of the operation within the mix
         * @return The global index into the operation
         */
        public int getOperationIdx(int mixId, int opId) {
            int idx = 0;
            for (int i = 0; i < mixId; i++)
                idx += mix[i].operations.length;
            idx += opId;
            return idx;
        }
    }

    static class DriverMethod implements Serializable, Cloneable {
        String name;
        transient Method m;
    }

    static class Operation extends DriverMethod {
        double max90th;
        Timing timing;
        Cycle cycle;

        /**
         * Creates and returns a copy of this object.  The precise meaning
         * of "copy" may depend on the class of the object. The general
         * intent is that, for any object <tt>x</tt>, the expression:
         * <blockquote>
         * <pre>
         * x.clone() != x</pre></blockquote>
         * will be true, and that the expression:
         * <blockquote>
         * <pre>
         * x.clone().getClass() == x.getClass()</pre></blockquote>
         * will be <tt>true</tt>, but these are not absolute requirements.
         * While it is typically the case that:
         * <blockquote>
         * <pre>
         * x.clone().equals(x)</pre></blockquote>
         * will be <tt>true</tt>, this is not an absolute requirement.
         * <p/>
         * By convention, the returned object should be obtained by calling
         * <tt>super.clone</tt>.  If a class and all of its superclasses (except
         * <tt>Object</tt>) obey this convention, it will be the case that
         * <tt>x.clone().getClass() == x.getClass()</tt>.
         * <p/>
         * By convention, the object returned by this method should be independent
         * of this object (which is being cloned).  To achieve this independence,
         * it may be necessary to modify one or more fields of the object returned
         * by <tt>super.clone</tt> before returning it.  Typically, this means
         * copying any mutable objects that comprise the internal "deep structure"
         * of the object being cloned and replacing the references to these
         * objects with references to the copies.  If a class contains only
         * primitive fields or references to immutable objects, then it is usually
         * the case that no fields in the object returned by <tt>super.clone</tt>
         * need to be modified.
         * <p/>
         * The method <tt>clone</tt> for class <tt>Object</tt> performs a
         * specific cloning operation. First, if the class of this object does
         * not implement the interface <tt>Cloneable</tt>, then a
         * <tt>CloneNotSupportedException</tt> is thrown. Note that all arrays
         * are considered to implement the interface <tt>Cloneable</tt>.
         * Otherwise, this method creates a new instance of the class of this
         * object and initializes all its fields with exactly the contents of
         * the corresponding fields of this object, as if by assignment; the
         * contents of the fields are not themselves cloned. Thus, this method
         * performs a "shallow copy" of this object, not a "deep copy" operation.
         * <p/>
         * The class <tt>Object</tt> does not itself implement the interface
         * <tt>Cloneable</tt>, so calling the <tt>clone</tt> method on an object
         * whose class is <tt>Object</tt> will result in throwing an
         * exception at run time.
         *
         * @return a clone of this instance.
         * @throws CloneNotSupportedException if the object's class does not
         *                                    support the <code>Cloneable</code> interface. Subclasses
         *                                    that override the <code>clone</code> method can also
         *                                    throw this exception to indicate that an instance cannot
         *                                    be cloned.
         * @see Cloneable
         */
        public Object clone() throws CloneNotSupportedException {
            Operation clone = (Operation) super.clone();
            clone.cycle = (Cycle) cycle.clone();
            return clone;
        }
    }
}
