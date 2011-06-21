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
This class encapsulate the logic which is necessary to log into a table.
Used by JDBCAppender

<p><b>Author : </b><A HREF="mailto:t.fenner@klopotek.de">Thomas Fenner</A></p>

@since 1.0
*/
public class JDBCLogger
{
	//All columns of the log-table
	private ArrayList logcols = null;
   //Only columns which will be provided by logging
   private String column_list = null;
   //Number of all columns
	private int num = 0;
   //Status for successful execution of method configure()
	private boolean isconfigured = false;
   //Status for ready to do logging with method append()
	private boolean ready = false;
   //This message will be filled with a error-string when method ready() failes, and can be got by calling getMsg()
   private String errormsg = "";

	private Connection con = null;
	private Statement stmt = null;
	private ResultSet rs = null;
   private String table = null;

   //Variables for static SQL-statement logging
   private String sql = null;
	private String new_sql = null;
   private String new_sql_part1 = null;
   private String new_sql_part2 = null;
   private static final String msg_wildcard = "@MSG@";
	private int msg_wildcard_pos = 0;

	/**
	Writes a message into the database table.
	Throws an exception, if an database-error occurs !
	*/
	public void append(String _msg) throws Exception
	{
		if(!ready) if(!ready()) throw new Exception("JDBCLogger::append(), Not ready to append !");

      if(sql != null)
      {
      	appendSQL(_msg);
         return;
      }

		LogColumn logcol;

		rs.moveToInsertRow();

		for(int i=0; i<num; i++)
		{
        	logcol = (LogColumn)logcols.get(i);

			if(logcol.logtype == LogType.MSG)
			{
				rs.updateObject(logcol.name, _msg);
			}
			else if(logcol.logtype == LogType.ID)
			{
				rs.updateObject(logcol.name, logcol.idhandler.getID());
			}
			else if(logcol.logtype == LogType.STATIC)
			{
				rs.updateObject(logcol.name, logcol.value);
			}
			else if(logcol.logtype == LogType.TIMESTAMP)
			{
				rs.updateObject(logcol.name, new Timestamp((new java.util.Date()).getTime()));
			}
		}

		rs.insertRow();
	}

	/**
	Writes a message into the database using a given sql-statement.
	Throws an exception, if an database-error occurs !
	*/
	public void appendSQL(String _msg) throws Exception
	{
		if(!ready) if(!ready()) throw new Exception("JDBCLogger::appendSQL(), Not ready to append !");

      if(sql == null) throw new Exception("JDBCLogger::appendSQL(), No SQL-Statement configured !");

      if(msg_wildcard_pos > 0)
      {
			new_sql = new_sql_part1 + _msg + new_sql_part2;
      }
		else new_sql = sql;

      try
      {
			stmt.executeUpdate(new_sql);
      }
      catch(Exception e)
      {
      	errormsg = new_sql;
         throw e;
		}
	}


	/**
	Configures this class, by reading in the structure of the log-table
	Throws an exception, if an database-error occurs !
	*/
	public void configureTable(String _table) throws Exception
	{
   	if(isconfigured) return;

		//Fill logcols with META-informations of the table-columns
		stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
		rs = stmt.executeQuery("SELECT * FROM " + _table + " WHERE 1 = 2");

		LogColumn logcol;

		ResultSetMetaData rsmd = rs.getMetaData();

		num = rsmd.getColumnCount();

		logcols = new ArrayList(num);

		for(int i=1; i<=num; i++)
		{
			logcol = new LogColumn();
			logcol.name = rsmd.getColumnName(i).toUpperCase();
			logcol.type = rsmd.getColumnTypeName(i);
			logcol.nullable = (rsmd.isNullable(i) == rsmd.columnNullable);
         logcol.isWritable = rsmd.isWritable(i);
         if(!logcol.isWritable) logcol.ignore = true;
         logcols.add(logcol);
		}

      table = _table;

		isconfigured = true;
	}

