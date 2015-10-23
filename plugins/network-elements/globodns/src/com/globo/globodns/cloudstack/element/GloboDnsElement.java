// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.globo.globodns.cloudstack.element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.element.NetworkElement;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.globodns.cloudstack.api.AddGloboDnsHostCmd;
import com.globo.globodns.cloudstack.commands.CreateOrUpdateDomainCommand;
import com.globo.globodns.cloudstack.commands.CreateOrUpdateRecordAndReverseCommand;
import com.globo.globodns.cloudstack.commands.RemoveDomainCommand;
import com.globo.globodns.cloudstack.commands.RemoveRecordCommand;
import com.globo.globodns.cloudstack.commands.SignInCommand;
import com.globo.globodns.cloudstack.resource.GloboDnsResource;

@Component
@Local(NetworkElement.class)
public class GloboDnsElement extends AdapterBase implements ResourceStateAdapter, NetworkElement, GloboDnsElementService, Configurable {

    private static final Logger s_logger = Logger.getLogger(GloboDnsElement.class);

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    private static final ConfigKey<Long> GloboDNSTemplateId = new ConfigKey<Long>("Advanced", Long.class, "globodns.domain.templateid", "1",
            "Template id to be used when creating domains in GloboDNS", true, ConfigKey.Scope.Global);
    private static final ConfigKey<Boolean> GloboDNSOverride = new ConfigKey<Boolean>("Advanced", Boolean.class, "globodns.override.entries", "true",
            "Allow GloboDns to override entries that already exist", true, ConfigKey.Scope.Global);

    // DAOs
    @Inject
    DataCenterDao _dcDao;
    @Inject
    HostDao _hostDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;

    // Managers
    @Inject
    AgentManager _agentMgr;
    @Inject
    ResourceManager _resourceMgr;

    protected boolean isTypeSupported(VirtualMachine.Type type) {
        return type == VirtualMachine.Type.User || type == VirtualMachine.Type.ConsoleProxy || type == VirtualMachine.Type.DomainRouter;
    }

    @Override
    @DB
    public boolean implement(final Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {

        Long zoneId = network.getDataCenterId();
        DataCenter zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new CloudRuntimeException("Could not find zone associated to this network");
        }
        CreateOrUpdateDomainCommand cmd = new CreateOrUpdateDomainCommand(network.getNetworkDomain(), GloboDNSTemplateId.value());
        callCommand(cmd, zoneId);
        return true;
    }

    protected String hostNameOfVirtualMachine(VirtualMachineProfile vm) {
        return vm.getHostName().toLowerCase();
    }

    @Override
    @DB
    public boolean prepare(final Network network, final NicProfile nic, final VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {

        if (!isTypeSupported(vm.getType())) {
            s_logger.info("GloboDNS only manages records for VMs of type User, ConsoleProxy and DomainRouter. VM " + vm + " is " + vm.getType());
            return false;
        }

        Long zoneId = network.getDataCenterId();
        final DataCenter zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new CloudRuntimeException("Could not find zone associated to this network");
        }

        /* Create new A record in GloboDNS */
        // We allow only lower case names in DNS, so force lower case names for VMs
        String vmName = vm.getHostName();
        String vmHostname = hostNameOfVirtualMachine(vm);
        if (!vmName.equals(vmHostname) && vm.getType() == VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("VM name should contain only lower case letters and digits: " + vmName + " - " + vm);
        }

        CreateOrUpdateRecordAndReverseCommand cmd = new CreateOrUpdateRecordAndReverseCommand(vmHostname, nic.getIPv4Address(), network.getNetworkDomain(),
                GloboDNSTemplateId.value(), GloboDNSOverride.value());
        callCommand(cmd, zoneId);
        return true;
    }

    @Override
    @DB
    public boolean release(final Network network, NicProfile nic, final VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException {

        if (!isTypeSupported(vm.getType())) {
            s_logger.info("GloboDNS only manages records for VMs of type User, ConsoleProxy and DomainRouter. VM " + vm + " is " + vm.getType());
            return false;
        }

        Long zoneId = network.getDataCenterId();
        final DataCenter zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new CloudRuntimeException("Could not find zone associated to this network");
        }

        RemoveRecordCommand cmd = new RemoveRecordCommand(hostNameOfVirtualMachine(vm), nic.getIPv4Address(), network.getNetworkDomain(), GloboDNSOverride.value());
        callCommand(cmd, zoneId);
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    @DB
    public boolean destroy(final Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        Long zoneId = network.getDataCenterId();
        final DataCenter zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new CloudRuntimeException("Could not find zone associated to this network");
        }

        RemoveDomainCommand cmd = new RemoveDomainCommand(network.getNetworkDomain(), GloboDNSOverride.value());
        callCommand(cmd, zoneId);
        return true;
    }

