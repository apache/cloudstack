package org.apache.cloudstack.api.command.admin.zone;

public interface ListVmwareDcItems {
    String getVcenter();

    String getDatacenterName();

    String getUsername();

    String getPassword();

    Long getExistingVcenterId();
}
