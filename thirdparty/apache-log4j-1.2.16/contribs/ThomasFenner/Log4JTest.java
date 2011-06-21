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
/**
// Class JDBCAppender, writes messages into a database

// The JDBCAppender is configurable at runtime in two alternatives :
// 1. Configuration-file
//    Define the options in a file and call a PropertyConfigurator.configure(file)-method.
// 2. method JDBCAppender::setOption(JDBCAppender.xxx_OPTION, String value)

// The sequence of some options is important :
// 1. Connector-option OR/AND Database-options
//    Any database connection is required !
// 2. (Table-option AND Columns-option) OR SQL-option
//		Any statement is required !
// 3. All other options can be set at any time...
//    The other options are optional and have a default initialization, which can be custumized.

// All available options are defined as static String-constants in JDBCAppender named xxx_OPTION.

// Here is a description of all available options :
// 1. Database-options to connect to the database
//    - URL_OPTION			: a database url of the form jdbc:subprotocol:subname
//    - USERNAME_OPTION		: the database user on whose behalf the connection is being made
//    - PASSWORD_OPTION		: the user's password
//
// 2. Connector-option to specify your own JDBCConnectionHandler
//    - CONNECTOR_OPTION	: a classname which is implementing the JDBCConnectionHandler-interface
//    This interface is used to get a customized connection.
//    If in addition the database-options are given, these options will be used
//    for invocation of the JDBCConnectionHandler-interface to get a connection.
//    Else if no database-options are given, the JDBCConnectionHandler-interface is called without these options.
//
// 3. SQL-option to specify a static sql-statement which will be performed with every occuring message-event
//    - SQL_OPTION			: a sql-statement which will be used to write to the database
//    If you give this option, the table-option and columns-option will be ignored !
//    Use the variable @MSG@ on that location in the statement, which has to be dynamically replaced by the message.
//
// 4. Table-option to specify one table contained by the database
//    - TABLE_OPTION			: the table in which the logging will be done
//
// 5. Columns-option to describe the important columns of the table (Not nullable columns are mandatory to describe!)
//    - COLUMNS_OPTION		: a formatted list of column-descriptions
//    Each column description consists of
//       - the name of the column (required)
//			- a logtype which is a static constant of class LogType (required)
//       - and a value which depends by the LogType (optional/required, depending by logtype)
//    Here is a description of the available logtypes of class LogType :
//       o MSG			= a value will be ignored, the column will get the message. (One columns need to be of this type!)
//       o STATIC		= the value will be filled into the column with every logged message. (Ensure that the type of value can be casted into the sql-type of the column!)
//       o ID			= value must be a classname, which implements the JDBCIDHandler-interface.
//       o TIMESTAMP	= a value will be ignored, the column will be filled with a actually timestamp with every logged message.
//       o EMPTY		= a value will be ignored, the column will be ignored when writing to the database (Ensure to fill not nullable columns by a database trigger!)
//    If there are more than one column to describe, the columns must be separated by a TAB-delimiter ('	') !
//    The arguments of a column-description must be separated by the delimiter '~' !
//		(Example :  name1~logtype1~value1	name2~logtype2~value2...)
//
// 6. Layout-options to define the layout of the messages (optional)
//    - the layout wont be set by a xxx_OPTION
//    Configuration-file			: see at the following configuration-file example
//    JDBCAppender::setOption()	: see at the following code example
//    The default is a layout of class org.apache.log4j.PatternLayout with ConversionPattern=%m
//
// 7. Buffer-option to define the size of the message-event-buffer (optional)
//    - BUFFER_OPTION		: define how many messages will be buffered until they will be updated to the database.
//    The default is a update on every message (buffer=1=no buffer).
//
// 8. Commit-option to define a auto-commitment
//    - COMMIT_OPTION		: define whether updated messages should be committed to the database (Y) or not (N).
//    The default is a commit on every buffer-flush.


// Here is a Configuration-file example, which can be used with the PropertyConfigurator :
//
// Declare a appender variable named JDBC
log4j.rootCategory=JDBC

// JDBC is a class of JDBCAppender, which writes messages into a database
log4j.appender.JDBC=JDBCAppender

// 1. Database-options to connect to the database
log4j.appender.JDBC.url=jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(COMMUNITY=tcp.world)(PROTOCOL=TCP)(Host=LENZI)(Port=1521))(ADDRESS=(COMMUNITY=tcp.world)(PROTOCOL=TCP)(Host=LENZI)(Port=1526)))(CONNECT_DATA=(SID=LENZI)))
log4j.appender.JDBC.username=mex_pr_dev60
log4j.appender.JDBC.password=mex_pr_dev60

// 2. Connector-option to specify your own JDBCConnectionHandler
log4j.appender.JDBC.connector=MyConnectionHandler

// 3. SQL-option to specify a static sql-statement which will be performed with every occuring message-event
log4j.appender.JDBC.sql=INSERT INTO LOGTEST (id, msg, created_on, created_by) VALUES (1, @MSG@, sysdate, 'me')

// 4. Table-option to specify one table contained by the database
log4j.appender.JDBC.table=logtest

// 5. Columns-option to describe the important columns of the table (Not nullable columns are mandatory to describe!)
log4j.appender.JDBC.columns=id_seq~EMPTY	id~ID~MyIDHandler	msg~MSG	created_on~TIMESTAMP	created_by~STATIC~Thomas Fenner (t.fenner@klopotek.de)

// 6. Layout-options to define the layout of the messages (optional)
log4j.appender.JDBC.layout=org.apache.log4j.PatternLayout
log4j.appender.JDBC.layout.ConversionPattern=%m

// 7. Buffer-option to define the size of the message-event-buffer (optional)
log4j.appender.JDBC.buffer=1

// 8. Commit-option to define a auto-commitment
log4j.appender.JDBC.commit=Y
*/

