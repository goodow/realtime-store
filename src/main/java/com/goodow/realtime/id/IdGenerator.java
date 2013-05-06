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
package com.goodow.realtime.id;

import java.util.Random;

public class IdGenerator {

  /** valid characters. */
  static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

  private final Random random;

  public IdGenerator() {
    this(new Random());
  }

  public IdGenerator(Random random) {
    this.random = random;
  }

  /**
   * Returns a string with {@code length} random characters.
   */
  public String next(int length) {
    StringBuilder result = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      result.append(ALPHABET[random.nextInt(36)]);
    }
    return result.toString();
  }
}