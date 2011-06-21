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

package org.apache.log4j;

import junit.framework.TestCase;


/**
 *    Tests for LogManager
 *
 * @author Curt Arnold
 **/
public class LogManagerTest extends TestCase {
  /**
   * Create new instance of LogManagerTest.
   * @param testName test name
   */
  public LogManagerTest(final String testName) {
    super(testName);
  }

  /**
   *  Check value of DEFAULT_CONFIGURATION_FILE.
   *  @deprecated since constant is deprecated
   */
  public void testDefaultConfigurationFile() {
     assertEquals("log4j.properties", LogManager.DEFAULT_CONFIGURATION_FILE);
  }

  /**
   *  Check value of DEFAULT_XML_CONFIGURATION_FILE.
   */
  public void testDefaultXmlConfigurationFile() {
     assertEquals("log4j.xml", LogManager.DEFAULT_XML_CONFIGURATION_FILE);
  }
  
  /**
   *  Check value of DEFAULT_CONFIGURATION_KEY.
   *  @deprecated since constant is deprecated
   */
  public void testDefaultConfigurationKey() {
     assertEquals("log4j.configuration", LogManager.DEFAULT_CONFIGURATION_KEY);
  }
  
  /**
   *  Check value of CONFIGURATOR_CLASS_KEY.
   *  @deprecated since constant is deprecated
   */
  public void testConfiguratorClassKey() {
     assertEquals("log4j.configuratorClass", LogManager.CONFIGURATOR_CLASS_KEY);
  }
  
  /**
   *  Check value of DEFAULT_INIT_OVERRIDE_KEY.
   *  @deprecated since constant is deprecated
   */
  public void testDefaultInitOverrideKey() {
     assertEquals("log4j.defaultInitOverride", LogManager.DEFAULT_INIT_OVERRIDE_KEY);
  }
}