    ///////// Provider control methods ////////////
    private Answer callCommand(Command cmd, Long zoneId) {

        HostVO globoDnsHost = getGloboDnsHost(zoneId);
        if (globoDnsHost == null) {
            throw new CloudRuntimeException("Could not find the GloboDNS resource");
        }

        Answer answer = _agentMgr.easySend(globoDnsHost.getId(), cmd);
        if (answer == null || !answer.getResult()) {
            String msg = "Error executing command " + cmd;
            msg = answer == null ? msg : answer.getDetails();
            throw new CloudRuntimeException(msg);
        }

        return answer;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> caps = new HashMap<Service, Map<Capability, String>>();
        Map<Capability, String> dnsCapabilities = new HashMap<Capability, String>();
        // FIXME
        dnsCapabilities.put(Capability.AllowDnsSuffixModification, "true");
        caps.put(Service.Dns, dnsCapabilities);
        return caps;
    }

    @Override
    public Provider getProvider() {
        return Provider.GloboDns;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        PhysicalNetwork pNtwk = _physicalNetworkDao.findById(provider.getPhysicalNetworkId());
        Host host = getGloboDnsHost(pNtwk.getDataCenterId());
        if (host != null) {
            _resourceMgr.deleteHost(host.getId(), true, false);
        }
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }

    ////// Configurable methods /////////////
    @Override
    public String getConfigComponentName() {
        return GloboDnsElement.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {GloboDNSTemplateId, GloboDNSOverride};
    }

    ////////// Resource/Host methods ////////////
    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddGloboDnsHostCmd.class);
        return cmdList;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        if (!(startup[0] instanceof StartupCommand && resource instanceof GloboDnsResource)) {
            return null;
        }
        host.setType(Host.Type.L2Networking);
        return host;
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (!(host.getType() == Host.Type.L2Networking)) {
            return null;
        }
        return new DeleteHostAnswer(true);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(name, this);
        return true;
    }

    private HostVO getGloboDnsHost(Long zoneId) {
        return _hostDao.findByTypeNameAndZoneId(zoneId, Provider.GloboDns.getName(), Type.L2Networking);
    }

    @Override
    @DB
    public Host addGloboDnsHost(Long physicalNetworkId, final String username, final String password, String url) {

        if (username == null || username.trim().isEmpty()) {
            throw new InvalidParameterValueException("Invalid username: " + username);
        }

        if (password == null || password.trim().isEmpty()) {
            throw new InvalidParameterValueException("Invalid password: " + password);
        }

        if (url == null || url.trim().isEmpty()) {
            throw new InvalidParameterValueException("Invalid url: " + url);
        }

        // validate physical network and zone
        // Check if physical network exists
        PhysicalNetwork pNtwk = null;
        if (physicalNetworkId != null) {
            pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNtwk == null) {
                throw new InvalidParameterValueException("Unable to find a physical network having the specified physical network id");
            }
        } else {
            throw new InvalidParameterValueException("Invalid physicalNetworkId: " + physicalNetworkId);
        }

        final Long zoneId = pNtwk.getDataCenterId();

        final Map<String, String> params = new HashMap<String, String>();
        params.put("guid", "globodns-" + String.valueOf(zoneId));
        params.put("zoneId", String.valueOf(zoneId));
        params.put("name", Provider.GloboDns.getName());

        params.put("url", url);
        params.put("username", username);
        params.put("password", password);

        final Map<String, Object> hostDetails = new HashMap<String, Object>();
        hostDetails.putAll(params);

        Host host = Transaction.execute(new TransactionCallbackWithException<Host, CloudRuntimeException>() {

            @Override
            public Host doInTransaction(TransactionStatus status) throws CloudRuntimeException {
                try {
                    GloboDnsResource resource = new GloboDnsResource();
                    resource.configure(Provider.GloboDns.getName(), hostDetails);

                    Host host = _resourceMgr.addHost(zoneId, resource, resource.getType(), params);

                    if (host == null) {
                        throw new CloudRuntimeException("Failed to add GloboDNS host");
                    }

                    // Validate username and password by logging in
                    SignInCommand cmd = new SignInCommand(username, password);
                    Answer answer = callCommand(cmd, zoneId);
                    if (answer == null || !answer.getResult()) {
                        // Could not sign in on GloboDNS
                        throw new ConfigurationException("Could not sign in on GloboDNS. Please verify URL, username and password.");
                    }

                    return host;
                } catch (ConfigurationException e) {
                    throw new CloudRuntimeException(e);
                }
            }
        });

        return host;
    }
}
