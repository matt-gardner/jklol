package com.jayantkrish.jklol.cvsm;

import java.io.Serializable;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jayantkrish.jklol.ccg.lambda.ApplicationExpression;
import com.jayantkrish.jklol.ccg.lambda.ConstantExpression;
import com.jayantkrish.jklol.ccg.lambda.Expression;
import com.jayantkrish.jklol.tensor.Tensor;
import com.jayantkrish.jklol.util.IndexedList;

/**
 * A compositional vector space model.
 * 
 * @author jayant
 */
public class Cvsm implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private final IndexedList<String> tensorNames;
  private final List<Tensor> tensors;

  public Cvsm(IndexedList<String> tensorNames, List<Tensor> tensors) {
    this.tensorNames = Preconditions.checkNotNull(tensorNames);
    this.tensors = Preconditions.checkNotNull(tensors);
    Preconditions.checkArgument(tensors.size() == tensorNames.size());
  }
  
  public CvsmTree getInterpretationTree(Expression logicalForm) {
    if (logicalForm instanceof ConstantExpression) {
      String value = ((ConstantExpression) logicalForm).getName();
      Preconditions.checkArgument(tensorNames.contains(value));
      return new CvsmTensorTree(value, tensors.get(tensorNames.getIndex(value)));
    } else if (logicalForm instanceof ApplicationExpression) {
      ApplicationExpression app = ((ApplicationExpression) logicalForm);
      String functionName = ((ConstantExpression) app.getFunction()).getName();
      List<Expression> args = app.getArguments();
      if (functionName.equals("op:hadamard")) {
        Preconditions.checkArgument(args.size() == 2);
        
        CvsmTree bigTree = getInterpretationTree(args.get(0));
        CvsmTree smallTree = getInterpretationTree(args.get(1));

        return new CvsmProductTree(bigTree, smallTree);
      } else if (functionName.equals("op:sum_out")) {
        Preconditions.checkArgument(args.size() >= 2);
        
        CvsmTree subtree = getInterpretationTree(args.get(0));
        int[] dimsToEliminate = new int[args.size() - 1];
        for (int i = 1; i < args.size(); i++) {
          dimsToEliminate[i - 1] = Integer.parseInt(((ConstantExpression) args.get(i)).getName());
        }

        CvsmTree tree = new CvsmReduceTree(dimsToEliminate, subtree);
        int[] dims = tree.getValue().getDimensionNumbers();
        BiMap<Integer, Integer> relabeling = HashBiMap.create();
        for (int i = 0; i < dims.length; i++) {
          relabeling.put(dims[i], i);
        }

        return new CvsmRelabelDimsTree(tree, relabeling);
      }

      throw new IllegalArgumentException("Unknown function name: " + functionName);
    } else {
      throw new IllegalArgumentException("Unknown expression type: " + logicalForm);
    }
  }
}
