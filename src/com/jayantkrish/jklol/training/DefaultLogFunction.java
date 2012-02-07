package com.jayantkrish.jklol.training;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.jayantkrish.jklol.models.FactorGraph;
import com.jayantkrish.jklol.util.Assignment;

/**
 * A simple default logging function.
 */ 
public class DefaultLogFunction extends AbstractLogFunction {
  
  private final int logInterval;

  public DefaultLogFunction() { super(); logInterval = 1; }
  
  public DefaultLogFunction(int logInterval) { 
    super(); 
    this.logInterval = logInterval; 
  }
  
	@Override
	public void log(Assignment example, FactorGraph graph) {
	  System.out.println("?.?: example: " + graph.assignmentToObject(example));
	}

	@Override
	public void log(int iteration, int exampleNum, Assignment example, FactorGraph graph) {
	  if (iteration % logInterval == 0) {
	    String prob = "";
	    if (example.containsAll(graph.getVariables().getVariableNums())) {
	      prob = Double.toString(graph.getUnnormalizedProbability(example));
	    } 
	    System.out.println(iteration + "." + exampleNum + " " + prob + ": example: " + graph.assignmentToObject(example));
	  }
	}

	@Override
	public void notifyIterationStart(int iteration) {
	  if (iteration % logInterval == 0) {
	    System.out.println("*** ITERATION " + iteration + " ***");
	  }
		startTimer("iteration");
	}

	@Override
	public void notifyIterationEnd(int iteration) {
	  long elapsedTime = stopTimer("iteration");
	  if (iteration % logInterval == 0) {
	    System.out.println(iteration + " done. Elapsed: " + elapsedTime + " ms");
	    printTimeStatistics();
	  }
	}

  @Override
  public void logStatistic(int iteration, String statisticName, String value) {
    if (iteration % logInterval == 0) {
      System.out.println(iteration + ": " + statisticName + "=" + value);
    }
  }
  
  public void printTimeStatistics() {
    System.out.println("Elapsed time statistics:");

    List<String> timers = Lists.newArrayList(getAllTimers());
    Collections.sort(timers);
    for (String timer : timers) {
      long total = getTimerElapsedTime(timer);
      long invocations = getTimerInvocations(timer);
      double average = ((double) total) / invocations;
      System.out.println(timer + ": " +  total + " ms (" + average + " * " + invocations + ")");
    }
  }
}
