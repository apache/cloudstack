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

package com.cloud.async;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.log4j.Logger;

import com.cloud.async.SyncQueueItemVO;
import com.cloud.async.SyncQueueVO;
import com.cloud.async.dao.SyncQueueDao;
import com.cloud.async.dao.SyncQueueItemDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;

@Local(value={SyncQueueManager.class})
public class SyncQueueManagerImpl implements SyncQueueManager {
    public static final Logger s_logger = Logger.getLogger(SyncQueueManagerImpl.class.getName());
    
    private String _name;
    
    private SyncQueueDao _syncQueueDao;
    private SyncQueueItemDao _syncQueueItemDao;

    @Override
    @DB
    public SyncQueueVO queue(String syncObjType, long syncObjId, String itemType, long itemId) {
        Transaction txn = Transaction.currentTxn();
    	try {
    		txn.start();
    		
    		_syncQueueDao.ensureQueue(syncObjType, syncObjId);
    		SyncQueueVO queueVO = _syncQueueDao.find(syncObjType, syncObjId);
    		if(queueVO == null)
    			throw new CloudRuntimeException("Unable to queue item into DB, DB is full?");

    		
			Date dt = DateUtil.currentGMTTime();
    		SyncQueueItemVO item = new SyncQueueItemVO();
    		item.setQueueId(queueVO.getId());
    		item.setContentType(itemType);
    		item.setContentId(itemId);
    		item.setCreated(dt);
    		
    		_syncQueueItemDao.persist(item);
    		txn.commit();
    		
    		return queueVO;
    	} catch(Exception e) {
    		s_logger.error("Unexpected exception: ", e);
    		txn.rollback();
    	}
    	return null;
    }
    
    @Override
    @DB
    public SyncQueueItemVO dequeueFromOne(long queueId, Long msid) {
    	Transaction txt = Transaction.currentTxn();
    	try {
    		txt.start();
    		
    		SyncQueueVO queueVO = _syncQueueDao.lock(queueId, true);
    		if(queueVO == null) {
    			s_logger.error("Sync queue(id: " + queueId + ") does not exist");
    			txt.commit();
    			return null;
    		}
    		
    		if(queueVO.getLastProcessTime() == null) {
    			SyncQueueItemVO itemVO = _syncQueueItemDao.getNextQueueItem(queueVO.getId());
    			if(itemVO != null) {
	    			Long processNumber = queueVO.getLastProcessNumber();
	    			if(processNumber == null)
	    				processNumber = new Long(1);
	    			else
	    				processNumber = processNumber + 1;
	    			Date dt = DateUtil.currentGMTTime();
	    			queueVO.setLastProcessMsid(msid);
	    			queueVO.setLastProcessNumber(processNumber);
	    			queueVO.setLastProcessTime(dt);
	    			queueVO.setLastUpdated(dt);
	    			_syncQueueDao.update(queueVO.getId(), queueVO);
	    			
	    			itemVO.setLastProcessMsid(msid);
	    			itemVO.setLastProcessNumber(processNumber);
	    			_syncQueueItemDao.update(itemVO.getId(), itemVO);
	    			
	        		txt.commit();
	    			return itemVO;
    			} else {
        			if(s_logger.isDebugEnabled())
        				s_logger.debug("Sync queue (" + queueId + ") is currently empty");
    			}
    		} else {
    			if(s_logger.isDebugEnabled())
    				s_logger.debug("There is a pending process in sync queue(id: " + queueId + ")");
    		}
    		txt.commit();
    	} catch(Exception e) {
    		s_logger.error("Unexpected exception: ", e);
    		txt.rollback();
    	}
    	
    	return null;
    }
    
    @Override
    @DB
    public List<SyncQueueItemVO> dequeueFromAny(Long msid, int maxItems) {
    	
    	List<SyncQueueItemVO> resultList = new ArrayList<SyncQueueItemVO>();
    	Transaction txt = Transaction.currentTxn();
    	try {
    		txt.start();
    		
    		List<SyncQueueItemVO> l = _syncQueueItemDao.getNextQueueItems(maxItems);
    		if(l != null && l.size() > 0) {
    			for(SyncQueueItemVO item : l) {
    				SyncQueueVO queueVO = _syncQueueDao.lock(item.getQueueId(), true);
	    			SyncQueueItemVO itemVO = _syncQueueItemDao.lock(item.getId(), true);
    				if(queueVO.getLastProcessTime() == null && itemVO.getLastProcessNumber() == null) {
		    			Long processNumber = queueVO.getLastProcessNumber();
		    			if(processNumber == null)
		    				processNumber = new Long(1);
		    			else
		    				processNumber = processNumber + 1;
		    			
		    			Date dt = DateUtil.currentGMTTime();
		    			queueVO.setLastProcessMsid(msid);
		    			queueVO.setLastProcessNumber(processNumber);
		    			queueVO.setLastProcessTime(dt);
		    			queueVO.setLastUpdated(dt);
		    			_syncQueueDao.update(queueVO.getId(), queueVO);
		    			
		    			itemVO.setLastProcessMsid(msid);
		    			itemVO.setLastProcessNumber(processNumber);
		    			_syncQueueItemDao.update(item.getId(), itemVO);
		    			
		    			resultList.add(item);
    				}
    			}
    		}
    		txt.commit();
    		return resultList;
    	} catch(Exception e) {
    		s_logger.error("Unexpected exception: ", e);
    		txt.rollback();
    	}
    	return null;
    }
    
    @Override
    @DB
    public void purgeItem(long queueItemId) {
    	Transaction txt = Transaction.currentTxn();
    	try {
    		txt.start();
    		
			SyncQueueItemVO itemVO = _syncQueueItemDao.findById(queueItemId);
			if(itemVO != null) {
				SyncQueueVO queueVO = _syncQueueDao.lock(itemVO.getQueueId(), true);
				
				_syncQueueItemDao.delete(itemVO.getId());
				
				queueVO.setLastProcessTime(null);
				queueVO.setLastUpdated(DateUtil.currentGMTTime());
				_syncQueueDao.update(queueVO.getId(), queueVO);
			}
    		txt.commit();
    	} catch(Exception e) {
    		s_logger.error("Unexpected exception: ", e);
    		txt.rollback();
    	}
    }
    
    @Override
	public List<SyncQueueItemVO> getActiveQueueItems(Long msid) {
    	return _syncQueueItemDao.getActiveQueueItems(msid);
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
    	_name = name;
		ComponentLocator locator = ComponentLocator.getCurrentLocator();
		
		_syncQueueDao = locator.getDao(SyncQueueDao.class);
		if (_syncQueueDao == null) {
			throw new ConfigurationException("Unable to get "
					+ SyncQueueDao.class.getName());
		}
		
		_syncQueueItemDao = locator.getDao(SyncQueueItemDao.class);
		if (_syncQueueItemDao == null) {
			throw new ConfigurationException("Unable to get "
					+ SyncQueueDao.class.getName());
		}
    	
    	return true;
    }
    
    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    @Override
    public String getName() {
    	return _name;
    }
}

