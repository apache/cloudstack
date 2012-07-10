package com.cloud.network.vpn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.api.commands.CreateVpnConnectionCmd;
import com.cloud.api.commands.CreateVpnCustomerGatewayCmd;
import com.cloud.api.commands.CreateVpnGatewayCmd;
import com.cloud.api.commands.DeleteVpnConnectionCmd;
import com.cloud.api.commands.DeleteVpnCustomerGatewayCmd;
import com.cloud.api.commands.DeleteVpnGatewayCmd;
import com.cloud.api.commands.ListVpnConnectionsCmd;
import com.cloud.api.commands.ListVpnCustomerGatewaysCmd;
import com.cloud.api.commands.ListVpnGatewaysCmd;
import com.cloud.api.commands.ResetVpnConnectionCmd;
import com.cloud.api.commands.UpdateVpnCustomerGatewayCmd;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.NetworkManager;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteCustomerGatewayVO;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnConnection.State;
import com.cloud.network.Site2SiteVpnConnectionVO;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.Site2SiteVpnGatewayVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.vpc.Dao.VpcDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.net.NetUtils;

@Local(value = Site2SiteVpnService.class)
public class Site2SiteVpnManagerImpl implements Site2SiteVpnService, Manager {
    private static final Logger s_logger = Logger.getLogger(Site2SiteVpnManagerImpl.class);

    @Inject Site2SiteCustomerGatewayDao _customerGatewayDao;
    @Inject Site2SiteVpnGatewayDao _vpnGatewayDao;
    @Inject Site2SiteVpnConnectionDao _vpnConnectionDao;
    @Inject NetworkManager _networkMgr;
    @Inject VpcDao _vpcDao;
    @Inject IPAddressDao _ipAddressDao;
    
    String _name;
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Site2SiteVpnGateway createVpnGateway(CreateVpnGatewayCmd cmd) {
        Long ipId = cmd.getPublicIpId();
	    IpAddress ip = _networkMgr.getIp(ipId);
	    if (ip.getVpcId() == null) {
            throw new InvalidParameterValueException("The VPN gateway cannot create with ip not belong to VPC");
	    }
        if (_vpnGatewayDao.findByIpAddrId(ipId) != null) {
            throw new InvalidParameterValueException("The VPN gateway with ip ID " + ipId + " already existed!");
        }
        Site2SiteVpnGatewayVO gw = new Site2SiteVpnGatewayVO(ipId);
        _vpnGatewayDao.persist(gw);
        return gw;
    }

