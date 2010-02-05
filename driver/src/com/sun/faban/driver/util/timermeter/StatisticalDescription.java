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
import java.util.logging.Logger;

/**
 * A container for statistical values.
 * The parameter-taking constructor checks the passed values.
 * The field <code>median_real_element</code> has the following meaning:
 * if the data for which this <code>StatisticalDescription</code> was created
 * has an even number of elements, the conventional median is the average of
 * the <b>two</b> middle elements - in such a case,
 * <code>median_real_element</code> carries the value of the "left" element of
 * these two. If there is an uneven number of elements, the value of field
 * <code>median_real_element</code> should <code>equal</code> that of field
 * <code>median</code>.
 * @author Michael Kuperberg
 * @version 0.9
 */
public class StatisticalDescription implements Serializable {

//	private enum STATISTICAL_DATA_TYPES{INTEGER,LONG,FLOAT,DOUBLE};
	private static final long serialVersionUID = 1L;

	private Logger logger;

	private long max;

	private double mean;

	private double median;

	private long median_real_element;

	private long min;

	private double standardDeviation;

	private double variance;

	public StatisticalDescription(){
		this(
				Long.MIN_VALUE,
				Double.NaN,
				Double.NaN,
				Long.MIN_VALUE,
				Long.MAX_VALUE,
				Double.NaN,
				Double.NaN
			);
	}

	public StatisticalDescription(
			Long max,
			Double mean,
			Double median,
			Long median_real_element,
			Long min,
			Double standardDeviation,
			Double variance
			) {
		super();
		logger = Logger.getLogger(this.getClass().getName());
		if(!max.equals(Long.MIN_VALUE) && max<mean){
			logger.severe("Max smaller than mean - impossible, skipping initialisation");
			return;
		}
		if(!max.equals(Long.MIN_VALUE) && max<median){
			logger.severe("Max smaller than median - impossible, skipping initialisation");
			return;
		}
		if(!max.equals(Long.MIN_VALUE) && max<median_real_element){
			logger.severe("Max smaller than median_real_element - impossible, skipping initialisation");
			return;
		}
		if(!max.equals(Long.MIN_VALUE) && max<min){
			logger.severe("Max smaller than min - impossible, skipping initialisation");
			return;
		}
		if(!mean.equals(Double.NaN) && mean<min){
			logger.severe("Mean smaller than min - impossible, skipping initialisation");
			return;
		}
		if(!median.equals(Double.NaN) && median<median_real_element){
			logger.severe("Median smaller than median_real_element - impossible, skipping initialisation");
			return;
		}
		if(!median.equals(Double.NaN) && median<min){
			logger.severe("Median smaller than min - impossible, skipping initialisation");
			return;
		}
		if(!standardDeviation.equals(Double.NaN) && standardDeviation<0){
			logger.severe("Standard deviation cannot be negative, skipping initialisation");
			return;
		}
		if(!variance.equals(Double.NaN) && variance<0){
			logger.severe("Variance cannot be negative, skipping initialisation");
			return;
		}
		if(!variance.equals(Double.NaN) && !variance.equals(standardDeviation*standardDeviation)){
			logger.severe("Variance should be a square of the standard deviation, skipping initialisation");
			return;
		}
		this.max = max;
		this.mean = mean;
		this.median = median;
		this.median_real_element = median_real_element;
		this.min = min;
		this.standardDeviation = standardDeviation;
		this.variance = variance;
	}

