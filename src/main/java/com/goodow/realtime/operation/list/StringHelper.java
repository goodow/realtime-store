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

import com.goodow.realtime.operation.list.algorithm.ListHelper;
import com.goodow.realtime.operation.list.algorithm.ListOp;

class StringHelper implements ListHelper<String> {

  @Override
  public StringNormalizer createNormalizer() {
    return new StringNormalizer();
  }

  @Override
  public int length(String str) {
    return str.length();
  }

  @Override
  public ListOp<String> newOp() {
    return new StringOp();
  }

  @Override
  public boolean startsWith(String str, String prefix) {
    return str.startsWith(prefix);
  }

  @Override
  public String subset(String str, int beginIdx) {
    return str.substring(beginIdx);
  }

  @Override
  public String subset(String str, int beginIdx, int endIndex) {
    return str.substring(beginIdx, endIndex);
  }
}