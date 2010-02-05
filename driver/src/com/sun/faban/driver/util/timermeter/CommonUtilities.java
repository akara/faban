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

public class CommonUtilities {

    private static Logger logger =
            Logger.getLogger(CommonUtilities.class.getName());

    /**
     * @param source
     * @param destination
     */
    public static void rearrangeMeasurementsByValue(
            SortedMap<Integer, SortedMap<Long, Integer>> source, SortedMap<Long,
                    SortedMap<Integer, Integer>> destination) {
        Integer currInputKey;
        SortedMap<Long, Integer> currInputValue;
        Long currInputValue_currInternalKey;
        Integer currInputValue_currInternalValue;
        SortedMap<Integer, Integer> currOutputValue;
        Iterator<Integer> currOuterIterator = source.keySet().iterator();
        Iterator<Long> currInnerIterator;
        int processIndex=0;
        for (; currOuterIterator.hasNext();) {//iterate over histogram elements by process
            //process' index
            currInputKey = currOuterIterator.next();
            //process' histogram
            currInputValue = source.get(currInputKey);
            //iterator over current process' histogram KEYS (i.e. diff values)
            currInnerIterator = currInputValue.keySet().iterator();
            for( ; currInnerIterator.hasNext(); ){
                //timer diff VALUE for the current process
                currInputValue_currInternalKey = currInnerIterator.next();
                //timer diff COUNT for the current process
                currInputValue_currInternalValue = currInputValue.get(currInputValue_currInternalKey);
                try{
                    //1. trying to get a histogram entry for this diff
                    currOutputValue = destination.get(currInputValue_currInternalKey);
                    //1a. OK, such a map exists
                    currOutputValue.put(currInputKey, currInputValue_currInternalValue);
    //					currValue = new Integer(currValue+1);
    //					histogram.put(currKey,currValue);
                }catch (Exception e) {
                    //1b.1 such a map does not exists, create it
                    currOutputValue = new TreeMap<Integer, Integer>();
                    //1b.2 add <process_index, count_for_this_long_value_for_this_process> to the map
                    currOutputValue.put(currInputKey, currInputValue_currInternalValue); //TODO check if this is effective
                    //1b.3 add the "inner map" to the "outer map"
                    destination.put(currInputValue_currInternalKey, currOutputValue);
                }
            }
            processIndex++;
        }
    //		o.println("Input "+timerMeterMeasurements);
    }

