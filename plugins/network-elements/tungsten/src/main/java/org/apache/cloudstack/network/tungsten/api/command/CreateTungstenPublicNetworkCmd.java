package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.service.TungstenFabricUtils;
import org.apache.log4j.Logger;

import java.util.List;

import javax.inject.Inject;

@APICommand(name = "createTungstenPublicNetwork", description = "create tungsten public network", responseObject =
    SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateTungstenPublicNetworkCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateTungstenPublicNetworkCmd.class.getName());

    private static final String s_name = "createtungstenpublicnetworkresponse";

    @Inject
    VlanDao vlanDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    TungstenFabricUtils _tunstenFabricUtils;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true
        , description = "the ID of zone")
    private Long zoneId;

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final Long zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        List<NetworkVO> publicNetworkVOList = _networkDao.listByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        NetworkVO publicNetwork = publicNetworkVOList.get(0);

        // create public ip address
        SearchCriteria<VlanVO> sc = vlanDao.createSearchCriteria();
        sc.setParameters("network_id", publicNetwork.getId());
        VlanVO pubVlanVO = vlanDao.findOneBy(sc);
        String publicNetworkCidr = NetUtils.getCidrFromGatewayAndNetmask(pubVlanVO.getVlanGateway(),
            pubVlanVO.getVlanNetmask());
        Pair<String, Integer> publicPair = NetUtils.getCidr(publicNetworkCidr);
        String pubIp = this.getPublicIPAddress(publicNetwork);

        // create public network
        CreateTungstenNetworkCommand createTungstenPublicNetworkCommand = new CreateTungstenNetworkCommand(
            publicNetwork.getUuid(), TungstenUtils.getPublicNetworkName(zoneId), null, true, false, publicPair.first(),
            publicPair.second(), pubVlanVO.getVlanGateway(), true, null, pubIp, pubIp, false);
        TungstenAnswer createPublicNetworkAnswer = _tunstenFabricUtils.sendTungstenCommand(
            createTungstenPublicNetworkCommand, zoneId);
        if (!createPublicNetworkAnswer.getResult()) {
            throw new CloudRuntimeException("can not create tungsten public network");
        }
        VirtualNetwork publicVirtualNetwork = (VirtualNetwork) createPublicNetworkAnswer.getApiObjectBase();

        // create floating ip pool
        CreateTungstenFloatingIpPoolCommand createTungstenFloatingIpPoolCommand =
            new CreateTungstenFloatingIpPoolCommand(
            publicNetwork.getUuid(), TungstenUtils.getFloatingIpPoolName(zoneId));
        TungstenAnswer createFloatingIpPoolAnswer = _tunstenFabricUtils.sendTungstenCommand(
            createTungstenFloatingIpPoolCommand, zoneId);
        if (!createFloatingIpPoolAnswer.getResult()) {
            throw new CloudRuntimeException("can not create tungsten floating ip pool");
        }

        // create logical router with public network
        CreateTungstenLogicalRouterCommand createTungstenLogicalRouterCommand = new CreateTungstenLogicalRouterCommand(
            TungstenUtils.getLogicalRouterName(zoneId), null, publicVirtualNetwork.getUuid());
        TungstenAnswer createLogicalRouterAnswer = _tunstenFabricUtils.sendTungstenCommand(
            createTungstenLogicalRouterCommand, zoneId);
        if (!createLogicalRouterAnswer.getResult()) {
            throw new CloudRuntimeException("can not create tungsten logical router");
        }

        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setDisplayText("create tungsten public network successfully");
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    private String getPublicIPAddress(Network network) {
        List<IPAddressVO> allocatedIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), true);
        for (IPAddressVO ip : allocatedIps) {
            if (ip.isSourceNat()) {
                return ip.getAddress().addr();
            }
        }

        try {
            PublicIp publicIp = _ipAddrMgr.assignSourceNatIpAddressToGuestNetwork(
                _accountDao.findById(network.getAccountId()), network);
            IPAddressVO ip = publicIp.ip();
            _ipAddressDao.acquireInLockTable(ip.getId());
            _ipAddressDao.update(ip.getId(), ip);
            _ipAddressDao.releaseFromLockTable(ip.getId());
            return ip.getAddress().addr();
        } catch (Exception e) {
            s_logger.error("Unable to allocate source nat ip: " + e);
        }

        return null;
    }

}
