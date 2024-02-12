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
package org.apache.cloudstack.network.element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


import org.apache.cloudstack.api.commands.AddSspCmd;
import org.apache.cloudstack.api.commands.DeleteSspCmd;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.dao.SspCredentialDao;
import org.apache.cloudstack.network.dao.SspCredentialVO;
import org.apache.cloudstack.network.dao.SspTenantDao;
import org.apache.cloudstack.network.dao.SspTenantVO;
import org.apache.cloudstack.network.dao.SspUuidDao;
import org.apache.cloudstack.network.dao.SspUuidVO;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkMigrationResponder;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkServiceProvider.State;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.element.ConnectivityProvider;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

/**
 * Stratosphere sdn platform network element
 *
 * This class will be called per network setup operations.
 * This class also have ssp specific methods.
 *
 * Current implementation use HostVO for storage of api endpoint information,
 * but note this is not necessary. The other way is create our own database
 * table for that information.
 */
public class SspElement extends AdapterBase implements ConnectivityProvider, SspManager, SspService, NetworkMigrationResponder {
    public static final String s_SSP_NAME = "StratosphereSsp";
    private static final Provider s_ssp_provider = new Provider(s_SSP_NAME, false);

    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    SspCredentialDao _sspCredentialDao;
    @Inject
    SspTenantDao _sspTenantDao;
    @Inject
    SspUuidDao _sspUuidDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    HostDao _hostDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    NicDao _nicDao = null;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return super.configure(name, params);
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        capabilities.put(Service.Connectivity, new HashMap<Capability, String>()); // XXX: We might need some more category here.
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return s_ssp_provider;
    }

    private List<SspClient> fetchSspClients(Long physicalNetworkId, Long dataCenterId, boolean enabledOnly) {
        ArrayList<SspClient> clients = new ArrayList<SspClient>();

        boolean provider_found = false;
        PhysicalNetworkServiceProviderVO provider = _physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetworkId, s_SSP_NAME);
        if (enabledOnly) {
            if (provider != null && provider.getState() == State.Enabled) {
                provider_found = true;
            }
        } else {
            provider_found = true;
        }

        if (physicalNetworkId != null && provider_found) {
            SspCredentialVO credential = _sspCredentialDao.findByZone(dataCenterId);
            List<HostVO> hosts = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.L2Networking, dataCenterId);
            for (HostVO host : hosts) {
                assert (credential != null);
                _hostDao.loadDetails(host);
                if ("v1Api".equals(host.getDetail("sspHost"))) {
                    clients.add(new SspClient(host.getDetail("url"), credential.getUsername(), credential.getPassword()));
                }
            }
        }
        if (clients.size() == 0) {
            String global_apiUrl = _configDao.getValueAndInitIfNotExist("ssp.url", "Network", null);
            String global_username = _configDao.getValueAndInitIfNotExist("ssp.username", "Network", null);
            String global_password = _configDao.getValueAndInitIfNotExist("ssp.password", "Network", null);
            if (global_apiUrl != null && global_username != null && global_password != null) {
                clients.add(new SspClient(global_apiUrl, global_username, global_password));
            }
        }
        return clients;
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.network.element.NetworkElement#isReady(com.cloud.network.PhysicalNetworkServiceProvider)
     */
    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(provider.getPhysicalNetworkId());
        assert (physicalNetwork != null);
        if (fetchSspClients(physicalNetwork.getId(), physicalNetwork.getDataCenterId(), false).size() > 0) {
            return true;
        }
        logger.warn("Ssp api endpoint not found. " + physicalNetwork.toString());
        return false;
    }

    /* (non-Javadoc)
     * If this element is ready, then it can be enabled.
     * @see org.apache.cloudstack.network.element.SspManager#isEnabled(com.cloud.network.PhysicalNetwork)
     */
    @Override
    public boolean canHandle(PhysicalNetwork physicalNetwork) {
        if (physicalNetwork != null) {
            if (fetchSspClients(physicalNetwork.getId(), physicalNetwork.getDataCenterId(), true).size() > 0) {
                return true;
            }
            logger.warn("enabled Ssp api endpoint not found. " + physicalNetwork.toString());
        } else {
            logger.warn("PhysicalNetwork is NULL.");
        }
        return false;
    }

    private boolean canHandle(Network network) {
        if (canHandle(_physicalNetworkDao.findById(network.getPhysicalNetworkId()))) {
            if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(), Service.Connectivity, getProvider())) {
                logger.info("SSP is implicitly active for " + network);
            }
            return true;
        }
        return false;
    }

    @Override
    public Host addSspHost(AddSspCmd cmd) {
        SspClient client = new SspClient(cmd.getUrl(), cmd.getUsername(), cmd.getPassword());
        if (!client.login()) {
            throw new CloudRuntimeException("Ssp login failed.");
        }

        long zoneId = cmd.getZoneId();
        SspCredentialVO credential = _sspCredentialDao.findByZone(zoneId);
        if (credential == null) {
            if (cmd.getUsername() == null || cmd.getPassword() == null) {
                throw new InvalidParameterValueException("Initial credential required for zone: " + zoneId);
            }
            credential = new SspCredentialVO();
            credential.setZoneId(zoneId);
            credential.setUsername(cmd.getUsername());
            credential.setPassword(cmd.getPassword());
            _sspCredentialDao.persist(credential);
        } else {
            if (cmd.getUsername() != null || cmd.getPassword() != null) {
                logger.warn("Tenant credential already configured for zone:" + zoneId);
            }
        }

        String tenantUuid = _sspTenantDao.findUuidByZone(zoneId);
        if (tenantUuid == null) {
            if (cmd.getTenantUuid() == null) {
                throw new InvalidParameterValueException("Initial tenant uuid required for zone: " + zoneId);
            }
            SspTenantVO tenant = new SspTenantVO();
            tenant.setZoneId(zoneId);
            tenant.setUuid(cmd.getTenantUuid());
            _sspTenantDao.persist(tenant);
        } else {
            if (cmd.getTenantUuid() != null) {
                logger.warn("Tenant uuid already configured for zone:" + zoneId);
            }
        }

        String normalizedUrl = null;
        String hostname = null;
        try {
            URL url = new URL(cmd.getUrl());
            normalizedUrl = url.toString();
            hostname = url.getHost();
        } catch (MalformedURLException e1) {
            throw new CloudRuntimeException("Invalid url " + cmd.getUrl());
        }

        List<HostVO> hosts = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.L2Networking, zoneId);
        for (HostVO host : hosts) {
            assert (credential != null);
            _hostDao.loadDetails(host);
            if ("v1Api".equals(host.getDetail("sspHost"))) {
                if (normalizedUrl.equals(host.getDetail("url"))) {
                    logger.warn("Ssp host already registered " + normalizedUrl);
                    return host;
                }
            }
        }
        // SspHost HostVO will be created per zone and url.
        HostVO host = new HostVO(UUID.randomUUID().toString());
        host.setDataCenterId(zoneId);
        host.setType(Host.Type.L2Networking);
        host.setPrivateIpAddress(hostname); // db schema not null. It may be a name, not IP address.
        //        host.setPrivateMacAddress(""); // db schema nullable
        //        host.setPrivateNetmask(""); // db schema nullable
        host.setVersion("1"); // strange db schema not null
        host.setName(cmd.getName());

        host.setDetails(new HashMap<String, String>());
        host.setDetail("sspHost", "v1Api");
        host.setDetail("url", normalizedUrl);
        return _hostDao.persist(host);
    }

    @Override
    public boolean deleteSspHost(DeleteSspCmd cmd) {
        logger.info("deleteStratosphereSsp");
        return _hostDao.remove(cmd.getHostId());
    }

    @Override
    public boolean createNetwork(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) {
        if (_sspUuidDao.findUuidByNetwork(network) != null) {
            logger.info("Network already has ssp TenantNetwork uuid :" + network.toString());
            return true;
        }
        if (!canHandle(network)) {
            return false;
        }

        String tenantUuid = _sspTenantDao.findUuidByZone(network.getDataCenterId());
        if (tenantUuid == null) {
            tenantUuid = _configDao.getValueAndInitIfNotExist("ssp.tenant", "Network", null);
        }

        boolean processed = false;
        for (SspClient client : fetchSspClients(network.getPhysicalNetworkId(), network.getDataCenterId(), true)) {
            SspClient.TenantNetwork sspNet = client.createTenantNetwork(tenantUuid, network.getName());
            if (sspNet != null) {
                SspUuidVO uuid = new SspUuidVO();
                uuid.setUuid(sspNet.uuid);
                uuid.setObjClass(SspUuidVO.objClassNetwork);
                uuid.setObjId(network.getId());
                _sspUuidDao.persist(uuid);
                return true;
            }
            processed = true;
        }
        if (processed) {
            logger.error("Could not allocate an uuid for network " + network.toString());
            return false;
        } else {
            logger.error("Skipping #createNetwork() for " + network.toString());
            return true;
        }
    }

    @Override
    public boolean deleteNetwork(Network network) {
        String tenantNetworkUuid = _sspUuidDao.findUuidByNetwork(network);
        if (tenantNetworkUuid != null) {
            boolean processed = false;
            for (SspClient client : fetchSspClients(network.getPhysicalNetworkId(), network.getDataCenterId(), true)) {
                if (client.deleteTenantNetwork(tenantNetworkUuid)) {
                    _sspUuidDao.removeUuid(tenantNetworkUuid);
                    processed = true;
                    break;
                }
            }
            if (!processed) {
                logger.error("Ssp api tenant network deletion failed " + network.toString());
            }
        } else {
            logger.debug("Silently skipping #deleteNetwork() for " + network.toString());
        }
        return true;
    }

    // we use context.reservationId for dedup of guru & element operations.
    @Override
    public boolean createNicEnv(Network network, NicProfile nic, DeployDestination dest, ReservationContext context) {
        String tenantNetworkUuid = _sspUuidDao.findUuidByNetwork(network);
        if (tenantNetworkUuid == null) {
            logger.debug("Skipping #createNicEnv() for nic on " + network.toString());
            return true;
        }

        String reservationId = context.getReservationId();
        List<SspUuidVO> tenantPortUuidVos = _sspUuidDao.listUUidVoByNicProfile(nic);
        for (SspUuidVO tenantPortUuidVo : tenantPortUuidVos) {
            if (reservationId.equals(tenantPortUuidVo.getReservationId())) {
                logger.info("Skipping because reservation found " + reservationId);
                return true;
            }
        }

        String tenantPortUuid = null;
        for (SspClient client : fetchSspClients(network.getPhysicalNetworkId(), network.getDataCenterId(), true)) {
            SspClient.TenantPort sspPort = client.createTenantPort(tenantNetworkUuid);
            if (sspPort != null) {
                tenantPortUuid = sspPort.uuid;
                nic.setReservationId(reservationId);

                SspUuidVO uuid = new SspUuidVO();
                uuid.setUuid(tenantPortUuid);
                uuid.setObjClass(SspUuidVO.objClassNicProfile);
                uuid.setObjId(nic.getId());
                uuid.setReservationId(reservationId);
                _sspUuidDao.persist(uuid);
                break;
            }
        }
        if (tenantPortUuid == null) {
            logger.debug("#createNicEnv() failed for nic on " + network.toString());
            return false;
        }

        for (SspClient client : fetchSspClients(network.getPhysicalNetworkId(), network.getDataCenterId(), true)) {
            SspClient.TenantPort sspPort = client.updateTenantVifBinding(tenantPortUuid, dest.getHost().getPrivateIpAddress());
            if (sspPort != null) {
                if (sspPort.vlanId != null) {
                    nic.setBroadcastType(BroadcastDomainType.Vlan);
                    nic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(String.valueOf(sspPort.vlanId)));
                }
                return true;
            }
        }
        logger.error("Updating vif failed " + nic.toString());
        return false;
    }

    @Override
    public boolean deleteNicEnv(Network network, NicProfile nic, ReservationContext context) {
        if (context == null) {
            logger.error("ReservationContext was null for " + nic + " " + network);
            return false;
        }
        String reservationId = context.getReservationId();

        SspUuidVO deleteTarget = null;
        SspUuidVO remainingTarget = null;
        List<SspUuidVO> tenantPortUuidVos = _sspUuidDao.listUUidVoByNicProfile(nic);
        for (SspUuidVO tenantPortUuidVo : tenantPortUuidVos) {
            if (reservationId.equals(tenantPortUuidVo.getReservationId())) {
                deleteTarget = tenantPortUuidVo;
            } else {
                remainingTarget = tenantPortUuidVo;
            }
        }

        if (deleteTarget != null) { // delete the target ssp uuid (tenant-port)
            String tenantPortUuid = deleteTarget.getUuid();
            boolean processed = false;
            for (SspClient client : fetchSspClients(network.getPhysicalNetworkId(), network.getDataCenterId(), true)) {
                SspClient.TenantPort sspPort = client.updateTenantVifBinding(tenantPortUuid, null);
                if (sspPort != null) {
                    processed = true;
                    break;
                }
            }
            if (!processed) {
                logger.warn("Ssp api nic detach failed " + nic.toString());
            }
            processed = false;
            for (SspClient client : fetchSspClients(network.getPhysicalNetworkId(), network.getDataCenterId(), true)) {
                if (client.deleteTenantPort(tenantPortUuid)) {
                    _sspUuidDao.removeUuid(tenantPortUuid);
                    processed = true;
                    break;
                }
            }
            if (!processed) {
                logger.warn("Ssp api tenant port deletion failed " + nic.toString());
            }
            _sspUuidDao.removeUuid(tenantPortUuid);
        }
        if (remainingTarget != null) {
            NicVO nicVo = _nicDao.findById(nic.getId());
            nicVo.setReservationId(remainingTarget.getReservationId());
            _nicDao.persist(nicVo); // persist the new reservationId
        }
        return true;
    }

    /* (non-Javadoc)
     * Implements a network using ssp element.
     *
     * This method will be called right after NetworkGuru#implement().
     * see also {@link #shutdown(Network, ReservationContext, boolean)}
     * @see org.apache.cloudstack.network.element.NetworkElement#implement(com.cloud.network.Network, com.cloud.offering.NetworkOffering, com.cloud.deploy.DeployDestination, com.cloud.vm.ReservationContext)
     */
    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException, InsufficientCapacityException {
        logger.info("implement");
        return createNetwork(network, offering, dest, context);
    }

    /* (non-Javadoc)
     * Shutdown the network implementation
     *
     * This method will be called right BEFORE NetworkGuru#shutdown().
     * The entities was acquired by {@link #implement(Network, NetworkOffering, DeployDestination, ReservationContext)}
     * @see org.apache.cloudstack.network.element.NetworkElement#shutdown(com.cloud.network.Network, com.cloud.vm.ReservationContext, boolean)
     */
    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        logger.trace("shutdown");
        return deleteNetwork(network);
    }

    /* (non-Javadoc)
     * Prepares a network environment for a VM nic.
     *
     * This method will be called right after NetworkGuru#reserve().
     * The entities will be released by {@link #release(Network, NicProfile, VirtualMachineProfile, ReservationContext)}
     * @see org.apache.cloudstack.network.element.NetworkElement#prepare(com.cloud.network.Network, com.cloud.vm.NicProfile, com.cloud.vm.VirtualMachineProfile, com.cloud.deploy.DeployDestination, com.cloud.vm.ReservationContext)
     */
    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        logger.trace("prepare");
        return createNicEnv(network, nic, dest, context);
    }

    /* (non-Javadoc)
     * Release the network environment that was prepared for a VM nic.
     *
     * This method will be called right AFTER NetworkGuru#release().
     * The entities was acquired in {@link #prepare(Network, NicProfile, VirtualMachineProfile, DeployDestination, ReservationContext)}
     * @see org.apache.cloudstack.network.element.NetworkElement#release(com.cloud.network.Network, com.cloud.vm.NicProfile, com.cloud.vm.VirtualMachineProfile, com.cloud.vm.ReservationContext)
     */
    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        logger.trace("release");
        return deleteNicEnv(network, nic, context);
    }

    /* (non-Javadoc)
     * Destroy a network implementation.
     *
     * This method will be called right BEFORE NetworkGuru#trash() in "Expunge" phase.
     * @see org.apache.cloudstack.network.element.NetworkElement#destroy(com.cloud.network.Network)
     */
    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        logger.trace("destroy");
        // nothing to do here.
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        logger.trace("shutdownProviderInstances");
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        logger.trace("canEnableIndividualServices");
        return true; // because there is only Connectivity
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        logger.trace("verifyServicesCombination " + services.toString());
        return true;
    }

    @Override
    public boolean prepareMigration(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) {
        try {
            prepare(network, nic, vm, dest, context);
        } catch (ConcurrentOperationException e) {
            logger.error("prepareForMigration failed.", e);
            return false;
        } catch (ResourceUnavailableException e) {
            logger.error("prepareForMigration failed.", e);
            return false;
        } catch (InsufficientCapacityException e) {
            logger.error("prepareForMigration failed.", e);
            return false;
        }
        return true;
    }

    @Override
    public void rollbackMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {
        try {
            release(network, nic, vm, dst);
        } catch (ConcurrentOperationException e) {
            logger.error("rollbackMigration failed.", e);
        } catch (ResourceUnavailableException e) {
            logger.error("rollbackMigration failed.", e);
        }
    }

    @Override
    public void commitMigration(NicProfile nic, Network network, VirtualMachineProfile vm, ReservationContext src, ReservationContext dst) {
        try {
            release(network, nic, vm, src);
        } catch (ConcurrentOperationException e) {
            logger.error("commitMigration failed.", e);
        } catch (ResourceUnavailableException e) {
            logger.error("commitMigration failed.", e);
        }
    }

    @Override
    public List<Class<?>> getCommands() {
        return Arrays.<Class<?>> asList(AddSspCmd.class, DeleteSspCmd.class);
    }
}
