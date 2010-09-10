/**
 * 
 */
package com.cloud.network;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConflictingNetworkSettingsException;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.VirtualMachine;

/**
 * NetworkProfiler takes the list of network offerings requested and figures
 * out what are the additional network profiles that are needed to add
 * to the account in order to support this network. 
 *
 */
public interface NetworkProfiler extends Adapter {
    NetworkConfiguration convert(NetworkOffering offering, DeploymentPlan plan, Map<String, String> params, Account owner);
    
    List<? extends NetworkConfiguration>  convert(Collection<? extends NetworkOffering> networkOfferings, Account owner);
    boolean check(VirtualMachine vm, ServiceOffering serviceOffering, Collection<? extends NetworkConfiguration> networkProfiles) throws ConflictingNetworkSettingsException;
}
