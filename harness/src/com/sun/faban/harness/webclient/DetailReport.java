/**
 * Copyright 2012, Shanti Subramanyam. All Rights Reserved.
 *
 */
package com.sun.faban.harness.webclient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class DetailReport {
    ArrayList<Double> timeList = new ArrayList<Double>();
    ArrayList<Double> opThruList[];  // each array element is for one operation
    double opAvgThruput[];  // avg. thruput for whole run for each operation
    String operations[];    // names of operations
    ArrayList<Double> respList[] = null;   // each array element is RT for one operation
    ArrayList<Double> thruput = new ArrayList<Double>();
    String detFile;
    int numOps = 0;

    public DetailReport(String detFile) throws IOException {
        this.detFile = detFile;
        processThruput();
        processResponseTimes();
    }

    /*
    * Parse detail.xan for throughput info
    * Get the total thruput per interval by summing across all operations
    *
    */
    int timeInterval;   // holds time interval for x-axis

    private void processThruput() throws IOException {
        BufferedReader bi;
        try {
            bi = new BufferedReader(new FileReader(detFile));
        } catch (FileNotFoundException e) {
            // should never come here as file has already been verified to exist
            return;
        }
        String line;
        while ((line = bi.readLine()) != null) {
            if (line.startsWith("Time")) {
                // get names of operations
                String token[] = line.split("\\s+");
                operations = new String[token.length - 2];  //ignore 'Time (s)' which is 2 tokens
                for (int i = 2; i < token.length; i++)
                    operations[i - 2] = token[i];
                break;
            }
        }
        // Process thruput histogram
        bi.readLine();  //Skip dashes line
        int t = 0;
        // initialize array to hold individual operation thruputs
        opThruList = new ArrayList[operations.length];
        opAvgThruput = new double[operations.length];
        for (int j = 0; j < operations.length; j++) {
            opThruList[j] = new ArrayList<Double>();
        }

        while ((line = bi.readLine()) != null && line.trim().length() > 0) {

            // This line's output should look like (one value per operation):
            //0   527.30  197.90     661.90       491.20         55.80      17.30     41.30
            String token[] = line.split("\\s+");

            double d = 0.0;
            timeList.add(Double.parseDouble(token[0]));


            for (int j = 0; j < token.length - 1; j++) {
                double dop = Double.parseDouble(token[j + 1]);
                opAvgThruput[j] += dop;
                opThruList[j].add(dop);
                d += dop;  // to compute total thruput across all ops
            }
            thruput.add(d);
        }

        // compute averages
        for (int i = 0; i < opAvgThruput.length; i++) {
            opAvgThruput[i] /= timeList.size();
        }
        return;
    }

    private void processResponseTimes() throws IOException {

        BufferedReader bi;
        try {
            bi = new BufferedReader(new FileReader(detFile));
        } catch (FileNotFoundException e) {
            // should never come here as file has already been verified to exist
            return;
        }
        String line;
        while ((line = bi.readLine()) != null) {
            if (line.matches("Section:.* Response Times.*")) {
                bi.readLine();  //Skip Display line
                line = bi.readLine();
                // get names of operations
                String token[] = line.split("\\s+");
                operations = new String[token.length - 2];  //ignore 'Time (s)'

                respList = new ArrayList[operations.length];
                for (int i = 2; i < token.length; i++) {
                    operations[i - 2] = token[i];
                    respList[i - 2] = new ArrayList<Double>();
                }
                bi.readLine();  // Skip dashes after header line
                break;
            }
        }
        // Process response time data
        int t = 0;

        while ((line = bi.readLine()) != null && line.trim().length() > 0) {

            // This line's output should look like (one value per operation):
            //0   527.30  197.90     661.90       491.20         55.80      17.30     41.30
            String token[] = line.split("\\s+");

            double d = 0.0;
            //timeList.add(Double.parseDouble(token[0]));        Already added in getThroughput()

            for (int j = 0; j < token.length - 1; j++) {
                double dop = Double.parseDouble(token[j + 1]);
                respList[j].add(dop);
            }
        }
    }

    /*
    * This method returns the operation name
    */

    public String getOpName(int opIdx) {
        if (operations == null || operations.length < opIdx)
            return null;
        else
            return operations[opIdx];
    }

    /*
    * Return overall throughput over run
    *
    * @return ArrayList<Double> array of thruput values, one per interval
    */

    public ArrayList<Double> getThruput() {
        return (thruput);
    }

    /*
    * This method returns the avg. thruput for the specified operation
    * @param int opIdx 0-<numOps-1> to select operation
    * @return double - avg. throughput for this operation over run
    */

    public double getOpAvgThruput(int opIdx) {
        if (opAvgThruput == null || opAvgThruput.length < opIdx)
            return 0.0;
        else
            return (opAvgThruput[opIdx]);
    }

    /*
    * This method returns the thruput over time for the specified operation
    * @param int opIdx 0-<numOps-1> to select operation
    * @return ArrayList<Double> - all throughput for this operation
    */

    public ArrayList<Double> getOpThruput(int opIdx) {
        if (opThruList == null || opThruList.length < opIdx)
            return null;
        else
            return (opThruList[opIdx]);
    }

    /*
    * This method returns the Response Time over time for the specified operation
    * @param int opIdx 0-<numOps-1> to select operation
    * @return ArrayList<Double> - all throughput for this operation
    */

    public ArrayList<Double> getOpRT(int opIdx) throws IOException {
        if (respList == null || respList.length < opIdx)
            return null;
        else
            return (respList[opIdx]);
    }

    /*
    * This method returns the Time array for the x-axis
    * @return ArrayList<Double> - all time values in detail file
    */

    public ArrayList<Double> getTimes() throws IOException {
        return (timeList);
    }
}
