package com.jayantkrish.jklol.inference;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.jayantkrish.jklol.models.Factor;
import com.jayantkrish.jklol.models.TableFactorBuilder;
import com.jayantkrish.jklol.models.VariableNumMap;
import com.jayantkrish.jklol.tensor.SparseTensorBuilder;
import com.jayantkrish.jklol.util.Assignment;

/**
 * A {@link MarginalSet} that computes approximate marginals from a set of
 * samples. These samples are typically drawn from an approximation of a true
 * marginal distribution (for example, from a {@link GibbsSampler}).
 * 
 * @author jayant
 * 
 */
public class SampleMarginalSet extends AbstractMarginalSet {

  private final VariableNumMap factorGraphVariables;
  private final ImmutableList<Assignment> samples;

  public SampleMarginalSet(VariableNumMap factorGraphVariables, List<Assignment> samples,
      VariableNumMap conditionedVariables, Assignment conditionedValues) {
    super(factorGraphVariables, conditionedVariables, conditionedValues);
    this.factorGraphVariables = factorGraphVariables;
    this.samples = ImmutableList.copyOf(samples);
  }

  @Override
  public Factor getMarginal(Collection<Integer> varNums) {
    Preconditions.checkNotNull(varNums);
    VariableNumMap varsToRetain = factorGraphVariables.intersection(varNums);
    TableFactorBuilder builder = new TableFactorBuilder(varsToRetain, SparseTensorBuilder.getFactory());
    double increment = 1.0 / samples.size();
    for (Assignment sample : samples) {
      Assignment factorSample = sample.intersection(Ints.toArray(varNums));
      builder.setWeight(factorSample, 
          builder.getWeight(factorSample) + increment);
    }
    return builder.build();
  }

  @Override
  public double getLogPartitionFunction() {
    // This is definitely not right.
    return Math.log(samples.size());
  }
}
