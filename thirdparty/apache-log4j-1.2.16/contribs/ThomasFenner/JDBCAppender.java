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

package com.klopotek.utils.log;

import java.sql.*;
import java.util.*;
import org.apache.log4j.*;
import org.apache.log4j.helpers.*;
import org.apache.log4j.spi.*;

/**
The JDBCAppender, writes messages into a database

<p><b>The JDBCAppender is configurable at runtime by setting options in two alternatives : </b></p>
<dir>
	<p><b>1. Use a configuration-file</b></p>
	<p>Define the options in a file (<A HREF="configfile_example.txt">example</A>) and call a <code>PropertyConfigurator.configure(filename)</code> in your code.</p>
	<p><b>2. Use the methods of JDBCAppender to do it</b></p>
	<p>Call <code>JDBCAppender::setOption(JDBCAppender.xxx_OPTION, String value)</code> to do it analogically without a configuration-file (<A HREF="code_example2.java">example</A>)</p>
</dir>

<p>All available options are defined as static String-constants in JDBCAppender named xxx_OPTION.</p>

<p><b>Here is a description of all available options :</b></p>
<dir>
	<p><b>1. Database-options to connect to the database</b></p>
	<p>- <b>URL_OPTION</b>			: a database url of the form jdbc:subprotocol:subname</p>
	<p>- <b>USERNAME_OPTION</b>	: the database user on whose behalf the connection is being made</p>
	<p>- <b>PASSWORD_OPTION</b>	: the user's password</p>

	<p><b>2. Connector-option to specify your own JDBCConnectionHandler</b></p>
	<p>- <b>CONNECTOR_OPTION</b>	: a classname which is implementing the JDBCConnectionHandler-interface</p>
	<p>This interface is used to get a customized connection.</p>
	<p>If in addition the database-options are given, these options will be used as arguments for the JDBCConnectionHandler-interface to get a connection.</p>
	<p>Else if no database-options are given, the JDBCConnectionHandler-interface is called without them.</p>
	<p>Else if this option is not defined, the database-options are required to open a connection by the JDBCAppender.</p>

	<p><b>3. SQL-option to specify a static sql-statement which will be performed with every occuring message-event</b></p>
	<p>- <b>SQL_OPTION</b>			: a sql-statement which will be used to write to the database</p>
	<p>Use the variable <b>@MSG@</b> on a location in the statement, which has to be dynamically replaced by the message-text.</p>
	<p>If you give this option, the table-option and columns-option will be ignored !</p>

	<p><b>4. Table-option to specify a table contained by the database</b></p>
	<p>- <b>TABLE_OPTION</b>		: the table in which the logging will be done</p>

	<p><b>5. Columns-option to describe the important columns of the table (Not nullable columns are mandatory to describe!)</b></p>
	<p>- <b>COLUMNS_OPTION</b>		: a formatted list of column-descriptions</p>
	<p>Each column description consists of</p>
	<dir>
		<p>- the <b><i>name</i></b> of the column (required)</p>
		<p>- a <b><i>logtype</i></b> which is a static constant of class LogType (required)</p>
		<p>- and a <b><i>value</i></b> which depends by the LogType (optional/required, depending by logtype)</p>
	</dir>
	<p>Here is a description of the available logtypes of class <b>{@link LogType}</b> and how to handle the <b><i>value</i></b>:</p>
	<dir>
		<p>o <b>MSG</b>			= a value will be ignored, the column will get the message. (One columns need to be of this type!)</p>
		<p>o <b>STATIC</b>		= the value will be filled into the column with every logged message. (Ensure that the type of value can be casted into the sql-type of the column!)</p>
		<p>o <b>ID</b>			= value must be a classname, which implements the JDBCIDHandler-interface.</p>
		<p>o <b>TIMESTAMP</b>	= a value will be ignored, the column will be filled with a actually timestamp with every logged message.</p>
		<p>o <b>EMPTY</b>		= a value will be ignored, the column will be ignored when writing to the database (Ensure to fill not nullable columns by a database trigger!)</p>
	</dir>
	<p>If there are more than one column to describe, the columns must be separated by a Tabulator-delimiter (unicode0008) !</p>
	<p>The arguments of a column-description must be separated by the delimiter '~' !</p>
	<p><i>(Example :  name1~logtype1~value1   name2~logtype2~value2...)</i></p>

	<p><b>6. Layout-options to define the layout of the messages (optional)</b></p>
	<p>- <b>_</b> : the layout wont be set by a xxx_OPTION</p>
	<p>See the configuration-file and code examples below...</p>
	<p>The default is a layout of the class {@link org.apache.log4j.PatternLayout} with the pattern=%m which representate only the message.</p>

	<p><b>7. Buffer-option to define the size of the message-event-buffer (optional)</b></p>
	<p>- <b>BUFFER_OPTION</b>		: define how many messages will be buffered until they will be updated to the database.</p>
	<p>The default is buffer=1, which will do a update with every happened message-event.</p>

	<p><b>8. Commit-option to define a auto-commitment</b></p>
	<p>- <b>COMMIT_OPTION</b>		: define whether updated messages should be committed to the database (Y) or not (N).</p>
	<p>The default is commit=Y.</p>
</dir>

<p><b>The sequence of some options is important :</b></p>
<dir>
	<p><b>1. Connector-option OR/AND Database-options</b></p>
	<p>Any database connection is required !</p>
	<p><b>2. (Table-option AND Columns-option) OR SQL-option</b></p>
	<p>Anything of that is required ! Whether where to write something OR what to write somewhere...;-)</p>
	<p><b>3. All other options can be set at any time...</b></p>
	<p>The other options are optional and have a default initialization, which can be customized.</p>
</dir>

<p><b>Here is a <b>configuration-file example</b>, which can be used as argument for the <b>PropertyConfigurator</b> : </b><A HREF="configfile_example.txt"> configfile_example.txt</A></p>

<p><b>Here is a <b>code-example</b> to configure the JDBCAppender <b>with a configuration-file</b> : </b><A HREF="code_example1.java"> code_example1.java</A></p>

<p><b>Here is a <b>another code-example</b> to configure the JDBCAppender <b>without a configuration-file</b> : </b><A HREF="code_example2.java"> code_example2.java</A></p>



<p><b>Author : </b><A HREF="mailto:t.fenner@klopotek.de">Thomas Fenner</A></p>

@since 1.0
*/
public class JDBCAppender extends AppenderSkeleton
{
	/**
	A database-option to to set a database url of the form jdbc:subprotocol:subname.
	*/
	public static final String URL_OPTION			= "url";

