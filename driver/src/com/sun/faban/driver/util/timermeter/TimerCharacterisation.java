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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Logger;


/**
 * @author Michael Kuperberg
 * Invocation cost statistics and accuracy (resolution) still missing.
 * TODO implement defined "distilled results only" behaviour
 * @version 0.9
 */
public class TimerCharacterisation implements Serializable/*, Comparable<TimerCharacterisation>*/{

	private static final long serialVersionUID = 1L;

	private long accuracy;
	private double externallyDeterminedTimerFrequency;
	private double externallyDeterminedTimerInvocationCost;
	private boolean frequencyOfTimerSpecifiedExternally;
	private StatisticalDescription invocationCost;
	private boolean invocationCostOfTimerSpecifiedExternally;
	private Logger logger;
	@SuppressWarnings("unused")
	private Object platformCharacterisation; //TODO - sollte von Vasili kommen
	private StatisticalDescription processed_allMeasurementsCharacterisation;
	private SortedMap<Long, Integer> processed_allMeasurementsHistogram;
	private List<MeasurementGroupsCluster> processed_clusters;
	private SortedMap<Integer, SortedMap<Long, Integer>> processed_histogramElementsByProcess;
	private SortedMap<Long, SortedMap<Integer, Integer>> processed_histogramElementsByValue;
	private List<StatisticalDescription> processed_processCharacterisations;
	private long[][] raw_allInitialMeasurements; //TODO make transient? or shift to a MeasurementResults instance?
	private long[] raw_allMeasurementsAppendedAndSorted; //TODO make transient? or shift to a MeasurementResults instance?

	public TimerCharacterisation(){
		logger = Logger.getLogger(this.getClass().getName());
	}

	public StatisticalDescription computeClusterWidthCharacteristicsAcrossProcesses(long clusterValue){
		logger.fine("Starting computeClusterWidthCharacteristicsAcrossProcesses");
		if(this.processed_histogramElementsByValue!=null){
			SortedMap<Integer,Integer> map = this.processed_histogramElementsByValue.get(clusterValue);
			Iterator<Integer> iterator = map.values().iterator();//note: iterating over values!
			long[] mapValuesAsLongArray = new long[map.size()];
			int arrayIndex = 0;
			for (; iterator.hasNext();) {
				mapValuesAsLongArray[arrayIndex] = iterator.next().longValue();
				arrayIndex++;
			}
			return computeCharacteristics_detailed(mapValuesAsLongArray, false);
		}else{
			logger.severe("processed_histogramElementsByValue is null; " +
					"returning null from computeClusterWidthCharacteristicsAcrossProcesses");
			return null;
		}
	}

