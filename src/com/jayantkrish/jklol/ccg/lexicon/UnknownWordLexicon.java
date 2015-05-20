package com.jayantkrish.jklol.ccg.lexicon;

import java.util.Iterator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.jayantkrish.jklol.ccg.CcgCategory;
import com.jayantkrish.jklol.ccg.LexiconEntry;
import com.jayantkrish.jklol.models.DiscreteFactor;
import com.jayantkrish.jklol.models.DiscreteFactor.Outcome;
import com.jayantkrish.jklol.models.VariableNumMap;
import com.jayantkrish.jklol.util.Assignment;

/**
 * Lexicon for handling unknown words.
 * 
 * @author jayant
 *
 */
public class UnknownWordLexicon extends AbstractCcgLexicon {
  private static final long serialVersionUID = 1L;

  private final VariableNumMap posVar;
  private final VariableNumMap ccgCategoryVar;
  private final DiscreteFactor posCategoryDistribution;

  public UnknownWordLexicon(VariableNumMap terminalVar, VariableNumMap posVar,
      VariableNumMap ccgCategoryVar, DiscreteFactor posCategoryDistribution) {
    super(terminalVar);
    this.posVar = Preconditions.checkNotNull(posVar);
    this.ccgCategoryVar = Preconditions.checkNotNull(ccgCategoryVar);
    this.posCategoryDistribution = Preconditions.checkNotNull(posCategoryDistribution);
  }

  @Override
  public List<LexiconEntry> getLexiconEntries(List<String> wordSequence, List<String> posSequence,
      List<LexiconEntry> alreadyGenerated) {

    List<LexiconEntry> lexiconEntries = Lists.newArrayList();
    if (alreadyGenerated.size() == 0 && posSequence.size() == 1) {
      String pos = posSequence.get(0);
      Assignment assignment = posVar.outcomeArrayToAssignment(pos);

      Iterator<Outcome> iterator = posCategoryDistribution.outcomePrefixIterator(assignment);
      while (iterator.hasNext()) {
        Outcome bestOutcome = iterator.next();
        CcgCategory ccgCategory = (CcgCategory) bestOutcome.getAssignment().getValue(
            ccgCategoryVar.getOnlyVariableNum());

        lexiconEntries.add(new LexiconEntry(wordSequence, ccgCategory));
      }
    }
    return lexiconEntries;
  }

  @Override
  public double getCategoryWeight(List<String> wordSequence, List<String> posSequence,
      CcgCategory category) {
    Preconditions.checkArgument(posSequence.size() == 1);
    String pos = posSequence.get(0);

    Assignment terminalAssignment = posVar.outcomeArrayToAssignment(pos);
    Assignment categoryAssignment = ccgCategoryVar.outcomeArrayToAssignment(category);
    Assignment a = terminalAssignment.union(categoryAssignment);
    return posCategoryDistribution.getUnnormalizedProbability(a);
  }
}
