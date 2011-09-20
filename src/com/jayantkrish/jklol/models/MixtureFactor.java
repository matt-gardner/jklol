package com.jayantkrish.jklol.models;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jayantkrish.jklol.models.loglinear.FeatureFunction;
import com.jayantkrish.jklol.util.Assignment;

public class MixtureFactor extends AbstractFactor {

  private final ImmutableList<Factor> factors;
  private final ImmutableList<Double> weights;
  private final VariableNumMap sharedVars;

  /**
   * In most cases, {@link #create} is more convenient.
   * 
   * @param vars
   * @param factors
   * @param weights
   * @param useSum
   */
  public MixtureFactor(VariableNumMap allVars, VariableNumMap sharedVars, List<Factor> factors,
      List<Double> weights) {
    super(allVars);
    this.sharedVars = sharedVars;
    this.factors = ImmutableList.copyOf(Preconditions.checkNotNull(factors));
    this.weights = ImmutableList.copyOf(Preconditions.checkNotNull(weights));
    Preconditions.checkArgument(factors.size() == weights.size());
  }

  /**
   * Creates a {@link Factor} which mixes the passed-in list of factors. The
   * resulting probability distribution is a weighted sum of the {@code factors}
   * .
   * 
   * @param factors
   * @param weights
   * @return
   */
  public static MixtureFactor create(List<Factor> factors, List<Double> weights) {
    for (Double weight : weights) {
      Preconditions.checkArgument(weight >= 0);
    }

    VariableNumMap allVars = VariableNumMap.emptyMap();
    for (Factor factor : factors) {
      allVars = allVars.union(factor.getVars());
    }

    VariableNumMap sharedVars = allVars;
    for (Factor factor : factors) {
      sharedVars = sharedVars.intersection(factor.getVars());
    }

    // Each variable in the created factor must either be shared by all
    // subfactors in exactly one subfactor. Check this condition and fail if
    // it is not satisfied.
    VariableNumMap disjointVars = VariableNumMap.emptyMap();
    for (Factor factor : factors) {
      VariableNumMap otherFactorVars = factor.getVars().removeAll(sharedVars);
      if (disjointVars.intersection(otherFactorVars).size() > 0) {
        throw new IllegalArgumentException(
            "All factors must share a subset of shared variables, and not share any others.");
      }
      disjointVars = disjointVars.union(otherFactorVars);
    }

    return new MixtureFactor(allVars, sharedVars, factors, weights);
  }

  @Override
  public double getUnnormalizedProbability(Assignment assignment) {
    Preconditions.checkNotNull(assignment);
    Preconditions.checkArgument(assignment.containsVars(getVars().getVariableNums()));

    double probability = 0.0;
    for (int i = 0; i < factors.size(); i++) {
      if (factors.get(i).getVars().size() > 0 || assignment.size() == 0) {
        probability += weights.get(i) * factors.get(i).getUnnormalizedProbability(assignment);
      }
    }
    return probability;
  }