    /**
     * TODO move back to AbstractTimerMeter and rename to show this method's specific nature
     *
     * Sketch of work for threshold==2 ("entry" := one element of
     * <code>allMeasurementsHistogram</code>):
     * if a cluster with 3 elements (i.e. x, x+1, x+2) appears:
     * re-visit existing clusters and break them into one-entry-per-cluster
     * (or, simpler: discard existing clusters and re-traverse
     * <code>allMeasurementsHistogram</code> with a threshold of <1).
     *
     * The return of this method should be postprocessed by another method
     * that computes the accuracy as greatest common divisor of cluster
     * distances. However, this is tricky, as some two-classes clusters may
     * not have any values from one class, e.g. [(100,101), (200,201), (300), (400,401)],
     * or some classes may have been skipped altogether...
     * Just work with "+-1" cluster distances (this does not work with "2" or
     * other small accuracies)? Or should we "fill" non-existing classes?
     * In fact, we cannot guarantee that all classes are present...
     *
     * For future work: can we make any statements about the "Nachkommastelle"
     * from the frequencies of the classes?
     *
     * @param allMeasurementsHistogram
     * @param clusterFormationThresholdDistance the maximum distance (incl.)
     * from the leftmost [smalles] element of the current cluster
     * so that the considered <code>allMeasurementsHistogram</code> entry
     * can still be added to the current cluster.
     * @return
     */
    public static List<MeasurementGroupsCluster> clusterTimerValuesFromHistogram(
            SortedMap<Long, Integer> allMeasurementsHistogram,
            int clusterFormationThresholdDistance) {
        if(clusterFormationThresholdDistance!=2){
            logger.severe("Cluster formation threshold distance must be 2; exiting prematurely.");
            return null;
        }
        if(allMeasurementsHistogram==null){
            logger.severe("Cannot cluster a null histogram, returning null");
            return null;
        }
        if(allMeasurementsHistogram.isEmpty()){
            logger.severe("Cannot cluster an empty histogram, returning null");
            return null;
        }
        List<MeasurementGroupsCluster> clusterList = new ArrayList<MeasurementGroupsCluster>();
        Iterator<Long> keyIter = allMeasurementsHistogram.keySet().iterator();
//		boolean lastClusterNotYetAdded

        Long lastKey = keyIter.next();
        Integer lastValue = allMeasurementsHistogram.get(lastKey);
        Long currClusterLeftBound = lastKey;
        MeasurementGroupsCluster currCluster = new MeasurementGroupsCluster(new TreeMap<Long, Integer>());
        currCluster.addData(lastKey, lastValue);
        int numberOfClassesInCurrentCluster=1;

        Long currKey;
        Integer currValue;
        boolean clusterHaveTwoOrLessClasses=true;

        while(keyIter.hasNext() && clusterHaveTwoOrLessClasses){
            currKey = keyIter.next();
            currValue = allMeasurementsHistogram.get(currKey);
            if(currClusterLeftBound+clusterFormationThresholdDistance>=currKey){
                if(numberOfClassesInCurrentCluster==2){
                    // equal to leaving the loop - sometimes, on Linux,
                    // this unexpected case indeed happens -
                    // we assume this is because of rounding issues inside Linux,
                    // which was not present in Kernel 2.6.25
                    // clusterHaveTwoOrLessClasses=false;

                    logger.fine("a further class (key="+currKey+") about " +
                            "to be added to a cluster with already 2 classes : "+
                            currCluster);
                    MeasurementGroupsCluster clusterNeighbor;
                    if(clusterList.size()>=1){
                        clusterNeighbor = clusterList.get(clusterList.size()-1);
                        logger.fine("cluster to the left: " + clusterNeighbor);
                        if(clusterList.size()>=2){
                            clusterNeighbor = clusterList.get(clusterList.size()-2);
                            logger.fine("cluster to the left of the left neighbor: " + clusterNeighbor);
                        }
                    }
                }else{
                    currCluster.addData(currKey, currValue);
                    numberOfClassesInCurrentCluster++;
                }
            }else{
                clusterList.add(currCluster);
                currCluster = new MeasurementGroupsCluster(new TreeMap<Long, Integer>());
                currClusterLeftBound = currKey;
                currCluster.addData(currKey, currValue);
                numberOfClassesInCurrentCluster = 1;
            }
        }
        clusterList.add(currCluster);//last cluster is always dangling...


        if(!clusterHaveTwoOrLessClasses){
            clusterList = new ArrayList<MeasurementGroupsCluster>();
            for (Iterator<Long> iterator = allMeasurementsHistogram.keySet().iterator(); iterator.hasNext();) {
                currKey = iterator.next();
                currValue = allMeasurementsHistogram.get(currKey);
                clusterList.add(new MeasurementGroupsCluster(currKey,currValue));
            }
        }

        return clusterList;
    }

