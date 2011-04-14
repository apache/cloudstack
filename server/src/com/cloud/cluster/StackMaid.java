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

package com.cloud.cluster;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.cluster.dao.StackMaidDao;
import com.cloud.cluster.dao.StackMaidDaoImpl;
import com.cloud.serializer.SerializerHelper;
import com.cloud.utils.CleanupDelegate;
import com.cloud.utils.db.Transaction;

public class StackMaid {
    protected final static Logger s_logger = Logger.getLogger(StackMaid.class);
	
	private static ThreadLocal<StackMaid> threadMaid = new ThreadLocal<StackMaid>();
	
	private static long msid_setby_manager = 0;

	// tired of using component locator as I want this class to be accessed freely
	private StackMaidDao maidDao = new StackMaidDaoImpl(); 
	private int currentSeq = 0;
	private Map<String, Object> context = new HashMap<String, Object>();

	public static void init(long msid) {
		msid_setby_manager = msid;
	}
	
	public static StackMaid current() {
		StackMaid maid = threadMaid.get();
		if(maid == null) {
			maid = new StackMaid();
			threadMaid.set(maid);
		}
		return maid;
	}
	
	public void registerContext(String key, Object contextObject) {
		assert(!context.containsKey(key)) : "Context key has already been registered";
		context.put(key, contextObject);
	}
	
	public Object getContext(String key) {
		return context.get(key);
	}
	
	public void expungeMaidItem(long maidId) {
		// this is a bit ugly, but when it is not loaded by component locator, this is just a workable way for now
		Transaction txn = Transaction.open(Transaction.CLOUD_DB);
		try {
			maidDao.expunge(maidId);
		} finally {
			txn.close();
		}
	}

	public int push(String delegateClzName, Object context) {
		assert(msid_setby_manager != 0) : "Fatal, make sure StackMaidManager is loaded";
		if(msid_setby_manager == 0)
			s_logger.error("Fatal, make sure StackMaidManager is loaded");
		
		return push(msid_setby_manager, delegateClzName, context);
	}
	
	public int push(long currentMsid, String delegateClzName, Object context) {
		int savePoint = currentSeq;
		maidDao.pushCleanupDelegate(currentMsid, currentSeq++, delegateClzName, context);
		return savePoint;
	}

	public void pop(int savePoint) {
		assert(msid_setby_manager != 0) : "Fatal, make sure StackMaidManager is loaded";
		if(msid_setby_manager == 0)
			s_logger.error("Fatal, make sure StackMaidManager is loaded");
		
		pop(msid_setby_manager, savePoint);
	}
	
	public void pop() {
	    if(currentSeq > 0)
	        pop(currentSeq -1);
	}
	
	/**
	 * must be called within thread context
	 * @param currentMsid
	 */
	public void pop(long currentMsid, int savePoint) {
		while(currentSeq > savePoint) {
			maidDao.popCleanupDelegate(currentMsid);
			currentSeq--;
		}
	}
	
	public void exitCleanup() {
		exitCleanup(msid_setby_manager);
	}
	
	public void exitCleanup(long currentMsid) {
		if(currentSeq > 0) {
			CheckPointVO maid = null;
			while((maid = maidDao.popCleanupDelegate(currentMsid)) != null) {
				doCleanup(maid);
			}
			currentSeq = 0;
		}
		
		context.clear();
	}
	
	public static boolean doCleanup(CheckPointVO maid) {
		if(maid.getDelegate() != null) {
			try {
				Class<?> clz = Class.forName(maid.getDelegate());
				Object delegate = clz.newInstance();
				if(delegate instanceof CleanupDelegate) {
					return ((CleanupDelegate)delegate).cleanup(SerializerHelper.fromSerializedString(maid.getContext()), maid);
				} else {
					assert(false);
				}
			} catch (final ClassNotFoundException e) {
				s_logger.error("Unable to load StackMaid delegate class: " + maid.getDelegate(), e);
			} catch (final SecurityException e) {
				s_logger.error("Security excetion when loading resource: " + maid.getDelegate());
            } catch (final IllegalArgumentException e) {
            	s_logger.error("Illegal argument excetion when loading resource: " + maid.getDelegate());
            } catch (final InstantiationException e) {
            	s_logger.error("Instantiation excetion when loading resource: " + maid.getDelegate());
            } catch (final IllegalAccessException e) {
            	s_logger.error("Illegal access exception when loading resource: " + maid.getDelegate());
            } 
            
            return false;
		}
		return true;
	}
}
