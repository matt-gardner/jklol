package com.jayantkrish.jklol.cvsm.eval;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class Environment {

  private final Map<String, Value> boundVariables;
  private final Environment parentEnvironment;
  
  public Environment(Map<String, Value> bindings, Environment parentEnvironment) {
    this.boundVariables = Preconditions.checkNotNull(bindings);
    this.parentEnvironment = parentEnvironment;
  }

  public Environment bindName(String name, Value value) {
    return bindNames(Arrays.asList(name), Arrays.asList(value));
  }

  public Environment bindNames(List<String> names, List<Value> values) {
    Preconditions.checkArgument(names.size() == values.size());
    Map<String, Value> bindings = Maps.newHashMap();
    for (int i = 0; i < names.size(); i++) {
      bindings.put(names.get(i), values.get(i));
    }
    return new Environment(bindings, this);
  }
  
  public Value getValue(String name) {
    if (boundVariables.containsKey(name)) {
      return boundVariables.get(name);
    }

    Preconditions.checkState(parentEnvironment != null,
        "Tried accessing unbound variable: %s", name);
    return parentEnvironment.getValue(name);
  }
}
