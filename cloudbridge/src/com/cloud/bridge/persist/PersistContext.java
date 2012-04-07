/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.cloud.bridge.util.CloudSessionFactory;
import com.cloud.bridge.util.Tuple;

/**
 * @author Kelven Yang
 * 
 * We use Per-thread based hibernate session and transaction pattern. Transaction will be
 * started implicitly by EntityDao instances and be committed implicitly in the end of
 * request-process cycle. All requests are guarded by a dynamic proxy.
 * 
 * We will try to keep transaction management as implicit as we can, so that
 * most of service layer implementation contains business-logic only, all business logic are
 * built on top of domain object model, and all persistent layer handling lie within persist layer 
 * in Dao classes.
 *  
 * PersistContext class also provides per-thread based registry service and global named-lock service
 */
public class PersistContext {
    protected final static Logger logger = Logger.getLogger(PersistContext.class);
	
	private static final CloudSessionFactory sessionFactory;
	
	private static final ThreadLocal<Session> threadSession = new ThreadLocal<Session>();
	private static final ThreadLocal<Transaction> threadTransaction = new ThreadLocal<Transaction>();
	private static final ThreadLocal<Map<String, Object>> threadStore = new ThreadLocal<Map<String, Object>>(); 
	
	static {
		try {
			sessionFactory = CloudSessionFactory.getInstance();
		} catch(HibernateException e) {
			logger.error("Exception " + e.getMessage(), e);
			throw new PersistException(e);
		}
	}
	
	public static Session getSession() {
		Session s = threadSession.get();
		try {
			if(s == null) {
				s = sessionFactory.openSession();
				threadSession.set(s);
			}
		} catch(HibernateException e) {
			logger.error("Exception " + e.getMessage(), e);
			throw new PersistException(e);
		}
		return s;
	}
	
	public static void closeSession() {
		try {
			Session s = (Session) threadSession.get();
			threadSession.set(null);
			
			if (s != null && s.isOpen())
				s.close();
		} catch(HibernateException e) {
			logger.error("Exception " + e.getMessage(), e);
			throw new PersistException(e);
		}		
	}
	
	public static void beginTransaction() {
		Transaction tx = threadTransaction.get();
		try {
			if (tx == null) {
				tx = getSession().beginTransaction();
				threadTransaction.set(tx);
			}
		} catch(HibernateException e) {
			logger.error("Exception " + e.getMessage(), e);
			throw new PersistException(e);
		}		
	}
	
	public static void commitTransaction() {
		Transaction tx = threadTransaction.get();
		try {
			if ( tx != null && !tx.wasCommitted() && !tx.wasRolledBack() )
				tx.commit();
			threadTransaction.set(null);
		} catch (HibernateException e) {
			logger.error("Exception " + e.getMessage(), e);
			
			rollbackTransaction();
			throw new PersistException(e);
		}		
	}
	
	public static void rollbackTransaction() {
		Transaction tx = (Transaction) threadTransaction.get();
		try {
			threadTransaction.set(null);
			if ( tx != null && !tx.wasCommitted() && !tx.wasRolledBack() ) {
				tx.rollback();
			}
		} catch (HibernateException e) {
			logger.error("Exception " + e.getMessage(), e);
			throw new PersistException(e);
		} finally {
			closeSession();
		}
	}
	
  	public static void flush() {
  		commitTransaction();
  		beginTransaction();
	}

