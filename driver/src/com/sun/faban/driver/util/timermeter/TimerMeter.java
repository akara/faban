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
 * $Id$
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.util.timermeter;

import java.util.*;
import java.util.logging.Logger;

/**
 * The TimerMeter code in large is a modification from Michael Kuperberg's
 * TimerMeter work. We thank him for this work.<br>
 * <br>
 * Michael Kuperberg (mkuper@ipd.uka.de),<br>
 * Chair for Software Design and Quality (SDQ),<br>
 * Institute for Program Structures and Data Organisation (IPD),<br>
 * Faculty of Informatics, University of Karlsruhe, Germany<br>
 * http://sdq.ipd.uka.de
 */
public class TimerMeter implements Runnable {

    /**
     * This dummy variable is used for warmup; it is intentionally global
     * because of JIT / dead code elimination prevention.
     */
    protected static long publicDummy = 0L;

    /**
     * See class description.
     */
    private int clusterFormationThresholdDistance;

    /**
     * not private because needed in <code>obtainMeasurementsUsingDirectInvocation</code>
     */
    protected int currentUpperLoopSizeBound=0;

    /**
     * Global variable used by the core algorithm.
     */
    public int increased;

    /**
     * See class description.
     */
    protected int iterationIncreaseStepWidth;

    /**
     * The logger instance, sometimes supplemented by <code>e</code> and <code>o</code>.
     * */
    private static Logger logger = Logger.getLogger(TimerMeter.class.getName());

    /**
     * Protected because it must be set and read in subclasses' that override
     * the <code>obtainMeasurementsUsingDirectInvocation</code> method
     */
    protected long methodReturnAtMeasurementFinish = 0L;

    /**
     * protected because it must be set and read in subclasses' that override
     * the <code>obtainMeasurementsUsingDirectInvocation</code> method
     */
    protected long methodReturnAtMeasurementStart = 0L;

    /**
     * Actual maximum number of for loop iterations that are executed between
     * two <code>nanoTime</code> invocations (cf. class description)
     */
    protected int numberOfIterationIncreaseSteps;

    /**
     * See class description.
     */
    protected int numberOfMeasurements;

    /**
     * Number of times the entire process is repeated.
     */
    protected int numberOfProcessRepetitions;

    /**
     * See class description.
     */
    protected int numberOfWarmupTimerInvocations;

    /**
     * TODO document assumption: parameterless and no class/package name!
     *      --> no parentheses: (){}[]<> and no "."s and ";" s
     */
    protected String timerMethodName;

    /**
     * Warning message displayed when the timer value difference of two
     * consecutive is negative.
     */
    protected final String warningOnNegativeDiff = "Difference "
        +"of time measurements smaller than 0! "
        +"This points to inconsistencies in "+timerMethodName+" implementation";


    /**
	 * Test main method.
	 * @param args Command line arguments, unused.
	 */
	public static void main(String[] args){
		TimerMeter tm = new TimerMeter(1000000, 10, 1000, 2, 100, 2);
		tm.run();
	}

    public TimerMeter(int warmupLoops, int repeats, int measurements,
                      int increaseWidth, int increaseAfter,
                      int clusterThreshold) {
        numberOfWarmupTimerInvocations = warmupLoops;
        numberOfProcessRepetitions = repeats;
        numberOfMeasurements = measurements;

        iterationIncreaseStepWidth = increaseWidth;
        numberOfIterationIncreaseSteps = increaseAfter;
        clusterFormationThresholdDistance = clusterThreshold;
    }

