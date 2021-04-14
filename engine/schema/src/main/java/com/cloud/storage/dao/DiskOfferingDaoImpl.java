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
package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;

import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.springframework.stereotype.Component;

import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DiskOfferingDaoImpl extends GenericDaoBase<DiskOfferingVO, Long> implements DiskOfferingDao {

    @Inject
    protected DiskOfferingDetailsDao detailsDao;

    private final SearchBuilder<DiskOfferingVO> PrivateDiskOfferingSearch;
    private final SearchBuilder<DiskOfferingVO> PublicDiskOfferingSearch;
    protected final SearchBuilder<DiskOfferingVO> UniqueNameSearch;
    private final String SizeDiskOfferingSearch = "SELECT * FROM disk_offering WHERE " +
            "disk_size = ? AND provisioning_type = ? AND removed IS NULL";

    private final Attribute _computeOnlyAttr;
    protected final static long GB_UNIT_BYTES = 1024 * 1024 * 1024;

    protected DiskOfferingDaoImpl() {
        PrivateDiskOfferingSearch = createSearchBuilder();
        PrivateDiskOfferingSearch.and("diskSize", PrivateDiskOfferingSearch.entity().getDiskSize(), SearchCriteria.Op.EQ);
        PrivateDiskOfferingSearch.done();

        PublicDiskOfferingSearch = createSearchBuilder();
        PublicDiskOfferingSearch.and("system", PublicDiskOfferingSearch.entity().isSystemUse(), SearchCriteria.Op.EQ);
        PublicDiskOfferingSearch.and("removed", PublicDiskOfferingSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        PublicDiskOfferingSearch.done();

        UniqueNameSearch = createSearchBuilder();
        UniqueNameSearch.and("name", UniqueNameSearch.entity().getUniqueName(), SearchCriteria.Op.EQ);
        UniqueNameSearch.done();

        _computeOnlyAttr = _allAttributes.get("compute_only");
    }

    @Override
    public List<DiskOfferingVO> findPrivateDiskOffering() {
        SearchCriteria<DiskOfferingVO> sc = PrivateDiskOfferingSearch.create();
        sc.setParameters("diskSize", 0);
        return listBy(sc);
    }

    @Override
    public List<DiskOfferingVO> searchIncludingRemoved(SearchCriteria<DiskOfferingVO> sc, final Filter filter, final Boolean lock, final boolean cache) {
        sc.addAnd(_computeOnlyAttr, Op.EQ, false);
        return super.searchIncludingRemoved(sc, filter, lock, cache);
    }

    @Override
    public <K> List<K> customSearchIncludingRemoved(SearchCriteria<K> sc, final Filter filter) {
        sc.addAnd(_computeOnlyAttr, Op.EQ, false);
        return super.customSearchIncludingRemoved(sc, filter);
    }

    @Override
    protected List<DiskOfferingVO> executeList(final String sql, final Object... params) {
        StringBuilder builder = new StringBuilder(sql);
        int index = builder.indexOf("WHERE");
        if (index == -1) {
            builder.append(" WHERE compute_only=?");
        } else {
            builder.insert(index + 6, "compute_only=? ");
        }

        return super.executeList(sql, false, params);
    }

    @Override
    public List<DiskOfferingVO> findPublicDiskOfferings() {
        SearchCriteria<DiskOfferingVO> sc = PublicDiskOfferingSearch.create();
        sc.setParameters("system", false);
        List<DiskOfferingVO> offerings = listBy(sc);
        if(offerings!=null) {
            offerings.removeIf(o -> (!detailsDao.findDomainIds(o.getId()).isEmpty()));
        }
        return offerings;
    }

    @Override
    public DiskOfferingVO findByUniqueName(String uniqueName) {
        SearchCriteria<DiskOfferingVO> sc = UniqueNameSearch.create();
        sc.setParameters("name", uniqueName);
        List<DiskOfferingVO> vos = search(sc, null, null, false);
        if (vos.size() == 0) {
            return null;
        }

        return vos.get(0);
    }

    @Override
    public DiskOfferingVO persistDeafultDiskOffering(DiskOfferingVO offering) {
        assert offering.getUniqueName() != null : "unique name shouldn't be null for the disk offering";
        DiskOfferingVO vo = findByUniqueName(offering.getUniqueName());
        if (vo != null) {
            return vo;
        }
        try {
            return persist(offering);
        } catch (EntityExistsException e) {
            // Assume it's conflict on unique name
            return findByUniqueName(offering.getUniqueName());
        }
    }

    protected long getClosestDiskSizeInGB(long sizeInBytes) {
        if (sizeInBytes < 0) {
            throw new CloudRuntimeException("Disk size should be greater than 0 bytes, received: " + sizeInBytes + " bytes");
        }
        return (long) Math.ceil(1.0 * sizeInBytes / GB_UNIT_BYTES);
    }

    @Override
    public List<DiskOfferingVO> listAllBySizeAndProvisioningType(long size, Storage.ProvisioningType provisioningType) {
        StringBuilder sql = new StringBuilder(SizeDiskOfferingSearch);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<DiskOfferingVO> offerings = new ArrayList<>();
        try(PreparedStatement pstmt = txn.prepareStatement(sql.toString());){
            if(pstmt != null) {
                pstmt.setLong(1, size);
                pstmt.setString(2, provisioningType.toString());
                try(ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        offerings.add(toEntityBean(rs, false));
                    }
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Exception while listing disk offerings by size: " + e.getMessage(), e);
                }
            }
            return offerings;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while listing disk offerings by size: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean remove(Long id) {
        DiskOfferingVO diskOffering = createForUpdate();
        diskOffering.setRemoved(new Date());

        return update(id, diskOffering);
    }
}