    @Override
    public Site2SiteCustomerGateway createCustomerGateway(CreateVpnCustomerGatewayCmd cmd) {
        String gatewayIp = cmd.getGatewayIp();
        if (!NetUtils.isValidIp(gatewayIp)) {
            throw new InvalidParameterValueException("The customer gateway ip " + gatewayIp + " is invalid!");
        }
        String guestCidrList = cmd.getGuestCidrList();
        if (!NetUtils.validateGuestCidrList(guestCidrList)) {
            throw new InvalidParameterValueException("The customer gateway guest cidr list " + guestCidrList + " is invalid guest cidr!");
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
        Long lifetime = cmd.getLifetime();
        if (lifetime == null) {
            // Default value of lifetime is 1 day
            lifetime = (long) 86400;
        }
        if (lifetime > 86400) {
            throw new InvalidParameterValueException("The lifetime " + lifetime + " of vpn connection is invalid!");
        }
        if (_customerGatewayDao.findByGatewayIp(gatewayIp) != null) {
            throw new InvalidParameterValueException("The customer gateway with ip " + gatewayIp + " already existed!");
        }
        Site2SiteCustomerGatewayVO gw = new Site2SiteCustomerGatewayVO(gatewayIp, guestCidrList, ipsecPsk,
                ikePolicy, espPolicy, lifetime);
        _customerGatewayDao.persist(gw);
        return gw;
    }

    @Override
    public Site2SiteVpnConnection createVpnConnection(CreateVpnConnectionCmd cmd) throws NetworkRuleConflictException {
        Long customerGatewayId = cmd.getCustomerGatewayId();
        Site2SiteCustomerGateway customerGateway = _customerGatewayDao.findById(customerGatewayId);
        if (customerGateway == null) {
            throw new InvalidParameterValueException("Unable to found specified Site to Site VPN customer gateway " + customerGatewayId + " !");
        }
        Long vpnGatewayId = cmd.getVpnGatewayId();
        Site2SiteVpnGateway vpnGateway = _vpnGatewayDao.findById(vpnGatewayId);
        if (vpnGateway == null) {
            throw new InvalidParameterValueException("Unable to found specified Site to Site VPN gateway " + vpnGatewayId + " !");
        }
        if (_vpnConnectionDao.findByCustomerGatewayId(customerGatewayId) != null ||
                _vpnConnectionDao.findByVpnGatewayId(vpnGatewayId) != null) {
            throw new InvalidParameterValueException("The vpn connection with customer gateway id " + customerGatewayId + " or vpn gateway id " 
                    + vpnGatewayId + " already existed!");
        }
        Site2SiteVpnConnectionVO conn = new Site2SiteVpnConnectionVO(vpnGatewayId, customerGatewayId);
        conn.setState(State.Pending);
        _vpnConnectionDao.persist(conn);
        return conn;
    }

    @Override
    public Site2SiteVpnConnection startVpnConnection(long id) throws ResourceUnavailableException {
        Site2SiteVpnConnectionVO conn = _vpnConnectionDao.findById(id);
        if (conn.getState() != State.Pending && conn.getState() != State.Disconnected) {
            throw new InvalidParameterValueException("Site to site VPN connection " + id + " not in correct state(pending or disconnected) to process!");
        }

        conn.setState(State.Pending);
        _vpnConnectionDao.persist(conn);
        List <? extends Site2SiteVpnServiceProvider> elements = _networkMgr.getSite2SiteVpnElements();
        boolean result = true;
        for (Site2SiteVpnServiceProvider element : elements) {
            result = result & element.startSite2SiteVpn(conn);
        }

        if (result) {
            conn.setState(State.Connected);
            _vpnConnectionDao.persist(conn);
            return conn;
        }
        conn.setState(State.Error);
        _vpnConnectionDao.persist(conn);
        throw new ResourceUnavailableException("Failed to apply site-to-site VPN", Site2SiteVpnConnection.class, id);
    }

    @Override
    public IpAddress getVpnGatewayIp(Long vpnGatewayId) {
        Site2SiteVpnGatewayVO gateway = _vpnGatewayDao.findById(vpnGatewayId);
        IpAddress ip = _networkMgr.getIp(gateway.getAddrId());
        return ip;
    }

    @Override
    public Site2SiteCustomerGateway deleteCustomerGateway(DeleteVpnCustomerGatewayCmd cmd) {
        Long id = cmd.getId();
        Site2SiteCustomerGateway customerGateway = _customerGatewayDao.findById(id);
        if (customerGateway == null) {
            throw new InvalidParameterValueException("Fail to find customer gateway with " + id + " !");
        }
        _customerGatewayDao.remove(id);
        return customerGateway;
    }

    @Override
    public Site2SiteVpnGateway deleteVpnGateway(DeleteVpnGatewayCmd cmd) {
        Long id = cmd.getId();
        Site2SiteVpnGateway vpnGateway = _vpnGatewayDao.findById(id);
        if (vpnGateway == null) {
            throw new InvalidParameterValueException("Fail to find vpn gateway with " + id + " !");
        }
        _vpnGatewayDao.remove(id);
        return vpnGateway;
    }

    @Override
    public Site2SiteCustomerGateway updateCustomerGateway(UpdateVpnCustomerGatewayCmd cmd) {
        Long id = cmd.getId();
        Site2SiteCustomerGatewayVO gw = _customerGatewayDao.findById(id);
        if (gw == null) {
            throw new InvalidParameterValueException("Find to find customer gateway with id " + id);
        }
        Site2SiteVpnConnection conn = _vpnConnectionDao.findByCustomerGatewayId(id);
        if (conn != null && (conn.getState() != State.Disconnected || conn.getState() != State.Error)) {
            throw new InvalidParameterValueException("Unable to update customer gateway because there is the correlate VPN connection " + conn.getId()
                    + " still active!");
        }
        String gatewayIp = cmd.getGatewayIp();
        if (!NetUtils.isValidIp(gatewayIp)) {
            throw new InvalidParameterValueException("The customer gateway ip " + gatewayIp + " is invalid!");
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
        Long lifetime = cmd.getLifetime();
        if (lifetime == null) {
            // Default value of lifetime is 1 day
            lifetime = (long) 86400;
        }
        if (lifetime > 86400) {
            throw new InvalidParameterValueException("The lifetime " + lifetime + " of vpn connection is invalid!");
        }
        gw.setGatewayIp(gatewayIp);
        gw.setGuestCidrList(guestCidrList);
        gw.setIkePolicy(ikePolicy);
        gw.setEspPolicy(espPolicy);
        gw.setIpsecPsk(ipsecPsk);
        gw.setLifetime(lifetime);
        _customerGatewayDao.persist(gw);
        return gw;
    }

    @Override
    public Site2SiteVpnConnection deleteVpnConnection(DeleteVpnConnectionCmd cmd) throws ResourceUnavailableException {
        Long id = cmd.getId();
        Site2SiteVpnConnectionVO conn = _vpnConnectionDao.findById(id);
        if (conn == null) {
            throw new InvalidParameterValueException("Fail to find site to site VPN connection " + id + " to delete!");
        }
        if (conn.getState() == State.Connected) {
            stopVpnConnection(id);
        }
        _vpnConnectionDao.remove(id);
        return conn;
    }

    private void stopVpnConnection(Long id) throws ResourceUnavailableException {
        Site2SiteVpnConnectionVO conn = _vpnConnectionDao.findById(id);
        if (conn.getState() != State.Connected && conn.getState() != State.Error) {
            throw new InvalidParameterValueException("Site to site VPN connection " + id + " not in correct state(connected) to process disconnect!");
        }

        List <? extends Site2SiteVpnServiceProvider> elements = _networkMgr.getSite2SiteVpnElements();
        boolean result = true;
        conn.setState(State.Disconnected);
        _vpnConnectionDao.persist(conn);
        for (Site2SiteVpnServiceProvider element : elements) {
            result = result & element.stopSite2SiteVpn(conn);
        }

        if (!result) {
            conn.setState(State.Error);
            _vpnConnectionDao.persist(conn);
            throw new ResourceUnavailableException("Failed to apply site-to-site VPN", Site2SiteVpnConnection.class, id);
        }
    }

    @Override
    public Site2SiteVpnConnection resetVpnConnection(ResetVpnConnectionCmd cmd) throws ResourceUnavailableException {
        Long id = cmd.getId();
        Site2SiteVpnConnectionVO conn = _vpnConnectionDao.findById(id);
        if (conn == null) {
            throw new InvalidParameterValueException("Fail to find site to site VPN connection " + id + " to reset!");
        }
        if (conn.getState() == State.Connected || conn.getState() == State.Error) {
            stopVpnConnection(id);
        }
        startVpnConnection(id);
        return conn;
    }

    @Override
    public List<Site2SiteCustomerGateway> searchForCustomerGateways(ListVpnCustomerGatewaysCmd cmd) {
        Long id = cmd.getId();
        List<Site2SiteCustomerGateway> results = new ArrayList<Site2SiteCustomerGateway>();
        if (id != null) {
            results.add(_customerGatewayDao.findById(cmd.getId()));
        } else {
            results.addAll(_customerGatewayDao.listAll());
        }
        return results;
    }

    @Override
    public List<Site2SiteVpnGateway> searchForVpnGateways(ListVpnGatewaysCmd cmd) {
        Long id = cmd.getId();
        List<Site2SiteVpnGateway> results = new ArrayList<Site2SiteVpnGateway>();
        if (id != null) {
            results.add(_vpnGatewayDao.findById(cmd.getId()));
        } else {
            results.addAll(_vpnGatewayDao.listAll());
        }
        return results;
    }

    @Override
    public List<Site2SiteVpnConnection> searchForVpnConnections(ListVpnConnectionsCmd cmd) {
        Long id = cmd.getId();
        List<Site2SiteVpnConnection> results = new ArrayList<Site2SiteVpnConnection>();
        if (id != null) {
            results.add(_vpnConnectionDao.findById(cmd.getId()));
        } else {
            results.addAll(_vpnConnectionDao.listAll());
        }
        return results;
    }
}
