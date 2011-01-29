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

package org.apache.log4j.rule;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.LoggingEventFieldResolver;

import java.io.IOException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A Rule class supporting java.util.regex regular expression syntax.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class LikeRule extends AbstractRule {
    /**
     * Serialization ID.
     */
  static final long serialVersionUID = -3375458885595683156L;

    /**
     * Resolver.
     */
  private static final LoggingEventFieldResolver RESOLVER =
            LoggingEventFieldResolver.getInstance();
    /**
     * Pattern.
     */
  private transient Pattern pattern;
    /**
     * Regular expression matcher.
     */
  private transient Matcher matcher = null;
    /**
     * Field.
     */
  private transient String field;

    /**
     * Create new instance.
     * @param field field
     * @param pattern pattern
     */
  private LikeRule(final String field, final Pattern pattern) {
    super();
    if (!RESOLVER.isField(field)) {
        throw new IllegalArgumentException(
                "Invalid LIKE rule - " + field + " is not a supported field");
    }

    this.field = field;
    this.pattern = pattern;
  }

    /**
     * Create new instance from top two elements of stack.
     * @param stack stack
     * @return new instance
     */
  public static Rule getRule(final Stack stack) {
      if (stack.size() < 2) {
          throw new IllegalArgumentException(
                  "Invalid LIKE rule - expected two parameters but received "
                          + stack.size());
      }

      String p2 = stack.pop().toString();
      String p1 = stack.pop().toString();
      return getRule(p1, p2);
  }

    /**
     * Create new instance.
     * @param field field
     * @param pattern pattern
     * @return new instance
     */
  public static Rule getRule(final String field, final String pattern) {
    try {
        return new LikeRule(field, Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
    } catch (PatternSyntaxException e) {
        throw new IllegalArgumentException(
                "Invalid LIKE rule - " + e.getMessage());
    }
  }

    /** {@inheritDoc} */
  public boolean evaluate(final LoggingEvent event) {
    Object input = RESOLVER.getValue(field, event);
    if((input != null) && (pattern != null)) {
        if (matcher == null) {
            matcher = pattern.matcher(input.toString());
        } else {
            matcher.reset(input.toString());
        }
        return matcher.matches();
    }
    return false;
  }

  /**
    * Deserialize the state of the object.
    *
    * @param in object input stream
    *
    * @throws IOException if IOException during deserialization
    * @throws ClassNotFoundException if class not found.
    */
   private void readObject(final java.io.ObjectInputStream in)
     throws IOException, ClassNotFoundException {
         try {
           field = (String) in.readObject();
           String patternString = (String) in.readObject();
           pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
         } catch (PatternSyntaxException e) {
             throw new IOException("Invalid LIKE rule - " + e.getMessage());
         }
   }

   /**
    * Serialize the state of the object.
    *
    * @param out object output stream
    *
    * @throws IOException if IOException during serialization
    */
   private void writeObject(final java.io.ObjectOutputStream out)
     throws IOException {
     out.writeObject(field);
     out.writeObject(pattern.pattern());
   }
}
