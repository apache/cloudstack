/**
 * 
 */
package com.cloud.network;

import java.util.Map;

import com.cloud.deploy.DeploymentPlan;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.component.Adapter;

/**
 * NetworkProfiler takes the list of network offerings requested and figures
 * out what are the additional network profiles that are needed to add
 * to the account in order to support this network. 
 *
 */
public interface NetworkProfiler extends Adapter {
    NetworkConfiguration convert(NetworkOffering offering, DeploymentPlan plan, Map<String, String> params, Account owner);
}
