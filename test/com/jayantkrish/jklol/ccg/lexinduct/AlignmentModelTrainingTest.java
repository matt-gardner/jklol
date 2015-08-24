package com.jayantkrish.jklol.ccg.lexinduct;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jayantkrish.jklol.ccg.cli.AlignmentLexiconInduction;
import com.jayantkrish.jklol.ccg.lambda.ExpressionParser;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.ccg.lexinduct.LagrangianAlignmentTrainer.ParametersAndLagrangeMultipliers;
import com.jayantkrish.jklol.inference.JunctionTree;
import com.jayantkrish.jklol.models.TableFactor;
import com.jayantkrish.jklol.models.VariableNumMap;
import com.jayantkrish.jklol.models.parametric.SufficientStatistics;
import com.jayantkrish.jklol.preprocessing.DictionaryFeatureVectorGenerator;
import com.jayantkrish.jklol.preprocessing.FeatureVectorGenerator;
import com.jayantkrish.jklol.tensor.Tensor;
import com.jayantkrish.jklol.training.DefaultLogFunction;
import com.jayantkrish.jklol.training.ExpectationMaximization;

public class AlignmentModelTrainingTest extends TestCase {

  String[][] dataSet1 = new String[][] {{"plano in texas", "(in plano texas)"},
      {"what plano in us", "(in plano us)"},
      {"texas in us", "(in texas us)"},
      {"us in plano", "(in us plano)"},
      {"what texas borders plano", "(border texas plano)"},
      {"cities in texas", "(lambda x (and (city x) (in x texas)))"}};

  VariableNumMap wordVarPattern, expressionVarPattern;
  
  List<AlignmentExample> examples;
  FeatureVectorGenerator<Expression2> featureGenerator;
  
  public void setUp() {
    examples = parseData(dataSet1);
    
    Set<Expression2> allExpressions = Sets.newHashSet();
    for (AlignmentExample example : examples) {
      example.getTree().getAllExpressions(allExpressions);
    }
    featureGenerator = DictionaryFeatureVectorGenerator.createFromData(allExpressions,
        new ExpressionTokenFeatureGenerator(Collections.<String>emptyList()), false);
    
    examples = AlignmentLexiconInduction.applyFeatureVectorGenerator(featureGenerator, examples);
  }

  public static List<AlignmentExample> parseData(String[][] data) {
    List<AlignmentExample> examples = Lists.newArrayList();
    for (int i = 0; i < data.length; i++) {
      ExpressionTree tree = ExpressionTree.fromExpression(ExpressionParser
        .expression2().parseSingleExpression(data[i][1]));
      List<String> words = Arrays.asList(data[i][0].split(" "));
      System.out.println(words);
      System.out.println(tree);
      examples.add(new AlignmentExample(words, tree));
    }
    return examples;
  }

  public void testTrainingSimple() {
    ParametricAlignmentModel pam = ParametricAlignmentModel.buildAlignmentModel(examples, true, false, featureGenerator);

    SufficientStatistics smoothing = pam.getNewSufficientStatistics();
    smoothing.increment(0.1);

    SufficientStatistics initial = pam.getNewSufficientStatistics();
    initial.increment(1);

    ExpectationMaximization em = new ExpectationMaximization(30, new DefaultLogFunction());
    /*
    SufficientStatistics trainedParameters = em.train(new AlignmentEmOracle(pam, new JunctionTree(), smoothing),
        initial, examples);

    // TODO: put in an actual test here.
    System.out.println(pam.getParameterDescription(trainedParameters, 30));
    */

    pam = pam.updateUseTreeConstraint(true);
    SufficientStatistics trainedParameters2 = em.train(new AlignmentEmOracle(pam, new JunctionTree(), smoothing, true),
        initial, examples);
    
    // TODO: put in an actual test here.
    System.out.println(pam.getParameterDescription(trainedParameters2, 30));
    AlignmentModel model = pam.getModelFromParameters(trainedParameters2);
    for (AlignmentExample example : examples) {
      System.out.println(example.getWords());
      System.out.println(model.getBestAlignment(example));
    }
  }

  public void testTrainingCfg() {
    ParametricCfgAlignmentModel pam = ParametricCfgAlignmentModel.buildAlignmentModelWithNGrams(
        examples, featureGenerator, 1, false);

    SufficientStatistics smoothing = pam.getNewSufficientStatistics();
    smoothing.increment(0.1);

    SufficientStatistics initial = pam.getNewSufficientStatistics();
    initial.increment(1);

    ExpectationMaximization em = new ExpectationMaximization(30, new DefaultLogFunction());
    SufficientStatistics trainedParameters2 = em.train(new CfgAlignmentEmOracle(pam, smoothing),
        initial, examples);

    // TODO: put in an actual test here.
    System.out.println(pam.getParameterDescription(trainedParameters2, 30));
    CfgAlignmentModel model = pam.getModelFromParameters(trainedParameters2);
    for (AlignmentExample example : examples) {
      System.out.println(example.getWords());
      System.out.println(model.getBestAlignment(example));
    }
  }
  
  public void testLagrangianRelaxation() {
    ParametricCfgAlignmentModel pam = ParametricCfgAlignmentModel.buildAlignmentModelWithNGrams(
        examples, featureGenerator, 1, false);

    SufficientStatistics smoothing = pam.getNewSufficientStatistics();
    smoothing.increment(0.1);

    SufficientStatistics initial = pam.getNewSufficientStatistics();
    initial.increment(1);
    
    ExpectationMaximization em = new ExpectationMaximization(0, new DefaultLogFunction(1, false));
    LagrangianAlignmentTrainer trainer = new LagrangianAlignmentTrainer(300, em);
    ParametersAndLagrangeMultipliers trainedParameters = trainer.train(pam, initial, smoothing, examples);

    // TODO: put in an actual test here.
    System.out.println(pam.getParameterDescription(trainedParameters.getParameters(), 30));
    CfgAlignmentModel model = pam.getModelFromParameters(trainedParameters.getParameters());
    for (int i = 0; i < examples.size(); i++) {
      AlignmentExample example = examples.get(i);
      Tensor exampleMultipliers = trainedParameters.getLagrangeMultipliers().slice(
            new int[] {0}, new int[] {i});
      TableFactor exampleWeights = new TableFactor(model.getParentVar().union(model.getTerminalVar()),
          exampleMultipliers.elementwiseProduct(-1.0).elementwiseExp());

      System.out.println(example.getWords());
      System.out.println(model.getBestAlignment(example, exampleWeights));
    }
  }
}
