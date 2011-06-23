/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.spi;

import java.io.PrintWriter;
import java.util.Vector;

/**
  * VectorWriter is an obsolete class provided only for
  *  binary compatibility with earlier versions of log4j and should not be used.
  *
  * @deprecated
  */
class VectorWriter extends PrintWriter {

  private Vector v;

    /**
     * @deprecated
     */
  VectorWriter() {
    super(new NullWriter());
    v = new Vector();
  }

  public void print(Object o) {
    v.addElement(String.valueOf(o));
  }

  public void print(char[] chars) {
    v.addElement(new String(chars));
  }

  public void print(String s) {
    v.addElement(s);
  }

  public void println(Object o) {
    v.addElement(String.valueOf(o));
  }

  // JDK 1.1.x apprenly uses this form of println while in
  // printStackTrace()
  public
  void println(char[] chars) {
    v.addElement(new String(chars));
  }

  public
  void println(String s) {
    v.addElement(s);
  }

  public void write(char[] chars) {
    v.addElement(new String(chars));
  }

  public void write(char[] chars, int off, int len) {
    v.addElement(new String(chars, off, len));
  }

  public void write(String s, int off, int len) {
    v.addElement(s.substring(off, off+len));
  }

  public void write(String s) {
     v.addElement(s);
  }

  public String[] toStringArray() {
    int len = v.size();
    String[] sa = new String[len];
    for(int i = 0; i < len; i++) {
      sa[i] = (String) v.elementAt(i);
    }
    return sa;
  }

}

