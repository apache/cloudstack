package org.apache.cloudstack.api.net;

public interface NetworkPrerequisite {
    int getAddressOnParent();

    boolean isAutomaticAllocation();

    String getNicDescription();

    String getElementName();

    int getInstanceID();

    String getResourceSubType();

    String getResourceType();

    String getName();

    String getNetworkDescription();
}
