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
package com.goodow.realtime.util.impl;

/*-[
 #import "GDR.h"
 #import "ObjCModelFactory+OCNI.h"
 ]-*/
import com.goodow.realtime.CollaborativeString;
import com.goodow.realtime.util.ModelFactory;

// @formatter:off
public class ObjCModelFactory implements ModelFactory {

  @Override
  public native void scheduleDeferred(Runnable cmd) /*-[
    #if TARGET_OS_IPHONE == 1 || TARGET_OS_IPHONE_SIMULATOR == 1
      [[NSRunLoop currentRunLoop] performSelector:@selector(run) target:cmd argument:nil order:0 modes:@[NSDefaultRunLoopMode]];
    #else
      [cmd run];
    #endif
  ]-*/;

  @Override
  public native void scheduleFixedDelay(Runnable cmd, int delayMs) /*-[
    [NSTimer scheduledTimerWithTimeInterval:delayMs/1000 
                                     target:cmd
                                   selector:@selector(run)
                                   userInfo:nil
                                    repeats:NO];
  ]-*/;

  @Override
  public native void setText(CollaborativeString str, String text) /*-[
    [self setTextImpl:str text:text];
  ]-*/;

}
   