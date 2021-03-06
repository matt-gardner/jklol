package com.jayantkrish.jklol.models.parametric;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.jayantkrish.jklol.models.Factor;
import com.jayantkrish.jklol.models.Factors;
import com.jayantkrish.jklol.models.TableFactor;
import com.jayantkrish.jklol.models.VariableNumMap;
import com.jayantkrish.jklol.tensor.FactoredTensor;
import com.jayantkrish.jklol.tensor.Tensor;
import com.jayantkrish.jklol.util.Assignment;

/**
 * Combines a set of parametric factors into a single factor by
 * concatenating their features. With loglinear factors, this
 * combination operation is identical to constructing one big factor
 * containing each independent set of features. This class may be used
 * to improve efficiency, e.g., by using different representations for
 * different sets of parameters.
 * 
 * @author jayant
 */
public class CombiningParametricFactor extends AbstractParametricFactor {
  private static final long serialVersionUID = 1L;

  private final List<String> factorNames;
  private final List<? extends ParametricFactor> parametricFactors;
  
  private final boolean returnFactoredTensor;

  public CombiningParametricFactor(VariableNumMap variables, List<String> factorNames,
      List<? extends ParametricFactor> parametricFactors, boolean returnFactoredTensor) { 
    super(variables);
    Preconditions.checkArgument(parametricFactors.size() == factorNames.size());
    VariableNumMap factorVars = VariableNumMap.EMPTY;
    for (ParametricFactor factor : parametricFactors) {
      Preconditions.checkArgument(variables.containsAll(factor.getVars()));
      factorVars = factorVars.union(factor.getVars());
    }
    Preconditions.checkArgument(factorVars.equals(variables));
    
    this.parametricFactors = ImmutableList.copyOf(parametricFactors);
    this.factorNames = ImmutableList.copyOf(factorNames);
    this.returnFactoredTensor = returnFactoredTensor;
  }

  @Override
  public Factor getModelFromParameters(SufficientStatistics parameters) {
    List<SufficientStatistics> parameterList = getParameterList(parameters);

    List<Factor> factors = Lists.newArrayList();
    for (int i = 0; i < parametricFactors.size(); i++) {
      Factor factor = parametricFactors.get(i).getModelFromParameters(parameterList.get(i));
      factors.add(factor);
    }
    
    if (!returnFactoredTensor) {
      return Factors.product(factors);
    } else {
      VariableNumMap allVars = VariableNumMap.EMPTY;
      Tensor[] tensors = new Tensor[factors.size()];
      for (int i = 0; i < factors.size(); i++) {
        Factor factor = factors.get(i);
        allVars = allVars.union(factor.getVars());
        tensors[i] = factor.coerceToDiscrete().getWeights();
      }

      return new TableFactor(allVars, new FactoredTensor(allVars.getVariableNumsArray(), 
          allVars.getVariableSizes(), tensors));
    }
  }

  @Override
  public String getParameterDescription(SufficientStatistics parameters, int numFeatures) {
    List<SufficientStatistics> parameterList = getParameterList(parameters);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parametricFactors.size(); i++) {
      sb.append(parametricFactors.get(i).getParameterDescription(parameterList.get(i), numFeatures));
    }
    return sb.toString();
  }

  @Override
  public SufficientStatistics getNewSufficientStatistics() {
    List<SufficientStatistics> parameterList = Lists.newArrayList();
    for (int i = 0; i < parametricFactors.size(); i++) {
      parameterList.add(parametricFactors.get(i).getNewSufficientStatistics());
    }
    return new ListSufficientStatistics(factorNames, parameterList);
  }

  @Override
  public void incrementSufficientStatisticsFromAssignment(SufficientStatistics gradient,
      SufficientStatistics currentParameters, Assignment assignment, double count) {
    List<SufficientStatistics> gradientList = getParameterList(gradient);
    List<SufficientStatistics> parameterList = getParameterList(currentParameters);
    for (int i = 0; i < parametricFactors.size(); i++) {
      parametricFactors.get(i).incrementSufficientStatisticsFromAssignment(gradientList.get(i),
          parameterList.get(i), assignment, count);
    }
  }

  @Override
  public void incrementSufficientStatisticsFromMarginal(SufficientStatistics gradient,
      SufficientStatistics currentParameters, Factor marginal, Assignment conditionalAssignment,
      double count, double partitionFunction) {
    List<SufficientStatistics> gradientList = getParameterList(gradient);
    List<SufficientStatistics> parameterList = getParameterList(currentParameters);
    for (int i = 0; i < parametricFactors.size(); i++) {
      Factor componentMarginal = marginal.marginalize(getVars().removeAll(
          parametricFactors.get(i).getVars()));
      parametricFactors.get(i).incrementSufficientStatisticsFromMarginal(gradientList.get(i),
          parameterList.get(i), componentMarginal, conditionalAssignment, count, partitionFunction);
    }
  }

  private List<SufficientStatistics> getParameterList(SufficientStatistics parameters) {
    List<SufficientStatistics> parameterList = parameters.coerceToList().getStatistics();
    Preconditions.checkArgument(parameterList.size() == parametricFactors.size());
    return parameterList;
  }
}