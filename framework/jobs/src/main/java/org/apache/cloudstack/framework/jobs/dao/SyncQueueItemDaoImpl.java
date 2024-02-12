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

package org.apache.cloudstack.framework.jobs.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


import org.apache.cloudstack.framework.jobs.impl.SyncQueueItemVO;

import com.cloud.utils.DateUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

@DB
public class SyncQueueItemDaoImpl extends GenericDaoBase<SyncQueueItemVO, Long> implements SyncQueueItemDao {
    final GenericSearchBuilder<SyncQueueItemVO, Long> queueIdSearch;
    final GenericSearchBuilder<SyncQueueItemVO, Integer> queueActiveItemSearch;

    public SyncQueueItemDaoImpl() {
        super();
        queueIdSearch = createSearchBuilder(Long.class);
        queueIdSearch.and("contentId", queueIdSearch.entity().getContentId(), Op.EQ);
        queueIdSearch.and("contentType", queueIdSearch.entity().getContentType(), Op.EQ);
        queueIdSearch.selectFields(queueIdSearch.entity().getId());
        queueIdSearch.done();

        queueActiveItemSearch = createSearchBuilder(Integer.class);
        queueActiveItemSearch.and("queueId", queueActiveItemSearch.entity().getQueueId(), Op.EQ);
        queueActiveItemSearch.and("processNumber", queueActiveItemSearch.entity().getLastProcessNumber(), Op.NNULL);
        queueActiveItemSearch.select(null, Func.COUNT, queueActiveItemSearch.entity().getId());
        queueActiveItemSearch.done();
    }

    @Override
    public SyncQueueItemVO getNextQueueItem(long queueId) {

        SearchBuilder<SyncQueueItemVO> sb = createSearchBuilder();
        sb.and("queueId", sb.entity().getQueueId(), SearchCriteria.Op.EQ);
        sb.and("lastProcessNumber", sb.entity().getLastProcessNumber(), SearchCriteria.Op.NULL);
        sb.done();

        SearchCriteria<SyncQueueItemVO> sc = sb.create();
        sc.setParameters("queueId", queueId);

        Filter filter = new Filter(SyncQueueItemVO.class, "created", true, 0L, 1L);
        List<SyncQueueItemVO> l = listBy(sc, filter);
        if(l != null && l.size() > 0)
            return l.get(0);

        return null;
    }

    @Override
    public int getActiveQueueItemCount(long queueId) {
        SearchCriteria<Integer> sc = queueActiveItemSearch.create();
        sc.setParameters("queueId", queueId);

        List<Integer> count = customSearch(sc, null);
        return count.get(0);
    }

    @Override
    public List<SyncQueueItemVO> getNextQueueItems(int maxItems) {
        List<SyncQueueItemVO> l = new ArrayList<SyncQueueItemVO>();

        String sql = "SELECT i.id, i.queue_id, i.content_type, i.content_id, i.created " +
                " FROM sync_queue AS q JOIN sync_queue_item AS i ON q.id = i.queue_id " +
                     " WHERE i.queue_proc_number IS NULL " +
                " GROUP BY q.id " +
                " ORDER BY i.id " +
                " LIMIT 0, ?";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            pstmt.setInt(1, maxItems);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                SyncQueueItemVO item = new SyncQueueItemVO();
                item.setId(rs.getLong(1));
                item.setQueueId(rs.getLong(2));
                item.setContentType(rs.getString(3));
                item.setContentId(rs.getLong(4));
                item.setCreated(DateUtil.parseDateString(TimeZone.getTimeZone("GMT"), rs.getString(5)));
                l.add(item);
            }
        } catch (SQLException e) {
            logger.error("Unexpected sql exception, ", e);
        } catch (Throwable e) {
            logger.error("Unexpected exception, ", e);
        }
        return l;
    }

    @Override
    public List<SyncQueueItemVO> getActiveQueueItems(Long msid, boolean exclusive) {
        SearchBuilder<SyncQueueItemVO> sb = createSearchBuilder();
        sb.and("lastProcessMsid", sb.entity().getLastProcessMsid(),
                SearchCriteria.Op.EQ);
        sb.done();

        SearchCriteria<SyncQueueItemVO> sc = sb.create();
        sc.setParameters("lastProcessMsid", msid);

        Filter filter = new Filter(SyncQueueItemVO.class, "created", true, null, null);

        if (exclusive)
            return lockRows(sc, filter, true);
        return listBy(sc, filter);
    }

    @Override
    public List<SyncQueueItemVO> getBlockedQueueItems(long thresholdMs, boolean exclusive) {
        Date cutTime = DateUtil.currentGMTTime();

        SearchBuilder<SyncQueueItemVO> sbItem = createSearchBuilder();
        sbItem.and("lastProcessMsid", sbItem.entity().getLastProcessMsid(), SearchCriteria.Op.NNULL);
        sbItem.and("lastProcessNumber", sbItem.entity().getLastProcessNumber(), SearchCriteria.Op.NNULL);
        sbItem.and("lastProcessTime", sbItem.entity().getLastProcessTime(), SearchCriteria.Op.NNULL);
        sbItem.and("lastProcessTime2", sbItem.entity().getLastProcessTime(), SearchCriteria.Op.LT);

        sbItem.done();

        SearchCriteria<SyncQueueItemVO> sc = sbItem.create();
        sc.setParameters("lastProcessTime2", new Date(cutTime.getTime() - thresholdMs));

        if(exclusive)
            return lockRows(sc, null, true);
        return listBy(sc, null);
    }

    @Override
    public Long getQueueItemIdByContentIdAndType(long contentId, String contentType) {
        SearchCriteria<Long> sc = queueIdSearch.create();
        sc.setParameters("contentId", contentId);
        sc.setParameters("contentType", contentType);
        List<Long> id = customSearch(sc, null);

        return id.size() == 0 ? null : id.get(0);
    }
}