  @Override
  public Set<SeparatorSet> getComputableOutboundMessages(Map<SeparatorSet, Factor> inboundMessages) {
    Preconditions.checkNotNull(inboundMessages);

    boolean allMessagesReceived = true;
    List<SeparatorSet> sharedVarMessages = Lists.newArrayList();
    Set<SeparatorSet> possibleOutbound = Sets.newHashSet();
    for (Map.Entry<SeparatorSet, Factor> inboundMessage : inboundMessages.entrySet()) {
      if (inboundMessage.getValue() == null) {
        allMessagesReceived = false;
      }

      if (sharedVars.containsAny(inboundMessage.getKey().getSharedVars())) {
        sharedVarMessages.add(inboundMessage.getKey());
      } else {
        possibleOutbound.add(inboundMessage.getKey());
      }
    }

    // If every separator set has a factor on it, then we can compute a message
    // for every separator set.
    if (allMessagesReceived) {
      return inboundMessages.keySet();
    }

    boolean recievedAllInboundSharedVars = true;
    for (SeparatorSet separatorSet : sharedVarMessages) {
      if (inboundMessages.get(separatorSet) == null) {
        recievedAllInboundSharedVars = false;
      }
    }

    if (recievedAllInboundSharedVars) {
      // Messages on the mixed factors can be sent out if this factor has
      // received all of the inbound messages on the shared variable.
      return possibleOutbound;
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public Factor marginalize(Collection<Integer> varNumsToEliminate) {
    checkComputableMessage(varNumsToEliminate);

    List<Factor> marginalizedFactors = Lists.newArrayList();
    for (int i = 0; i < factors.size(); i++) {
      marginalizedFactors.add(factors.get(i).marginalize(varNumsToEliminate));
    }
    return MixtureFactor.create(marginalizedFactors, weights);
  }

  @Override
  public Factor maxMarginalize(Collection<Integer> varNumsToEliminate) {
    checkComputableMessage(varNumsToEliminate);

    Set<Integer> eliminateSet = Sets.newHashSet(varNumsToEliminate);
    eliminateSet.removeAll(sharedVars.getVariableNums());
    boolean retainAnyUnsharedVars = eliminateSet.size() < getVars().size() - sharedVars.size();  

    if (retainAnyUnsharedVars) {
      Set<Integer> varNumsToEliminateSet = Sets.newHashSet(varNumsToEliminate);
      List<Factor> maxMarginalizedFactors = Lists.newArrayList();
      List<Double> newWeights = Lists.newArrayList();
      for (int i = 0; i < factors.size(); i++) {
        // Efficiency improvement: Ignore any variables which are 
        if (varNumsToEliminateSet.containsAll(factors.get(i).getVars().getVariableNums())) {
          continue;
        }
        maxMarginalizedFactors.add(factors.get(i).maxMarginalize(varNumsToEliminate));
        newWeights.add(weights.get(i));
      }
      Preconditions.checkState(maxMarginalizedFactors.size() == 1);
      return MixtureFactor.create(maxMarginalizedFactors, newWeights);
    } else {
      // First maximize out the individual factors to get a factor over the shared variable.
      List<Factor> weightedMaxMarginalizedFactors = Lists.newArrayList();
      for (int i = 0; i < factors.size(); i++) {
        weightedMaxMarginalizedFactors.add(factors.get(i).maxMarginalize(eliminateSet).product(
            weights.get(i)));
      }
      Factor maxFactor = weightedMaxMarginalizedFactors.get(0).maximum(
          weightedMaxMarginalizedFactors.subList(1, factors.size()));
      
      // Max-marginalize out any shared variables which were requested.
      Set<Integer> sharedEliminateSet = Sets.newHashSet(varNumsToEliminate);
      sharedEliminateSet.retainAll(sharedVars.getVariableNums());
      return maxFactor.maxMarginalize(sharedEliminateSet);
    }
  }

  @Override
  public Factor add(Factor other) {
    throw new UnsupportedOperationException("MixtureFactor does not support add()");
  }

  @Override
  public Factor maximum(Factor other) {
    throw new UnsupportedOperationException("MixtureFactor does not support maximum()");
  }

  @Override
  public Factor product(Factor other) {
    Preconditions.checkNotNull(other);
    List<Factor> multipliedFactors = Lists.newArrayList();
    for (int i = 0; i < factors.size(); i++) {
      if (factors.get(i).getVars().containsAll(other.getVars())) {
        multipliedFactors.add(factors.get(i).product(other));
      } else if (factors.get(i).getVars().containsAny(other.getVars())) {
        throw new IllegalArgumentException("MixtureFactors can only be multiplied by factors "
            + "which are wholly subsumed by elements of the mixture factor.");
      } else {
        multipliedFactors.add(factors.get(i));
      }
    }
    return MixtureFactor.create(multipliedFactors, weights);
  }

  @Override
  public Factor product(double constant) {
    Preconditions.checkArgument(constant >= 0.0);
    List<Double> newWeights = Lists.newArrayList();
    for (Double weight : weights) {
      newWeights.add(weight * constant);
    }
    return MixtureFactor.create(factors, newWeights);
  }
  
  @Override
  public Factor inverse() {
    throw new UnsupportedOperationException("Cannot invert a MixtureFactor.");
  }
  
  @Override
  public double size() {
    double size = 0.0;
    for (Factor factor : factors) {
      size += factor.size();
    }
    return size;
  }

  @Override
  public Assignment sample() {
    throw new UnsupportedOperationException("Cannot sample from a MixtureFactor.");
  }
  
  @Override
  public List<Assignment> getMostLikelyAssignments(int numAssignments) {
    throw new UnsupportedOperationException("Not yet implemented.");
  }

  @Override
  public Factor conditional(Assignment a) {
    List<Factor> conditionedFactors = Lists.newArrayList();
    for (int i = 0; i < factors.size(); i++) {
      conditionedFactors.add(factors.get(i).conditional(a));
    }
    return MixtureFactor.create(conditionedFactors, weights);
  }

  @Override
  public double computeExpectation(FeatureFunction feature) {
    throw new UnsupportedOperationException();
  }

  /**
   * A {@code MixtureFactor}s can be coerced into {@code DiscreteFactor} if all
   * of the mixed factors contain the same set of variables or contain no
   * variables (i.e., only contribute to the partition function).
   */
  @Override
  public DiscreteFactor coerceToDiscrete() {
    VariableNumMap vars = null;
    for (Factor factor : factors) {
      if (factor.getVars().size() > 0) {
        if (vars == null) {
          vars = factor.getVars();
        } else if (!factor.getVars().equals(vars)) {
          throw new CoercionError("Cannot coerce " + this + " to DiscreteFactor.");
        }
      }
    }

    List<Factor> subFactors = Lists.newArrayList();
    for (int i = 0; i < factors.size(); i++) {
      if (factors.get(i).getVars().size() > 0) {
        subFactors.add(factors.get(i).product(weights.get(i)));
      }
    }
    return FactorUtils.add(subFactors).coerceToDiscrete();
  }

  /**
   * Checks that this factor can compute a valid message by marginalizing out
   * {@code varNumsToEliminate}.
   * 
   * @param varNumsToEliminate variables which are eliminated to create the
   * message.
   */
  private void checkComputableMessage(Collection<Integer> varNumsToEliminate) {
    Preconditions.checkNotNull(varNumsToEliminate);
    // Check that all of the uneliminated vars are either in sharedVars or
    // the unshared portion of a single factor.
    Set<Integer> uneliminatedVars = Sets.newHashSet(getVars().getVariableNums());
    uneliminatedVars.removeAll(varNumsToEliminate);
    boolean validMessage = sharedVars.containsAll(uneliminatedVars);
    for (int i = 0; i < factors.size(); i++) {
      validMessage = validMessage
          || factors.get(i).getVars().removeAll(sharedVars).containsAll(uneliminatedVars);
    }
    Preconditions.checkArgument(validMessage);
  }
}