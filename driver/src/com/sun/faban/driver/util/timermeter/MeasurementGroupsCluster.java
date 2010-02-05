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
import java.util.*;

/**
 * A cluster which consists of key-value pairs that "aggregate" same-valued cluster elements.
 * That is, instead of containing two items with the value 10, this cluster
 * contains one entry with key 10 and value 2. Elements cannot be removed from the cluster.
 * TODO optimise through "lazy computation" of median value and slot size
 * @author Michael Kuperberg
 * TODO test me
 * TODO integrate with WEKA and similar?
 */
public class MeasurementGroupsCluster implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * the cluster elements (internal representation)
	 */
	private SortedMap<Long,Integer> clusterElements;

	private long clusterGroupSizeMaximum;

	private double clusterGroupSizeMean;

//	private long clusterGroupSizeMinimum;

	private long clusterGroupSizeSum;

	private List<Integer> counts;

	private long totalClusterValuesSum;

	public MeasurementGroupsCluster() {
		super();
		this.clusterElements = new TreeMap<Long, Integer>();
		this.clusterGroupSizeMaximum = Long.MIN_VALUE;
		this.clusterGroupSizeMean = 0D;
//		this.clusterGroupSizeMinimum = Long.MAX_VALUE;
		this.clusterGroupSizeSum = 0L;
		this.counts = new ArrayList<Integer>();
		this.totalClusterValuesSum = 0L;
	}

	public MeasurementGroupsCluster(Long key, Integer value){
		this();
		this.addData(key, value);
	}

	public MeasurementGroupsCluster(SortedMap<Long,Integer> clusterElements){
			this();
			this.addData(clusterElements);
//		this.clusterElements = clusterElements;
//		Long currKey;
//		Integer currCount;
//		for (Iterator<Long> iterator = this.clusterElements.keySet().iterator(); iterator.hasNext();) {
//			currKey = iterator.next();
//			currCount = this.clusterElements.get(currKey);
//			this.totalClusterElementsCount += currCount;
//			this.totalClusterValuesSum += currCount*currKey;
//		}
	}

	/**
	 * TODO check for negatives?
	 * @param key
	 * @param value
	 * @return
	 */
	public synchronized double addData(Long key, Integer value){
		this.totalClusterValuesSum += value*key;
		this.clusterGroupSizeSum += value;
		Integer entry = this.clusterElements.get(key);
		Integer valuePutIntoClusterElements = 0;
//		boolean valueExisted = false;
		if(entry!=null){
			valuePutIntoClusterElements = entry+value;
//			valueExisted = true;
			this.counts.remove(entry);//because the entry with this key existed before!
			this.counts.add(valuePutIntoClusterElements);
		}else{
			valuePutIntoClusterElements = value;
//			valueExisted = false;
			this.counts.add(valuePutIntoClusterElements);
		}
		this.clusterElements.put(key, valuePutIntoClusterElements); //if the key already exists, the data gets overwritten as intended here
		this.clusterGroupSizeMean = (double) this.clusterGroupSizeSum/(double) this.clusterElements.size();

		if(valuePutIntoClusterElements>this.clusterGroupSizeMaximum){
			this.clusterGroupSizeMaximum=valuePutIntoClusterElements;
		}
//		the following computation does not work because it remembers minimum sizes that later might be increase by adding to a cluster...
//		if(valuePutIntoClusterElements<this.clusterGroupSizeMinimum){
//			this.clusterGroupSizeMinimum=valuePutIntoClusterElements;
//		}
		return this.getClusterValueMean();
	}

	public synchronized double addData(Map<Long, Integer> keyValueMap){
		Long currKey;
		Integer currValue;
		for (Iterator<Long> iterator = keyValueMap.keySet().iterator(); iterator.hasNext(); ) {
			currKey = iterator.next();
			currValue = keyValueMap.get(currKey);
			this.addData(currKey, currValue);
		}
		return this.getClusterValueMean();
	}

	public int getClassesNumberInCluster(){
		if(this.clusterElements==null || this.clusterElements.isEmpty()){
			return 0;
		}else{
			return this.clusterElements.size();//TODO check that there are no zero-counts inside...
		}
	}

	public synchronized long getClusterGroupSizeMaximum(){
		return this.clusterGroupSizeMaximum;
	}

	public synchronized double getClusterGroupSizeMean(){
		return this.clusterGroupSizeMean;
	}

	public synchronized long getClusterGroupSizeMedian(){
		Collections.sort(this.counts);
		return this.counts.get(counts.size()/2);
	}

	public synchronized long getClusterGroupSizeMinimum(){
		Collections.sort(this.counts);
		return this.counts.get(0);
	}

	public synchronized long getClusterGroupSizeSum(){
		return this.clusterGroupSizeSum;
	}

	/**
	 * //TODO test me
	 * @return
	 */
	public synchronized long getClusterValueMaximum(){
		Long[] asArray = (Long[]) clusterElements.keySet().toArray(new Long[0]);
		return asArray[clusterElements.size()-1];
	}

	public synchronized double getClusterValueMean(){
		return ((double) this.totalClusterValuesSum/ (double) this.clusterGroupSizeSum);
	}

	/**
	 * TODO more effective median *re*computation? --> think about it!
	 * TODO test me
	 * @return
	 */
	public synchronized double getClusterValueMedian(){
		if(this.clusterGroupSizeSum==0){
			return 0D;//new double[]{0D,0D,0D};
		}
//		totalClusterValuesSum=0;
		int currentClusterElementsNumber = 0;
		double clusterMedian=-1D;
		Long currKey;
		Integer currValue;
		boolean medianNotFound = true;

		for (Iterator<Long> iterator = clusterElements.keySet().iterator(); iterator.hasNext();) {
			currKey = iterator.next();
			currValue = clusterElements.get(currKey);
//			totalClusterValuesSum += currKey*currValue;
			currentClusterElementsNumber+=currValue;
			if(medianNotFound && currentClusterElementsNumber>=((clusterGroupSizeSum/2)-1)){
				clusterMedian = currKey;
				medianNotFound = false;
			}
		}
		return /*new double[]{
				totalClusterValuesSum/clusterGroupSizeSum,
				*/clusterMedian/*,
				totalClusterValuesSum
		}*/;
	}

	/**
	 * TODO test me
	 * @return
	 */
	public synchronized long getClusterValueMinimum(){
		return clusterElements.keySet().iterator().next(); //TODO reengineer for performance
	}

	public synchronized long getClusterValueSum(){
		return this.totalClusterValuesSum;
	}

	public String toString(){
		return this.clusterElements.toString();
	}
}
