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
package com.cloud.dc;

import com.cloud.dc.dao.ASNumberDao;
import com.cloud.dc.dao.ASNumberRangeDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.admin.bgp.ListASNumbersCmd;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BGPServiceImpl implements BGPService {

    public static final Logger LOGGER = Logger.getLogger(BGPServiceImpl.class);

    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ASNumberRangeDao asNumberRangeDao;
    @Inject
    private ASNumberDao asNumberDao;

    public BGPServiceImpl() {
    }

    @Override
    public ASNumberRange createASNumberRange(long zoneId, long startASNumber, long endASNumber) {
        DataCenterVO zone = dataCenterDao.findById(zoneId);
        if (zone == null) {
            String msg = String.format("Cannot find a zone with ID %s", zoneId);
            LOGGER.error(msg);
            throw new InvalidParameterException(msg);
        }
        if (startASNumber > endASNumber) {
            String msg = "Please specify a valid AS Number range";
            LOGGER.error(msg);
            throw new InvalidParameterException(msg);
        }

        try {
            return Transaction.execute((TransactionCallback<ASNumberRange>) status -> {
                LOGGER.debug(String.format("Persisting AS Number Range %s-%s for the zone %s", startASNumber, endASNumber, zone.getName()));
                ASNumberRangeVO asNumberRangeVO = new ASNumberRangeVO(zoneId, startASNumber, endASNumber);
                asNumberRangeDao.persist(asNumberRangeVO);

                for (long asn = startASNumber; asn <= endASNumber; asn++) {
                    LOGGER.debug(String.format("Persisting AS Number %s for zone %s", asn, zone.getName()));
                    ASNumberVO asNumber = new ASNumberVO(asn, asNumberRangeVO.getId(), zoneId);
                    asNumberDao.persist(asNumber);
                }
                return asNumberRangeVO;
            });
        } catch (Exception e) {
            String err = String.format("Error creating AS Number range %s-%s for zone %s: %s", startASNumber, endASNumber, zone.getName(), e.getMessage());
            LOGGER.error(err, e);
            throw new CloudRuntimeException(err);
        }
    }

    @Override
    public List<ASNumberRange> listASNumberRanges(Long zoneId) {
        List<ASNumberRangeVO> rangeVOList = zoneId != null ? asNumberRangeDao.listByZoneId(zoneId) : asNumberRangeDao.listAll();
        return new ArrayList<>(rangeVOList);
    }

    @Override
    public Pair<List<ASNumber>, Integer> listASNumbers(ListASNumbersCmd cmd) {
        Long zoneId = cmd.getZoneId();
        Long asNumberRangeId = cmd.getAsNumberRangeId();
        Integer asNumber = cmd.getAsNumber();
        Boolean allocated = cmd.getAllocated();
        Long startIndex = cmd.getStartIndex();
        Long pageSizeVal = cmd.getPageSizeVal();
        Pair<List<ASNumberVO>, Integer> pair = asNumberDao.searchAndCountByZoneOrRangeOrAllocated(zoneId, asNumberRangeId,
                asNumber, allocated, startIndex, pageSizeVal);
        return new Pair<>(new ArrayList<>(pair.first()), pair.second());
    }

    @Override
    public boolean releaseASNumber(long zoneId, long asNumber) {
        ASNumberVO asNumberVO = asNumberDao.findByAsNumber(asNumber);
        if (asNumberVO == null) {
            String msg = String.format("Cannot find AS Number %s on zone %s", asNumber, zoneId);
            LOGGER.error(msg);
            throw new InvalidParameterException(msg);
        }
        if (!asNumberVO.isAllocated()) {
            String msg = String.format("The AS Number %s is not allocated to any network on zone %s, ignoring release", asNumber, zoneId);
            LOGGER.debug(msg);
            return false;
        }
        LOGGER.debug(String.format("Releasing AS Number %s on zone %s from previous allocation", asNumber, zoneId));
        asNumberVO.setAllocated(false);
        asNumberVO.setAllocatedTime(null);
        asNumberVO.setDomainId(null);
        asNumberVO.setAccountId(null);
        asNumberVO.setNetworkId(null);
        return asNumberDao.update(asNumberVO.getId(), asNumberVO);
    }

    @Override
    public boolean deleteASRange(long id) {
        final ASNumberRange asRange = asNumberRangeDao.findById(id);
        if (asRange == null) {
            throw new CloudRuntimeException(String.format("Could not find a AS range with id: %s", id));
        }

        List<ASNumberVO> allocatedAsNumbers = asNumberDao.listAllocatedByASRange(asRange.getId());
        if (Objects.nonNull(allocatedAsNumbers) && !allocatedAsNumbers.isEmpty()) {
            throw new CloudRuntimeException(String.format("AS numbers from range %s are in use", asRange.getId()));
        }
        return asNumberRangeDao.remove(id);
    }
}
