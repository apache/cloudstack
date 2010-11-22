package com.cloud.maid;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.maid.dao.StackMaidDao;
import com.cloud.maid.dao.StackMaidDaoImpl;
import com.cloud.serializer.SerializerHelper;
import com.cloud.utils.ActionDelegate;

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
/*
		assert(msid_setby_manager != 0) : "Fatal, make sure StackMaidManager is loaded";
		if(msid_setby_manager == 0)
			s_logger.error("Fatal, make sure StackMaidManager is loaded");
*/			
		
		exitCleanup(msid_setby_manager);
	}
	
	public void exitCleanup(long currentMsid) {
		if(currentSeq > 0) {
			StackMaidVO maid = null;
			while((maid = maidDao.popCleanupDelegate(currentMsid)) != null) {
				doCleanup(maid);
			}
			currentSeq = 0;
		}
		
		context.clear();
	}
	
	public static void doCleanup(StackMaidVO maid) {
		if(maid.getDelegate() != null) {
			try {
				Class<?> clz = Class.forName(maid.getDelegate());
				Object delegate = clz.newInstance();
				if(delegate instanceof ActionDelegate) {
					((ActionDelegate)delegate).action(SerializerHelper.fromSerializedString(maid.getContext()));
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
		}
	}
}
