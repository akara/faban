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
import java.util.List;

public class DetailReport {
    ArrayList<Double> timeList = new ArrayList<Double>();
    ArrayList<Double> opThruList[];  // each array element is for one operation
    double opAvgThruput[];  // avg. thruput for whole run for each operation
    String operations[];    // names of operations
    ArrayList<Double> thruput = new ArrayList<Double>();

    ArrayList<Double> respList[] = null;   // each array element is RT for one operation
    ArrayList<Integer> distRespList[] = null; // for response time distribution
    ArrayList<Double> distTimeList = new ArrayList<Double>(); // time axis for RT distribution
    boolean hasNextDriver = false;

    private static final Double ZERO = new Double(0.0);

    String detFile;
    int numOps = 0;

    public DetailReport(String detFile, int targetDriver) throws IOException {
        this.detFile = detFile;
        processThruput(targetDriver);
        processResponseTimes(targetDriver);
    }

    /*
    * Parse detail.xan for throughput info
    * Get the total thruput per interval by summing across all operations
    *
    */
    int timeInterval;   // holds time interval for x-axis

    private void processThruput(int targetDriver) throws IOException {
        BufferedReader bi;
        try {
            bi = new BufferedReader(new FileReader(detFile));
        } catch (FileNotFoundException e) {
            // should never come here as file has already been verified to exist
            return;
        }
        String line;
        int driverCount = 0;
        while ((line = bi.readLine()) != null) {
            //skip to content for a driver
            if (line.endsWith("Throughput")) {
                driverCount += 1;
            }
            if (driverCount < targetDriver) {
                continue;
            }
            if (line.startsWith("Time (s)")) {
                // get names of operations
                String token[] = line.split("\\s{2}");
                operations = new String[token.length - 1];  //ignore 'Time (s)' which is 2 tokens
                for (int i = 1; i < token.length; i++)
                    operations[i - 1] = token[i];
                break;
            }
        }
        // Process thruput histogram
        bi.readLine();  //Skip dashes line
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

        while ((line = bi.readLine()) != null) {
            if (operations != null && line.endsWith("Throughput")) {
                hasNextDriver = true;
                break;
            }
        }
        // compute averages
        for (int i = 0; i < opAvgThruput.length; i++) {
            opAvgThruput[i] /= timeList.size();
        }
        bi.close();
        return;
    }

    private void processResponseTimes(int targetDriver) throws IOException {

        BufferedReader bi;
        try {
            bi = new BufferedReader(new FileReader(detFile));
        } catch (FileNotFoundException e) {
            // should never come here as file has already been verified to exist
            return;
        }
        String line;
        int driverCount = 0;
        while ((line = bi.readLine()) != null) {
            if (line.endsWith("Throughput")) {
                driverCount += 1;
            }
            if (driverCount < targetDriver) {
                continue;
            }
            if (!line.matches("Section:.* Response Times.*")) {
                continue;
            }
            bi.readLine();
            line = bi.readLine();
            if (line.startsWith("Time (s)")) {
                // get names of operations
                String token[] = line.split("\\s{2}");
                List<String> opNames = new ArrayList();
                for (int i = 1; i < token.length; i++) {
                    if (!token[i].trim().equals("")) {
                        opNames.add(token[i]);
                    }
                }
                operations = new String[opNames.size()];
                operations = opNames.toArray(operations);
                respList = new ArrayList[opNames.size()];
                for (int i = 0; i < opNames.size(); i++) {
                    respList[i] = new ArrayList<Double>();
                }
                bi.readLine();  // Skip dashes after header line
                break;
            }
        }
        // Process response time data
        while ((line = bi.readLine()) != null && line.trim().length() > 0) {
            // This line's output should look like (one value per operation):
            //0   527.30  197.90     661.90       491.20         55.80      17.30     41.30
            String token[] = line.split("\\s+");

            for (int j = 0; j < token.length - 1; j++) {
                double dop = Double.parseDouble(token[j + 1]);
                respList[j].add(dop);
            }
        }

        // Below the response time over time data, is the frequency distribution of response times
        distRespList = new ArrayList[operations.length];
        while ((line = bi.readLine()) != null) {
            if (line.matches("Section:.* Frequency Distribution of Response Times.*")) {
                bi.readLine();  // skip Display Line
                bi.readLine();  // skip header line and dashes
                bi.readLine();
                break;
            }
        }

        //Process RT distribution data
        boolean first = true;
        while ((line = bi.readLine()) != null && line.trim().length() > 0) {
            // This line's output should look like (one value per operation):
            //0.025000   0  1    2       1        3      0     1
            String token[] = line.split("\\s+");
            distTimeList.add(Double.parseDouble(token[0]));

            for (int j = 0; j < token.length - 1; j++) {
                if (first)
                    distRespList[j] = new ArrayList<Integer>();
                Integer val = Integer.parseInt(token[j + 1]);
                distRespList[j].add(val);
            }
            if (first)
                first = false;
        }
        bi.close();
        return;
    }

    /**
    * This method returns the operation name
    */
    public String getOpName(int opIdx) {
        if (operations == null || operations.length < opIdx)
            return null;
        else
            return operations[opIdx];
    }

    /**
    * Return overall throughput over run
    *
    * @return ArrayList<Double> array of thruput values, one per interval
    */
    public ArrayList<Double> getThruput() {
        return (thruput);
    }

    /**
    * Return overall throughput over run
    *
    * @return ArrayList<Double> array of thruput values, one per interval
    */
    public ArrayList<Double> getThruput(int expected) {
        return pad(getThruput(), expected);
    }
        
    private ArrayList<Double> pad(ArrayList<Double> values, int expected) {
        ArrayList paddedTimes = null;
        if (values.size() == expected){
            paddedTimes = values;
        } else {
            paddedTimes = new ArrayList(expected);
            paddedTimes.addAll(values);
            for ( int i = values.size(); i < expected; i++){
                paddedTimes.add(ZERO);
            }
        }
        return paddedTimes;
    }

    /**
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

    /**
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

    /**
    * This method returns the Response Time over time for the specified operation
    * @param int opIdx 0-<numOps-1> to select operation
    * @return ArrayList<Double> - all RT for this operation
    */
    public ArrayList<Double> getOpRT(int opIdx) throws IOException {
        if (respList == null || respList.length < opIdx)
            return null;
        else
            return (respList[opIdx]);
    }

   /**
    * This method returns the Response Time distribution for the specified operation
    * @param int opIdx 0-<numOps-1> to select operation
    * @return ArrayList<Double> - RT distribution for this operation
    */
    public ArrayList<Integer> getOpRTDist(int opIdx) throws IOException {
        if (distRespList == null || distRespList.length < opIdx)
            return null;
        else
            return (distRespList[opIdx]);
    }

    /**
    * This method returns the Time array for the x-axis
    * @return ArrayList<Double> - all time values in detail file
    */
    public ArrayList<Double> getTimes() throws IOException {
        return (timeList);
    }

    /**
    * This method returns the Time array for the x-axis for the RT distribution
    * @return ArrayList<Double> - all time values in detail file for RT distribution
    */
    public ArrayList<Double> getTimesDist() throws IOException {
        return (distTimeList);
    }

    /**
    * This method returns the Time array for the x-axis for the RT distribution
    * @param expected - expected number of values
    * @return ArrayList<Double> - all time values in detail file for RT distribution
    */
    public ArrayList<Double> getTimesDist(int expected) throws IOException {
        return pad(distTimeList, expected);
    }


}