    /**
     * TODO document me please
     * TODO replace through greatest-common-divisor scheme (or add it testwise...)
     * @param clusters
     * @return
     */
    public static long computeAccuracyFromClusters(List<MeasurementGroupsCluster> clusters, boolean verbose) {
        Iterator<MeasurementGroupsCluster> iter = clusters.iterator();
        MeasurementGroupsCluster second = iter.next();//TODO check if there
        MeasurementGroupsCluster first;
        long accuracyFromTwoGroupedClusters = Long.MAX_VALUE;
        long accuracyAnyClusters = Long.MAX_VALUE;
        long firstLeftBound = 0L;
        long secondLeftBound = 0L;
        int firstNumberOfClasses;
        int secondNumberOfClasses;
        long boundDistance;
        int clusterIndex=0;
        while(iter.hasNext()){
            first = second;
            second = iter.next();
            firstLeftBound = first.getClusterValueMinimum();
            secondLeftBound = second.getClusterValueMinimum();
            firstNumberOfClasses = first.getClassesNumberInCluster();
            secondNumberOfClasses = second.getClassesNumberInCluster();
            boundDistance = secondLeftBound-firstLeftBound;
            if(firstNumberOfClasses==2 && secondNumberOfClasses==2){
                if(boundDistance<accuracyFromTwoGroupedClusters){
                    if(verbose) logger.fine("Distance "+boundDistance+" is smaller " +
                            "than the current accuracy from two-grouped " +
                            "clusters ("+accuracyFromTwoGroupedClusters+")");
                    accuracyFromTwoGroupedClusters = boundDistance;
                }else{
                    if(verbose) logger.fine("Distance "+boundDistance+" is not smaller " +
                            "than the current accuracy from two-grouped " +
                            "clusters ("+accuracyFromTwoGroupedClusters+")");
                }
            }else{
                if(verbose) logger.fine("Clusters with " +
                        "index "+clusterIndex+"(in-cluster min: "+firstLeftBound+") and " +
                        "index "+(clusterIndex+1)+"(in-cluster min: "+secondLeftBound+") " +
                        "do not both have 2 innerclasses.");
            }
            if(boundDistance<accuracyAnyClusters){
                if(verbose) logger.fine("Distance "+boundDistance+" is smaller " +
                        "than the current accuracy from " +
                        "any clusters ("+accuracyAnyClusters+")");
                accuracyAnyClusters = boundDistance;
            }else{
                if(verbose) logger.fine("Distance "+boundDistance+" is not smaller " +
                        "than the current accuracy from " +
                        "any clusters ("+accuracyAnyClusters+")");
            }
            clusterIndex++;
        }
        if(verbose) logger.fine("At the end: accuracy from two-classed clusters: " +
                accuracyFromTwoGroupedClusters+", accuracy from " +
                "all clusters: "+accuracyAnyClusters+".");
        if(((accuracyAnyClusters-accuracyFromTwoGroupedClusters)*(accuracyAnyClusters-accuracyFromTwoGroupedClusters))<=1){
            //TODO document
            return accuracyFromTwoGroupedClusters;
        }else{
            //TODO document
            return accuracyAnyClusters;
        }
    }
    /**
     * @param measurements
     * @param logValues
     * @return
     * TODO test me
     * TODO set all fields!
     */
    public static StatisticalDescription computeCharacteristics_detailed(
            long[] measurements,
            boolean logValues){
        StatisticalDescription ret = new StatisticalDescription();
        long[] sortedValues = measurements.clone();//TODO document cloning
        Arrays.sort(sortedValues);

        ret.setMax(sortedValues[sortedValues.length-1]);

        Double sum = 0D;
        Double mean = 0D;
        int numberOfMeasurements = sortedValues.length;
        for (int i = 0; i < numberOfMeasurements; i++) {
            if(logValues){
                logger.fine("Measurement "+(i+1)+" " +
                        "(out of "+numberOfMeasurements+"):"+
                        sortedValues[i]+", ");
            }
            sum+=sortedValues[i];
        }
        if(logValues){
            System.out.println(" DONE: sum is "+sum);
        }
        mean=sum/numberOfMeasurements;
        ret.setMean(mean);

        if(sortedValues.length%2==0){//TODO test me
            int index = sortedValues.length/2;
            ret.setMedian((sortedValues[index-1]+sortedValues[index])/2);
            ret.setMedian_real_element(sortedValues[index-1]);
        }else{
            ret.setMedian(sortedValues[sortedValues.length/2]);
            ret.setMedian_real_element(sortedValues[sortedValues.length/2]);
        }

        ret.setMin(sortedValues[0]);

        Double sumOfSquaredDistances = 0D;
        for (int i = 0; i < numberOfMeasurements; i++) {
            if(logValues){
                logger.fine("difference: "+(sortedValues[i]-mean)+", ");
            }
            sumOfSquaredDistances += Math.pow((sortedValues[i]-mean),2);
            if(logValues) {
                logger.fine("sum of squared distances is now "+sumOfSquaredDistances+",");
            }
        }
        if(logValues) {
            logger.info(" DONE: sumOfSquaredDistances is "+sumOfSquaredDistances);
        }
        ret.setVariance(sumOfSquaredDistances/numberOfMeasurements);
        ret.setStandardDeviation(Math.pow(ret.getVariance(),0.5));
        return ret;
    }

    /**
     * TODO test me!
     * Computes a simple sorted histogram from <code>measurements</code>
     * @param measurements the input
     * @return the resulting histogram
     */
    public static SortedMap<Long, Integer> computeHistogram(long[] measurements) {
        SortedMap<Long, Integer> histogram = new TreeMap<Long, Integer>();
        int l = measurements.length;
        Integer currValue;
        Long currKey;
        for(int i=0; i<l; i++){
            currKey = measurements[i];
            try{
                currValue = histogram.get(measurements[i]);
                currValue = currValue + 1;
                histogram.put(currKey,currValue);
            }catch (Exception e) {
                histogram.put(currKey,1);
            }
        }
        return histogram;
    }
}
