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
package org.apache.log4j.helpers;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.pattern.LogEvent;

import java.lang.reflect.Method;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;


public final class MDCKeySetExtractor {
    private final Method getKeySetMethod;
    public static final MDCKeySetExtractor INSTANCE =
            new MDCKeySetExtractor();


    private MDCKeySetExtractor() {
        //
        //  log4j 1.2.15 and later will have method to get names
        //     of all keys in MDC
        //
      Method getMethod = null;

        try {
           getMethod = LoggingEvent.class.getMethod(
                      "getPropertyKeySet", null);
        } catch(Exception ex) {
            getMethod = null;
        }
      getKeySetMethod = getMethod;

    }

    public Set getPropertyKeySet(final LoggingEvent event) throws Exception {
        //
        //  MDC keys are not visible prior to log4j 1.2.15
        //
        Set keySet = null;
        if (getKeySetMethod != null) {
              keySet = (Set) getKeySetMethod.invoke(event, null);
        } else {
            //
            //  for 1.2.14 and earlier could serialize and
            //    extract MDC content
              ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
              ObjectOutputStream os = new ObjectOutputStream(outBytes);
              os.writeObject(event);
              os.close();

              byte[] raw = outBytes.toByteArray();
              //
              //   bytes 6 and 7 should be the length of the original classname
              //     should be the same as our substitute class name
              final String subClassName = LogEvent.class.getName();
              if (raw[6] == 0 || raw[7] == subClassName.length()) {
                  //
                  //  manipulate stream to use our class name
                  //
                  for (int i = 0; i < subClassName.length(); i++) {
                      raw[8 + i] = (byte) subClassName.charAt(i);
                  }
                  ByteArrayInputStream inBytes = new ByteArrayInputStream(raw);
                  ObjectInputStream is = new ObjectInputStream(inBytes);
                  Object cracked = is.readObject();
                  if (cracked instanceof LogEvent) {
                      keySet = ((LogEvent) cracked).getPropertyKeySet();
                  }
                  is.close();
              }
        }
        return keySet;        
    }
}
