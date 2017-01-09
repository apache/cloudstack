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
package org.apache.cloudstack.framework.jobs.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.framework.jobs.dao.SyncQueueDao;
import org.apache.cloudstack.framework.jobs.dao.SyncQueueItemDao;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.api.BaseAsyncCmd;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.VMInstanceDao;

public class SyncQueueManagerImpl extends ManagerBase implements SyncQueueManager {
    public static final Logger s_logger = Logger.getLogger(SyncQueueManagerImpl.class.getName());

    @Inject
    private SyncQueueDao _syncQueueDao;
    @Inject
    private SyncQueueItemDao _syncQueueItemDao;
    @Inject
    private AsyncJobDao _asyncJobDao;
    @Inject
    private VMInstanceDao _vmInstanceDao;

    @Override
    @DB
    public SyncQueueVO queue(final String syncObjType, final long syncObjId, final String itemType, final long itemId) {
        try {
            return Transaction.execute(new TransactionCallback<SyncQueueVO>() {
                @Override
                public SyncQueueVO doInTransaction(TransactionStatus status) {
                    _syncQueueDao.ensureQueue(syncObjType, syncObjId);
                    SyncQueueVO queueVO = _syncQueueDao.find(syncObjType, syncObjId);
                    if (queueVO == null)
                        throw new CloudRuntimeException("Unable to queue item into DB, DB is full?");

                    Date dt = DateUtil.currentGMTTime();
                    SyncQueueItemVO item = new SyncQueueItemVO();
                    item.setQueueId(queueVO.getId());
                    item.setContentType(itemType);
                    item.setContentId(itemId);
                    item.setCreated(dt);

                    _syncQueueItemDao.persist(item);
                    return queueVO;
                }
            });
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
        }
        return null;
    }

    @Override
    @DB
    public SyncQueueVO setQueueLimit(final String syncObjType, final long syncObjId, final long limit) {
        if (s_logger.isDebugEnabled())
            s_logger.debug("Setting queue limit: " + limit + " for sync object type: " + syncObjType + " with id: " + syncObjId);

        try {
            return Transaction.execute(new TransactionCallback<SyncQueueVO>() {
                @Override
                public SyncQueueVO doInTransaction(TransactionStatus status) {
                    _syncQueueDao.ensureQueue(syncObjType, syncObjId);
                    SyncQueueVO queueVO = _syncQueueDao.find(syncObjType, syncObjId);
                    if (queueVO == null)
                        throw new CloudRuntimeException("Unable to queue item into DB, DB is full?");

                    queueVO.setQueueSizeLimit(limit);
                    _syncQueueDao.update(queueVO.getId(), queueVO);

                    return queueVO;
                }
            });
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
        }
        return null;
    }

    @Override
    @DB
    public SyncQueueItemVO dequeueFromOne(final long queueId, final Long msid) {
        try {
            return Transaction.execute(new TransactionCallback<SyncQueueItemVO>() {
                @Override
                public SyncQueueItemVO doInTransaction(TransactionStatus status) {
                    SyncQueueVO queueVO = _syncQueueDao.findById(queueId);
                    if(queueVO == null) {
                        s_logger.error("Sync queue(id: " + queueId + ") does not exist");
                        return null;
                    }

                    SyncQueueItemVO itemVO = _syncQueueItemDao.getNextQueueItem(queueVO.getId());
                    if (itemVO != null) {
                        if (queueReadyToProcess(queueVO, itemVO)) {
                            String jobCmd = getJobCmd(itemVO);
                            if (!queueReadyToProcessJob(queueVO, jobCmd))
                                return null;

                            updateSyncQueue(queueVO, true);
                            if (allowSubsequentConcurrencyForJob(jobCmd)) {
                                incrementSyncQueueLimit(queueVO);
                            }
                            _syncQueueDao.update(queueVO.getId(), queueVO);

                            updateSyncQueueForJob(queueVO, jobCmd, true);
                            updateSyncQueueItem(itemVO, msid, queueVO.getLastProcessNumber());
                            _syncQueueItemDao.update(itemVO.getId(), itemVO);

                            return itemVO;
                        } else {
                            if (s_logger.isDebugEnabled())
                                s_logger.debug("There is a pending process in sync queue(id: " + queueId + ")");
                        }
                    } else {
                        if (s_logger.isDebugEnabled())
                            s_logger.debug("Sync queue (" + queueId + ") is currently empty");
                    }

                    return null;
                }
            });
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
        }

        return null;
    }

