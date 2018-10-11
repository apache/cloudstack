package com.cloud.storage.copy;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;


public interface ManagementServerCopier {

    boolean copy(TemplateInfo srcTemplate, DataStore destStore);
}
