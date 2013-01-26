package com.jayantkrish.jklol.util;

public class ArrayUtils {

  /**
   * Identical to {@code Arrays.copyOf}, but GWT compatible.
   */
  public static int[] copyOf(int[] old, int length) {
    int[] newArray = new int[length];
    int minLength = Math.min(old.length, length);
    System.arraycopy(old, 0, newArray, 0, minLength);
    return newArray;
  }

  /**
   * Identical to {@code Arrays.copyOf}, but GWT compatible.
   */
  public static double[] copyOf(double[] old, int length) {
    double[] newArray = new double[length];
    int minLength = Math.min(old.length, length);
    System.arraycopy(old, 0, newArray, 0, minLength);
    return newArray;
  }

  /**
   * Identical to {@code Arrays.copyOf}, but GWT compatible.
   */
  public static long[] copyOf(long[] old, int length) {
    long[] newArray = new long[length];
    int minLength = Math.min(old.length, length);
    System.arraycopy(old, 0, newArray, 0, minLength);
    return newArray;
  }

  /**
   * Identical to {@code Arrays.copyOf}, but GWT compatible.
   */
  public static String[] copyOf(String[] old, int length) {
    String[] newArray = new String[length];
    int minLength = Math.min(old.length, length);
    System.arraycopy(old, 0, newArray, 0, minLength);
    return newArray;
  }

  /**
   * Identical to {@code Arrays.copyOfRange}, but GWT compatible.
   */
  public static int[] copyOfRange(int[] old, int from, int to) {
    int length = to - from;
    int[] newArray = new int[length];
    int minLength = Math.min(old.length - from, length);
    System.arraycopy(old, from, newArray, 0, minLength);
    return newArray;
  }

  /**
   * Identical to {@code Arrays.copyOfRange}, but GWT compatible.
   */
  public static long[] copyOfRange(long[] old, int from, int to) {
    int length = to - from;
    long[] newArray = new long[length];
    int minLength = Math.min(old.length - from, length);
    System.arraycopy(old, from, newArray, 0, minLength);
    return newArray;
  }

  /**
   * Identical to {@code Arrays.copyOfRange}, but GWT compatible.
   */
  public static double[] copyOfRange(double[] old, int from, int to) {
    int length = to - from;
    double[] newArray = new double[length];
    int minLength = Math.min(old.length - from, length);
    System.arraycopy(old, from, newArray, 0, minLength);
    return newArray;
  }

  /**
   * Identical to {@code Arrays.copyOfRange}, but GWT compatible.
   */
  public static String[] copyOfRange(String[] old, int from, int to) {
    int length = to - from;
    String[] newArray = new String[length];
    int minLength = Math.min(old.length - from, length);
    System.arraycopy(old, from, newArray, 0, minLength);
    return newArray;
  }

  public static boolean subarrayEquals(int[] array, int[] subarray, int startIndex) {
    for (int i = 0; i < subarray.length; i++) {
      if (array[i + startIndex] != subarray[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns an array of the form {@code [min, min + 1, ..., max - 1]}
   * 
   * @param min
   * @param max
   */
  public static int[] range(int min, int max) {
    int[] range = new int[max - min];
    for (int i = 0; i < (max - min); i++) {
      range[i] = i + min;
    }
    return range;
  }

  /**
   * Sorts a portion of the given key/value pairs by key. This method
   * sorts the section of {@code keys} from {@code startInd}
   * (inclusive) to {@code endInd} (not inclusive), simultaneously
   * swapping the corresponding entries of {@code values}.
   * 
   * @param keys
   * @param values
   * @param startInd
   * @param endInd
   */
  public static void sortKeyValuePairs(long[] keys, double[] values,
      int startInd, int endInd) {
    // Base case.
    if (startInd == endInd) {
      return;
    }

    // Choose pivot.
    int pivotInd = (int) (Math.random() * (endInd - startInd)) + startInd;

    // Perform swaps to partition array around the pivot.
    swap(keys, values, startInd, pivotInd);
    pivotInd = startInd;

    for (int i = startInd + 1; i < endInd; i++) {
      if (keys[i] < keys[pivotInd]) {
        swap(keys, values, pivotInd, pivotInd + 1);
        if (i != pivotInd + 1) {
          swap(keys, values, pivotInd, i);
        }
        pivotInd++;
      }
    }

    // Recursively sort the subcomponents of the arrays.
    sortKeyValuePairs(keys, values, startInd, pivotInd);
    sortKeyValuePairs(keys, values, pivotInd + 1, endInd);
  }

  /**
   * Swaps the keys and values at {@code i} with those at {@code j}
   * 
   * @param keys
   * @param values
   * @param i
   * @param j
   */
  private static void swap(long[] keys, double[] values, int i, int j) {
    long keySwap = keys[i];
    keys[i] = keys[j];
    keys[j] = keySwap;

    double swapValue = values[i];
    values[i] = values[j];
    values[j] = swapValue;
  }
  
  /**
   * Sorts a portion of the given key/value pairs by key. This method
   * sorts the section of {@code keys} from {@code startInd}
   * (inclusive) to {@code endInd} (not inclusive), simultaneously
   * swapping the corresponding entries of {@code values}.
   * 
   * @param keys
   * @param values
   * @param startInd
   * @param endInd
   */
  public static void sortKeyValuePairs(int[] keys, int[] values,
      int startInd, int endInd) {
    // TODO: check that case with duplicate keys works. What if all of keys is the same number?
    
    // Base case.
    if (startInd == endInd) {
      return;
    }

    // Choose pivot.
    int pivotInd = (int) (Math.random() * (endInd - startInd)) + startInd;

    // Perform swaps to partition array around the pivot.
    swap(keys, values, startInd, pivotInd);
    pivotInd = startInd;

    for (int i = startInd + 1; i < endInd; i++) {
      if (keys[i] < keys[pivotInd]) {
        swap(keys, values, pivotInd, pivotInd + 1);
        if (i != pivotInd + 1) {
          swap(keys, values, pivotInd, i);
        }
        pivotInd++;
      }
    }

    // Recursively sort the subcomponents of the arrays.
    sortKeyValuePairs(keys, values, startInd, pivotInd);
    sortKeyValuePairs(keys, values, pivotInd + 1, endInd);
  }

  /**
   * Swaps the keys and values at {@code i} with those at {@code j}
   * 
   * @param keys
   * @param values
   * @param i
   * @param j
   */
  private static void swap(int[] keys, int[] values, int i, int j) {
    int keySwap = keys[i];
    keys[i] = keys[j];
    keys[j] = keySwap;

    int swapValue = values[i];
    values[i] = values[j];
    values[j] = swapValue;
  }

  private ArrayUtils() {
    // Prevent instantiation.
  }
}