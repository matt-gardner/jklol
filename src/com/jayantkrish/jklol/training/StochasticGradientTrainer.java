package com.jayantkrish.jklol.training;

import java.util.Collections;
import java.util.List;

import com.jayantkrish.jklol.inference.MarginalCalculator;
import com.jayantkrish.jklol.inference.MarginalSet;
import com.jayantkrish.jklol.models.FactorGraph;
import com.jayantkrish.jklol.models.parametric.ParametricFactorGraph;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.util.Assignment;

/**
 * Trains the weights of a factor graph using stochastic gradient descent.
 */
public class StochasticGradientTrainer {

  private MarginalCalculator marginalCalculator;
  private int numIterations;

  public StochasticGradientTrainer(MarginalCalculator inferenceEngine, int numIterations) {
    this.marginalCalculator = inferenceEngine;
    this.numIterations = numIterations;
  }

  public SufficientStatistics train(ParametricFactorGraph logLinearModel, SufficientStatistics initialParameters,
      List<Assignment> trainingData) {
    Collections.shuffle(trainingData);
    for (int i = 0; i < numIterations; i++) {
      for (Assignment trainingExample : trainingData) {
        SufficientStatistics gradient = computeGradient(initialParameters, logLinearModel, trainingExample);
        // TODO: decay the gradient increment amount as (say)
        // sqrt(numIterations)
        initialParameters.increment(gradient, 1.0);
      }
    }
    return initialParameters;
  }

  /*
   * Computes and returns an estimate of the gradient at {@code parameters}
   * based on {@code trainingExample}.
   */
  private SufficientStatistics computeGradient(SufficientStatistics parameters,
      ParametricFactorGraph logLinearModel, Assignment trainingExample) {
    FactorGraph factorGraph = logLinearModel.getFactorGraphFromParameters(parameters);

    // The gradient is the conditional expected counts minus the unconditional
    // expected counts
    SufficientStatistics gradient = logLinearModel.getNewSufficientStatistics(); 

    // Compute the second term of the gradient, the unconditional expected
    // feature counts
    MarginalSet unconditionalMarginals = marginalCalculator.computeMarginals(factorGraph);
    logLinearModel.incrementSufficientStatistics(
        gradient, unconditionalMarginals, -1.0);

    // Compute the first term of the gradient, the model expectations
    // conditioned on the training example.
    FactorGraph conditionalFactorGraph = factorGraph.conditional(trainingExample);
    MarginalSet conditionalMarginals = marginalCalculator.computeMarginals(
        conditionalFactorGraph);
    logLinearModel.incrementSufficientStatistics(gradient, conditionalMarginals, 1.0);
    
    return gradient;
  }
}