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

package org.apache.log4j.rolling;

import junit.framework.TestCase;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.util.Compare;

import java.io.File;
import java.io.FileOutputStream;


/**
 *
 * Do not forget to call activateOptions when configuring programatically.
 * 
 * @author Ceki G&uuml;lc&uuml;
 *
 */
public class SizeBasedRollingTest extends TestCase {
  Logger logger = Logger.getLogger(SizeBasedRollingTest.class);
  Logger root = Logger.getRootLogger();

  public SizeBasedRollingTest(String name) {
    super(name);
  }

  public void setUp() {
    Appender ca = new ConsoleAppender(new PatternLayout("%d %level %c -%m%n"));
    ca.setName("CONSOLE");
    root.addAppender(ca);    
  }

  public void tearDown() {
    LogManager.shutdown();
  }

    /**
     * Tests that the lack of an explicit active file will use the
     * low index as the active file.
     *
     */
  public void test1() throws Exception {
        PatternLayout layout = new PatternLayout("%m\n");
        RollingFileAppender rfa = new RollingFileAppender();
        rfa.setName("ROLLING");
        rfa.setAppend(false);
        rfa.setLayout(layout);

        FixedWindowRollingPolicy swrp = new FixedWindowRollingPolicy();
        SizeBasedTriggeringPolicy sbtp = new SizeBasedTriggeringPolicy();

        sbtp.setMaxFileSize(100);
        swrp.setMinIndex(0);

        swrp.setFileNamePattern("sizeBased-test1.%i");
        swrp.activateOptions();

        rfa.setRollingPolicy(swrp);
        rfa.setTriggeringPolicy(sbtp);
        rfa.activateOptions();
        root.addAppender(rfa);

        // Write exactly 10 bytes with each log
        for (int i = 0; i < 25; i++) {
          if (i < 10) {
            logger.debug("Hello---" + i);
          } else if (i < 100) {
            logger.debug("Hello--" + i);
          }
        }

        assertTrue(new File("sizeBased-test1.0").exists());
        assertTrue(new File("sizeBased-test1.1").exists());
        assertTrue(new File("sizeBased-test1.2").exists());

        assertTrue(Compare.compare(SizeBasedRollingTest.class,
                "sizeBased-test1.0",
         "witness/rolling/sbr-test2.log"));
        assertTrue(Compare.compare(SizeBasedRollingTest.class,
                "sizeBased-test1.1",
         "witness/rolling/sbr-test2.0"));
        assertTrue(Compare.compare(SizeBasedRollingTest.class,
                "sizeBased-test1.2",
         "witness/rolling/sbr-test2.1"));
  }

    /** 
     * Test basic rolling functionality with explicit setting of FileAppender.file.
     */ 
  public void test2() throws Exception {
    PatternLayout layout = new PatternLayout("%m\n");
    RollingFileAppender rfa = new RollingFileAppender();
    rfa.setName("ROLLING");
    rfa.setAppend(false);
    rfa.setLayout(layout);
    rfa.setFile("sizeBased-test2.log");

    FixedWindowRollingPolicy swrp = new FixedWindowRollingPolicy();
    SizeBasedTriggeringPolicy sbtp = new SizeBasedTriggeringPolicy();

    sbtp.setMaxFileSize(100);
    swrp.setMinIndex(0);

    swrp.setFileNamePattern("sizeBased-test2.%i");
    swrp.activateOptions();
    
    rfa.setRollingPolicy(swrp);
    rfa.setTriggeringPolicy(sbtp);
    rfa.activateOptions();
    root.addAppender(rfa);

    // Write exactly 10 bytes with each log
    for (int i = 0; i < 25; i++) {
      if (i < 10) {
        logger.debug("Hello---" + i);
      } else if (i < 100) {
        logger.debug("Hello--" + i);
      }
    }

    assertTrue(new File("sizeBased-test2.log").exists());
    assertTrue(new File("sizeBased-test2.0").exists());
    assertTrue(new File("sizeBased-test2.1").exists());

    assertTrue(Compare.compare(SizeBasedRollingTest.class,
            "sizeBased-test2.log",
     "witness/rolling/sbr-test2.log"));
    assertTrue(Compare.compare(SizeBasedRollingTest.class,
            "sizeBased-test2.0",
     "witness/rolling/sbr-test2.0"));
    assertTrue(Compare.compare(SizeBasedRollingTest.class,
            "sizeBased-test2.1",
     "witness/rolling/sbr-test2.1"));
  }

