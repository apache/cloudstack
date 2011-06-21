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

// Here is a code example to configure the JDBCAppender without a configuration-file

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

