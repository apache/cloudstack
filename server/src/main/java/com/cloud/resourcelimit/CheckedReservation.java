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
import java.util.Objects;
import java.util.stream.Collectors;

import com.cloud.api.ApiDBUtils;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.reservation.ReservationVO;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.apache.cloudstack.resourcelimit.Reserver;
import org.apache.cloudstack.user.ResourceReservation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
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


public class CheckedReservation implements Reserver {
    protected Logger logger = LogManager.getLogger(getClass());

    private static final int TRY_TO_GET_LOCK_TIME = 120;
    private GlobalLock quotaLimitLock;
    ReservationDao reservationDao;

    ResourceLimitService resourceLimitService;
    private Account account;
    private Long domainId;
    private ResourceType resourceType;
    private Long resourceId;
    private Long reservationAmount;
    private List<String> reservationTags;
    private Long existingAmount;
    private List<String> existingLimitTags;
    private List<ResourceReservation> reservations;

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

    protected void checkLimitAndPersistReservations(Account account, Long domainId, ResourceType resourceType, Long resourceId, List<String> resourceLimitTags, Long amount) throws ResourceAllocationException {
        try {
            checkLimitAndPersistReservation(account, domainId, resourceType, resourceId, null, amount);
            if (CollectionUtils.isNotEmpty(resourceLimitTags)) {
                for (String tag : resourceLimitTags) {
                    checkLimitAndPersistReservation(account, domainId, resourceType, resourceId, tag, amount);
                }
            }
        } catch (ResourceAllocationException rae) {
            removeAllReservations();
            throw rae;
        }
    }

    protected void checkLimitAndPersistReservation(Account account, Long domainId, ResourceType resourceType, Long resourceId, String tag, Long amount) throws ResourceAllocationException {
        if (amount > 0) {
            resourceLimitService.checkResourceLimitWithTag(account, domainId, true, resourceType, tag, amount);
        }
        ReservationVO reservationVO = new ReservationVO(account.getAccountId(), domainId, resourceType, tag, amount);
        if (resourceId != null) {
            reservationVO.setResourceId(resourceId);
        }
        ResourceReservation reservation = reservationDao.persist(reservationVO);
        this.reservations.add(reservation);
    }

    // TODO: refactor these into a Builder to avoid having so many constructors
    public CheckedReservation(Account account, ResourceType resourceType, List<String> resourceLimitTags, Long reservationAmount,
                              ReservationDao reservationDao, ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        this(account, resourceType, null, resourceLimitTags, null, reservationAmount, null, reservationDao, resourceLimitService);
    }

    public CheckedReservation(Account account, ResourceType resourceType, Long resourceId, List<String> reservedTags,
                              List<String> existingTags, Long reservationAmount, Long existingAmount, ReservationDao reservationDao,
                              ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        this(account, null, resourceType, resourceId, reservedTags, existingTags, reservationAmount, existingAmount, reservationDao, resourceLimitService);
    }

    public CheckedReservation(Account account, Long domainId, ResourceType resourceType, Long resourceId, List<String> reservedTags,
                              Long reservationAmount, ReservationDao reservationDao, ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        this(account, domainId, resourceType, resourceId, reservedTags, null, reservationAmount, null, reservationDao, resourceLimitService);
    }

    public CheckedReservation(Account account, ResourceType resourceType, Long resourceId, List<String> reservedTags, Long reservationAmount, ReservationDao reservationDao, ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        this(account, null, resourceType, resourceId, reservedTags, null, reservationAmount, null, reservationDao, resourceLimitService);
    }

    /**
     * - check if adding a reservation is allowed
     * - create DB entry for reservation
     * - hold the id of this record as a ticket for implementation
     *
     * @param reservationAmount positive number of the resource type to reserve
     * @throws ResourceAllocationException
     */
    public CheckedReservation(Account account, Long domainId, ResourceType resourceType, Long resourceId, List<String> reservedTags,
                              List<String> existingTags, Long reservationAmount, Long existingAmount, ReservationDao reservationDao,
                              ResourceLimitService resourceLimitService) throws ResourceAllocationException {

        if (ObjectUtils.allNull(account, domainId)) {
            logger.debug("Not reserving any {} resources, as no account/domain was provided.", resourceType);
            return;
        }

        this.reservationDao = reservationDao;
        this.resourceLimitService = resourceLimitService;

        // When allocating to a domain instead of a specific account, consider the system account as the owner for the validations here.
        if (account == null) {
            account = ApiDBUtils.getSystemAccount();
        }
        this.account = account;

        if (domainId == null) {
            domainId = account.getDomainId();
        }
        this.domainId = domainId;

        this.resourceType = resourceType;
        this.reservationAmount = reservationAmount;
        this.existingAmount = existingAmount;
        this.reservations = new ArrayList<>();

        this.reservationTags = getTagsWithoutNull(reservedTags);
        this.existingLimitTags = getTagsWithoutNull(existingTags);

        // TODO: refactor me
        if (this.reservationAmount != null && this.reservationAmount != 0) {
            if (reservationAmount > 0) {
                setGlobalLock();
                if (quotaLimitLock.lock(TRY_TO_GET_LOCK_TIME)) {
                    try {
                        adjustCountToNotConsiderExistingAmount();
                        checkLimitAndPersistReservations(account, this.domainId, resourceType, resourceId, reservationTags, reservationAmount);
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
                checkLimitAndPersistReservations(account, this.domainId, resourceType, resourceId, reservationTags, reservationAmount);
            }
        } else {
            logger.debug("not reserving any amount of resources for {} in domain {}, type: {}, tag: {}",
                    account.getAccountName(), this.domainId, resourceType, getResourceLimitTagsAsString());
        }
    }

    protected List<String> getTagsWithoutNull(List<String> tags) {
        if (tags == null) {
            return null;
        }
        return tags.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    protected void adjustCountToNotConsiderExistingAmount() throws ResourceAllocationException {
        if (existingAmount == null || existingAmount == 0) {
            return;
        }
        checkLimitAndPersistReservations(account, domainId, resourceType, resourceId, existingLimitTags, -1 * existingAmount);
    }

    public CheckedReservation(Account account, ResourceType resourceType, Long amount, ReservationDao reservationDao,
                              ResourceLimitService resourceLimitService) throws ResourceAllocationException {
        this(account, resourceType, null, amount, reservationDao, resourceLimitService);
    }

    @NotNull
    private void setGlobalLock() {
        String lockName = String.format("CheckedReservation-%s/%d", this.domainId, resourceType.getOrdinal());
        setQuotaLimitLock(GlobalLock.getInternLock(lockName));
    }

    protected void setQuotaLimitLock(GlobalLock quotaLimitLock) {
        this.quotaLimitLock = quotaLimitLock;
    }

    @Override
    public void close() {
        removeAllReservations();
    }

    public Account getAccount() {
        return account;
    }

    public String getResourceLimitTagsAsString() {
        return CollectionUtils.isNotEmpty(reservationTags) ? StringUtils.join(reservationTags) : null;
    }

    public Long getReservedAmount() {
        return reservationAmount;
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
