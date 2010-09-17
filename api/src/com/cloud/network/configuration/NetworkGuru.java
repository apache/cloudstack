/**
 * 
 */
package com.cloud.network.configuration;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.NetworkConfiguration;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.Adapter;

/**
 * NetworkProfiler takes the list of network offerings requested and figures
 * out what are the additional network profiles that are needed to add
 * to the account in order to support this network. 
 *
 */
public interface NetworkGuru extends Adapter {
    NetworkConfiguration design(NetworkOffering offering, DeploymentPlan plan, NetworkConfiguration config, Account owner);
    NetworkConfiguration implement(NetworkConfiguration config, NetworkOffering offering, DeployDestination destination);
    
    
}
