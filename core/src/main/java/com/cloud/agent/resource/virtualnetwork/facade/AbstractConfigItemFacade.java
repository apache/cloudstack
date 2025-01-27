//
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
//

package com.cloud.agent.resource.virtualnetwork.facade;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


import com.cloud.agent.api.BumpUpPriorityCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.CreateIpAliasCommand;
import com.cloud.agent.api.routing.DeleteIpAliasCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetBgpPeersCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetIpv6FirewallRulesCommand;
import com.cloud.agent.api.routing.SetMonitorServiceCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.FileConfigItem;
import com.cloud.agent.resource.virtualnetwork.ScriptConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public abstract class AbstractConfigItemFacade {

    protected Logger logger = LogManager.getLogger(getClass());

    private final static Gson gson;

    private static Hashtable<Class<? extends NetworkElementCommand>, AbstractConfigItemFacade> flyweight = new Hashtable<Class<? extends NetworkElementCommand>, AbstractConfigItemFacade>();

    static {
        gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .disableHtmlEscaping()
            .create();

        flyweight.put(SetPortForwardingRulesVpcCommand.class, new SetPortForwardingRulesVpcConfigItem());
        flyweight.put(SetPortForwardingRulesCommand.class, new SetPortForwardingRulesConfigItem());
        flyweight.put(SetStaticRouteCommand.class, new SetStaticRouteConfigItem());
        flyweight.put(SetStaticNatRulesCommand.class, new SetStaticNatRulesConfigItem());
        flyweight.put(LoadBalancerConfigCommand.class, new LoadBalancerConfigItem());
        flyweight.put(SavePasswordCommand.class, new SavePasswordConfigItem());
        flyweight.put(DhcpEntryCommand.class, new DhcpEntryConfigItem());
        flyweight.put(CreateIpAliasCommand.class, new CreateIpAliasConfigItem());
        flyweight.put(DnsMasqConfigCommand.class, new DnsMasqConfigItem());
        flyweight.put(DeleteIpAliasCommand.class, new DeleteIpAliasConfigItem());
        flyweight.put(VmDataCommand.class, new VmDataConfigItem());
        flyweight.put(SetFirewallRulesCommand.class, new SetFirewallRulesConfigItem());
        flyweight.put(SetIpv6FirewallRulesCommand.class, new SetIpv6FirewallRulesConfigItem());
        flyweight.put(BumpUpPriorityCommand.class, new BumpUpPriorityConfigItem());
        flyweight.put(RemoteAccessVpnCfgCommand.class, new RemoteAccessVpnConfigItem());
        flyweight.put(VpnUsersCfgCommand.class, new VpnUsersConfigItem());
        flyweight.put(Site2SiteVpnCfgCommand.class, new Site2SiteVpnConfigItem());
        flyweight.put(SetMonitorServiceCommand.class, new SetMonitorServiceConfigItem());
        flyweight.put(SetupGuestNetworkCommand.class, new SetGuestNetworkConfigItem());
        flyweight.put(SetNetworkACLCommand.class, new SetNetworkAclConfigItem());
        flyweight.put(SetSourceNatCommand.class, new SetSourceNatConfigItem());
        flyweight.put(IpAssocCommand.class, new IpAssociationConfigItem());
        flyweight.put(IpAssocVpcCommand.class, new IpAssociationConfigItem());
        flyweight.put(SetBgpPeersCommand.class, new SetBgpPeersConfigItem());
    }

    protected String destinationFile;

    public static AbstractConfigItemFacade getInstance(final Class<? extends NetworkElementCommand> key) {
        if (!flyweight.containsKey(key)) {
            throw new CloudRuntimeException("Unable to process the configuration for " + key.getClass().getName());
        }

        final AbstractConfigItemFacade instance = flyweight.get(key);

        return instance;
    }

    private static String appendUuidToJsonFiles(final String filename) {
        String remoteFileName = new String(filename);
        if (remoteFileName.endsWith("json")) {
            remoteFileName += "." + UUID.randomUUID().toString();
        }
        return remoteFileName;
    }

    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        final List<ConfigItem> cfg = new LinkedList<>();

        final String remoteFilename = appendUuidToJsonFiles(destinationFile);
        if (logger.isDebugEnabled()) {
            logger.debug("Transformed filename: " + destinationFile + " to: " + remoteFilename);
        }

        final ConfigItem configFile = new FileConfigItem(VRScripts.CONFIG_PERSIST_LOCATION, remoteFilename, gson.toJson(configuration));
        cfg.add(configFile);

        // By default keep files in processed cache on VR
        final String args = configuration.shouldDeleteFromProcessedCache() ? remoteFilename + " false" : remoteFilename;

        final ConfigItem updateCommand = new ScriptConfigItem(VRScripts.UPDATE_CONFIG, args);
        cfg.add(updateCommand);

        return cfg;
    }

    public abstract List<ConfigItem> generateConfig(NetworkElementCommand cmd);
}
