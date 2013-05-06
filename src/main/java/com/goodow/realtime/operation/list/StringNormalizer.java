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

import com.goodow.realtime.operation.list.algorithm.ListNormalizer;

class StringNormalizer extends ListNormalizer<String> {

  private static class StringAppender implements Appender<String> {
    private StringBuilder sb = new StringBuilder();

    @Override
    public void append(String str) {
      sb.append(str);
    }

    @Override
    public String flush() {
      try {
        return sb.toString();
      } finally {
        sb = new StringBuilder();
      }
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }

  StringNormalizer() {
    super(new StringOp(), new StringAppender());
  }

  @Override
  protected boolean isEmpty(String str) {
    return str.isEmpty();
  }
}