  	/**
  	 * acquireNamedLock/releaseNamedLock must be called in pairs and within the same thread
  	 * they can not be called recursively neither
  	 * 
  	 * @param name
  	 * @param timeoutSeconds
  	 * @return
  	 */
  	public static boolean acquireNamedLock(String name, int timeoutSeconds) {
  		Connection conn = getJDBCConnection(name, true);
  		if(conn == null) {
  			logger.warn("Unable to acquire named lock connection for named lock: " + name);
  			return false;
  		}
  		
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement("SELECT COALESCE(GET_LOCK(?, ?),0)");

            pstmt.setString(1, name);
            pstmt.setInt(2, timeoutSeconds);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs != null && rs.first()) {
            	 if(rs.getInt(1) > 0) {
            		 return true;
            	 } else {
                     logger.error("GET_LOCK() timed out on lock : " + name);
            	 }
            }
        } catch (SQLException e) {
            logger.error("GET_LOCK() throws exception ", e);
        } catch (Throwable e) {
            logger.error("GET_LOCK() throws exception ", e);
        } finally {
        	if (pstmt != null) {
        		try {
        			pstmt.close();
        		} catch (SQLException e) {
        			logger.error("Unexpected exception " + e.getMessage(), e);
        		}
        	}
        }
        
        releaseJDBCConnection(name);
        return false;
  	}
  	
    public static boolean releaseNamedLock(String name) {
        Connection conn = getJDBCConnection(name, false);
        if(conn == null) {
            logger.error("Unable to acquire DB connection for global lock system");
        	return false;
        }
        
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement("SELECT COALESCE(RELEASE_LOCK(?), 0)");
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if(rs != null && rs.first())
            	return rs.getInt(1) > 0;
            logger.error("RELEASE_LOCK() returns unexpected result : " + rs.getInt(1));
        } catch (SQLException e) {
            logger.error("RELEASE_LOCK() throws exception ", e);
        } catch (Throwable e) {
            logger.error("RELEASE_LOCK() throws exception ", e);
        } finally {
        	releaseJDBCConnection(name);
        }
        return false;
    }
  	
  	@SuppressWarnings("deprecation")
	private static Connection getJDBCConnection(String name, boolean allocNew) {
  		String registryKey = "JDBC-Connection." + name;
  		Tuple<Session, Connection> info = (Tuple<Session, Connection>)getThreadStoreObject(registryKey);
  		if(info == null && allocNew) {
  			Session session = sessionFactory.openSession();
  			Connection connection = session.connection();
  			if(connection == null) {
  				session.close();
  				return null;
  			}
  				
  			try {
  				connection.setAutoCommit(true);
  			} catch(SQLException e) {
  				logger.warn("Unexpected exception " + e.getMessage(), e);
				try {
					connection.close();
					session.close();
				} catch(Throwable ex) {
	  				logger.warn("Unexpected exception " + e.getMessage(), e);
				}
				return null;
  			}
  			
  			registerThreadStoreObject(registryKey, new Tuple<Session, Connection>(session, connection));
  			return connection;
  		}
  		
  		if(info != null)
  			return info.getSecond();
  		
  		return null;
  	}
  	
  	private static void releaseJDBCConnection(String name) {
  		String registryKey = "JDBC-Connection." + name;
  		Tuple<Session, Connection> info = (Tuple<Session, Connection>)unregisterThreadStoreObject(registryKey);
  		if(info != null) {
  			try {
  				info.getSecond().close();
  				info.getFirst().close();
  			} catch(Throwable e) {
  				logger.warn("Unexpected exception " + e.getMessage(), e);
  			}
  		}
  	}
  	
  	public static void registerThreadStoreObject(String name, Object object) {
  		Map<String, Object> store = getThreadStore();
  		store.put(name, object);
  	}
  	
  	public static Object getThreadStoreObject(String name) {
  		Map<String, Object> store = getThreadStore();
  		return store.get(name);
  	}
  	
  	public static Object unregisterThreadStoreObject(String name) {
  		Map<String, Object> store = getThreadStore();
  		if(store.containsKey(name)) {
  			Object value = store.get(name);
  			store.remove(name);
  			return value;
  		}
  		return null;
  	}
  	
  	private static Map<String, Object> getThreadStore() {
  		Map<String, Object> store = threadStore.get();
  		if(store == null) {
  			store = new HashMap<String, Object>();
  			threadStore.set(store);
  		}
  		return store;
  	}
}
