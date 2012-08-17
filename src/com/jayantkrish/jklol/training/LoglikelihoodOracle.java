package com.jayantkrish.jklol.training;

import com.google.common.base.Preconditions;
import com.jayantkrish.jklol.evaluation.Example;
import com.jayantkrish.jklol.inference.MarginalCalculator;
import com.jayantkrish.jklol.inference.MarginalSet;
import com.jayantkrish.jklol.models.FactorGraph;
import com.jayantkrish.jklol.models.dynamic.DynamicAssignment;
import com.jayantkrish.jklol.models.dynamic.DynamicFactorGraph;
import com.jayantkrish.jklol.models.parametric.ParametricFactorGraph;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.util.Assignment;

/**
 * Loglikelihood optimization objective for log-linear distributions represented as {@link
 * ParametricFactorGraph}s. This oracle works in both the fully-observed and partially-observed
 * settings, though the optimization objective is nonconvex when the model includes latent
 * variables.
 * <p>
 * Note that this objective is equivalent to maximum-entropy training.
 *
 * @author jayantk
 */
public class LoglikelihoodOracle implements GradientOracle<DynamicFactorGraph, 
Example<DynamicAssignment, DynamicAssignment>> {

  private final ParametricFactorGraph family;
  private final MarginalCalculator marginalCalculator;

  public LoglikelihoodOracle(ParametricFactorGraph family, MarginalCalculator marginalCalculator) {
    this.family = Preconditions.checkNotNull(family);
    this.marginalCalculator = Preconditions.checkNotNull(marginalCalculator);
  }

  @Override
  public SufficientStatistics initializeGradient() {
    return family.getNewSufficientStatistics();
  }
  
  @Override
  public DynamicFactorGraph instantiateModel(SufficientStatistics parameters) {
    return family.getFactorGraphFromParameters(parameters);
  }

  @Override
  public void accumulateGradient(SufficientStatistics gradient, DynamicFactorGraph dynamicFactorGraph,
      Example<DynamicAssignment, DynamicAssignment> dynamicExample, LogFunction log) {
    // Instantiate any replicated factors, etc.
    log.startTimer("update_gradient/get_factor_graph_from_parameters");
    FactorGraph factorGraph = dynamicFactorGraph.getFactorGraph(dynamicExample.getInput());
    Assignment input = dynamicFactorGraph.getVariables().toAssignment(dynamicExample.getInput());
    Assignment observed = dynamicFactorGraph.getVariables().toAssignment(
        dynamicExample.getOutput().union(dynamicExample.getInput()));

    log.stopTimer("update_gradient/get_factor_graph_from_parameters");
    log.log(input, factorGraph);
    log.log(observed, factorGraph);

    log.startTimer("update_gradient/input_marginal");
    // Compute the second term of the gradient, the unconditional expected
    // feature counts
    FactorGraph inputFactorGraph = factorGraph.conditional(input);
    // System.out.println("input factor graph:");
    // System.out.println(inputFactorGraph.getParameterDescription());

    MarginalSet inputMarginals = marginalCalculator.computeMarginals(inputFactorGraph);
    log.stopTimer("update_gradient/input_marginal");

    log.startTimer("update_gradient/output_marginal");
    // Compute the first term of the gradient, the model expectations
    // conditioned on the training example.
    FactorGraph outputFactorGraph = factorGraph.conditional(observed);
    // System.out.println("output factor graph:");
    // System.out.println(outputFactorGraph.getParameterDescription());
    MarginalSet outputMarginals = marginalCalculator.computeMarginals(
        outputFactorGraph);
    log.stopTimer("update_gradient/output_marginal");

    // Perform the gradient update. Note that this occurs after both marginal
    // calculations, since the marginal calculations may throw ZeroProbabilityErrors
    // (if inference in the graphical model fails.)
    log.startTimer("update_gradient/increment");
    family.incrementSufficientStatistics(gradient, inputMarginals, -1.0);
    // System.out.println("=== input marginals ===");
    // System.out.println(inputMarginals);
    family.incrementSufficientStatistics(gradient, outputMarginals, 1.0);
    // System.out.println("=== output marginals ===");
    // System.out.println(outputMarginals);
    log.stopTimer("update_gradient/increment");
  }
}