	/**
	Configures this class, by storing and parsing the given sql-statement.
	Throws an exception, if somethings wrong !
	*/
	public void configureSQL(String _sql) throws Exception
	{
   	if(isconfigured) return;

		if(!isConnected()) throw new Exception("JDBCLogger::configureSQL(), Not connected to database !");

		if(_sql == null || _sql.trim().equals("")) throw new Exception("JDBCLogger::configureSQL(), Invalid SQL-Statement !");

		sql = _sql.trim();

      stmt = con.createStatement();

		msg_wildcard_pos = sql.indexOf(msg_wildcard);

      if(msg_wildcard_pos > 0)
      {
			new_sql_part1 = sql.substring(0, msg_wildcard_pos-1) + "'";
         //between the message...
         new_sql_part2 = "'" + sql.substring(msg_wildcard_pos+msg_wildcard.length());
		}

		isconfigured = true;
	}

	/**
   Sets a connection. Throws an exception, if the connection is not open !
	*/
	public void setConnection(Connection _con) throws Exception
	{
		con = _con;

		if(!isConnected()) throw new Exception("JDBCLogger::setConnection(), Given connection isnt connected to database !");
	}


	/**
	Sets a columns logtype (LogTypes) and value, which depends on that logtype.
	Throws an exception, if the given arguments arent correct !
   */
	public void setLogType(String _name, int _logtype, Object _value) throws Exception
	{
		if(!isconfigured) throw new Exception("JDBCLogger::setLogType(), Not configured !");

		//setLogType() makes only sense for further configuration of configureTable()
      if(sql != null) return;

      _name = _name.toUpperCase();

		if(_name == null || !(_name.trim().length() > 0)) throw new Exception("JDBCLogger::setLogType(), Missing argument name !");
		if(!LogType.isLogType(_logtype)) throw new Exception("JDBCLogger::setLogType(), Invalid logtype '" + _logtype + "' !");
		if((_logtype != LogType.MSG && _logtype != LogType.EMPTY) && _value == null) throw new Exception("JDBCLogger::setLogType(), Missing argument value !");

  		LogColumn logcol;

		for(int i=0; i<num; i++)
		{
        	logcol = (LogColumn)logcols.get(i);

			if(logcol.name.equals(_name))
			{
         	if(!logcol.isWritable) throw new Exception("JDBCLogger::setLogType(), Column " + _name + " is not writeable !");

				//Column gets the message
				if(_logtype == LogType.MSG)
            {
            	logcol.logtype = _logtype;
               return;
				}
				//Column will be provided by JDBCIDHandler::getID()
				else if(_logtype == LogType.ID)
				{
					logcol.logtype = _logtype;

               try
               {
               	//Try to cast directly Object to JDBCIDHandler
						logcol.idhandler = (JDBCIDHandler)_value;
               }
               catch(Exception e)
               {
               	try
                  {
                  	//Assuming _value is of class string which contains the classname of a JDBCIDHandler
							logcol.idhandler = (JDBCIDHandler)(Class.forName((String)_value).newInstance());
                  }
                  catch(Exception e2)
                  {
							throw new Exception("JDBCLogger::setLogType(), Cannot cast value of class " + _value.getClass() + " to class JDBCIDHandler !");
                  }
               }

               return;
				}

				//Column will be statically defined with Object _value
				else if(_logtype == LogType.STATIC)
				{
					logcol.logtype = _logtype;
					logcol.value = _value;
               return;
				}

				//Column will be provided with a actually timestamp
				else if(_logtype == LogType.TIMESTAMP)
				{
					logcol.logtype = _logtype;
               return;
				}

            //Column will be fully ignored during process.
            //If this column is not nullable, the column has to be filled by a database trigger,
            //else a database error occurs !
            //Columns which are not nullable, but should be not filled, must be explicit assigned with LogType.EMPTY,
            //else a value is required !
				else if(_logtype == LogType.EMPTY)
				{
					logcol.logtype = _logtype;
					logcol.ignore = true;
               return;
				}
			}
		}
	}


