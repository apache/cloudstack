// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.async;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.async.dao.SyncQueueDao;
import com.cloud.async.dao.SyncQueueItemDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local(value={SyncQueueManager.class})
public class SyncQueueManagerImpl extends ManagerBase implements SyncQueueManager {
    public static final Logger s_logger = Logger.getLogger(SyncQueueManagerImpl.class.getName());

    @Inject private SyncQueueDao _syncQueueDao;
    @Inject private SyncQueueItemDao _syncQueueItemDao;

    @Override
    @DB
    public SyncQueueVO queue(String syncObjType, long syncObjId, String itemType, long itemId, long queueSizeLimit) {
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();

            _syncQueueDao.ensureQueue(syncObjType, syncObjId);
            SyncQueueVO queueVO = _syncQueueDao.find(syncObjType, syncObjId);
            if(queueVO == null)
                throw new CloudRuntimeException("Unable to queue item into DB, DB is full?");

            queueVO.setQueueSizeLimit(queueSizeLimit);
            _syncQueueDao.update(queueVO.getId(), queueVO);

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

            SyncQueueVO queueVO = _syncQueueDao.lockRow(queueId, true);
            if(queueVO == null) {
                s_logger.error("Sync queue(id: " + queueId + ") does not exist");
                txt.commit();
                return null;
            }

            if(queueReadyToProcess(queueVO)) {
                SyncQueueItemVO itemVO = _syncQueueItemDao.getNextQueueItem(queueVO.getId());
                if(itemVO != null) {
                    Long processNumber = queueVO.getLastProcessNumber();
                    if(processNumber == null)
                        processNumber = new Long(1);
                    else
                        processNumber = processNumber + 1;
                    Date dt = DateUtil.currentGMTTime();
                    queueVO.setLastProcessNumber(processNumber);
                    queueVO.setLastUpdated(dt);
                    queueVO.setQueueSize(queueVO.getQueueSize() + 1);
                    _syncQueueDao.update(queueVO.getId(), queueVO);

                    itemVO.setLastProcessMsid(msid);
                    itemVO.setLastProcessNumber(processNumber);
                    itemVO.setLastProcessTime(dt);
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
                    SyncQueueVO queueVO = _syncQueueDao.lockRow(item.getQueueId(), true);
                    SyncQueueItemVO itemVO = _syncQueueItemDao.lockRow(item.getId(), true);
                    if(queueReadyToProcess(queueVO) && itemVO.getLastProcessNumber() == null) {
                        Long processNumber = queueVO.getLastProcessNumber();
                        if(processNumber == null)
                            processNumber = new Long(1);
                        else
                            processNumber = processNumber + 1;

                        Date dt = DateUtil.currentGMTTime();
                        queueVO.setLastProcessNumber(processNumber);
                        queueVO.setLastUpdated(dt);
                        queueVO.setQueueSize(queueVO.getQueueSize() + 1);
                        _syncQueueDao.update(queueVO.getId(), queueVO);

                        itemVO.setLastProcessMsid(msid);
                        itemVO.setLastProcessNumber(processNumber);
                        itemVO.setLastProcessTime(dt);
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
                SyncQueueVO queueVO = _syncQueueDao.lockRow(itemVO.getQueueId(), true);

                _syncQueueItemDao.expunge(itemVO.getId());

                //if item is active, reset queue information
                if (itemVO.getLastProcessMsid() != null) {
                    queueVO.setLastUpdated(DateUtil.currentGMTTime());
                    //decrement the count
                    assert (queueVO.getQueueSize() > 0) : "Count reduce happens when it's already <= 0!";
                    queueVO.setQueueSize(queueVO.getQueueSize() - 1);
                    _syncQueueDao.update(queueVO.getId(), queueVO);
                }
            }
            txt.commit();
        } catch(Exception e) {
            s_logger.error("Unexpected exception: ", e);
            txt.rollback();
        }
    }

    @Override
    @DB
    public void returnItem(long queueItemId) {
        Transaction txt = Transaction.currentTxn();
        try {
            txt.start();

            SyncQueueItemVO itemVO = _syncQueueItemDao.findById(queueItemId);
            if(itemVO != null) {
                SyncQueueVO queueVO = _syncQueueDao.lockRow(itemVO.getQueueId(), true);

                itemVO.setLastProcessMsid(null);
                itemVO.setLastProcessNumber(null);
                itemVO.setLastProcessTime(null);
                _syncQueueItemDao.update(queueItemId, itemVO);

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
    public List<SyncQueueItemVO> getActiveQueueItems(Long msid, boolean exclusive) {
        return _syncQueueItemDao.getActiveQueueItems(msid, exclusive);
    }

    @Override
    public List<SyncQueueItemVO> getBlockedQueueItems(long thresholdMs, boolean exclusive) {
        return _syncQueueItemDao.getBlockedQueueItems(thresholdMs, exclusive);
    }

    private boolean queueReadyToProcess(SyncQueueVO queueVO) {
        return queueVO.getQueueSize() < queueVO.getQueueSizeLimit();
    }

    @Override
    public void purgeAsyncJobQueueItemId(long asyncJobId) {
        Long itemId = _syncQueueItemDao.getQueueItemIdByContentIdAndType(asyncJobId, SyncQueueItem.AsyncJobContentType);
        if (itemId != null) {
            purgeItem(itemId);
        }
    }
}
