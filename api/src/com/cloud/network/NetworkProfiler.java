/**
 * 
 */
package com.cloud.network;

import java.util.Collection;
import java.util.List;

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
    List<? extends NetworkProfile>  convert(Collection<? extends NetworkOffering> networkOfferings, Account owner);
    boolean check(VirtualMachine vm, ServiceOffering serviceOffering, Collection<? extends NetworkProfile> networkProfiles) throws ConflictingNetworkSettingsException;
}
