package com.jayantkrish.jklol.tensor;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.jayantkrish.jklol.tensor.TensorProtos.LogSpaceAdapterProto;
import com.jayantkrish.jklol.tensor.TensorProtos.TensorProto;

/**
 * A tensor which stores all weights in logarithmic space. This transformation
 * allows the tensor to be used in computations with a much larger range of
 * weights, but comes at a cost of numerical precision.
 * 
 * Most mathematical operations on {@code LogSpaceTensorAdapter} are performed
 * in log space. If the passed-in tensor (to binary operations) is not
 * represented in log space, it is automatically converted to log space before
 * the operation. Addition operations are the only exception to this rule. 
 * 
 * @author jayantk
 */
public class LogSpaceTensorAdapter extends AbstractTensorBase implements Tensor {

  private final DenseTensor logWeights;

  public LogSpaceTensorAdapter(DenseTensor logWeights) {
    super(logWeights.getDimensionNumbers(), logWeights.getDimensionSizes());
    this.logWeights = logWeights;
  }
  
  public static LogSpaceTensorAdapter fromProto(LogSpaceAdapterProto proto) {
    Preconditions.checkArgument(proto.hasLogWeights());
    return new LogSpaceTensorAdapter(DenseTensor.fromProto(proto.getLogWeights()));
  }

  @Override
  public int size() {
    return logWeights.size();
  }

  @Override
  public double getByIndex(int index) {
    return Math.exp(logWeights.getByIndex(index));
  }
  
  @Override
  public double getLogByIndex(int index) {
    return logWeights.getByIndex(index);
  }

  @Override
  public int keyNumToIndex(long keyNum) {
    return logWeights.keyNumToIndex(keyNum);
  }

  @Override
  public long indexToKeyNum(int index) {
    return logWeights.indexToKeyNum(index);
  }

  @Override
  public Iterator<KeyValue> keyValueIterator() {
    return new LogSpaceKeyValueIterator(logWeights.keyValueIterator());
  }

  @Override
  public Iterator<KeyValue> keyValuePrefixIterator(int[] keyPrefix) {
    return new LogSpaceKeyValueIterator(logWeights.keyValuePrefixIterator(keyPrefix));
  }

  @Override
  public double getL2Norm() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Tensor slice(int[] dimensionNumbers, int[] keys) {
    return new LogSpaceTensorAdapter(logWeights.slice(dimensionNumbers, keys));
  }

  @Override
  public Tensor elementwiseProduct(Tensor other) {
    return new LogSpaceTensorAdapter(logWeights.elementwiseAddition(other.elementwiseLog()));
  }

  @Override
  public Tensor elementwiseAddition(Tensor other) {
    return logWeights.elementwiseExp().elementwiseAddition(other);
  }

  @Override
  public Tensor elementwiseMaximum(Tensor other) {
    return new LogSpaceTensorAdapter(logWeights.elementwiseMaximum(other.elementwiseLog()));
  }

  @Override
  public Tensor elementwiseInverse() {
    throw new UnsupportedOperationException("Not implemented.");
  }

  @Override
  public Tensor elementwiseLog() {
    return logWeights;
  }
  
  @Override
  public Tensor elementwiseExp() {
    return new LogSpaceTensorAdapter(logWeights.elementwiseExp());
  }

  @Override
  public Tensor sumOutDimensions(Collection<Integer> dimensionsToEliminate) {
    return logWeights.elementwiseExp().sumOutDimensions(dimensionsToEliminate);
  }

  @Override
  public Tensor maxOutDimensions(Collection<Integer> dimensionsToEliminate) {
    return new LogSpaceTensorAdapter(logWeights.maxOutDimensions(dimensionsToEliminate));
  }

  @Override
  public Tensor relabelDimensions(int[] newDimensions) {
    return new LogSpaceTensorAdapter(logWeights.relabelDimensions(newDimensions));
  }

  @Override
  public Tensor relabelDimensions(Map<Integer, Integer> relabeling) {
    return new LogSpaceTensorAdapter(logWeights.relabelDimensions(relabeling));
  }

  @Override
  public int getNearestIndex(long keyNum) {
    return logWeights.getNearestIndex(keyNum);
  }

  @Override
  public double[] getValues() {
    throw new UnsupportedOperationException("Not implemented.");
  } 
  
  @Override
  public TensorProto toProto() {
    TensorProto.Builder builder = TensorProto.newBuilder();
    builder.setType(TensorProto.TensorType.LOG_ADAPTER);
    TensorProto logWeightProto = logWeights.toProto();
    Preconditions.checkState(logWeightProto.hasDenseTensor());
    builder.getLogTensorBuilder().setLogWeights(logWeightProto.getDenseTensor());
    return builder.build();
  }
  
  private static final class LogSpaceKeyValueIterator implements Iterator<KeyValue> {
    private final Iterator<KeyValue> logIterator;

    public LogSpaceKeyValueIterator(Iterator<KeyValue> logIterator) {
      this.logIterator = Preconditions.checkNotNull(logIterator);
    }

    @Override
    public boolean hasNext() {
      return logIterator.hasNext();
    }

    @Override
    public KeyValue next() {
      KeyValue next = logIterator.next();
      next.setValue(Math.exp(next.getValue()));
      return next;
    }

    @Override
    public void remove() {
      logIterator.remove();
    }
  }
}
