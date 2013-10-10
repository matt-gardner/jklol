package com.jayantkrish.jklol.cvsm.ccg;

import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import com.jayantkrish.jklol.ccg.CcgExactInference;
import com.jayantkrish.jklol.ccg.CcgParse;
import com.jayantkrish.jklol.ccg.CcgParser;
import com.jayantkrish.jklol.ccg.lambda.ApplicationExpression;
import com.jayantkrish.jklol.ccg.lambda.ConstantExpression;
import com.jayantkrish.jklol.ccg.lambda.Expression;
import com.jayantkrish.jklol.ccg.lambda.ForAllExpression;
import com.jayantkrish.jklol.ccg.lambda.LambdaExpression;
import com.jayantkrish.jklol.ccg.lambda.QuantifierExpression;
import com.jayantkrish.jklol.ccg.SupertaggingCcgParser;
import com.jayantkrish.jklol.ccg.SupertaggingCcgParser.CcgParseResult;
import com.jayantkrish.jklol.ccg.supertag.Supertagger;
import com.jayantkrish.jklol.cli.AbstractCli;
import com.jayantkrish.jklol.cli.ParseCcg;
import com.jayantkrish.jklol.util.IoUtils;

public class ParseToLogicalForm extends AbstractCli {
  
  private OptionSpec<String> parser;
  private OptionSpec<String> supertagger;
  private OptionSpec<Double> multitagThresholds;
  private OptionSpec<String> lfTemplates;
  private OptionSpec<String> inputFile;

  private OptionSpec<Long> maxParseTimeMillis;
  
  public ParseToLogicalForm() {
    super();
  }
  
  @Override
  public void initializeOptions(OptionParser optionParser) {
    // Required arguments.
    parser = optionParser.accepts("parser", "File containing serialized CCG parser.").withRequiredArg()
        .ofType(String.class).required();
    supertagger = optionParser.accepts("supertagger").withRequiredArg().ofType(String.class).required();
    multitagThresholds = optionParser.accepts("multitagThreshold").withRequiredArg()
        .ofType(Double.class).withValuesSeparatedBy(',').required();
    
    lfTemplates = optionParser.accepts("lfTemplates").withRequiredArg().ofType(String.class).required();
    inputFile = optionParser.accepts("inputFile").withRequiredArg().ofType(String.class).required();
    
    // Optional arguments
    maxParseTimeMillis = optionParser.accepts("maxParseTimeMillis").withRequiredArg()
        .ofType(Long.class).defaultsTo(-1L);
  }
  
  @Override
  public void run(OptionSet options) {
    // Read in supertagger and CCG parser.
    CcgParser ccgParser = IoUtils.readSerializedObject(options.valueOf(parser), CcgParser.class);
    Supertagger tagger = IoUtils.readSerializedObject(options.valueOf(supertagger), Supertagger.class);
    double[] tagThresholds = Doubles.toArray(options.valuesOf(multitagThresholds));

    SupertaggingCcgParser supertaggingParser = new SupertaggingCcgParser(ccgParser,
        new CcgExactInference(null, options.valueOf(maxParseTimeMillis)), tagger, tagThresholds);

    // Read the logical form templates.
    CcgParseAugmenter augmenter = CcgParseAugmenter.parseFrom(IoUtils.readLines(options.valueOf(lfTemplates)));
    
    for (String line : IoUtils.readLines(options.valueOf(inputFile))) {
      List<String> words = Lists.newArrayList();
      List<String> posTags = Lists.newArrayList();
      ParseCcg.parsePosTaggedInput(Arrays.asList(line.split("\\s")), words, posTags);
      
      CcgParseResult result = supertaggingParser.parse(words, posTags);
      if (result == null || !result.getParse().getSyntacticCategory().isAtomic()) {
        System.out.println("NO PARSE");
      } else {
        CcgParse parse = result.getParse();
        CcgParse augmentedParse = augmenter.addLogicalForms(parse);

        Expression lf = augmentedParse.getLogicalForm();

        // System.out.println(parse);
        if (lf != null) {
          lf = lf.simplify();
          if (lf instanceof LambdaExpression) {
            LambdaExpression lambdaExp = (LambdaExpression) lf;
            List<ConstantExpression> arguments = ConstantExpression.generateUniqueVariables(
              lambdaExp.getArguments().size());
            lf = new QuantifierExpression("exists", arguments, new ApplicationExpression(lambdaExp, arguments));
            lf = lf.simplify();
          }

          if (lf instanceof ForAllExpression) {
            lf = ((ForAllExpression) lf).expandQuantifier().simplify();
          }

          System.out.println(lf);
        } else {
          System.out.println("NO LF CONVERSION");
        }
      }
    }
  }

  public static void main(String[] args) {
    new ParseToLogicalForm().run(args);
  }
}
