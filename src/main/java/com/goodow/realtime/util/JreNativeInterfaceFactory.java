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

import com.goodow.realtime.CollaborativeString;

import java.util.LinkedList;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;

public class JreNativeInterfaceFactory implements NativeInterfaceFactory {

  @Override
  public void scheduleDeferred(Runnable cmd) {
    cmd.run();
  }

  @Override
  public void setText(CollaborativeString str, String text) {
    diff_match_patch dmp = new diff_match_patch();
    LinkedList<Diff> diffs = dmp.diff_main(str.getText(), text);
    dmp.diff_cleanupSemantic(diffs);
    int cursor = 0;
    for (Diff diff : diffs) {
      text = diff.text;
      int len = text.length();
      switch (diff.operation) {
        case EQUAL:
          cursor += len;
          break;
        case INSERT:
          str.insertString(cursor, text);
          cursor += len;
          break;
        case DELETE:
          str.removeRange(cursor, cursor + len);
          break;
        default:
          throw new RuntimeException("Shouldn't reach here!");
      }
    }
    assert cursor == str.length();
  }

}