    /**
     * Same as testBasic but also with GZ compression.
     */
  public void test3() throws Exception {
     PatternLayout layout = new PatternLayout("%m\n");
     RollingFileAppender rfa = new RollingFileAppender();
     rfa.setAppend(false);
     rfa.setLayout(layout);

     FixedWindowRollingPolicy  fwrp = new FixedWindowRollingPolicy();
     SizeBasedTriggeringPolicy sbtp = new SizeBasedTriggeringPolicy();

     sbtp.setMaxFileSize(100);
     fwrp.setMinIndex(0);
     rfa.setFile("sbr-test3.log");
     fwrp.setFileNamePattern("sbr-test3.%i.gz");
     fwrp.activateOptions();
     rfa.setRollingPolicy(fwrp);
     rfa.setTriggeringPolicy(sbtp);
     rfa.activateOptions();
     root.addAppender(rfa);

     // Write exactly 10 bytes with each log
     for (int i = 0; i < 25; i++) {
       Thread.sleep(100);
       if (i < 10) {
         logger.debug("Hello---" + i);
       } else if (i < 100) {
         logger.debug("Hello--" + i);
       }
     }

    assertTrue(new File("sbr-test3.log").exists());
    assertTrue(new File("sbr-test3.0.gz").exists());
    assertTrue(new File("sbr-test3.1.gz").exists());

    assertTrue(Compare.compare(SizeBasedRollingTest.class,
            "sbr-test3.log",  "witness/rolling/sbr-test3.log"));
    assertTrue(Compare.gzCompare(SizeBasedRollingTest.class,
            "sbr-test3.0.gz", "witness/rolling/sbr-test3.0.gz"));
    assertTrue(Compare.gzCompare(SizeBasedRollingTest.class,
            "sbr-test3.1.gz", "witness/rolling/sbr-test3.1.gz"));
  }

    /**
     * Test basic rolling functionality with bogus path in file name pattern.
     */
  public void test4() throws Exception {
    PatternLayout layout = new PatternLayout("%m\n");
    RollingFileAppender rfa = new RollingFileAppender();
    rfa.setName("ROLLING");
    rfa.setAppend(false);
    rfa.setLayout(layout);
    rfa.setFile("sizeBased-test4.log");

    FixedWindowRollingPolicy swrp = new FixedWindowRollingPolicy();
    SizeBasedTriggeringPolicy sbtp = new SizeBasedTriggeringPolicy();

    sbtp.setMaxFileSize(100);
    swrp.setMinIndex(0);

    //
    //   test4 directory should not exists.  Should cause all rollover attempts to fail.
    //
    swrp.setFileNamePattern("test4/sizeBased-test4.%i");
    swrp.activateOptions();

    rfa.setRollingPolicy(swrp);
    rfa.setTriggeringPolicy(sbtp);
    rfa.activateOptions();
    root.addAppender(rfa);

    // Write exactly 10 bytes with each log
    for (int i = 0; i < 25; i++) {
      if (i < 10) {
        logger.debug("Hello---" + i);
      } else if (i < 100) {
        logger.debug("Hello--" + i);
      }
    }

    assertTrue(new File("sizeBased-test4.log").exists());

    assertTrue(Compare.compare(SizeBasedRollingTest.class,
            "sizeBased-test4.log",
     "witness/rolling/sbr-test4.log"));
  }

