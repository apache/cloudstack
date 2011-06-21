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
package org.apache.log4j.varia.test;


import org.apache.log4j.varia.JDBCAppender;
import org.apache.log4j.*;


public class JDBCTest
{
    public static void main (String argv[])
    {
        try {
      Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }


     Category rootLog = Category.getRoot();
        Layout layout = new PatternLayout("%p [%t] %c - %m%n");
        JDBCAppender appender = new JDBCAppender();
        appender.setLayout(layout);
        appender.setOption(JDBCAppender.URL_OPTION, "jdbc:odbc:someDB");


        appender.setOption(JDBCAppender.USER_OPTION, "auser");
        appender.setOption(JDBCAppender.PASSWORD_OPTION, "thepassword");



        rootLog.addAppender(appender);


        try {
            Category log = Category.getInstance("main");
            log.debug("Debug 1");
            Thread.sleep(500);
            log.info("info 1");
            Thread.sleep(500);
            log.warn("warn 1");
            Thread.sleep(500);
            log.error("error 1");
            Thread.sleep(500);
            log.fatal("fatal 1");
            Thread.sleep(500);


            appender.setOption(JDBCAppender.BUFFER_OPTION, "5");
            log.debug("Debug 2");
            Thread.sleep(500);
            log.info("info 2");
            Thread.sleep(500);
            log.warn("warn 2");
            Thread.sleep(500);
            log.error("error 2");
            Thread.sleep(500);
            log.fatal("fatal 2");
            Thread.sleep(500);


            appender.setOption(JDBCAppender.BUFFER_OPTION, "2");
            appender.setThreshold(Priority.WARN);
            log.debug("Debug 3");
            Thread.sleep(500);
            log.info("info 3");
            Thread.sleep(500);
            log.warn("warn 3");
            Thread.sleep(500);
            log.error("error 3");
            Thread.sleep(500);
            log.fatal("fatal 3");
        }
        catch (InterruptedException e)
        {
            System.out.println("Interrupted");
        }
    }
}
