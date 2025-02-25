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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.reservation.ReservationVO;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.apache.cloudstack.user.ResourceReservation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;


public class CheckedReservation  implements AutoCloseable {
    protected Logger logger = LogManager.getLogger(getClass());

    private static final int TRY_TO_GET_LOCK_TIME = 120;
    private GlobalLock quotaLimitLock;
    ReservationDao reservationDao;

    ResourceLimitService resourceLimitService;
    private final Account account;
    private final ResourceType resourceType;
    private Long amount;
    private List<ResourceReservation> reservations;
    private List<String> resourceLimitTags;

    private String getContextParameterKey() {
        return getResourceReservationContextParameterKey(resourceType);
    }

    public static String getResourceReservationContextParameterKey(final ResourceType type) {
        return String.format("%s-%s", ResourceReservation.class.getSimpleName(), type.getName());
    }

    private void removeAllReservations() {
        if (CollectionUtils.isEmpty(reservations)) {
            return;
        }
        CallContext.current().removeContextParameter(getContextParameterKey());
        for (ResourceReservation reservation : reservations) {
            reservationDao.remove(reservation.getId());
        }
        this.reservations = null;
    }

    protected void checkLimitAndPersistReservations(Account account, ResourceType resourceType, Long resourceId, List<String> resourceLimitTags, Long amount) throws ResourceAllocationException {
        try {
            checkLimitAndPersistReservation(account, resourceType, resourceId, null, amount);
            if (CollectionUtils.isNotEmpty(resourceLimitTags)) {
                for (String tag : resourceLimitTags) {
                    checkLimitAndPersistReservation(account, resourceType, resourceId, tag, amount);
                }
            }
        } catch (ResourceAllocationException rae) {
            removeAllReservations();
            throw rae;
        }
    }

    protected void checkLimitAndPersistReservation(Account account, ResourceType resourceType, Long resourceId, String tag, Long amount) throws ResourceAllocationException {
        if (amount > 0) {
            resourceLimitService.checkResourceLimitWithTag(account, resourceType, tag, amount);
        }
        ReservationVO reservationVO = new ReservationVO(account.getAccountId(), account.getDomainId(), resourceType, tag, amount);
        if (resourceId != null) {
            reservationVO.setResourceId(resourceId);
        }
        ResourceReservation reservation = reservationDao.persist(reservationVO);
        this.reservations.add(reservation);
    }

    public CheckedReservation(Account account, ResourceType resourceType, List<String> resourceLimitTags, Long amount,
            ReservationDao reservationDao, ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        this(account, resourceType, null, resourceLimitTags, amount, reservationDao, resourceLimitService);
    }

    /**
     * - check if adding a reservation is allowed
     * - create DB entry for reservation
     * - hold the id of this record as a ticket for implementation
     *
     * @param amount positive number of the resource type to reserve
     * @throws ResourceAllocationException
     */
    public CheckedReservation(Account account, ResourceType resourceType, Long resourceId, List<String> resourceLimitTags, Long amount,
                              ReservationDao reservationDao, ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        this.reservationDao = reservationDao;
        this.resourceLimitService = resourceLimitService;
        this.account = account;
        this.resourceType = resourceType;
        this.amount = amount;
        this.reservations = new ArrayList<>();
        this.resourceLimitTags = resourceLimitTags;

        if (this.amount != null && this.amount != 0) {
            if (amount > 0) {
                setGlobalLock();
                if (quotaLimitLock.lock(TRY_TO_GET_LOCK_TIME)) {
                    try {
                        checkLimitAndPersistReservations(account, resourceType, resourceId, resourceLimitTags, amount);
                        CallContext.current().putContextParameter(getContextParameterKey(), getIds());
                    } catch (NullPointerException npe) {
                        throw new CloudRuntimeException("not enough means to check limits", npe);
                    } finally {
                        quotaLimitLock.unlock();
                    }
                } else {
                    throw new ResourceAllocationException(String.format("unable to acquire resource reservation \"%s\"", quotaLimitLock.getName()), resourceType);
                }
            } else {
                checkLimitAndPersistReservations(account, resourceType, resourceId, resourceLimitTags, amount);
            }
        } else {
            logger.debug("not reserving any amount of resources for {} in domain {}, type: {}, tag: {}",
                    account.getAccountName(), account.getDomainId(), resourceType, getResourceLimitTagsAsString());
        }
    }

    public CheckedReservation(Account account, ResourceType resourceType, Long amount, ReservationDao reservationDao,
                              ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        this(account, resourceType, null, amount, reservationDao, resourceLimitService);
    }

    @NotNull
    private void setGlobalLock() {
        String lockName = String.format("CheckedReservation-%s/%d", account.getDomainId(), resourceType.getOrdinal());
        setQuotaLimitLock(GlobalLock.getInternLock(lockName));
    }

    protected void setQuotaLimitLock(GlobalLock quotaLimitLock) {
        this.quotaLimitLock = quotaLimitLock;
    }

    @Override
    public void close() throws Exception {
        removeAllReservations();
    }

    public Account getAccount() {
        return account;
    }

    public String getResourceLimitTagsAsString() {
        return CollectionUtils.isNotEmpty(resourceLimitTags) ? StringUtils.join(resourceLimitTags) : null;
    }

    public Long getReservedAmount() {
        return amount;
    }

    public List<ResourceReservation> getReservations() {
        return reservations;
    }

    public List<Long> getIds() {
        if (CollectionUtils.isEmpty(reservations)) {
            return new ArrayList<>();
        }
        return reservations.stream().map(ResourceReservation::getId).collect(Collectors.toList());
    }
}
