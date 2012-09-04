package com.jayantkrish.jklol.ccg;

import java.util.List;

import com.google.common.collect.Lists;

public class SyntacticCategory {

  public enum Direction {
    LEFT("\\"), RIGHT("/");

    private final String slash;
    private Direction(final String slash) {
      this.slash = slash;
    }
    @Override
    public String toString() {
      return slash;
    }
  };

  //
  public enum HeadValue {
    ARGUMENT(">"), RETURN("");
    
    private final String str;
    private HeadValue(final String str) {
      this.str = str;
    }
    @Override
    public String toString() {
      return str;
    }
  };

  // If this category is atomic, then it has a value.
  private final String value;

  // If this category is functional, it has an argument direction, an argument
  // and a return type.
  private final Direction direction;
  private final HeadValue head;
  private final SyntacticCategory returnType;
  private final SyntacticCategory argumentType;
  
  // NOTE: remember to update .equals() and .hashCode() if the members change.

  public SyntacticCategory(String value, Direction direction, HeadValue head,
      SyntacticCategory returnType, SyntacticCategory argumentType) {
    this.value = value;
    this.direction = direction;
    this.head = head;
    this.returnType = returnType;
    this.argumentType = argumentType;
  }

  /**
   * Parses a CCG syntactic type string into a tree.
   * 
   * @param typeString
   * @return
   */
  public static SyntacticCategory parseFrom(String typeString) {
    return parseSyntacticTypeStringHelper(typeString);
  }

  private static SyntacticCategory parseSyntacticTypeStringHelper(String typeString) {
    int index = 0;
    int parenDepth = 0;

    int minParenDepth = Integer.MAX_VALUE;
    int minParenDepthIndex = -1;

    while (index < typeString.length()) {
      if (typeString.charAt(index) == '\\' || typeString.charAt(index) == '/') {
        if (parenDepth < minParenDepth) {
          minParenDepth = parenDepth;
          minParenDepthIndex = index;
        }
      } else if (typeString.charAt(index) == '(') {
        parenDepth++;
      } else if (typeString.charAt(index) == ')') {
        parenDepth--;
      }
      index++;
    }

    if (minParenDepthIndex == -1) {
      // Atomic category.
      // Strip any parentheses around the variable name, then remove any feature
      // subcategorization.
      String baseSyntacticType = typeString.replaceAll("[\\(\\)]", "").split("\\[")[0].intern();
      return new SyntacticCategory(baseSyntacticType, null, null, null, null);
    } else {
      // Find the string corresponding to the operator.
      int returnTypeIndex = minParenDepthIndex + 1;
      if (typeString.charAt(returnTypeIndex) == '>') {
        // Type string wants the return argument to inherit the head from the
        // right.
        returnTypeIndex++;
      }

      SyntacticCategory left = parseSyntacticTypeStringHelper(
          typeString.substring(0, minParenDepthIndex));

      SyntacticCategory right = parseSyntacticTypeStringHelper(
          typeString.substring(returnTypeIndex, typeString.length()));

      String directionString = typeString.substring(minParenDepthIndex, returnTypeIndex);
      Direction direction = null;
      if (directionString.startsWith("\\")) {
        direction = Direction.LEFT;
      } else if (directionString.startsWith("/")) {
        direction = Direction.RIGHT;
      } else {
        throw new IllegalArgumentException("Invalid argument direction: " + directionString);
      }

      HeadValue head = HeadValue.RETURN;
      if (directionString.length() > 1) {
        if (directionString.charAt(1) == '>') {
          head = HeadValue.ARGUMENT;
        }
      }

      return new SyntacticCategory(null, direction, head, left, right);
    }
  }

  public boolean isAtomic() {
    return value != null;
  }

  public String getValue() {
    return value;
  }

  /**
   * Where the head word of the return type (result of applying this category)
   * is inherited from.
   * 
   * @return
   */
  public HeadValue getHead() {
    return head;
  }

  public boolean acceptsArgumentOn(Direction direction) {
    return direction != null && this.direction == direction;
  }

  /**
   * Gets the sequence of arguments that this category accepts. Note that the
   * returned arguments themselves may be functional types.
   * 
   * The sequence is returned with the first required argument at the end of the
   * list.
   * 
   * @return
   */
  public List<SyntacticCategory> getArgumentList() {
    if (isAtomic()) {
      return Lists.newArrayList();
    } else {
      List<SyntacticCategory> args = getReturn().getArgumentList();
      args.add(getArgument());
      return args;
    }
  }

  public SyntacticCategory getReturn() {
    return returnType;
  }

  public SyntacticCategory getArgument() {
    return argumentType;
  }

  public boolean hasSameSyntacticType(SyntacticCategory other) {
    if (isAtomic() && other.isAtomic()) {
      return value.equals(other.value);
    } else if (!isAtomic() && !other.isAtomic() && direction.equals(other.direction) && head.equals(other.head)) {
      return returnType.hasSameSyntacticType(other.getReturn()) &&
          argumentType.hasSameSyntacticType(other.getArgument());
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((argumentType == null) ? 0 : argumentType.hashCode());
    result = prime * result + ((direction == null) ? 0 : direction.hashCode());
    result = prime * result + ((head == null) ? 0 : head.hashCode());
    result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SyntacticCategory other = (SyntacticCategory) obj;
    if (argumentType == null) {
      if (other.argumentType != null)
        return false;
    } else if (!argumentType.equals(other.argumentType))
      return false;
    if (direction != other.direction)
      return false;
    if (head != other.head)
      return false;
    if (returnType == null) {
      if (other.returnType != null)
        return false;
    } else if (!returnType.equals(other.returnType))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }

  @Override
  public String toString() {
    if (isAtomic()) {
      return value.toString();
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append("(");
      if (returnType != null) {
        sb.append(returnType.toString());
      }

      sb.append(direction.toString());
      sb.append(head.toString());

      if (argumentType != null) {
        sb.append(argumentType.toString());
      }
      sb.append(")");

      return sb.toString();
    }
  }
}
