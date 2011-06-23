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

// Here is a code example to configure the JDBCAppender with a configuration-file

import org.apache.log4j.*;
import java.sql.*;
import java.lang.*;
import java.util.*;

public class Log4JTest
{
   // Create a category instance for this class
   static Category cat = Category.getInstance(Log4JTest.class.getName());

   public static void main(String[] args)
   {
      // Ensure to have all necessary drivers installed !
      try
      {
         Driver d = (Driver)(Class.forName("oracle.jdbc.driver.OracleDriver").newInstance());
         DriverManager.registerDriver(d);
      }
      catch(Exception e){}

      // Set the priority which messages have to be logged
      cat.setPriority(Priority.INFO);

      // Configuration with configuration-file
      PropertyConfigurator.configure("log4jtestprops.txt");

      // These messages with Priority >= setted priority will be logged to the database.
      cat.debug("debug");  //this not, because Priority DEBUG is less than INFO
      cat.info("info");
      cat.error("error");
      cat.fatal("fatal");
   }
}

