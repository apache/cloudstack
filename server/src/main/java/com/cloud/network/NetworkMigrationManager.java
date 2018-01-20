// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.network;

import com.cloud.network.vpc.Vpc;
import com.cloud.offering.NetworkOffering;

/**
 * The NetworkMigrationManager should be used for migration purpose ONLY.
 */
public interface NetworkMigrationManager {

    String MIGRATION = "migration";
    /**
     * Returns a copy of the provided network.
     * All nics in the orginal network will be re-assigned to the newly created network.
     * All network details will be copied to the newly created network.
     * @param network the network to be copied
     * @param networkOffering the network offering of the network that is copied
     * @return the networkid of the copy
     */
    long makeCopyOfNetwork(Network network, NetworkOffering networkOffering, Long vpcId);

    /**
     * Returns a copy of the provided vpc
     * All acls rules and public ips will be re-assigned to the newly created vpc.
     * @param vpcId the vpc id of the vpc that needs to be copied
     * @param vpcOfferingId the new vpc offering id
     * @return the vpc id of the copy
     */
    Long makeCopyOfVpc(long vpcId, long vpcOfferingId);

    /**
     * Starts the vpc if, the vpc is not already started
     */
    void startVpc(Vpc vpc);

    /**
     * Upgrade the physical network and the offering of a network.
     * @param networkId the networkid of the network that needs to be upgraded.
     * @param newPhysicalNetworkId the id of the new physical network where the network should be moved to.
     * @param networkOfferingId the new network offering.
     * @return the networkid of the upgraded network
     */
    Network upgradeNetworkToNewNetworkOffering (long networkId, long newPhysicalNetworkId, long networkOfferingId, Long vpcId);

    /**
     * Deletes the copy of a network which was previously created by the networkMigrationManager
     * For deletion of the copy the old UUID of the original network is used to assure that plugins, using the UUID, clean up the network correctly.
     * @param networkCopyId the networkId of the copied network
     * @param originalNetworkId the networkId of the original network
     */
    void deleteCopyOfNetwork(long networkCopyId, long originalNetworkId);

    /**
     * Deletes the copy of a vpc which was previously created by the networkMigrationManager
     * For deletion the copy of the old UUID of the original vpc is used to assure that plugins, using the UUID, clean up the vpc correctly.
     * @param vpcCopyId the vpc id of the copied vpc
     * @param originalVpcId the vpc id of the original vpc
     */
    void deleteCopyOfVpc(long vpcCopyId, long originalVpcId);

    /**
     * Moves all the nics from the srcNetwork to the network in the new physical network.
     * The nics in the source network will be marked as removed afterwards.
     * @param srcNetwork the source network containing the nics to be moved
     * @param networkInNewPhysicalNet the destination network where the nics need to be recreated.
     */
    void assignNicsToNewPhysicalNetwork(Network srcNetwork, Network networkInNewPhysicalNet);
}
