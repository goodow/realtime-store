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
package com.goodow.realtime.operation.list;

import com.goodow.realtime.operation.list.StringNormalizer;
import com.goodow.realtime.operation.list.StringOp;
import com.goodow.realtime.operation.list.algorithm.ListOp;

import junit.framework.TestCase;

public class StringNormalizerTest extends TestCase {

  public void testEmptyCharactersNormalization() {
    StringNormalizer normalizer = new StringNormalizer();
    normalizer.retain(1).insert("").retain(1);
    ListOp<String> op = normalizer.finish();
    ListOp<String> expected = new StringOp().retain(2);
    assertEquals(expected.toString(), op.toString());
  }

  public void testEmptyDeleteCharactersNormalization() {
    StringNormalizer normalizer = new StringNormalizer();
    normalizer.retain(1).delete("").retain(1);
    ListOp<String> op = normalizer.finish();
    StringOp expected = new StringOp();
    expected.retain(2);
    assertEquals(expected.toString(), op.toString());
  }

  public void testEmptyRetainNormalization() {
    StringNormalizer normalizer = new StringNormalizer();
    normalizer.retain(1).insert("a").retain(0).insert("b").retain(1);
    ListOp<String> op = normalizer.finish();
    ListOp<String> expected = new StringOp().retain(1).insert("ab").retain(1);
    assertEquals(expected.toString(), op.toString());
  }

  public void testMultipleCharactersNormalization() {
    StringNormalizer normalizer = new StringNormalizer();
    normalizer.retain(1).insert("a").insert("b").insert("c").retain(1);
    ListOp<String> op = normalizer.finish();
    ListOp<String> expected = new StringOp().retain(1).insert("abc").retain(1);
    assertEquals(expected.toString(), op.toString());
  }

  public void testMultipleDeleteCharactersNormalization() {
    StringNormalizer normalizer = new StringNormalizer();
    normalizer.retain(1).delete("a").delete("b").delete("c").retain(1);
    ListOp<String> op = normalizer.finish();
    ListOp<String> expected = new StringOp().retain(1).delete("abc").retain(1);
    assertEquals(expected.toString(), op.toString());
  }

  public void testMultipleRetainNormalization() {
    StringNormalizer normalizer = new StringNormalizer();
    normalizer.retain(1).insert("a").retain(1).retain(1).retain(1).insert("b").retain(1);
    ListOp<String> op = normalizer.finish();
    ListOp<String> expected = new StringOp().retain(1).insert("a").retain(3).insert("b").retain(1);
    assertEquals(expected.toString(), op.toString());
  }

}
