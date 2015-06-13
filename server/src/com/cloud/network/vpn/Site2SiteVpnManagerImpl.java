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
package com.cloud.network.vpn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.command.user.vpn.CreateVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnConnectionsCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnCustomerGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnGatewaysCmd;
import org.apache.cloudstack.api.command.user.vpn.ResetVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.UpdateVpnCustomerGatewayCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.configuration.Config;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnConnection.State;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;

@Component
@Local(value = {Site2SiteVpnManager.class, Site2SiteVpnService.class})
public class Site2SiteVpnManagerImpl extends ManagerBase implements Site2SiteVpnManager {
    private static final Logger s_logger = Logger.getLogger(Site2SiteVpnManagerImpl.class);

    List<Site2SiteVpnServiceProvider> _s2sProviders;
    @Inject
    Site2SiteCustomerGatewayDao _customerGatewayDao;
    @Inject
    Site2SiteVpnGatewayDao _vpnGatewayDao;
    @Inject
    Site2SiteVpnConnectionDao _vpnConnectionDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    AccountManager _accountMgr;

    String _name;
    int _connLimit;
    int _subnetsLimit;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = _configDao.getConfiguration(params);
        _connLimit = NumbersUtil.parseInt(configs.get(Config.Site2SiteVpnConnectionPerVpnGatewayLimit.key()), 4);
        _subnetsLimit = NumbersUtil.parseInt(configs.get(Config.Site2SiteVpnSubnetsPerCustomerGatewayLimit.key()), 10);
        assert (_s2sProviders.iterator().hasNext()) : "Did not get injected with a list of S2S providers!";
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_GATEWAY_CREATE, eventDescription = "creating s2s vpn gateway", async = true)
    public Site2SiteVpnGateway createVpnGateway(CreateVpnGatewayCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.getAccount(cmd.getEntityOwnerId());

        //Verify that caller can perform actions in behalf of vpc owner
        _accountMgr.checkAccess(caller, null, false, owner);

        Long vpcId = cmd.getVpcId();
        VpcVO vpc = _vpcDao.findById(vpcId);
        if (vpc == null) {
            throw new InvalidParameterValueException("Invalid VPC " + vpcId + " for site to site vpn gateway creation!");
        }
        Site2SiteVpnGatewayVO gws = _vpnGatewayDao.findByVpcId(vpcId);
        if (gws != null) {
            throw new InvalidParameterValueException("The VPN gateway of VPC " + vpcId + " already existed!");
        }
        //Use source NAT ip for VPC
        List<IPAddressVO> ips = _ipAddressDao.listByAssociatedVpc(vpcId, true);
        if (ips.size() != 1) {
            throw new CloudRuntimeException("Cannot found source nat ip of vpc " + vpcId);
        }

        Site2SiteVpnGatewayVO gw = new Site2SiteVpnGatewayVO(owner.getAccountId(), owner.getDomainId(), ips.get(0).getId(), vpcId);

        if (cmd.getDisplay() != null) {
            gw.setDisplay(cmd.getDisplay());
        }

        _vpnGatewayDao.persist(gw);
        return gw;
    }

    protected void checkCustomerGatewayCidrList(String guestCidrList) {
        String[] cidrList = guestCidrList.split(",");
        if (cidrList.length > _subnetsLimit) {
            throw new InvalidParameterValueException("Too many subnets of customer gateway! The limit is " + _subnetsLimit);
        }
        // Remote sub nets cannot overlap themselves
        for (int i = 0; i < cidrList.length - 1; i++) {
            for (int j = i + 1; j < cidrList.length; j++) {
                if (NetUtils.isNetworksOverlap(cidrList[i], cidrList[j])) {
                    throw new InvalidParameterValueException("The subnet of customer gateway " + cidrList[i] + " is overlapped with another subnet " + cidrList[j] +
                        " of customer gateway!");
                }
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_CUSTOMER_GATEWAY_CREATE, eventDescription = "creating s2s customer gateway", create = true)
    public Site2SiteCustomerGateway createCustomerGateway(CreateVpnCustomerGatewayCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.getAccount(cmd.getEntityOwnerId());

        //Verify that caller can perform actions in behalf of vpc owner
        _accountMgr.checkAccess(caller, null, false, owner);

        String name = cmd.getName();
        String gatewayIp = cmd.getGatewayIp();
        if (!NetUtils.isValidIp(gatewayIp)) {
            throw new InvalidParameterValueException("The customer gateway ip " + gatewayIp + " is invalid!");
        }
        if (name == null) {
            name = "VPN-" + gatewayIp;
        }
        String peerCidrList = cmd.getGuestCidrList();
        if (!NetUtils.isValidCidrList(peerCidrList)) {
            throw new InvalidParameterValueException("The customer gateway peer cidr list " + peerCidrList + " contains an invalid cidr!");
        }
        String ipsecPsk = cmd.getIpsecPsk();
        String ikePolicy = cmd.getIkePolicy();
        String espPolicy = cmd.getEspPolicy();
        if (!NetUtils.isValidS2SVpnPolicy(ikePolicy)) {
            throw new InvalidParameterValueException("The customer gateway IKE policy " + ikePolicy + " is invalid!");
        }
        if (!NetUtils.isValidS2SVpnPolicy(espPolicy)) {
            throw new InvalidParameterValueException("The customer gateway ESP policy " + espPolicy + " is invalid!");
        }
        Long ikeLifetime = cmd.getIkeLifetime();
        if (ikeLifetime == null) {
            // Default value of lifetime is 1 day
            ikeLifetime = (long)86400;
        }
        if (ikeLifetime > 86400) {
            throw new InvalidParameterValueException("The IKE lifetime " + ikeLifetime + " of vpn connection is invalid!");
        }
        Long espLifetime = cmd.getEspLifetime();
        if (espLifetime == null) {
            // Default value of lifetime is 1 hour
            espLifetime = (long)3600;
        }
        if (espLifetime > 86400) {
            throw new InvalidParameterValueException("The ESP lifetime " + espLifetime + " of vpn connection is invalid!");
        }

        Boolean dpd = cmd.getDpd();
        if (dpd == null) {
            dpd = false;
        }

        long accountId = owner.getAccountId();
        if (_customerGatewayDao.findByGatewayIpAndAccountId(gatewayIp, accountId) != null) {
            throw new InvalidParameterValueException("The customer gateway with ip " + gatewayIp + " already existed in the system!");
        }
        if (_customerGatewayDao.findByNameAndAccountId(name, accountId) != null) {
            throw new InvalidParameterValueException("The customer gateway with name " + name + " already existed!");
        }

        checkCustomerGatewayCidrList(peerCidrList);

        Site2SiteCustomerGatewayVO gw =
            new Site2SiteCustomerGatewayVO(name, accountId, owner.getDomainId(), gatewayIp, peerCidrList, ipsecPsk, ikePolicy, espPolicy, ikeLifetime, espLifetime, dpd);
        _customerGatewayDao.persist(gw);
        return gw;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_CONNECTION_CREATE, eventDescription = "creating s2s vpn connection", create = true)
    public Site2SiteVpnConnection createVpnConnection(CreateVpnConnectionCmd cmd) throws NetworkRuleConflictException {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.getAccount(cmd.getEntityOwnerId());

        //Verify that caller can perform actions in behalf of vpc owner
        _accountMgr.checkAccess(caller, null, false, owner);

        Long customerGatewayId = cmd.getCustomerGatewayId();
        Site2SiteCustomerGateway customerGateway = _customerGatewayDao.findById(customerGatewayId);
        if (customerGateway == null) {
            throw new InvalidParameterValueException("Unable to found specified Site to Site VPN customer gateway " + customerGatewayId + " !");
        }
        _accountMgr.checkAccess(caller, null, false, customerGateway);

        Long vpnGatewayId = cmd.getVpnGatewayId();
        Site2SiteVpnGateway vpnGateway = _vpnGatewayDao.findById(vpnGatewayId);
        if (vpnGateway == null) {
            throw new InvalidParameterValueException("Unable to found specified Site to Site VPN gateway " + vpnGatewayId + " !");
        }
        _accountMgr.checkAccess(caller, null, false, vpnGateway);

        if (customerGateway.getAccountId() != vpnGateway.getAccountId() || customerGateway.getDomainId() != vpnGateway.getDomainId()) {
            throw new InvalidParameterValueException("VPN connection can only be esitablished between same account's VPN gateway and customer gateway!");
        }

        if (_vpnConnectionDao.findByVpnGatewayIdAndCustomerGatewayId(vpnGatewayId, customerGatewayId) != null) {
            throw new InvalidParameterValueException("The vpn connection with customer gateway id " + customerGatewayId + " and vpn gateway id " + vpnGatewayId +
                " already existed!");
        }
        String[] cidrList = customerGateway.getGuestCidrList().split(",");

        // Remote sub nets cannot overlap VPC's sub net
        String vpcCidr = _vpcDao.findById(vpnGateway.getVpcId()).getCidr();
        for (String cidr : cidrList) {
            if (NetUtils.isNetworksOverlap(vpcCidr, cidr)) {
                throw new InvalidParameterValueException("The subnets of customer gateway " + customerGatewayId + "'s subnet " + cidr + " is overlapped with VPC cidr " +
                        vpcCidr + "!");
            }
        }

        // We also need to check if the new connection's remote CIDR is overlapped with existed connections
        List<Site2SiteVpnConnectionVO> conns = _vpnConnectionDao.listByVpnGatewayId(vpnGatewayId);
        if (conns.size() >= _connLimit) {
            throw new InvalidParameterValueException("There are too many VPN connections with current VPN gateway! The limit is " + _connLimit);
        }
        for (Site2SiteVpnConnectionVO vc : conns) {
            if (vc == null) {
                continue;
            }
            Site2SiteCustomerGatewayVO gw = _customerGatewayDao.findById(vc.getCustomerGatewayId());
            String[] oldCidrList = gw.getGuestCidrList().split(",");
            for (String oldCidr : oldCidrList) {
                for (String cidr : cidrList) {
                    if (NetUtils.isNetworksOverlap(cidr, oldCidr)) {
                        throw new InvalidParameterValueException("The new connection's remote subnet " + cidr +
                            " is overlapped with existed VPN connection to customer gateway " + gw.getName() + "'s subnet " + oldCidr);
                    }
                }
            }
        }

        Site2SiteVpnConnectionVO conn = new Site2SiteVpnConnectionVO(owner.getAccountId(), owner.getDomainId(), vpnGatewayId, customerGatewayId, cmd.isPassive());
        conn.setState(State.Pending);
        if (cmd.getDisplay() != null) {
            conn.setDisplay(cmd.getDisplay());
        }

        _vpnConnectionDao.persist(conn);
        return conn;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_CONNECTION_CREATE, eventDescription = "starting s2s vpn connection", async = true)
    public Site2SiteVpnConnection startVpnConnection(long id) throws ResourceUnavailableException {
        Site2SiteVpnConnectionVO conn = _vpnConnectionDao.acquireInLockTable(id);
        if (conn == null) {
            throw new CloudRuntimeException("Unable to acquire lock on " + conn);
        }
        try {
            if (conn.getState() != State.Pending && conn.getState() != State.Disconnected) {
                throw new InvalidParameterValueException(
                    "Site to site VPN connection with specified connectionId not in correct state(pending or disconnected) to process!");
            }

            conn.setState(State.Pending);
            _vpnConnectionDao.persist(conn);

            boolean result = true;
            for (Site2SiteVpnServiceProvider element : _s2sProviders) {
                result = result & element.startSite2SiteVpn(conn);
            }

            if (result) {
                if (conn.isPassive()) {
                    conn.setState(State.Disconnected);
                } else {
                    conn.setState(State.Connected);
                }
                _vpnConnectionDao.persist(conn);
                return conn;
            }
            conn.setState(State.Error);
            _vpnConnectionDao.persist(conn);
            throw new ResourceUnavailableException("Failed to apply site-to-site VPN", Site2SiteVpnConnection.class, id);
        } finally {
            _vpnConnectionDao.releaseFromLockTable(conn.getId());
        }
    }

    @Override
    public Site2SiteVpnGateway getVpnGateway(Long vpnGatewayId) {
        return _vpnGatewayDao.findById(vpnGatewayId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_CUSTOMER_GATEWAY_DELETE, eventDescription = "deleting s2s vpn customer gateway", create = true)
    public boolean deleteCustomerGateway(DeleteVpnCustomerGatewayCmd cmd) {
        CallContext.current().setEventDetails(" Id: " + cmd.getId());
        Account caller = CallContext.current().getCallingAccount();

        Long id = cmd.getId();
        Site2SiteCustomerGateway customerGateway = _customerGatewayDao.findById(id);
        if (customerGateway == null) {
            throw new InvalidParameterValueException("Fail to find customer gateway with " + id + " !");
        }
        _accountMgr.checkAccess(caller, null, false, customerGateway);

        return doDeleteCustomerGateway(customerGateway);
    }

    protected boolean doDeleteCustomerGateway(Site2SiteCustomerGateway gw) {
        long id = gw.getId();
        List<Site2SiteVpnConnectionVO> vpnConnections = _vpnConnectionDao.listByCustomerGatewayId(id);
        if (vpnConnections != null && vpnConnections.size() != 0) {
            throw new InvalidParameterValueException("Unable to delete VPN customer gateway with id " + id + " because there is still related VPN connections!");
        }
        _customerGatewayDao.remove(id);
        return true;
    }

    protected void doDeleteVpnGateway(Site2SiteVpnGateway gw) {
        List<Site2SiteVpnConnectionVO> conns = _vpnConnectionDao.listByVpnGatewayId(gw.getId());
        if (conns != null && conns.size() != 0) {
            throw new InvalidParameterValueException("Unable to delete VPN gateway " + gw.getId() + " because there is still related VPN connections!");
        }
        _vpnGatewayDao.remove(gw.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_GATEWAY_DELETE, eventDescription = "deleting s2s vpn gateway", async = true)
    public boolean deleteVpnGateway(DeleteVpnGatewayCmd cmd) {
        CallContext.current().setEventDetails(" Id: " + cmd.getId());
        Account caller = CallContext.current().getCallingAccount();

        Long id = cmd.getId();
        Site2SiteVpnGateway vpnGateway = _vpnGatewayDao.findById(id);
        if (vpnGateway == null) {
            throw new InvalidParameterValueException("Fail to find vpn gateway with " + id + " !");
        }

        _accountMgr.checkAccess(caller, null, false, vpnGateway);

        doDeleteVpnGateway(vpnGateway);
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_CUSTOMER_GATEWAY_UPDATE, eventDescription = "update s2s vpn customer gateway", create = true)
    public Site2SiteCustomerGateway updateCustomerGateway(UpdateVpnCustomerGatewayCmd cmd) {
        CallContext.current().setEventDetails(" Id: " + cmd.getId());
        Account caller = CallContext.current().getCallingAccount();

        Long id = cmd.getId();
        Site2SiteCustomerGatewayVO gw = _customerGatewayDao.findById(id);
        if (gw == null) {
            throw new InvalidParameterValueException("Find to find customer gateway with id " + id);
        }
        _accountMgr.checkAccess(caller, null, false, gw);

        List<Site2SiteVpnConnectionVO> conns = _vpnConnectionDao.listByCustomerGatewayId(id);
        if (conns != null) {
            for (Site2SiteVpnConnection conn : conns) {
                if (conn.getState() != State.Error) {
                    throw new InvalidParameterValueException("Unable to update customer gateway with connections in non-Error state!");
                }
            }
        }
        String name = cmd.getName();
        String gatewayIp = cmd.getGatewayIp();
        if (!NetUtils.isValidIp(gatewayIp)) {
            throw new InvalidParameterValueException("The customer gateway ip " + gatewayIp + " is invalid!");
        }
        if (name == null) {
            name = "VPN-" + gatewayIp;
        }
        String guestCidrList = cmd.getGuestCidrList();
        if (!NetUtils.validateGuestCidrList(guestCidrList)) {
            throw new InvalidParameterValueException("The customer gateway guest cidr list " + guestCidrList + " contains invalid guest cidr!");
        }
        String ipsecPsk = cmd.getIpsecPsk();
        String ikePolicy = cmd.getIkePolicy();
        String espPolicy = cmd.getEspPolicy();
        if (!NetUtils.isValidS2SVpnPolicy(ikePolicy)) {
            throw new InvalidParameterValueException("The customer gateway IKE policy" + ikePolicy + " is invalid!");
        }
        if (!NetUtils.isValidS2SVpnPolicy(espPolicy)) {
            throw new InvalidParameterValueException("The customer gateway ESP policy" + espPolicy + " is invalid!");
        }
        Long ikeLifetime = cmd.getIkeLifetime();
        if (ikeLifetime == null) {
            // Default value of lifetime is 1 day
            ikeLifetime = (long)86400;
        }
        if (ikeLifetime > 86400) {
            throw new InvalidParameterValueException("The IKE lifetime " + ikeLifetime + " of vpn connection is invalid!");
        }
        Long espLifetime = cmd.getEspLifetime();
        if (espLifetime == null) {
            // Default value of lifetime is 1 hour
            espLifetime = (long)3600;
        }
        if (espLifetime > 86400) {
            throw new InvalidParameterValueException("The ESP lifetime " + espLifetime + " of vpn connection is invalid!");
        }

        Boolean dpd = cmd.getDpd();
        if (dpd == null) {
            dpd = false;
        }

        checkCustomerGatewayCidrList(guestCidrList);

        long accountId = gw.getAccountId();
        Site2SiteCustomerGatewayVO existedGw = _customerGatewayDao.findByGatewayIpAndAccountId(gatewayIp, accountId);
        if (existedGw != null && existedGw.getId() != gw.getId()) {
            throw new InvalidParameterValueException("The customer gateway with ip " + gatewayIp + " already existed in the system!");
        }
        existedGw = _customerGatewayDao.findByNameAndAccountId(name, accountId);
        if (existedGw != null && existedGw.getId() != gw.getId()) {
            throw new InvalidParameterValueException("The customer gateway with name " + name + " already existed!");
        }

        gw.setName(name);
        gw.setGatewayIp(gatewayIp);
        gw.setGuestCidrList(guestCidrList);
        gw.setIkePolicy(ikePolicy);
        gw.setEspPolicy(espPolicy);
        gw.setIpsecPsk(ipsecPsk);
        gw.setIkeLifetime(ikeLifetime);
        gw.setEspLifetime(espLifetime);
        gw.setDpd(dpd);
        _customerGatewayDao.persist(gw);
        return gw;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_CONNECTION_DELETE, eventDescription = "deleting s2s vpn connection", create = true)
    public boolean deleteVpnConnection(DeleteVpnConnectionCmd cmd) throws ResourceUnavailableException {
        CallContext.current().setEventDetails(" Id: " + cmd.getId());
        Account caller = CallContext.current().getCallingAccount();

        Long id = cmd.getId();
        Site2SiteVpnConnectionVO conn = _vpnConnectionDao.findById(id);
        if (conn == null) {
            throw new InvalidParameterValueException("Fail to find site to site VPN connection " + id + " to delete!");
        }

        _accountMgr.checkAccess(caller, null, false, conn);

        if (conn.getState() == State.Connected) {
            stopVpnConnection(id);
        }
        _vpnConnectionDao.remove(id);
        return true;
    }

    @DB
    private void stopVpnConnection(Long id) throws ResourceUnavailableException {
        Site2SiteVpnConnectionVO conn = _vpnConnectionDao.acquireInLockTable(id);
        if (conn == null) {
            throw new CloudRuntimeException("Unable to acquire lock on " + conn);
        }
        try {
            if (conn.getState() != State.Connected && conn.getState() != State.Error) {
                throw new InvalidParameterValueException("Site to site VPN connection with specified id is not in correct state(connected) to process disconnect!");
            }

            conn.setState(State.Disconnected);
            _vpnConnectionDao.persist(conn);

            boolean result = true;
            for (Site2SiteVpnServiceProvider element : _s2sProviders) {
                result = result & element.stopSite2SiteVpn(conn);
            }

            if (!result) {
                conn.setState(State.Error);
                _vpnConnectionDao.persist(conn);
                throw new ResourceUnavailableException("Failed to apply site-to-site VPN", Site2SiteVpnConnection.class, id);
            }
        } finally {
            _vpnConnectionDao.releaseFromLockTable(conn.getId());
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_CONNECTION_RESET, eventDescription = "reseting s2s vpn connection", create = true)
    public Site2SiteVpnConnection resetVpnConnection(ResetVpnConnectionCmd cmd) throws ResourceUnavailableException {
        CallContext.current().setEventDetails(" Id: " + cmd.getId());
        Account caller = CallContext.current().getCallingAccount();

        Long id = cmd.getId();
        Site2SiteVpnConnectionVO conn = _vpnConnectionDao.findById(id);
        if (conn == null) {
            throw new InvalidParameterValueException("Fail to find site to site VPN connection " + id + " to reset!");
        }
        _accountMgr.checkAccess(caller, null, false, conn);

        if (conn.getState() == State.Pending) {
            throw new InvalidParameterValueException("VPN connection " + id + " cannot be reseted when state is Pending!");
        }
        if (conn.getState() == State.Connected || conn.getState() == State.Error) {
            stopVpnConnection(id);
        }
        startVpnConnection(id);
        conn = _vpnConnectionDao.findById(id);
        return conn;
    }

    @Override
    public Pair<List<? extends Site2SiteCustomerGateway>, Integer> searchForCustomerGateways(ListVpnCustomerGatewaysCmd cmd) {
        Long id = cmd.getId();
        Long domainId = cmd.getDomainId();
        boolean isRecursive = cmd.isRecursive();
        String accountName = cmd.getAccountName();
        boolean listAll = cmd.listAll();
        long startIndex = cmd.getStartIndex();
        long pageSizeVal = cmd.getPageSizeVal();

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean,
                ListProjectResourcesCriteria>(domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, null, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(Site2SiteCustomerGatewayVO.class, "id", false, startIndex, pageSizeVal);

        SearchBuilder<Site2SiteCustomerGatewayVO> sb = _customerGatewayDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);

        SearchCriteria<Site2SiteCustomerGatewayVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        Pair<List<Site2SiteCustomerGatewayVO>, Integer> result = _customerGatewayDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Site2SiteCustomerGateway>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Site2SiteVpnGateway>, Integer> searchForVpnGateways(ListVpnGatewaysCmd cmd) {
        Long id = cmd.getId();
        Long vpcId = cmd.getVpcId();
        Boolean display = cmd.getDisplay();

        Long domainId = cmd.getDomainId();
        boolean isRecursive = cmd.isRecursive();
        String accountName = cmd.getAccountName();
        boolean listAll = cmd.listAll();
        long startIndex = cmd.getStartIndex();
        long pageSizeVal = cmd.getPageSizeVal();

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean,
                ListProjectResourcesCriteria>(domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, listAll, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(Site2SiteVpnGatewayVO.class, "id", false, startIndex, pageSizeVal);

        SearchBuilder<Site2SiteVpnGatewayVO> sb = _vpnGatewayDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("vpcId", sb.entity().getVpcId(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplay(), SearchCriteria.Op.EQ);

        SearchCriteria<Site2SiteVpnGatewayVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (display != null) {
            sc.setParameters("display", display);
        }

        if (vpcId != null) {
            sc.addAnd("vpcId", SearchCriteria.Op.EQ, vpcId);
        }

        Pair<List<Site2SiteVpnGatewayVO>, Integer> result = _vpnGatewayDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Site2SiteVpnGateway>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends Site2SiteVpnConnection>, Integer> searchForVpnConnections(ListVpnConnectionsCmd cmd) {
        Long id = cmd.getId();
        Long vpcId = cmd.getVpcId();
        Boolean display = cmd.getDisplay();

        Long domainId = cmd.getDomainId();
        boolean isRecursive = cmd.isRecursive();
        String accountName = cmd.getAccountName();
        boolean listAll = cmd.listAll();
        long startIndex = cmd.getStartIndex();
        long pageSizeVal = cmd.getPageSizeVal();

        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean,
                ListProjectResourcesCriteria>(domainId, isRecursive, null);
        _accountMgr.buildACLSearchParameters(caller, id, accountName, null, permittedAccounts, domainIdRecursiveListProject, listAll, false);
        domainId = domainIdRecursiveListProject.first();
        isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(Site2SiteVpnConnectionVO.class, "id", false, startIndex, pageSizeVal);

        SearchBuilder<Site2SiteVpnConnectionVO> sb = _vpnConnectionDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("display", sb.entity().isDisplay(), SearchCriteria.Op.EQ);

        if (vpcId != null) {
            SearchBuilder<Site2SiteVpnGatewayVO> gwSearch = _vpnGatewayDao.createSearchBuilder();
            gwSearch.and("vpcId", gwSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
            sb.join("gwSearch", gwSearch, sb.entity().getVpnGatewayId(), gwSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<Site2SiteVpnConnectionVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        if (display != null) {
            sc.setParameters("display", display);
        }
        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (vpcId != null) {
            sc.setJoinParameters("gwSearch", "vpcId", vpcId);
        }

        Pair<List<Site2SiteVpnConnectionVO>, Integer> result = _vpnConnectionDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends Site2SiteVpnConnection>, Integer>(result.first(), result.second());
    }

    @Override
    public boolean cleanupVpnConnectionByVpc(long vpcId) {
        List<Site2SiteVpnConnectionVO> conns = _vpnConnectionDao.listByVpcId(vpcId);
        for (Site2SiteVpnConnection conn : conns) {
            _vpnConnectionDao.remove(conn.getId());
        }
        return true;
    }

    @Override
    public boolean cleanupVpnGatewayByVpc(long vpcId) {
        Site2SiteVpnGatewayVO gw = _vpnGatewayDao.findByVpcId(vpcId);
        if (gw == null) {
            return true;
        }
        doDeleteVpnGateway(gw);
        return true;
    }

    @Override
    @DB
    public void markDisconnectVpnConnByVpc(long vpcId) {
        List<Site2SiteVpnConnectionVO> conns = _vpnConnectionDao.listByVpcId(vpcId);
        for (Site2SiteVpnConnectionVO conn : conns) {
            if (conn == null) {
                continue;
            }
            Site2SiteVpnConnectionVO lock = _vpnConnectionDao.acquireInLockTable(conn.getId());
            if (lock == null) {
                throw new CloudRuntimeException("Unable to acquire lock on " + conn);
            }
            try {
                if (conn.getState() == Site2SiteVpnConnection.State.Connected) {
                    conn.setState(Site2SiteVpnConnection.State.Disconnected);
                    _vpnConnectionDao.persist(conn);
                }
            } finally {
                _vpnConnectionDao.releaseFromLockTable(lock.getId());
            }
        }
    }

    @Override
    public List<Site2SiteVpnConnectionVO> getConnectionsForRouter(DomainRouterVO router) {
        List<Site2SiteVpnConnectionVO> conns = new ArrayList<Site2SiteVpnConnectionVO>();
        // One router for one VPC
        Long vpcId = router.getVpcId();
        if (router.getVpcId() == null) {
            return conns;
        }
        conns.addAll(_vpnConnectionDao.listByVpcId(vpcId));
        return conns;
    }

    @Override
    public boolean deleteCustomerGatewayByAccount(long accountId) {
        boolean result = true;
        ;
        List<Site2SiteCustomerGatewayVO> gws = _customerGatewayDao.listByAccountId(accountId);
        for (Site2SiteCustomerGatewayVO gw : gws) {
            result = result & doDeleteCustomerGateway(gw);
        }
        return result;
    }

    @Override
    public void reconnectDisconnectedVpnByVpc(Long vpcId) {
        List<Site2SiteVpnConnectionVO> conns = _vpnConnectionDao.listByVpcId(vpcId);
        for (Site2SiteVpnConnectionVO conn : conns) {
            if (conn == null) {
                continue;
            }
            if (conn.getState() == Site2SiteVpnConnection.State.Disconnected) {
                try {
                    startVpnConnection(conn.getId());
                } catch (ResourceUnavailableException e) {
                    Site2SiteCustomerGatewayVO gw = _customerGatewayDao.findById(conn.getCustomerGatewayId());
                    s_logger.warn("Site2SiteVpnManager: Fail to re-initiate VPN connection " + conn.getId() + " which connect to " + gw.getName());
                }
            }
        }
    }

    public List<Site2SiteVpnServiceProvider> getS2sProviders() {
        return _s2sProviders;
    }

    @Inject
    public void setS2sProviders(List<Site2SiteVpnServiceProvider> s2sProviders) {
        _s2sProviders = s2sProviders;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_CONNECTION_UPDATE, eventDescription = "creating s2s vpn gateway", async = true)
    public Site2SiteVpnConnection updateVpnConnection(long id, String customId, Boolean forDisplay) {
        Account caller = CallContext.current().getCallingAccount();
        Site2SiteVpnConnectionVO conn = _vpnConnectionDao.findById(id);
        if (conn == null) {
            throw new InvalidParameterValueException("Fail to find site to site VPN connection " + id);
        }

        _accountMgr.checkAccess(caller, null, false, conn);
        if (customId != null) {
            conn.setUuid(customId);
        }

        if (forDisplay != null) {
            conn.setDisplay(forDisplay);
        }

        _vpnConnectionDao.update(id, conn);
        return _vpnConnectionDao.findById(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_S2S_VPN_GATEWAY_UPDATE, eventDescription = "updating s2s vpn gateway", async = true)
    public Site2SiteVpnGateway updateVpnGateway(Long id, String customId, Boolean forDisplay) {
        Account caller = CallContext.current().getCallingAccount();

        Site2SiteVpnGatewayVO vpnGateway = _vpnGatewayDao.findById(id);
        if (vpnGateway == null) {
            throw new InvalidParameterValueException("Fail to find vpn gateway with " + id);
        }

        _accountMgr.checkAccess(caller, null, false, vpnGateway);
        if (customId != null) {
            vpnGateway.setUuid(customId);
        }

        if (forDisplay != null) {
            vpnGateway.setDisplay(forDisplay);
        }

        _vpnGatewayDao.update(id, vpnGateway);
        return _vpnGatewayDao.findById(id);

    }
}