	/**
	A database-option to set the database user on whose behalf the connection is being made.
	*/
	public static final String USERNAME_OPTION	= "username";

	/**
	A database-option to set the user's password.
	*/
	public static final String PASSWORD_OPTION	= "password";

	/**
	A table-option to specify a table contained by the database
	*/
	public static final String TABLE_OPTION		= "table";

	/**
	A connector-option to specify your own JDBCConnectionHandler
	*/
	public static final String CONNECTOR_OPTION	= "connector";

	/**
   A columns-option to describe the important columns of the table
	*/
	public static final String COLUMNS_OPTION		= "columns";

	/**
	A sql-option to specify a static sql-statement which will be performed with every occuring message-event
   */
	public static final String SQL_OPTION			= "sql";

	/**
   A buffer-option to define the size of the message-event-buffer
	*/
	public static final String BUFFER_OPTION		= "buffer";

	/**
   A commit-option to define a auto-commitment
	*/
	public static final String COMMIT_OPTION		= "commit";


	//Variables to store the options values setted by setOption() :
	private String url		= null;
	private String username	= null;
	private String password	= null;
	private String table		= null;
	private String connection_class = null;
	private String sql		= null;
	private boolean docommit = true;
	private int buffer_size	= 1;
   private JDBCConnectionHandler connectionHandler = null;

	//This buffer stores message-events.
   //When the buffer_size is reached, the buffer will be flushed and the messages will updated to the database.
	private ArrayList buffer = new ArrayList();

   //Database-connection
	private Connection con = null;

	//This class encapsulate the logic which is necessary to log into a table
	private JDBCLogger jlogger = new JDBCLogger();

   //Flags :
   //A flag to indicate a established database connection
	private boolean connected = false;
   //A flag to indicate configuration status
	private boolean configured = false;
   //A flag to indicate that everything is ready to get append()-commands.
	private boolean ready = false;

