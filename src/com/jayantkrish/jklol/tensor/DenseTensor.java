package com.jayantkrish.jklol.tensor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.jayantkrish.jklol.util.IntegerArrayIterator;

/**
 * Immutable tensor, represented densely. The dense representation is faster
 * than {@link SparseTensor}, but requires more memory.
 * 
 * @author jayantk
 */
public class DenseTensor extends DenseTensorBase implements Tensor {

  /**
   * Creates a tensor that spans {@code dimensions}, and each dimension has the
   * corresponding size from {@code sizes}. Most users should use a
   * {@link DenseTensorBuilder} instead of this constructor.
   * 
   * @param dimensions
   * @param sizes
   * @param values
   */
  public DenseTensor(int[] dimensions, int[] sizes, double[] values) {
    super(dimensions, sizes, values);
  }

  @Override
  public Tensor elementwiseProduct(Tensor other) {
    return doElementwise(other, Operation.PRODUCT);
  }

  @Override
  public Tensor elementwiseAddition(Tensor other) {
    return doElementwise(other, Operation.SUM);
  }

  @Override
  public Tensor elementwiseMaximum(Tensor other) {
    return doElementwise(other, Operation.MAX);
  }

  private enum Operation {
    PRODUCT, SUM, MAX
  };

  /**
   * Performs elementwise operations, like products, sums and maxes.
   * 
   * @param other
   * @param op
   * @return
   */
  private Tensor doElementwise(Tensor other, Operation op) {
    DenseTensorBuilder outputBuilder = new DenseTensorBuilder(getDimensionNumbers(),
        getDimensionSizes());
    outputBuilder.increment(this);

    // Maps a key of other into a partial key of this.
    int[] dimensionMapping = getDimensionMapping(other.getDimensionNumbers());
    int[] partialKey = Arrays.copyOf(getDimensionSizes(), getDimensionSizes().length);
    for (int i = 0; i < dimensionMapping.length; i++) {
      partialKey[dimensionMapping[i]] = 1;
    }

    int numValues = 1;
    for (int i = 0; i < partialKey.length; i++) {
      numValues *= partialKey[i];
    }
    int[] keyOffsets = new int[numValues];
    Iterator<int[]> myKeyIterator = new IntegerArrayIterator(partialKey);
    int ind = 0;
    while (myKeyIterator.hasNext()) {
      keyOffsets[ind] = getIndex(myKeyIterator.next());
      ind++;
    }
    Preconditions.checkState(ind == keyOffsets.length);

    Iterator<int[]> otherKeys = other.keyIterator();
    while (otherKeys.hasNext()) {
      int[] otherKey = otherKeys.next();
      int baseOffset = 0;
      for (int i = 0; i < otherKey.length; i++) {
        baseOffset += otherKey[i] * indexOffsets[dimensionMapping[i]];
      }

      for (int i = 0; i < keyOffsets.length; i++) {
        switch (op) {
        case PRODUCT:
          outputBuilder.values[baseOffset + keyOffsets[i]] *= other.get(otherKey);
          break;
        case SUM:
          outputBuilder.values[baseOffset + keyOffsets[i]] += other.get(otherKey);
          break;
        case MAX:
          outputBuilder.values[baseOffset + keyOffsets[i]] = Math.max(other.get(otherKey),
              outputBuilder.values[baseOffset + keyOffsets[i]]);
          break;
        }
      }
    }
    return outputBuilder.buildNoCopy();
  }

  @Override
  public Tensor elementwiseInverse() {
    DenseTensorBuilder outputBuilder = new DenseTensorBuilder(getDimensionNumbers(),
        getDimensionSizes());
    for (int i = 0; i < values.length; i++) {
      outputBuilder.values[i] = (values[i] == 0) ? 0 : 1.0 / values[i];
    }
    return outputBuilder.buildNoCopy();
  }

  @Override
  public Tensor sumOutDimensions(Collection<Integer> dimensionsToEliminate) {
    return reduceDimensions(dimensionsToEliminate, true);
  }

  @Override
  public Tensor maxOutDimensions(Collection<Integer> dimensionsToEliminate) {
    return reduceDimensions(dimensionsToEliminate, false);
  }

