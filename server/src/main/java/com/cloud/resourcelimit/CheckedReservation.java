package com.cloud.resourcelimit;

import com.cloud.configuration.Resource;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.exception.CloudRuntimeException;

public class CheckedReservation  implements AutoCloseable, ResourceLimitService.ResourceReservation {
    private final Account account;
    private final Resource.ResourceType type;
    private final Long amount;

    public CheckedReservation(Account account, Resource.ResourceType type, Long amount) {
        if (amount == null || amount <= 0) {
            throw new CloudRuntimeException("resource reservations can not be made for no resources");
        }
        // synchronised:
        // - check if adding a reservation is allowed
        // - create DB entry for reservation
        this.account = account;
        this.type = type;
        this.amount = amount;
    }

    @Override
    public void close() throws Exception {
        // delete the reservation vo

    }

    public Account getAccount() {
        return account;
    }

    @Override
    public Long getAccountId() {
        return account.getId();
    }

    @Override
    public Resource.ResourceType getResourceType() {
        return null;
    }

    @Override
    public Long getReservedAmount() {
        return null;
    }
}