	/**
	If program terminates close the database-connection and flush the buffer
   */
	public void finalize()
	{
		close();
      super.finalize();
	}

	/**
	Internal method. Returns a array of strings containing the available options which can be set with method setOption()
	*/
	public String[] getOptionStrings()
   {
   	// The sequence of options in this string is important, because setOption() is called this way ...
		return new String[]{CONNECTOR_OPTION, URL_OPTION, USERNAME_OPTION, PASSWORD_OPTION, SQL_OPTION, TABLE_OPTION, COLUMNS_OPTION, BUFFER_OPTION, COMMIT_OPTION};
	}


	/**
     Sets all necessary options
	*/
	public void setOption(String _option, String _value)
	{
   	_option = _option.trim();
      _value = _value.trim();

		if(_option == null || _value == null) return;
		if(_option.length() == 0 || _value.length() == 0) return;

      _value = _value.trim();

		if(_option.equals(CONNECTOR_OPTION))
      {
      	if(!connected) connection_class = _value;
      }
		else if(_option.equals(URL_OPTION))
		{
			if(!connected) url = _value;
		}
		else if(_option.equals(USERNAME_OPTION))
		{
			if(!connected) username = _value;
		}
		else if(_option.equals(PASSWORD_OPTION))
		{
			if(!connected) password = _value;
		}
		else if(_option.equals(SQL_OPTION))
      {
			sql = _value;
      }
		else if(_option.equals(TABLE_OPTION))
      {
      	if(sql != null) return;
      	table = _value;
      }
		else if(_option.equals(COLUMNS_OPTION))
      {
      	if(sql != null) return;

			String name = null;
         int logtype = -1;
         String value = null;
         String column = null;
         String arg = null;
         int num_args = 0;
         int num_columns = 0;
			StringTokenizer st_col;
			StringTokenizer st_arg;

         //Columns are TAB-separated
			st_col = new StringTokenizer(_value,  "	");

			num_columns = st_col.countTokens();

         if(num_columns < 1)
  	      {
     	   	errorHandler.error("JDBCAppender::setOption(), Invalid COLUMN_OPTION value : " + _value + " !");
            return;
        	}

         for(int i=1; i<=num_columns; i++)
         {
				column = st_col.nextToken();

            //Arguments are ~-separated
				st_arg = new StringTokenizer(column, "~");

				num_args = st_arg.countTokens();

	         if(num_args < 2)
   	      {
      	   	errorHandler.error("JDBCAppender::setOption(), Invalid COLUMN_OPTION value : " + _value + " !");
               return;
         	}

	         for(int j=1; j<=num_args; j++)
   	      {
					arg = st_arg.nextToken();

					if(j == 1) name = arg;
					else if(j == 2)
      	      {
         	   	try
            	   {
							logtype = Integer.parseInt(arg);
	               }
   	            catch(Exception e)
      	         {
         	      	logtype = LogType.parseLogType(arg);
	               }

						if(!LogType.isLogType(logtype))
   	            {
	   	            errorHandler.error("JDBCAppender::setOption(), Invalid COLUMN_OPTION LogType : " + arg + " !");
                     return;
         	      }
            	}
					else if(j == 3) value = arg;
   	      }

	         if(!setLogType(name, logtype, value)) return;
         }
      }
		else if(_option.equals(BUFFER_OPTION))
      {
        	try
         {
				buffer_size = Integer.parseInt(_value);
         }
         catch(Exception e)
         {
	         errorHandler.error("JDBCAppender::setOption(), Invalid BUFFER_OPTION value : " + _value + " !");
				return;
         }
      }
		else if(_option.equals(COMMIT_OPTION))
      {
      	docommit = _value.equals("Y");
      }

      if(_option.equals(SQL_OPTION) || _option.equals(TABLE_OPTION))
      {
			if(!configured) configure();
      }
	}

	/**
	Internal method. Returns true, you may define your own layout...
	*/
	public boolean requiresLayout()
	{
		return true;
	}


	/**
	Internal method. Close the database connection & flush the buffer.
	*/
	public void close()
	{
	   flush_buffer();
      if(connection_class == null)
      {
			try{con.close();}catch(Exception e){errorHandler.error("JDBCAppender::close(), " + e);}
      }
		this.closed = true;
	}


