/*
 * Copyright 2012 Goodow.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.goodow.realtime.util;

/**
 * An immutable ordered pair of typed objects.
 * 
 * @param <A> Type of value 1
 * @param <B> Type of value 2
 */
public class Pair<A, B> {

  /**
   * Static constructor to save typing on generic arguments.
   */
  public static <A, B> Pair<A, B> of(A a, B b) {
    return new Pair<A, B>(a, b);
  }

  /**
   * The first element of the pair; see also {@link #getFirst}.
   */
  public final A first;

  /**
   * The second element of the pair; see also {@link #getSecond}.
   */
  public final B second;

  /**
   * Pair constructor
   * 
   * @param first Value 1
   * @param second Value 2
   */
  public Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Copy constructor
   * 
   * @param pair Pair to shallow copy from
   */
  public Pair(Pair<? extends A, ? extends B> pair) {
    first = pair.first;
    second = pair.second;
  }

  /**
   * {@inheritDoc}
   * 
   * NOTE: Not safe to override this method, hence final.
   */
  @Override
  public final boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o == this) {
      return true;
    }
    if (o instanceof Pair) {
      Pair<?, ?> p = (Pair<?, ?>) o;
      return (p.first == first || (first != null && first.equals(p.first)))
          && (p.second == second || (second != null && second.equals(p.second)));
    }
    return false;
  }

  /**
   * Returns the first element of this pair; see also {@link #first}.
   */
  public A getFirst() {
    return first;
  }

  /**
   * Returns the second element of this pair; see also {@link #second}.
   */
  public B getSecond() {
    return second;
  }

  /**
   * {@inheritDoc}
   * 
   * NOTE: Not safe to override this method, hence final.
   */
  @Override
  public final int hashCode() {
    return (first == null ? 0 : first.hashCode()) + 37 * (second == null ? 0 : second.hashCode());
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "(" + String.valueOf(first) + "," + String.valueOf(second) + ")";
  }
}