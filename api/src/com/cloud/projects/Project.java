package com.cloud.projects;

import java.util.Date;

import com.cloud.acl.ControlledEntity;

public interface Project extends ControlledEntity{

    String getDisplayText();

    long getDomainId();

    long getAccountId();

    long getId();

    Date getCreated();

    Date getRemoved();

    long getDataCenterId();

    String getName();

}
