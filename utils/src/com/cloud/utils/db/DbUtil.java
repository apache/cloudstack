/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.utils.db;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;

public class DbUtil {
    protected final static Logger s_logger = Logger.getLogger(DbUtil.class);
    
    private static Map<String, Connection> s_connectionForGlobalLocks = new HashMap<String, Connection>();
    
    public static Connection getConnectionForGlobalLocks(String name, boolean forLock) {
    	while(true) {
	    	synchronized(s_connectionForGlobalLocks) {
	    		if(forLock) {
	    			if(s_connectionForGlobalLocks.get(name) != null) {
	    				s_logger.error("Sanity check failed, global lock name " + name + " is in already in use");
	    			}
	    			
	    			Connection connection = Transaction.getStandaloneConnection();
	    			if(connection != null) {
	    				try {
							connection.setAutoCommit(true);
						} catch (SQLException e) {
							try {
								connection.close();
							} catch(SQLException sqlException) {
							}
							return null;
						}
						s_connectionForGlobalLocks.put(name, connection);
						return connection;
	    			}
	    		} else {
	    			Connection connection = s_connectionForGlobalLocks.get(name);
	    			s_connectionForGlobalLocks.remove(name);
	    			return connection;
	    		}
	    	}
	    	
			s_logger.warn("Unable to acquire dabase connection for global lock " + name + ", waiting for someone to release and retrying...");
	    	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
    	}
    }
    
    public static void removeConnectionForGlobalLocks(String name) {
    	synchronized(s_connectionForGlobalLocks) {
    		s_connectionForGlobalLocks.remove(name);
    	}
    }
	
    public static String getColumnName(Field field, AttributeOverride[] overrides) {
        if (overrides != null) {
            for (AttributeOverride override : overrides) {
                if (override.name().equals(field.getName())) {
                    return override.column().name();
                }
            }
        }
        
        assert(field.getAnnotation(Embedded.class) == null) : "Cannot get column name from embedded field: " + field.getName();
        
        Column column = field.getAnnotation(Column.class);
        return column != null ? column.name() : field.getName();
    }
    
    public static String getColumnName(Field field) {
        return getColumnName(field, null);
    }
    
    public static String getReferenceColumn(PrimaryKeyJoinColumn pkjc) {
        return pkjc.referencedColumnName().length() != 0
            ? pkjc.referencedColumnName()
            : pkjc.name();
    }
    
    public static PrimaryKeyJoinColumn[] getPrimaryKeyJoinColumns(Class<?> clazz) {
        PrimaryKeyJoinColumn pkjc = clazz.getAnnotation(PrimaryKeyJoinColumn.class);
        if (pkjc != null) {
            return new PrimaryKeyJoinColumn[] { pkjc };
        }
        
        PrimaryKeyJoinColumns pkjcs = clazz.getAnnotation(PrimaryKeyJoinColumns.class);
        if (pkjcs != null) {
            return pkjcs.value();
        }
        
        return null;
    }
    
    public static Field findField(Class<?> clazz, String columnName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getAnnotation(Embedded.class) != null || field.getAnnotation(EmbeddedId.class) != null) {
                findField(field.getClass(), columnName);
            } else {
                if (columnName.equals(DbUtil.getColumnName(field))) {
                    return field;
                }
            }
        }
        return null;
    }
    
    public static final AttributeOverride[] getAttributeOverrides(AnnotatedElement ae) {
        AttributeOverride[] overrides = null;
        
        AttributeOverrides aos = ae.getAnnotation(AttributeOverrides.class);
        if (aos != null) {
            overrides = aos.value();
        }
        
        if (overrides == null || overrides.length == 0) {
            AttributeOverride override = ae.getAnnotation(AttributeOverride.class);
            if (override != null) {
                overrides = new AttributeOverride[1];
                overrides[0] = override;
            } else {
                overrides = new AttributeOverride[0];
            }
        }
        
        return overrides;
    }
    
    public static final boolean isPersistable(Field field) {
        if (field.getAnnotation(Transient.class) != null) {
            return false;
        }
        
        int modifiers = field.getModifiers();
        return !(Modifier.isFinal(modifiers) ||
                 Modifier.isStatic(modifiers) ||
                 Modifier.isTransient(modifiers));
    }
    
    public static final boolean isIdField(Field field) {
        if (field.getAnnotation(Id.class) != null) {
            return true;
        }
        
        if (field.getAnnotation(EmbeddedId.class) != null) {
            assert (field.getClass().getAnnotation(Embeddable.class) != null) : "Class " + field.getClass().getName() + " must be Embeddable to be used as Embedded Id";
            return true;
        }
        
        return false;
    }
    
    public static final SecondaryTable[] getSecondaryTables(AnnotatedElement clazz) {
        SecondaryTable[] sts = null;
        SecondaryTable stAnnotation = clazz.getAnnotation(SecondaryTable.class);
        if (stAnnotation == null) {
            SecondaryTables stsAnnotation = clazz.getAnnotation(SecondaryTables.class);
            sts = stsAnnotation != null ? stsAnnotation.value() : new SecondaryTable[0];
        } else {
            sts = new SecondaryTable[] {stAnnotation};
        }
        
        return sts;
    }
    
    public static final String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        return table != null ? table.name() : clazz.getSimpleName();
    }
    
    public static boolean getGlobalLock(String name, int timeoutSeconds) {
        Connection conn = getConnectionForGlobalLocks(name, true);
        if(conn == null) {
            s_logger.error("Unable to acquire DB connection for global lock system");
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
            		 if(s_logger.isDebugEnabled())
            			 s_logger.debug("GET_LOCK() timed out on lock : " + name);
            	 }
            }
        } catch (SQLException e) {
            s_logger.error("GET_LOCK() throws exception ", e);
        } catch (Throwable e) {
            s_logger.error("GET_LOCK() throws exception ", e);
        } finally {
        	if (pstmt != null) {
        		try {
        			pstmt.close();
        		} catch (SQLException e) {
        			s_logger.error("What the heck? ", e);
        		}
        	}
        }
        
        removeConnectionForGlobalLocks(name);
        try {
			conn.close();
		} catch (SQLException e) {
		}
        return false;
    }
    
    public static boolean releaseGlobalLock(String name) {
        Connection conn = getConnectionForGlobalLocks(name, false);
        if(conn == null) {
            s_logger.error("Unable to acquire DB connection for global lock system");
        	return false;
        }
        
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement("SELECT COALESCE(RELEASE_LOCK(?), 0)");
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if(rs != null && rs.first())
            	return rs.getInt(1) > 0;
            s_logger.error("RELEASE_LOCK() returns unexpected result : " + rs.getInt(1));
        } catch (SQLException e) {
            s_logger.error("RELEASE_LOCK() throws exception ", e);
        } catch (Throwable e) {
            s_logger.error("RELEASE_LOCK() throws exception ", e);
        } finally {
        	try {
            	if (pstmt != null) {
    	        	pstmt.close();
            	}
        		conn.close();
        	} catch(SQLException e) {
        	}
        }
        return false;
    }
}