    /**
     * Checking handling of rename failures due to other access
     * to the indexed files.
     */
  public void test5() throws Exception {
    //
	//   delete any stray files that might confuse test
    new File("sizeBased-test5.2").delete();
    new File("sizeBased-test5.3").delete();
    PatternLayout layout = new PatternLayout("%m\n");
    RollingFileAppender rfa = new RollingFileAppender();
    rfa.setName("ROLLING");
    rfa.setAppend(false);
    rfa.setLayout(layout);
    rfa.setFile("sizeBased-test5.log");

    FixedWindowRollingPolicy swrp = new FixedWindowRollingPolicy();
    SizeBasedTriggeringPolicy sbtp = new SizeBasedTriggeringPolicy();

    sbtp.setMaxFileSize(100);
    swrp.setMinIndex(0);

    swrp.setFileNamePattern("sizeBased-test5.%i");
    swrp.activateOptions();

    rfa.setRollingPolicy(swrp);
    rfa.setTriggeringPolicy(sbtp);
    rfa.activateOptions();
    root.addAppender(rfa);

    //
    //   put stray file above locked file
    FileOutputStream os1 = new FileOutputStream("sizeBased-test5.1");
    os1.close();


    FileOutputStream os0 = new FileOutputStream("sizeBased-test5.0");

    // Write exactly 10 bytes with each log
    for (int i = 0; i < 25; i++) {
      if (i < 10) {
        logger.debug("Hello---" + i);
      } else if (i < 100) {
        logger.debug("Hello--" + i);
      }
    }

    os0.close();

    if (new File("sizeBased-test5.3").exists()) {
        //
        //    looks like platform where open files can be renamed
        //
        assertTrue(new File("sizeBased-test5.log").exists());
        assertTrue(new File("sizeBased-test5.0").exists());
        assertTrue(new File("sizeBased-test5.1").exists());
        assertTrue(new File("sizeBased-test5.2").exists());
        assertTrue(new File("sizeBased-test5.3").exists());

        assertTrue(Compare.compare(
                SizeBasedRollingTest.class,
                "sizeBased-test5.log",
         "witness/rolling/sbr-test2.log"));
        assertTrue(Compare.compare(SizeBasedRollingTest.class,
                "sizeBased-test5.0",
         "witness/rolling/sbr-test2.0"));
        assertTrue(Compare.compare(SizeBasedRollingTest.class,
                "sizeBased-test5.1",
         "witness/rolling/sbr-test2.1"));

    } else {
        //
        //  rollover attempts should all fail
        //    so initial log file should have all log content
        //    open file should be unaffected
        //    stray file should have only been moved one slot.
        assertTrue(new File("sizeBased-test5.log").exists());
        assertTrue(new File("sizeBased-test5.0").exists());
        assertTrue(new File("sizeBased-test5.2").exists());

        assertTrue(Compare.compare(
                SizeBasedRollingTest.class,"sizeBased-test5.log",
            "witness/rolling/sbr-test4.log"));
    }
  }


    /**
     * Test basic rolling functionality with explicit setting of
     * obsolete FixedWindowRollingPolicy.activeFileName.
     * @deprecated Tests deprecated method
     */
  public void test6() throws Exception {
    PatternLayout layout = new PatternLayout("%m\n");
    RollingFileAppender rfa = new RollingFileAppender();
    rfa.setName("ROLLING");
    rfa.setAppend(false);
    rfa.setLayout(layout);

    FixedWindowRollingPolicy swrp = new FixedWindowRollingPolicy();
    SizeBasedTriggeringPolicy sbtp = new SizeBasedTriggeringPolicy();

    sbtp.setMaxFileSize(100);
    swrp.setMinIndex(0);
    swrp.setActiveFileName("sizeBased-test6.log");

    swrp.setFileNamePattern("sizeBased-test6.%i");
    swrp.activateOptions();

    rfa.setRollingPolicy(swrp);
    rfa.setTriggeringPolicy(sbtp);
    rfa.activateOptions();
    root.addAppender(rfa);

    // Write exactly 10 bytes with each log
    for (int i = 0; i < 25; i++) {
      if (i < 10) {
        logger.debug("Hello---" + i);
      } else if (i < 100) {
        logger.debug("Hello--" + i);
      }
    }

    assertTrue(new File("sizeBased-test6.log").exists());
    assertTrue(new File("sizeBased-test6.0").exists());
    assertTrue(new File("sizeBased-test6.1").exists());

    assertTrue(Compare.compare(SizeBasedRollingTest.class,
            "sizeBased-test6.log",
     "witness/rolling/sbr-test2.log"));
    assertTrue(Compare.compare(SizeBasedRollingTest.class,
            "sizeBased-test6.0",
     "witness/rolling/sbr-test2.0"));
    assertTrue(Compare.compare(SizeBasedRollingTest.class,
            "sizeBased-test6.1",
     "witness/rolling/sbr-test2.1"));
  }


}