  /**
   * Performs reduction operations which eliminate some subset of the existing
   * dimensions.
   * 
   * @param dimensionsToEliminate
   * @param useSum
   * @return
   */
  private Tensor reduceDimensions(Collection<Integer> dimensionsToEliminate, boolean useSum) {
    SortedSet<Integer> dimensionsToKeep = Sets.newTreeSet(Ints.asList(getDimensionNumbers()));
    dimensionsToKeep.removeAll(dimensionsToEliminate);

    int[] myDimensionNumbers = getDimensionNumbers();
    int[] myDimensionSizes = getDimensionSizes();
    List<Integer> dimensionNumsToKeep = Lists.newArrayList();
    List<Integer> dimensionSizesToKeep = Lists.newArrayList();
    List<Integer> dimensionNumsToEliminate = Lists.newArrayList();
    List<Integer> dimensionSizesToEliminate = Lists.newArrayList();
    for (int i = 0; i < myDimensionNumbers.length; i++) {
      if (!dimensionsToEliminate.contains(myDimensionNumbers[i])) {
        dimensionNumsToKeep.add(myDimensionNumbers[i]);
        dimensionSizesToKeep.add(myDimensionSizes[i]);
      } else {
        dimensionNumsToEliminate.add(myDimensionNumbers[i]);
        dimensionSizesToEliminate.add(myDimensionSizes[i]);
      }
    }

    DenseTensorBuilder outputBuilder = new DenseTensorBuilder(Ints.toArray(dimensionNumsToKeep),
        Ints.toArray(dimensionSizesToKeep));

    // Maps a key of the reduced tensor into a partial key of this.
    int[] dimensionMapping = getDimensionMapping(outputBuilder.getDimensionNumbers());
    int[] partialKey = new int[getDimensionNumbers().length];
    Arrays.fill(partialKey, -1);
    Iterator<int[]> otherKeys = outputBuilder.keyIterator();
    while (otherKeys.hasNext()) {
      int[] otherKey = otherKeys.next();
      for (int i = 0; i < otherKey.length; i++) {
        partialKey[dimensionMapping[i]] = otherKey[i];
      }

      Iterator<int[]> myKeyIterator = new SliceIndexIterator(getDimensionSizes(), partialKey);
      double resultVal = (useSum) ? 0.0 : Double.NEGATIVE_INFINITY;
      while (myKeyIterator.hasNext()) {
        if (useSum) {
          resultVal += get(myKeyIterator.next());
        } else {
          resultVal = Math.max(resultVal, get(myKeyIterator.next()));
        }
      }
      outputBuilder.put(otherKey, resultVal);
    }
    return outputBuilder.buildNoCopy();
  }

  @Override
  public Tensor relabelDimensions(int[] newDimensions) {
    Preconditions.checkArgument(newDimensions.length == getDimensionNumbers().length);
    if (Ordering.natural().isOrdered(Ints.asList(newDimensions))) {
      // If the new dimension labels are in sorted order, then we don't have to
      // resort the outcome and value arrays. This is a big efficiency win if it
      // happens. Note that outcomes and values are (treated as) immutable, and
      // hence we don't need to copy them.
      return new DenseTensor(newDimensions, getDimensionSizes(), values);
    }

    int[] sortedDims = Arrays.copyOf(newDimensions, newDimensions.length);
    Arrays.sort(sortedDims);

    // Figure out the mapping from the new, sorted dimension indices to
    // the current dimension numbers.
    Map<Integer, Integer> currentDimInds = Maps.newHashMap();
    for (int i = 0; i < newDimensions.length; i++) {
      currentDimInds.put(newDimensions[i], i);
    }

    int[] sortedSizes = new int[newDimensions.length];
    int[] newOrder = new int[sortedDims.length];
    for (int i = 0; i < sortedDims.length; i++) {
      newOrder[i] = currentDimInds.get(sortedDims[i]);
      sortedSizes[i] = getDimensionSizes()[currentDimInds.get(sortedDims[i])];
    }

    DenseTensorBuilder builder = new DenseTensorBuilder(sortedDims, sortedSizes);
    Iterator<int[]> keyIter = keyIterator();
    int[] newKey = new int[newOrder.length];
    while (keyIter.hasNext()) {
      int[] oldKey = keyIter.next();
      for (int i = 0; i < newOrder.length; i++) {
        newKey[i] = oldKey[newOrder[i]];
      }
      builder.put(newKey, get(oldKey));
    }
    return builder.buildNoCopy();
  }

  @Override
  public Tensor relabelDimensions(Map<Integer, Integer> relabeling) {
    int[] newDimensions = new int[getDimensionNumbers().length];
    for (int i = 0; i < getDimensionNumbers().length; i++) {
      newDimensions[i] = relabeling.get(getDimensionNumbers()[i]);
    }
    return relabelDimensions(newDimensions);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (other instanceof DenseTensor) {
      DenseTensor otherTensor = (DenseTensor) other;
      if (Arrays.equals(otherTensor.getDimensionNumbers(), getDimensionNumbers()) &&
          Arrays.equals(otherTensor.getDimensionSizes(), getDimensionSizes())) {
        for (int i = 0; i < values.length; i++) {
          if (values[i] != otherTensor.values[i]) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[DenseTensor ");
    Iterator<int[]> keyIter = keyIterator();
    while (keyIter.hasNext()) {
      int[] key = keyIter.next();
      if (get(key) != 0.0) {
        sb.append(Arrays.toString(key));
        sb.append("=");
        sb.append(get(key));
        sb.append(", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }
}