// Here is a code example to configure the JDBCAppender with a configuration-file :

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

// Here is a code example to configure the JDBCAppender without a configuration-file :
/*
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
   	// A JDBCIDHandler
	   MyIDHandler idhandler = new MyIDHandler();

      // Ensure to have all necessary drivers installed !
   	try
      {
			Driver d = (Driver)(Class.forName("oracle.jdbc.driver.OracleDriver").newInstance());
			DriverManager.registerDriver(d);
		}
      catch(Exception e){}

      // Set the priority which messages have to be logged
		cat.setPriority(Priority.DEBUG);

      // Create a new instance of JDBCAppender
      JDBCAppender ja = new JDBCAppender();

      // Set options with method setOption()
      ja.setOption(JDBCAppender.CONNECTOR_OPTION, "MyConnectionHandler");
      ja.setOption(JDBCAppender.URL_OPTION, "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(COMMUNITY=tcp.world)(PROTOCOL=TCP)(Host=LENZI)(Port=1521))(ADDRESS=(COMMUNITY=tcp.world)(PROTOCOL=TCP)(Host=LENZI)(Port=1526)))(CONNECT_DATA=(SID=LENZI)))");
      ja.setOption(JDBCAppender.USERNAME_OPTION, "mex_pr_dev60");
      ja.setOption(JDBCAppender.PASSWORD_OPTION, "mex_pr_dev60");

		ja.setOption(JDBCAppender.TABLE_OPTION, "logtest");

      // There are two ways to setup the column-descriptions :
      // 1. Use the the method setOption(JDBCAppender.COLUMNS_OPTION, column-description)
		//ja.setOption(JDBCAppender.COLUMNS_OPTION, "id_seq~EMPTY	id~ID~MyIDHandler	msg~MSG	created_on~TIMESTAMP	created_by~STATIC~:-) Thomas Fenner (t.fenner@klopotek.de)");
		// 2. Use the better way of coding with method setLogType(String columnname, int LogType.xxx, Object xxx)
		ja.setLogType("id_seq", LogType.EMPTY, "");
		ja.setLogType("id", LogType.ID, idhandler);
		ja.setLogType("msg", LogType.MSG, "");
		ja.setLogType("created_on", LogType.TIMESTAMP, "");
		ja.setLogType("created_by", LogType.STATIC, "FEN");

      // If you just want to perform a static sql-statement, forget about the table- and columns-options,
      // and use this one :
		//ja.setOption(JDBCAppender.SQL_OPTION, "INSERT INTO LOGTEST (id, msg, created_on, created_by) VALUES (1, @MSG@, sysdate, 'me')");

      // other options
		//ja.setOption(JDBCAppender.BUFFER_OPTION, "1");
		//ja.setOption(JDBCAppender.COMMIT_OPTION, "Y");

      // Define a layout
		//ja.setLayout(new PatternLayout("%m"));

      // Add the appender to a category
      cat.addAppender(ja);

      // These messages with Priority >= setted priority will be logged to the database.
		cat.debug("debug");
      cat.info("info");
      cat.error("error");
      cat.fatal("fatal");
	}
}
*/

// Implement a sample JDBCConnectionHandler
class MyConnectionHandler implements JDBCConnectionHandler
{
	Connection con = null;
   //Default connection
	String url = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(COMMUNITY=tcp.world)(PROTOCOL=TCP)(Host=LENZI)(Port=1521))(ADDRESS=(COMMUNITY=tcp.world)(PROTOCOL=TCP)(Host=LENZI)(Port=1526)))(CONNECT_DATA=(SID=LENZI)))";
   String username = "mex_pr_dev60";
   String password = "mex_pr_dev60";

   public Connection getConnection()
   {
   	return getConnection(url, username, password);
   }

   public Connection getConnection(String _url, String _username, String _password)
   {
   	try
      {
   		if(con != null && !con.isClosed()) con.close();
			con = DriverManager.getConnection(_url, _username, _password);
			con.setAutoCommit(false);
      }
      catch(Exception e){}

   	return con;
   }
}


// Implement a sample JDBCIDHandler
class MyIDHandler implements JDBCIDHandler
{
	private static long id = 0;

	public synchronized Object getID()
   {
		return new Long(++id);
   }
}


