package com.sun.faban.driver.engine;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.MatchResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class VariableLoadHandler implements Iterator {
	
	private ArrayList<VariableLoad> load = new ArrayList<VariableLoad>();
	private int index = 0;
	
	public static class VariableLoad {
		
		public int runTime;
		public int threadCount;
		
		VariableLoad(int runTime, int threadCount) {
			this.runTime = runTime;
			this.threadCount = threadCount;
		}
		
	}
	
	public boolean hasNext() {
		if(index < load.size()) {
			return true;
		} else {
			return false;
		}
	}
	
	public VariableLoad next() {
		return load.get(index++);
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	VariableLoadHandler(String path) throws FileNotFoundException {
		File loadConfiguration = new File(path);
		Scanner loadScanner = new Scanner(loadConfiguration);	
		while(loadScanner.hasNext()) {
			String line = loadScanner.nextLine();
			Scanner lineScanner = new Scanner(line);
			lineScanner.findInLine("(\\d+),(\\d+)");
			MatchResult result = lineScanner.match();
			load.add(new VariableLoad(Integer.parseInt(result.group(1)), Integer.parseInt(result.group(2))));
			// for (int i=1; i<=result.groupCount(); i++)
			//	System.out.println(result.group(i));
			lineScanner.close();
		}
		loadScanner.close();
	}
	
	public static void main(String args[]) {
		try {
			VariableLoadHandler x = new VariableLoadHandler(args[0]);
			while(x.hasNext()) {
				VariableLoad l = x.next();
				System.out.println("Run time: " + l.runTime);
				System.out.println("Thread count: " + l.threadCount);
			}
		} catch(FileNotFoundException e) {
			System.err.println("Load configuration file not found!");
			System.exit(1);
		}
	}
	
}