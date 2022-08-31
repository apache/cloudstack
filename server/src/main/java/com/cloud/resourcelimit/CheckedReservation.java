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

import com.cloud.configuration.Resource;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.db.GlobalLock;
import org.apache.cloudstack.user.ResourceReservation;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.reservation.ReservationVO;
import org.apache.cloudstack.reservation.dao.ReservationDao;

public class CheckedReservation  implements AutoCloseable, ResourceReservation {

    private static final int TRY_TO_GET_LOCK_TIME = 60;
    ReservationDao reservationDao;
    private final Account account;
    private final Resource.ResourceType resourceType;
    private final Long amount;
    private ResourceReservation reservation;

    /**
     * - check if adding a reservation is allowed
     * - create DB entry for reservation
     * - hold the id of this record as a ticket for implementation
     *
     * @param amount positive number of the resource type to reserve
     * @throws ResourceAllocationException
     */
    public CheckedReservation(Account account, Resource.ResourceType resourceType, Long amount, ReservationDao reservationDao, ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        if (amount == null || amount <= 0) {
            throw new CloudRuntimeException("resource reservations can not be made for no resources");
        }
        this.reservationDao = reservationDao;
        this.account = account;
        this.resourceType = resourceType;
        this.amount = amount;

        // synchronised?:
        String lockName = String.format("CheckedReservation-%s/%d", account.getDomainId(), resourceType.getOrdinal());
        GlobalLock quotaLimitLock = GlobalLock.getInternLock(lockName);
        if(quotaLimitLock.lock(TRY_TO_GET_LOCK_TIME)) {
            try {
                resourceLimitService.checkResourceLimit(account,resourceType,amount);
                ReservationVO reservationVO = new ReservationVO(account.getAccountId(), account.getDomainId(), resourceType, amount);
                this.reservation = reservationDao.persist(reservationVO);
            } catch (NullPointerException npe) {
                throw new CloudRuntimeException("not enough means to check limits", npe);
            } finally {
                quotaLimitLock.unlock();
            }
        } else {
            throw new ResourceAllocationException(String.format("unable to acquire resource reservation \"%s\"", lockName), resourceType);
        }
    }

    @Override
    public void close() throws Exception {
        // delete the reservation vo
        reservationDao.remove(reservation.getId());
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
    public Resource.ResourceType getResourceType() {
        return null;
    }

    @Override
    public Long getReservedAmount() {
        return null;
    }

    @Override
    public long getId() {
        return this.reservation.getId();
    }
}