    /**
     * TODO engineer for performance: use one-dimensional array and work with offsets?
     * @param timerMeterMeasurements The measurement results
     * @param logValues Whether to log values or not
     * @return The timer characterization
     */
    public final TimerCharacterisation computeTimerCharacterization(
            long[][] timerMeterMeasurements,
            boolean logValues) {


        //number of measurements in one process
        int numberOfProcesses = timerMeterMeasurements.length;
        int processLength = timerMeterMeasurements[0].length;
        long[] allMeasurementsAppendedAndSorted = new long[numberOfProcesses*processLength];//TODO add security checks
        long[] invocationCostMeasurements = new long[numberOfProcesses*numberOfMeasurements];
        List<StatisticalDescription> processCharacterisations = new ArrayList<StatisticalDescription>();
        SortedMap<Integer, SortedMap<Long,Integer>> histogramElementsByProcessAcrossAllProcesses
            = new TreeMap<Integer, SortedMap<Long,Integer>>();

        for(int i=0; i<numberOfProcesses; i++){
            // copy the invocation cost measurements in an own flat structure
            System.arraycopy(timerMeterMeasurements[i], 0, invocationCostMeasurements, i * numberOfMeasurements, numberOfMeasurements);

            // copy all measurements in a flat structure
            System.arraycopy(timerMeterMeasurements[i], 0, allMeasurementsAppendedAndSorted, i*processLength, processLength);

            processCharacterisations.add(CommonUtilities.computeCharacteristics_detailed(timerMeterMeasurements[i], logValues));
            histogramElementsByProcessAcrossAllProcesses.put(i, CommonUtilities.computeHistogram(timerMeterMeasurements[i]));
        }

        Arrays.sort(allMeasurementsAppendedAndSorted);//TODO document this
        SortedMap<Long, Integer> allMeasurementsHistogram = CommonUtilities.computeHistogram(allMeasurementsAppendedAndSorted);
        StatisticalDescription allMeasurementsCharacterisation = CommonUtilities.computeCharacteristics_detailed(allMeasurementsAppendedAndSorted, logValues);

        //getting histogram elements by value (accross all processes

//		Integer currInputKey;
//		SortedMap<Long,Integer> currInputValue;
//		Long currInputValue_currInternalKey;
//		Integer currInputValue_currInternalValue;
//
////		Long currOutputKey;//equal to currInputValue_currInternalKey
//		SortedMap<Integer,Integer> currOutputValue;
//		Integer currOutputValue_currInternalKey; //equal to currInputKey
//		Integer currOutputValue_currInternalValue; //equal to currInputValue_currInternalValue

        SortedMap<Long, SortedMap<Integer,Integer>> histogramElementsByValueAcrossAllProcesses
            = new TreeMap<Long, SortedMap<Integer,Integer>>();
        CommonUtilities.rearrangeMeasurementsByValue(histogramElementsByProcessAcrossAllProcesses,
                histogramElementsByValueAcrossAllProcesses);

        Arrays.sort(invocationCostMeasurements);//ueberfluessig...?

        TimerCharacterisation tc = new TimerCharacterisation();
        tc.setAllInitialMeasurements(timerMeterMeasurements);//sorted?
        tc.setAllMeasurementsAppendedAndSorted(allMeasurementsAppendedAndSorted);//sorted?
        tc.setAllMeasurementsCharacterisation(allMeasurementsCharacterisation);
        tc.setAllMeasurementsHistogram(allMeasurementsHistogram);
        tc.setHistogramElementsByProcess(histogramElementsByProcessAcrossAllProcesses);
        tc.setHistogramElementsByValue(histogramElementsByValueAcrossAllProcesses);
        tc.setProcessCharacterisations(processCharacterisations);
        tc.setClusters(
                CommonUtilities.clusterTimerValuesFromHistogram(
                        allMeasurementsHistogram,
                        clusterFormationThresholdDistance)); //TODO evaluate clusters

        tc.setAccuracy(CommonUtilities.computeAccuracyFromClusters(tc.getClusters(), false));
        tc.setInvocationCost(CommonUtilities.computeCharacteristics_detailed(invocationCostMeasurements, false));

        return tc;
    }