    /**
     * @param measurements The measurement data
     * @param logValues The log values
     * @return The statistical description
     * TODO test me
     * TODO set all fields!
     */
    public StatisticalDescription computeCharacteristics_detailed(
            long[] measurements,
            boolean logValues){
        StatisticalDescription ret = new StatisticalDescription();
        long[] sortedValues = measurements.clone();//TODO document cloning
        Arrays.sort(sortedValues);

        ret.setMax(sortedValues[sortedValues.length-1]);

        Double sum = 0D;
        Double mean;
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


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 *
	 */
	@Override
	public boolean equals(Object obj) {//TODO document
		if(!(obj instanceof TimerCharacterisation)){
			return false;
		}else{
			TimerCharacterisation o = (TimerCharacterisation) obj;
			return Arrays.deepEquals(this.raw_allInitialMeasurements, o.raw_allInitialMeasurements);
//			if(this.allInitialMeasurements.length==o.allInitialMeasurements.length
//				&& this.allInitialMeasurements[0].length==o.allInitialMeasurements[0].length){
//				int i=0;
//				boolean result = true;
//				for(; i<this.allInitialMeasurements.length; i++){
//					System.
//				}
//				return result;
//			}else{
//				return false;
//			}
		}
	}

	public long getAccuracy() {
		return accuracy;
	}

	public long[][] getAllInitialMeasurements() {
		return raw_allInitialMeasurements;
	}

	public long[] getAllMeasurementsAppendedAndSorted() {
		return raw_allMeasurementsAppendedAndSorted;
	}

	public StatisticalDescription getAllMeasurementsCharacterisation() {
		return processed_allMeasurementsCharacterisation;
	}

	public SortedMap<Long, Integer> getAllMeasurementsHistogram() {
		return processed_allMeasurementsHistogram;
	}

	public List<MeasurementGroupsCluster> getClusters() {
		return processed_clusters;
	}

	public double getExternallyDeterminedTimerFrequency() {
		return externallyDeterminedTimerFrequency;
	}

	public double getExternallyDeterminedTimerInvocationCost() {
		return externallyDeterminedTimerInvocationCost;
	}

	public SortedMap<Integer, SortedMap<Long, Integer>> getHistogramElementsByProcess() {
		return processed_histogramElementsByProcess;
	}

	public SortedMap<Long, SortedMap<Integer, Integer>> getHistogramElementsByValue() {
		return processed_histogramElementsByValue;
	}

	public StatisticalDescription getInvocationCost() {
		return invocationCost;
	}

	public List<StatisticalDescription> getProcessCharacterisations() {
		return processed_processCharacterisations;
	}

	public StatisticalDescription getProcessed_allMeasurementsCharacterisation() {
		return processed_allMeasurementsCharacterisation;
	}

	public SortedMap<Long, Integer> getProcessed_allMeasurementsHistogram() {
		return processed_allMeasurementsHistogram;
	}

	public List<MeasurementGroupsCluster> getProcessed_clusters() {
		return processed_clusters;
	}

	public SortedMap<Integer, SortedMap<Long, Integer>> getProcessed_histogramElementsByProcess() {
		return processed_histogramElementsByProcess;
	}

	public SortedMap<Long, SortedMap<Integer, Integer>> getProcessed_histogramElementsByValue() {
		return processed_histogramElementsByValue;
	}

	public List<StatisticalDescription> getProcessed_processCharacterisations() {
		return processed_processCharacterisations;
	}

	public long[][] getRaw_allInitialMeasurements() {
		return raw_allInitialMeasurements;
	}

	public long[] getRaw_allMeasurementsAppendedAndSorted() {
		return raw_allMeasurementsAppendedAndSorted;
	}

	public boolean isFrequencyOfTimerSpecifiedExternally() {
		return frequencyOfTimerSpecifiedExternally;
	}

	public boolean isInvocationCostOfTimerSpecifiedExternally() {
		return invocationCostOfTimerSpecifiedExternally;
	}

	public void setAccuracy(long accuracy) {
		this.accuracy = accuracy;
	}

	public void setAllInitialMeasurements(long[][] timerMeterMeasurements) {
		this.raw_allInitialMeasurements = timerMeterMeasurements;
	}

	public void setAllMeasurementsAppendedAndSorted(long[] allMeasurementsAppendedAndSorted) {
		this.raw_allMeasurementsAppendedAndSorted = allMeasurementsAppendedAndSorted;
	}

	public void setAllMeasurementsCharacterisation(StatisticalDescription allMeasurementsCharacterisation) {
		this.processed_allMeasurementsCharacterisation = allMeasurementsCharacterisation;
	}

	public void setAllMeasurementsHistogram(SortedMap<Long, Integer> allMeasurementsHistogram) {
		this.processed_allMeasurementsHistogram = allMeasurementsHistogram;
	}

	public void setClusters(List<MeasurementGroupsCluster> clusters) {
		this.processed_clusters = clusters;
	}

	public void setExternallyDeterminedTimerFrequency(double frequency) {
		if(frequencyOfTimerSpecifiedExternally==false){
			this.externallyDeterminedTimerFrequency = frequency;
			frequencyOfTimerSpecifiedExternally = true;
		}else{
			logger.severe("Timer externallyDeterminedTimerFrequency " +
					"already specified, cannot be overwritten");
		}
	}

	public void setExternallyDeterminedTimerInvocationCost(double cost) {
		if(invocationCostOfTimerSpecifiedExternally==false){
			this.externallyDeterminedTimerInvocationCost = cost;
			invocationCostOfTimerSpecifiedExternally = true;
		}else{
			logger.severe("Timer externallyDeterminedTimerInvocationCost " +
					"already specified, cannot be overwritten");
		}
	}

	public void setHistogramElementsByProcess(
			SortedMap<Integer, SortedMap<Long, Integer>> histogramElementsByProcessAcrossAllProcesses) {
		this.processed_histogramElementsByProcess = histogramElementsByProcessAcrossAllProcesses;
	}

	public void setHistogramElementsByValue(
			SortedMap<Long, SortedMap<Integer, Integer>> histogramElementsByValueAcrossAllProcesses) {
		this.processed_histogramElementsByValue = histogramElementsByValueAcrossAllProcesses;
	}

	public void setInvocationCost(StatisticalDescription invocationCost) {
		this.invocationCost = invocationCost;
	}

	public void setProcessCharacterisations(List<StatisticalDescription> processCharacterisations) {
		this.processed_processCharacterisations = processCharacterisations;
	}

	public void setProcessed_allMeasurementsCharacterisation(
			StatisticalDescription processed_allMeasurementsCharacterisation) {
		this.processed_allMeasurementsCharacterisation = processed_allMeasurementsCharacterisation;
	}

	public void setProcessed_allMeasurementsHistogram(
			SortedMap<Long, Integer> processed_allMeasurementsHistogram) {
		this.processed_allMeasurementsHistogram = processed_allMeasurementsHistogram;
	}

	public void setProcessed_clusters(
			List<MeasurementGroupsCluster> processed_clusters) {
		this.processed_clusters = processed_clusters;
	}

	public void setProcessed_histogramElementsByProcess(
			SortedMap<Integer, SortedMap<Long, Integer>> processed_histogramElementsByProcess) {
		this.processed_histogramElementsByProcess = processed_histogramElementsByProcess;
	}

	public void setProcessed_histogramElementsByValue(
			SortedMap<Long, SortedMap<Integer, Integer>> processed_histogramElementsByValue) {
		this.processed_histogramElementsByValue = processed_histogramElementsByValue;
	}

	public void setProcessed_processCharacterisations(
			List<StatisticalDescription> processed_processCharacterisations) {
		this.processed_processCharacterisations = processed_processCharacterisations;
	}

	public void setRaw_allInitialMeasurements(long[][] raw_allInitialMeasurements) {
		this.raw_allInitialMeasurements = raw_allInitialMeasurements;
	}

	public void setRaw_allMeasurementsAppendedAndSorted(
			long[] raw_allMeasurementsAppendedAndSorted) {
		this.raw_allMeasurementsAppendedAndSorted = raw_allMeasurementsAppendedAndSorted;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
        sb.append("Accuracy=").append(this.accuracy).append(",\n");
		if(this.invocationCost!=null){
            sb.append("min. invocation cost=").append(
                    this.invocationCost.getMin()).append(",\n");
            sb.append("med. invocation cost (math)=").
                    append(this.invocationCost.getMedian()).append(",\n");
            sb.append("med. invocation cost (phys)=").
                    append(this.invocationCost.getMedian_real_element()).
                    append(",\n");
            sb.append("avg. invocation cost=").
                    append(this.invocationCost.getMean()).append(",\n");
            sb.append("max. invocation cost=").
                    append(this.invocationCost.getMax()).append(",\n");
            sb.append("variance of invocation cost=").
                    append(this.invocationCost.getVariance()).append(".");
		}else{
			//TODO
		}
		if(frequencyOfTimerSpecifiedExternally){
            sb.append("Externally obtained timer frequency =").
                    append(this.externallyDeterminedTimerFrequency).
                    append(",\n");
		}else{
			//TODO
		}
		if(invocationCostOfTimerSpecifiedExternally){
            sb.append("Externally obtained timer invocation cost=").
                    append(this.externallyDeterminedTimerInvocationCost).
                    append(", \n");
		}else{
			//TODO
		}
		return sb.toString();
	}

	public String toString_extensive(){
        //TODO add a "very extensive version that iterates over all
        // histograms and characterisation
		StringBuilder sb = new StringBuilder();
        sb.append(this).append('\n');

        sb.append("Characteristics accross all measurements: ").
                append(this.processed_allMeasurementsCharacterisation).
                append('\n');
        sb.append("Histogram: ").
                append(this.processed_allMeasurementsHistogram).append('\n');
        sb.append("Clusters: ").append(this.processed_clusters).append('\n');
        sb.append("First of HistogramColumns-By-Process: ").append(
                this.processed_histogramElementsByProcess.get(
                this.processed_histogramElementsByProcess.firstKey())).
                append('\n');
        sb.append("Last of HistogramColumns-By-Process: ").
                append(this.processed_histogramElementsByProcess.get(
                this.processed_histogramElementsByProcess.size() - 1)).
                append('\n');
        sb.append("First of HistogramColumns-By-Value: ").
                append(this.processed_histogramElementsByValue.get(
                this.processed_histogramElementsByValue.firstKey())).
                append('\n');
        sb.append("Last of HistogramColumns-By-Value: ").
                append(this.processed_histogramElementsByValue.get(
                this.processed_histogramElementsByValue.lastKey())).
                append('\n');
        sb.append("First process characterisation: ").
                append(this.processed_processCharacterisations.get(0)).
                append('\n');
        sb.append("Last process characterisation: ").append(
                this.processed_processCharacterisations.get(
                this.processed_processCharacterisations.size() - 1)).
                append('\n');

        sb.append("Stat. analysis of smallest diff value across all processes: ").
                append(this.computeClusterWidthCharacteristicsAcrossProcesses(
                this.processed_allMeasurementsCharacterisation.getMin())).
                append('\n');
        sb.append("Stat. analysis of median cluster value across all processes: ").
                append(this.computeClusterWidthCharacteristicsAcrossProcesses(
                this.processed_allMeasurementsCharacterisation.
                        getMedian_real_element()));
		return sb.toString();
	}

}
