package com.jayantkrish.jklol.ccg.lexicon;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgCategory;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.ccg.lambda2.Expression2;
import com.jayantkrish.jklol.models.VariableNumMap;

/**
 * CCG Lexicon that allows strings from the text to be 
 * used as lexicon entries. 
 * 
 * @author jayant
 *
 */
public class StringLexicon extends AbstractCcgLexicon {
  private static final long serialVersionUID = 1L;

  
  public static enum CategorySpanConfig {
    ALL_SPANS, WHOLE_SENTENCE,
  };
  
  private final List<CcgCategory> categories;
  private final List<CategorySpanConfig> spanConfig;

  /**
   * 
   * @param terminalVar
   * @param categories
   * @param spanConfig controls which sentence spans each category
   * is instantiated for.  
   */
  public StringLexicon(VariableNumMap terminalVar, List<CcgCategory> categories,
      List<CategorySpanConfig> spanConfig) {
    super(terminalVar);
    this.categories = ImmutableList.copyOf(categories);
    this.spanConfig = ImmutableList.copyOf(spanConfig);
    Preconditions.checkArgument(spanConfig.size() == categories.size());
  }

  @Override
  public List<LexiconEntry> getLexiconEntries(List<String> wordSequence, List<String> posTags, 
      List<LexiconEntry> alreadyGenerated, int spanStart, int spanEnd, List<String> sentenceWords) {
    List<LexiconEntry> entries = Lists.newArrayList();
    for (int i = 0; i < categories.size(); i++) {
      CcgCategory category = categories.get(i);
      CategorySpanConfig config = spanConfig.get(i);
      if (config == CategorySpanConfig.ALL_SPANS || (config == CategorySpanConfig.WHOLE_SENTENCE
          && spanStart == 0 && spanEnd == sentenceWords.size() - 1)) {
        Expression2 wordSequenceExpression = Expression2.constant(
            "\"" + Joiner.on(" ").join(sentenceWords.subList(spanStart, spanEnd + 1)) + "\"");
        Expression2 newLf = Expression2.nested(category.getLogicalForm(), wordSequenceExpression);
        CcgCategory newCategory = category.replaceLogicalForm(newLf);
        entries.add(new LexiconEntry(wordSequence, newCategory));
      }
    }
    return entries;
  }

  @Override
  public double getCategoryWeight(List<String> wordSequence, List<String> posTags, CcgCategory category) {
    return 1.0;
  }
}