    /** THIS METHOD MUST BE OVERRIDDEN IN SUBCLASSES. Implementation is
     * provided here as an example. Use it and replace the <code>nanoTime</code>.
     *
     * After "warming up" the timer method <code>numberOfWarmupTimerInvocations</code> times, this
     * method measures the duration of a 'for' loop. The measurement is repeated
     * <code>numberOfMeasurements</code> times for each loop iteration number from
     * 0 to <code>numberOfIterationIncreaseSteps</code>.
     *
     * @return <code>List</code> of measurements
     */
    protected long[][] obtainMeasurementsUsingDirectInvocation() {
        int totalNumberOfMeasurementsInOneProcessRun =
            (numberOfIterationIncreaseSteps + 1) * numberOfMeasurements;
        logger.fine("Creating and filling a data structure occupying at least 2*"+//TODO this is not very perf.-savvy
                numberOfProcessRepetitions+"*"+
                (numberOfIterationIncreaseSteps+1)+"*"+
                numberOfMeasurements+"="+
                (2 * numberOfProcessRepetitions *
                        (numberOfIterationIncreaseSteps + 1) *
                        numberOfMeasurements) +
                " bytes of DATA memory, not counting the linking structure...");

        //1. initialise data structure fully
        long[][] allResults = new long[numberOfProcessRepetitions][totalNumberOfMeasurementsInOneProcessRun];
        for (int i = 0; i < numberOfProcessRepetitions; i++) {
            for (int j = 0; j<totalNumberOfMeasurementsInOneProcessRun; j++) {
                allResults[i][j] = 0L;
            }
        }

        for (int w = 0; w < numberOfWarmupTimerInvocations; w++) {
            publicDummy += System.nanoTime();
        }
        logger.finer("DEBUG: Warmup finished (" +
                numberOfWarmupTimerInvocations +
                " calls to "+timerMethodName+", " +
                "publicDummy is " + publicDummy+").\n");
        long methodReturnDifference;
        for(int p=0; p < numberOfProcessRepetitions; p++){
            logger.fine("Process " + p + " of " + numberOfProcessRepetitions);
            // here: no work between timer invocations, i.e. no "for loop iterations" -
            // intentionally not part of the second loop below, but included in
            //measurements for obtaining invocation costs
            for (int i = 0; i < numberOfMeasurements; i++) {
//				System.gc();// TODO consider manual GC...
                methodReturnAtMeasurementStart = System.nanoTime();
                methodReturnAtMeasurementFinish = System.nanoTime();
                methodReturnDifference = methodReturnAtMeasurementFinish -
                                         methodReturnAtMeasurementStart;

                if (methodReturnDifference < 0) {
                    logger.warning(warningOnNegativeDiff);
                } else {
                    allResults[p][i] = methodReturnDifference;
                }
            }
            int offsetIntoArray = numberOfMeasurements - 1;

            currentUpperLoopSizeBound = 0; //TODO propagate to other classes
            // 0 to (numberOfIterationIncreaseSteps-1) outer "for loop iterations"
            for (int l = 0; l < numberOfIterationIncreaseSteps; l++) {//TODO check this
                currentUpperLoopSizeBound =
                    currentUpperLoopSizeBound + iterationIncreaseStepWidth;
                // 0 to (numberOfMeasurements-1) inner "for loop iterations"
                for (int m = 0; m < numberOfMeasurements; m++) {
                    offsetIntoArray++; //pointing to the place to store the diff
                    methodReturnAtMeasurementStart = System.nanoTime();
                    for (int a = 0; a < currentUpperLoopSizeBound ; a++) {
                        increased++;
                    }
                    methodReturnAtMeasurementFinish = System.nanoTime();
                    methodReturnDifference = methodReturnAtMeasurementFinish -
                                             methodReturnAtMeasurementStart;

                    if (methodReturnDifference < 0) {
                        logger.warning(warningOnNegativeDiff);
                    } else {
                        allResults[p][offsetIntoArray] = methodReturnDifference;
                    }
                }
            }
        }

        return allResults;
    }

    public String printTimerCharacterization() {
        TimerCharacterisation ch = computeTimerCharacterization(
                        obtainMeasurementsUsingDirectInvocation(), false);
        return ch.toString();
    }

    public void run() {
        logger.info("Timer characteristics: \n" + printTimerCharacterization());
    }
}
