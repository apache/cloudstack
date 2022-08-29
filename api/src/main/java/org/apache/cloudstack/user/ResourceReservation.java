package org.apache.cloudstack.user;

import com.cloud.configuration.Resource;
import org.apache.cloudstack.api.InternalIdentity;

/**
 * an interface defining an {code}AutoClosable{code} reservation object
 */
public interface
ResourceReservation extends InternalIdentity {

    Long getAccountId();

    Resource.ResourceType getResourceType();

    Long getReservedAmount();
}
