package org.apache.cloudstack.reservation;

import com.cloud.configuration.Resource;
import org.apache.cloudstack.user.ResourceReservation;
import com.cloud.utils.exception.CloudRuntimeException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "resource_reservation")
public class ReservationVO implements ResourceReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "resource_type", nullable = false)
    Resource.ResourceType resourceType;

    @Column(name = "amount")
    long amount;

    protected ReservationVO()
    {}

    public ReservationVO(Long accountId, Long domainId, Resource.ResourceType resourceType, Long delta) {
        if (delta == null || delta <= 0) {
            throw new CloudRuntimeException("resource reservations can not be made for no resources");
        }
        this.accountId = accountId;
        this.domainId = domainId;
        this.resourceType = resourceType;
        this.amount = delta;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }

    @Override
    public Resource.ResourceType getResourceType() {
        return resourceType;
    }

    @Override
    public Long getReservedAmount() {
        return amount;
    }
}