	public boolean checkConsistency(){
		boolean consistency = true;
		if(!new Long(max).equals(Long.MIN_VALUE)){
			logger.severe("Max is Long.MIN_VALUE");
			consistency=false;
		}
		if(!new Long(max).equals(Long.MAX_VALUE)){
			logger.severe("Max is Long.MAX_VALUE");
			consistency=false;
		}
		if(!new Double(mean).equals(Double.NaN)){
			logger.severe("Mean is Double.NaN");
			consistency=false;
		}
		if(!new Double(median).equals(Double.NaN)){
			logger.severe("Median is Double.NaN");
			consistency=false;
		}
		if(!new Long(median_real_element).equals(Long.MIN_VALUE)){
			logger.severe("Median_real_element is Long.MIN_VALUE");
			consistency=false;
		}
		if(!new Long(median_real_element).equals(Long.MAX_VALUE)){
			logger.severe("Median_real_element is Long.MAX_VALUE");
			consistency=false;
		}
		if(!new Long(min).equals(Long.MIN_VALUE)){
			logger.severe("Min is Long.MIN_VALUE");
			consistency=false;
		}
		if(!new Long(min).equals(Long.MAX_VALUE)){
			logger.severe("Min is Long.MAX_VALUE");
			consistency=false;
		}
		if(!new Double(standardDeviation).equals(Double.NaN)){
			logger.severe("Standard deviation is Double.NaN");
			consistency=false;
		}
		if(!new Double(variance).equals(Double.NaN)){
			logger.severe("Variance is Double.NaN");
			consistency=false;
		}
		if(max<mean){
			logger.severe("Max smaller than mean - impossible, skipping initialisation");
			consistency=false;
		}
		if(max<median){
			logger.severe("Max smaller than median - impossible, skipping initialisation");
			consistency=false;
		}
		if(max<median_real_element){
			logger.severe("Max smaller than median_real_element - impossible, skipping initialisation");
			consistency=false;
		}
		if(max<min){
			logger.severe("Max smaller than min - impossible, skipping initialisation");
			consistency=false;
		}
		if(mean<min){
			logger.severe("Mean smaller than min - impossible, skipping initialisation");
			consistency=false;
		}
		if(median<median_real_element){
			logger.severe("Median smaller than median_real_element - impossible, skipping initialisation");
			consistency=false;
		}
		if(median<min){
			logger.severe("Median smaller than min - impossible, skipping initialisation");
			consistency=false;
		}
		if(standardDeviation<0){
			logger.severe("Standard deviation cannot be negative, skipping initialisation");
			consistency=false;
		}
		if(variance<0){
			logger.severe("Variance cannot be negative, skipping initialisation");
			consistency=false;
		}
		if(!(new Double(variance).equals(standardDeviation*standardDeviation))){
			logger.severe("Variance should be a square of the standard deviation - skipping initialisation");
			consistency=false;
		}
		return consistency;
	}

	public boolean equals(Object obj){//TODO test
		if(!(obj instanceof StatisticalDescription)){
			return false;
		}else{
			StatisticalDescription sds = (StatisticalDescription) obj;
			if(sds.max==this.max
					&& new Double(sds.mean).equals(this.mean)
					&& new Double(sds.median).equals(this.median)
					&& sds.median_real_element == this.median_real_element
					&& sds.min == this.min
					&& new Double(sds.standardDeviation).equals(this.standardDeviation)
					&& new Double(sds.variance).equals(this.variance)
					){
				return true;
			}else{
				return false;
			}
		}
	}

	public Logger getLogger() {
		return logger;
	}

	public long getMax() {
		return max;
	}

	public double getMean() {
		return mean;
	}

	public double getMedian() {
		return median;
	}

	public long getMedian_real_element() {
		return median_real_element;
	}

	public long getMin() {
		return min;
	}

	public double getStandardDeviation() {
		return standardDeviation;
	}

//	private void setVariance(double pVariance) {
//		if (variance < 0) {
//			logger.debug("Variance can't be less than zero");
//		} else {
//			this.variance = pVariance;
//			this.standardDeviation = Math.pow(this.variance, 0.5);
//		}
//	}

	public double getVariance() {
		return variance;
	}

	public void setMax(long max) {
		this.max = max;
	}

	public void setMean(double mean) {
		this.mean = mean;
	}

	public void setMedian(double median) {
		this.median = median;
	}

	public void setMedian_real_element(long median_real_element) {
		this.median_real_element = median_real_element;
	}

	public void setMin(long min) {
		this.min = min;
	}

	public void setStandardDeviation(double standardDeviation) {
		this.standardDeviation = standardDeviation;
	}

	public void setVariance(double variance) {
		this.variance = variance;
	}

	/**
	 * Shallow because standard deviation and variance are not compared.
	 * @param obj
	 * @return
	 */
	public boolean shallowEquals(Object obj){//TODO test
		if(!(obj instanceof StatisticalDescription)){
			return false;
		}else{
			StatisticalDescription sds = (StatisticalDescription) obj;
			if(sds.max==this.max
					&& new Double(sds.mean).equals(this.mean)
					&& new Double(sds.median).equals(this.median)
					&& sds.median_real_element == this.median_real_element
					&& sds.min == this.min
					){
				return true;
			}else{
				return false;
			}
		}
	}

	@Override
	public String toString(){
		String retString =
			"Max="+max+", " +
			"Mean="+mean+", " +
			"Median="+median+", " +
			"Median_real_element="+median_real_element+", " +
			"Min="+min+", " +
			"SD="+standardDeviation+", " +
			"Variance="+variance+".";
		return retString;
	}

}