	/**
	Return true, if this class is ready to append(), else false.
	When not ready, a reason-String is stored in the instance-variable msg.
	*/
	public boolean ready()
	{
   	if(ready) return true;

		if(!isconfigured){ errormsg = "Not ready to append ! Call configure() first !"; return false;}

      //No need to doing the whole rest...
      if(sql != null)
      {
      	ready = true;
         return true;
      }

		boolean msgcol_defined = false;

		LogColumn logcol;

		for(int i=0; i<num; i++)
		{
      	logcol = (LogColumn)logcols.get(i);

         if(logcol.ignore || !logcol.isWritable) continue;
			if(!logcol.nullable && logcol.logtype == LogType.EMPTY)
         {
         	errormsg = "Not ready to append ! Column " + logcol.name + " is not nullable, and must be specified by setLogType() !";
            return false;
         }
			if(logcol.logtype == LogType.ID && logcol.idhandler == null)
         {
         	errormsg = "Not ready to append ! Column " + logcol.name + " is specified as an ID-column, and a JDBCIDHandler has to be set !";
            return false;
         }
			else if(logcol.logtype == LogType.STATIC && logcol.value == null)
         {
         	errormsg = "Not ready to append ! Column " + logcol.name + " is specified as a static field, and a value has to be set !";
            return false;
         }
         else if(logcol.logtype == LogType.MSG) msgcol_defined = true;
		}

      if(!msgcol_defined) return false;

      //create the column_list
		for(int i=0; i<num; i++)
		{
      	logcol = (LogColumn)logcols.get(i);

			if(logcol.ignore || !logcol.isWritable) continue;

         if(logcol.logtype != LogType.EMPTY)
         {
				if(column_list == null)
            {
            	column_list = logcol.name;
            }
            else column_list += ", " + logcol.name;
         }
		}

      try
      {
			rs = stmt.executeQuery("SELECT " + column_list + " FROM " + table + " WHERE 1 = 2");
		}
      catch(Exception e)
      {
			errormsg = "Not ready to append ! Cannot select columns '" + column_list + "' of table " + table + " !";
      	return false;
      }

		ready = true;

		return true;
	}

	/**
	Return true, if this class is configured, else false.
	*/
	public boolean isConfigured(){ return isconfigured;}

	/**
	Return true, if this connection is open, else false.
	*/
	public boolean isConnected()
   {
   	try
      {
   		return (con != null && !con.isClosed());
      }
      catch(Exception e){return false;}
   }

	/**
	Return the internal error message stored in instance variable msg.
	*/
   public String getErrorMsg(){String r = new String(errormsg); errormsg = null; return r;}
}


/**
This class encapsulate all by class JDBCLogger needed data around a column
*/
class LogColumn
{
	//column name
	String name = null;
   //column type
	String type = null;
   //not nullability means that this column is mandatory
	boolean nullable = false;
   //isWritable means that the column can be updated, else column is only readable
   boolean isWritable = false;
   //if ignore is true, this column will be ignored by building sql-statements.
   boolean ignore = false;

	//Must be filled for not nullable columns ! In other case it is optional.
	int logtype = LogType.EMPTY;
	Object value = null;				//Generic storage for typewrapper-classes Long, String, etc...
	JDBCIDHandler idhandler = null;
}


/**
This class contains all constants which are necessary to define a columns log-type.
*/
class LogType
{
	//A column of this type will receive the message.
	public static final int MSG = 1;

	//A column of this type will be a unique identifier of the logged row.
	public static final int ID = 2;

	//A column of this type will contain a static, one-time-defined value.
	public static final int STATIC = 3;

  	//A column of this type will be filled with an actual timestamp depending by the time the logging will be done.
	public static final int TIMESTAMP = 4;

	//A column of this type will contain no value and will not be included in logging insert-statement.
   //This could be a column which will be filled not by creation but otherwhere...
	public static final int EMPTY = 5;


	public static boolean isLogType(int _lt)
	{
		if(_lt == MSG || _lt == STATIC || _lt == ID || _lt == TIMESTAMP || _lt == EMPTY) return true;

		return false;
	}

   public static int parseLogType(String _lt)
   {
		if(_lt.equals("MSG")) return MSG;
		if(_lt.equals("ID")) return ID;
		if(_lt.equals("STATIC")) return STATIC;
		if(_lt.equals("TIMESTAMP")) return TIMESTAMP;
		if(_lt.equals("EMPTY")) return EMPTY;

      return -1;
   }
}





