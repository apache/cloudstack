package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkServiceMapVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import java.util.List;

import javax.inject.Inject;

@APICommand(name = "configTunstenPublicNetwork", description = "config tungsten public network", responseObject =
    SuccessResponse.class)
public class ConfigTungstenPublicNetworkCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ConfigTungstenPublicNetworkCmd.class.getName());
    private static final String s_name = "configtungstenpublicnetworkresponse";

    @Inject
    NetworkDao networkDao;
    @Inject
    NetworkServiceMapDao _ntwkSvcMap;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true
        , description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType =
        PhysicalNetworkResponse.class, required = true, description = "the ID of physical network")
    private Long physicalNetworkId;

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final Long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(final Long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "configuring public network";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {

        // TODO : check if it can enable when it create
        PhysicalNetworkServiceProviderVO physicalNetworkServiceProvider =
            _physicalNetworkServiceProviderDao.findByServiceProvider(
            physicalNetworkId, Network.Provider.Tungsten.getName());
        physicalNetworkServiceProvider.setState(PhysicalNetworkServiceProvider.State.Enabled);
        _physicalNetworkServiceProviderDao.persist(physicalNetworkServiceProvider);

        // create public network service map
        List<NetworkVO> publicNetworkVOList = networkDao.listByZoneAndTrafficType(zoneId, Networks.TrafficType.Public);
        NetworkVO publicNetwork = publicNetworkVOList.get(0);
        NetworkServiceMapVO networkServiceMapVO = new NetworkServiceMapVO(publicNetwork.getId(),
            Network.Service.Connectivity, Network.Provider.Tungsten);
        _ntwkSvcMap.persist(networkServiceMapVO);

        SuccessResponse response = new SuccessResponse(getCommandName());
        response.setDisplayText("config tungsten public network sucessfully");
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
}