	/**
	You have to call this function for all provided columns of your log-table !
   */
	public boolean setLogType(String _name, int _logtype, Object _value)
	{
   	if(sql != null) return true;

		if(!configured)
		{
			if(!configure()) return false;
		}

		try
		{
			jlogger.setLogType(_name, _logtype, _value);
		}
		catch(Exception e)
		{
			errorHandler.error("JDBCAppender::setLogType(), " + e);
			return false;
		}

		return true;
	}


	/**
	Internal method. Appends the message to the database table.
	*/
	public void append(LoggingEvent event)
	{
		if(!ready)
      {
      	if(!ready())
         {
				errorHandler.error("JDBCAppender::append(), Not ready to append !");
         	return;
			}
      }

		buffer.add(event);

		if(buffer.size() >= buffer_size) flush_buffer();
	}


	/**
	Internal method. Flushes the buffer.
	*/
   public void flush_buffer()
   {
   	try
      {
      	int size = buffer.size();

         if(size < 1) return;

        	for(int i=0; i<size; i++)
         {
				LoggingEvent event = (LoggingEvent)buffer.get(i);

				//Insert message into database
				jlogger.append(layout.format(event));
         }

         buffer.clear();

			if(docommit) con.commit();
      }
		catch(Exception e)
		{
			errorHandler.error("JDBCAppender::flush_buffer(), " + e + " : " + jlogger.getErrorMsg());
			try{con.rollback();} catch(Exception ex){}
			return;
		}
   }


	/**
	Internal method. Returns true, when the JDBCAppender is ready to append messages to the database, else false.
	*/
	public boolean ready()
	{
   	if(ready) return true;

		if(!configured) return false;

		ready = jlogger.ready();

      if(!ready){errorHandler.error(jlogger.getErrorMsg());}

      return ready;
	}


	/**
	Internal method. Connect to the database.
	*/
	protected void connect() throws Exception
	{
   	if(connected) return;

		try
		{
      	if(connection_class == null)
         {
				if(url == null)		throw new Exception("JDBCAppender::connect(), No URL defined.");

				if(username == null)	throw new Exception("JDBCAppender::connect(), No USERNAME defined.");

				if(password == null)	throw new Exception("JDBCAppender::connect(), No PASSWORD defined.");

				connectionHandler = new DefaultConnectionHandler();
			}
         else
         {
				connectionHandler = (JDBCConnectionHandler)(Class.forName(connection_class).newInstance());
         }

         if(url != null && username != null && password != null)
         {
				con = connectionHandler.getConnection(url, username, password);
         }
         else
         {
	     		con = connectionHandler.getConnection();
         }

         if(con.isClosed())
         {
         	throw new Exception("JDBCAppender::connect(), JDBCConnectionHandler returns no connected Connection !");
			}
		}
		catch(Exception e)
		{
			throw new Exception("JDBCAppender::connect(), " + e);
		}

      connected = true;
	}

	/**
	Internal method. Configures for appending...
	*/
	protected boolean configure()
	{
		if(configured) return true;

		if(!connected)
		{
      	if((connection_class == null) && (url == null || username == null || password == null))
			{
				errorHandler.error("JDBCAppender::configure(), Missing database-options or connector-option !");
				return false;
         }

         try
         {
				connect();
         }
         catch(Exception e)
         {
         	connection_class = null;
            url = null;
				errorHandler.error("JDBCAppender::configure(), " + e);
            return false;
         }
		}

		if(sql == null && table == null)
		{
			errorHandler.error("JDBCAppender::configure(), No SQL_OPTION or TABLE_OPTION given !");
			return false;
		}

		if(!jlogger.isConfigured())
		{
			try
         {
         	jlogger.setConnection(con);

         	if(sql == null)
            {
	         	jlogger.configureTable(table);
            }
            else jlogger.configureSQL(sql);
         }
         catch(Exception e)
         {
	         errorHandler.error("JDBCAppender::configure(), " + e);
         	return false;
         }
		}

      //Default Message-Layout
      if(layout == null)
      {
      	layout = new PatternLayout("%m");
      }

      configured = true;

		return true;
	}
}

/**
This is a default JDBCConnectionHandler used by JDBCAppender
*/
class DefaultConnectionHandler implements JDBCConnectionHandler
{
	Connection con = null;

   public Connection getConnection()
   {
   	return con;
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






