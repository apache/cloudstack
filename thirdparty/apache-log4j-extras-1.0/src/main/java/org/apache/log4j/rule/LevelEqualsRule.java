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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.helpers.UtilLoggingLevel;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A Rule class implementing equals against two levels.
 *
 * @author Scott Deboy (sdeboy@apache.org)
 */
public class LevelEqualsRule extends AbstractRule {
    /**
     * Serialization ID.
     */
    static final long serialVersionUID = -3638386582899583994L;

    /**
     * Level.
     */
    private transient Level level;

    /**
     * List of levels.
     */
    private static List levelList = new LinkedList();

    static {
        populateLevels();
    }

    /**
     * Create new instance.
     * @param level level.
     */
    private LevelEqualsRule(final Level level) {
        super();
        this.level = level;
    }

    /**
     * Populate list of levels.
     */
    private static void populateLevels() {
        levelList = new LinkedList();

        levelList.add(Level.FATAL.toString());
        levelList.add(Level.ERROR.toString());
        levelList.add(Level.WARN.toString());
        levelList.add(Level.INFO.toString());
        levelList.add(Level.DEBUG.toString());
		Level trace = Level.toLevel(5000, null);
		if (trace != null) {
			levelList.add(trace.toString());
	    }
    }

    /**
     * Create new rule.
     * @param value name of level.
     * @return instance of LevelEqualsRule.
     */
    public static Rule getRule(final String value) {
        Level thisLevel;
        if (levelList.contains(value.toUpperCase())) {
            thisLevel = Level.toLevel(value.toUpperCase());
          } else {
            thisLevel = UtilLoggingLevel.toLevel(value.toUpperCase());
        }

        return new LevelEqualsRule(thisLevel);
    }

    /**
     * {@inheritDoc}
     */
    public boolean evaluate(final LoggingEvent event) {
        return level.equals(event.getLevel());
    }

    /**
     * Deserialize the state of the object.
     *
     * @param in object input stream.
     *
     * @throws IOException if error in reading stream for deserialization.
     */
    private void readObject(final java.io.ObjectInputStream in)
            throws IOException {
        populateLevels();
        boolean isUtilLogging = in.readBoolean();
        int levelInt = in.readInt();
        if (isUtilLogging) {
            level = UtilLoggingLevel.toLevel(levelInt);
        } else {
            level = Level.toLevel(levelInt);
        }
    }

    /**
     * Serialize the state of the object.
     *
     * @param out object output stream.
     *
     * @throws IOException if error in writing stream during serialization.
     */
    private void writeObject(final java.io.ObjectOutputStream out)
            throws IOException {
        out.writeBoolean(level instanceof UtilLoggingLevel);
        out.writeInt(level.toInt());
    }
}
