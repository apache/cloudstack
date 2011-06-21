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

package org.apache.log4j.varia;
import junit.framework.TestCase;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RFATestCase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

/**
 *  Test of ExternallyRolledFileAppender.
 *
 * @author Curt Arnold
 */
public class ERFATestCase extends TestCase {

    /**
     * Create new instance of test.
     * @param name test name.
     */
  public ERFATestCase(final String name) {
    super(name);
  }

    /**
     * Reset configuration after test.
     */
  public void tearDown() {
      LogManager.resetConfiguration();
  }

    /**
     * Test ExternallyRolledFileAppender constructor.
     */
  public void testConstructor() {
      ExternallyRolledFileAppender appender =
              new ExternallyRolledFileAppender();
      assertEquals(0, appender.getPort());
  }

    /**
     * Send a message to the ERFA.
     * @param port port number.
     * @param msg message, may not be null.
     * @param expectedResponse expected response, may not be null.
     * @throws IOException thrown on IO error.
     */
  void sendMessage(int port, final String msg, final String expectedResponse) throws IOException {
      Socket socket = new Socket((String) null, port);
      DataInputStream reader = new DataInputStream(socket.getInputStream());
      DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
      writer.writeUTF(msg);
      String response = reader.readUTF();
      assertEquals(expectedResponse, response);
      reader.close();
      writer.close();
      socket.close();
  }

    /**
     * Test externally triggered rollover.
     * @throws IOException thrown on IO error.
     */
  public void testRollover() throws IOException {
      ExternallyRolledFileAppender erfa =
              new ExternallyRolledFileAppender();

      int port = 5500;

      Logger logger = Logger.getLogger(RFATestCase.class);
      Logger root = Logger.getRootLogger();
      PatternLayout layout = new PatternLayout("%m\n");
      erfa.setLayout(layout);
      erfa.setAppend(false);
      erfa.setMaxBackupIndex(2);
      erfa.setPort(port);
      erfa.setFile("output/ERFA-test2.log");
      try {
        erfa.activateOptions();
      } catch(SecurityException ex) {
          return;
      }
      try {
         Thread.sleep(100);
      } catch(InterruptedException ex) {
      }
      root.addAppender(erfa);


      // Write exactly 10 bytes with each log
      for (int i = 0; i < 55; i++) {
        if (i < 10) {
          logger.debug("Hello---" + i);
        } else if (i < 100) {
          logger.debug("Hello--" + i);
        }
        if ((i % 10) == 9) {
            try {
                sendMessage(port, "RollOver", "OK");
            } catch(SecurityException ex) {
                return;
            }
        }
      }

      try {
        sendMessage(port,
              "That's all folks.",
              "Expecting [RollOver] string.");
      } catch(SecurityException ex) {
          return;
      }


      assertTrue(new File("output/ERFA-test2.log").exists());
      assertTrue(new File("output/ERFA-test2.log.1").exists());
      assertTrue(new File("output/ERFA-test2.log.2").exists());
      assertFalse(new File("output/ERFA-test2.log.3").exists());
  }
}
