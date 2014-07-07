package com.cloud.baremetal.manager;

import com.cloud.baremetal.networkservice.BaremetalRctResponse;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.AddBaremetalRctCmd;

/**
 * Created by frank on 4/30/14.
 */
public interface BaremetalVlanManager extends Manager, PluggableService {
    BaremetalRctResponse addRct(AddBaremetalRctCmd cmd);
}
