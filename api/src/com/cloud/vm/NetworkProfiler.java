/**
 * 
 */
package com.cloud.vm;

import java.util.Collection;
import java.util.List;

import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Adapter;

public interface NetworkProfiler extends Adapter {
    Ternary<VmCharacteristics, List<NetworkCharacteristics>, List<DiskCharacteristics>> convert(VirtualMachine vm, ServiceOffering serviceOffering, List<NetworkOffering> networkOfferings, Collection<DiskOffering> diskOfferings, Account owner);
}