    @Override
    @DB
    public List<SyncQueueItemVO> dequeueFromAny(final Long msid, final int maxItems) {

        final List<SyncQueueItemVO> resultList = new ArrayList<SyncQueueItemVO>();

        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    List<SyncQueueItemVO> l = _syncQueueItemDao.getNextQueueItems(maxItems);
                    if(l != null && l.size() > 0) {
                        for (SyncQueueItemVO item : l) {
                            SyncQueueVO queueVO = _syncQueueDao.findById(item.getQueueId());
                            SyncQueueItemVO itemVO = _syncQueueItemDao.findById(item.getId());

                            if (itemVO != null && itemVO.getLastProcessNumber() == null && queueReadyToProcess(queueVO, itemVO)) {
                                String jobCmd = getJobCmd(itemVO);
                                if (!queueReadyToProcessJob(queueVO, jobCmd))
                                    continue;

                                updateSyncQueue(queueVO, true);
                                if (allowSubsequentConcurrencyForJob(jobCmd)) {
                                    incrementSyncQueueLimit(queueVO);
                                }
                                _syncQueueDao.update(queueVO.getId(), queueVO);

                                updateSyncQueueForJob(queueVO, jobCmd, true);
                                updateSyncQueueItem(itemVO, msid, queueVO.getLastProcessNumber());
                                _syncQueueItemDao.update(itemVO.getId(), itemVO);

                                resultList.add(itemVO);
                            }
                        }
                    }
                }
            });

            return resultList;
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
        }

        return null;
    }

    @Override
    @DB
    public void purgeItem(final long queueItemId) {
        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    SyncQueueItemVO itemVO = _syncQueueItemDao.findById(queueItemId);
                    if(itemVO != null) {
                        SyncQueueVO queueVO = _syncQueueDao.findById(itemVO.getQueueId());

                        _syncQueueItemDao.expunge(itemVO.getId());

                        // if item is active, reset queue information
                        if (itemVO.getLastProcessMsid() != null) {
                            updateSyncQueue(queueVO, false);
                            String jobCmd = getJobCmd(itemVO);
                            if (allowSubsequentConcurrencyForJob(jobCmd)) {
                                decrementSyncQueueLimit(queueVO);
                            }
                            _syncQueueDao.update(queueVO.getId(), queueVO);

                            updateSyncQueueForJob(queueVO, jobCmd, false);
                        }
                    }
                }
            });
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
        }
    }

    @Override
    @DB
    public void returnItem(final long queueItemId) {
        s_logger.info("Returning queue item " + queueItemId + " back to queue for second try in case of DB deadlock");
        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    SyncQueueItemVO itemVO = _syncQueueItemDao.findById(queueItemId);
                    if (itemVO != null) {
                        SyncQueueVO queueVO = _syncQueueDao.findById(itemVO.getQueueId());

                        updateSyncQueueItem(itemVO, null, null);
                        _syncQueueItemDao.update(itemVO.getId(), itemVO);

                        updateSyncQueue(queueVO, false);
                        String jobCmd = getJobCmd(itemVO);
                        if (allowSubsequentConcurrencyForJob(jobCmd)) {
                            decrementSyncQueueLimit(queueVO);
                        }
                        _syncQueueDao.update(queueVO.getId(), queueVO);

                        updateSyncQueueForJob(queueVO, jobCmd, false);
                    }
                }
            });
        } catch (Exception e) {
            s_logger.error("Unexpected exception: ", e);
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

    private boolean queueReadyToProcess(final SyncQueueVO queueVO, final SyncQueueItemVO itemVO) {
        int nActiveItems = _syncQueueItemDao.getActiveQueueItemCount(queueVO.getId());
        if (nActiveItems < queueVO.getQueueSizeLimit()) {
            if (queueVO.getQueueSizeLimit() > 1 && nActiveItems >= 1) {
                //VM job concurrency enabled with atleast one active job. Allow similar jobs (jobs that doesn't effect the VM operations sync) only.
                s_logger.debug("Job concurrency enabled for VM queue(id, size, limit): (" + queueVO.getId() + ", " + queueVO.getQueueSize() + ", " + queueVO.getQueueSizeLimit()
                        + ") with atleast one active job");
                return isJobTypeSameAsActiveJobInQueue(queueVO, itemVO);
            }

            return true;
        }

        if (s_logger.isDebugEnabled())
            s_logger.debug("Queue (queue id, sync type, sync id) - (" + queueVO.getId() + "," + queueVO.getSyncObjType() + ", " + queueVO.getSyncObjId()
                    + ") is reaching concurrency limit " + queueVO.getQueueSizeLimit());
        return false;
    }

    private boolean isJobTypeSameAsActiveJobInQueue(final SyncQueueVO queueVO, final SyncQueueItemVO itemVO) {
        String jobCmd = getJobCmd(itemVO);
        if (jobCmd == null)
            return false;

        List<SyncQueueItemVO> activeQueueItems = _syncQueueItemDao.getActiveQueueItems(queueVO.getId());
        if (activeQueueItems != null && !activeQueueItems.isEmpty()) {
            String activeJobCmd = getJobCmd(activeQueueItems.get(0));
            if (activeJobCmd == null)
                return false;

            if (jobCmd.equalsIgnoreCase(activeJobCmd)) {
                return true;
            }
        }

        return false;
    }

    private String getJobCmd(final SyncQueueItemVO itemVO) {
        if (itemVO != null) {
            AsyncJobVO jobVO = _asyncJobDao.findById(itemVO.getContentId());
            if (jobVO != null) {
                return jobVO.getCmd();
            }
        }

        return null;
    }

    private boolean allowSubsequentConcurrencyForJob(final String jobCmd) {
        if (jobCmd != null && jobCmd.equalsIgnoreCase(VOLUME_SNAPSHOT_JOB)) { //Add other job(s) here to allow subsequent concurrency for that job(s)
            return true;
        }

        return false;
    }

    private boolean queueReadyToProcessSyncObj(final String syncObjType, final long syncObjId) {
        SyncQueueVO queueVO = _syncQueueDao.find(syncObjType, syncObjId);
        if (queueVO == null) {
            if (s_logger.isDebugEnabled())
                s_logger.warn("No queue to process for sync type: " + syncObjType + " and id: " + syncObjId);
            return false;
        }

        if (queueVO.getQueueSizeLimit() <= 0) //Unlimited
            return true;

        if (queueVO.getQueueSize() < queueVO.getQueueSizeLimit()) //Limited
            return true;

        return false;
    }

    private void updateSyncQueue(SyncQueueVO queueVO, final boolean jobAdded) {
        if (queueVO == null) {
            if (s_logger.isDebugEnabled())
                s_logger.warn("No sync queue to update");
            return;
        }

        queueVO.setLastUpdated(DateUtil.currentGMTTime());

        if (jobAdded) {
            Long processNumber = queueVO.getLastProcessNumber();
            queueVO.setLastProcessNumber((processNumber == null) ? new Long(1) : processNumber + 1);
            queueVO.setQueueSize(queueVO.getQueueSize() + 1);
        } else {
            assert (queueVO.getQueueSize() > 0) : "Count reduce happens when it's already <= 0!";
            queueVO.setQueueSize(queueVO.getQueueSize() - 1);
        }
    }

    private void updateSyncQueueForJob(final SyncQueueVO queueVO, final String jobCmd, final boolean jobAdded) {
        if (queueVO == null) {
            return;
        }

        if (jobCmd == null) {
            if (s_logger.isDebugEnabled())
                s_logger.warn("No job command to update sync queue obj (type, id): (" + queueVO.getSyncObjType() + ", " + queueVO.getSyncObjId() + ")");
            return;
        }

        if (jobCmd.equalsIgnoreCase(VOLUME_SNAPSHOT_JOB)) {
            Long hostId = _vmInstanceDao.getHostId(queueVO.getSyncObjId());
            if (hostId != null) {
                SyncQueueVO hostQueueVO = _syncQueueDao.find(BaseAsyncCmd.snapshotHostSyncObject, hostId);
                if (hostQueueVO != null) {
                    updateSyncQueue(hostQueueVO, jobAdded);
                    _syncQueueDao.update(hostQueueVO.getId(), hostQueueVO);
                }
            }
        }

        //Add other job(s) [with condition(s)] here to update sync queue accordingly
    }

    private void incrementSyncQueueLimit(SyncQueueVO queueVO) {
        if (queueVO == null) {
            return;
        }

        queueVO.setQueueSizeLimit(queueVO.getQueueSizeLimit() + 1);
    }

    private void decrementSyncQueueLimit(SyncQueueVO queueVO) {
        if (queueVO == null) {
            return;
        }

        assert (queueVO.getQueueSizeLimit() > 0) : "Queue size limit count reduce happens when it's already <= 0!";
        queueVO.setQueueSizeLimit(queueVO.getQueueSizeLimit() - 1);
    }

    private void updateSyncQueueItem(SyncQueueItemVO itemVO, final Long msid, final Long processNumber) {
        if (itemVO == null) {
            return;
        }

        if (msid != null) {
            itemVO.setLastProcessMsid(msid);
            itemVO.setLastProcessNumber(processNumber);
            itemVO.setLastProcessTime(DateUtil.currentGMTTime());
        } else {
            itemVO.setLastProcessMsid(null);
            itemVO.setLastProcessNumber(null);
            itemVO.setLastProcessTime(null);
        }
    }

    private boolean queueReadyToProcessJob(final SyncQueueVO queueVO, final String jobCmd) {
        if (jobCmd == null) {
            if (s_logger.isDebugEnabled())
                s_logger.warn("No job command to process sync queue obj (type, id): (" + queueVO.getSyncObjType() + ", " + queueVO.getSyncObjId() + ")");
            return false;
        }

        if (jobCmd.equalsIgnoreCase(VOLUME_SNAPSHOT_JOB)) {
            Long hostId = _vmInstanceDao.getHostId(queueVO.getSyncObjId());
            if (hostId == null) { //No host - allow unlimited
                return true;
            }

            if (!queueReadyToProcessSyncObj(BaseAsyncCmd.snapshotHostSyncObject, hostId)) {
                if (s_logger.isDebugEnabled())
                    s_logger.debug("There is a pending process for sync obj (type, id): (" + BaseAsyncCmd.snapshotHostSyncObject + ", " + hostId + ")");
                return false;
            }
        }

        //Add other job(s) [with condition(s)] here to check that job(s) sync queue(s) is ready to process or not

        return true;
    }

    @Override
    public void purgeAsyncJobQueueItemId(long asyncJobId) {
        Long itemId = _syncQueueItemDao.getQueueItemIdByContentIdAndType(asyncJobId, SyncQueueItem.AsyncJobContentType);
        if (itemId != null) {
            purgeItem(itemId);
        }
    }

    @Override
    public void cleanupActiveQueueItems(Long msid, boolean exclusive) {
        List<SyncQueueItemVO> l = getActiveQueueItems(msid, false);
        for (SyncQueueItemVO item : l) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Discard left-over queue item: " + item.toString());
            }
            purgeItem(item.getId());
        }
    }

}
