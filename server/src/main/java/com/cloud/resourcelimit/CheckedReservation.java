//
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
//
package com.cloud.resourcelimit;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.reservation.ReservationVO;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.apache.cloudstack.user.ResourceReservation;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;


public class CheckedReservation  implements AutoCloseable, ResourceReservation {
    private static final Logger LOG = Logger.getLogger(CheckedReservation.class);

    private static final int TRY_TO_GET_LOCK_TIME = 120;
    private GlobalLock quotaLimitLock;
    ReservationDao reservationDao;
    private final Account account;
    private final ResourceType resourceType;
    private Long amount;
    private ResourceReservation reservation;

    private String getContextParameterKey() {
        return String.format("%s-%s", ResourceReservation.class.getSimpleName(), resourceType.getName());
    }

    /**
     * - check if adding a reservation is allowed
     * - create DB entry for reservation
     * - hold the id of this record as a ticket for implementation
     *
     * @param amount positive number of the resource type to reserve
     * @throws ResourceAllocationException
     */
    public CheckedReservation(Account account, ResourceType resourceType, Long amount, ReservationDao reservationDao, ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        this.reservationDao = reservationDao;
        this.account = account;
        this.resourceType = resourceType;
        this.amount = amount;
        this.reservation = null;
        setGlobalLock(account, resourceType);
        if (this.amount != null && this.amount <= 0) {
            if(LOG.isDebugEnabled()){
                LOG.debug(String.format("not reserving no amount of resources for %s in domain %d, type: %s, %s ", account.getAccountName(), account.getDomainId(), resourceType, amount));
            }
            this.amount = null;
        }

        if (this.amount != null) {
            if(quotaLimitLock.lock(TRY_TO_GET_LOCK_TIME)) {
                try {
                    resourceLimitService.checkResourceLimit(account,resourceType,amount);
                    ReservationVO reservationVO = new ReservationVO(account.getAccountId(), account.getDomainId(), resourceType, amount);
                    this.reservation = reservationDao.persist(reservationVO);
                    CallContext.current().putContextParameter(getContextParameterKey(), reservationVO.getId());
                } catch (NullPointerException npe) {
                    throw new CloudRuntimeException("not enough means to check limits", npe);
                } finally {
                    quotaLimitLock.unlock();
                }
            } else {
                throw new ResourceAllocationException(String.format("unable to acquire resource reservation \"%s\"", quotaLimitLock.getName()), resourceType);
            }
        } else {
            if(LOG.isDebugEnabled()){
                LOG.debug(String.format("not reserving no amount of resources for %s in domain %d, type: %s ", account.getAccountName(), account.getDomainId(), resourceType));
            }
        }
    }

    @NotNull
    private void setGlobalLock(Account account, ResourceType resourceType) {
        String lockName = String.format("CheckedReservation-%s/%d", account.getDomainId(), resourceType.getOrdinal());
        setQuotaLimitLock(GlobalLock.getInternLock(lockName));
    }

    protected void setQuotaLimitLock(GlobalLock quotaLimitLock) {
        this.quotaLimitLock = quotaLimitLock;
    }

    @Override
    public void close() throws Exception {
        if (this.reservation != null) {
            CallContext.current().removeContextParameter(getContextParameterKey());
            reservationDao.remove(reservation.getId());
            reservation = null;
        }
    }

    public Account getAccount() {
        return account;
    }

    @Override
    public Long getAccountId() {
        return account.getId();
    }

    @Override
    public Long getDomainId() {
        return account.getDomainId();
    }

    @Override
    public ResourceType getResourceType() {
        return resourceType;
    }

    @Override
    public Long getReservedAmount() {
        return amount;
    }

    @Override
    public long getId() {
        return this.reservation.getId();
    }
}
