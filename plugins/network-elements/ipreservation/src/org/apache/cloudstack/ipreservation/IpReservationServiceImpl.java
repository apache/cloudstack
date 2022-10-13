package org.apache.cloudstack.ipreservation;

import com.cloud.network.IpReservationVO;
import com.cloud.network.Network;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.IpReservationDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.ipreservation.api.commands.AddIpReservationCmd;
import org.apache.cloudstack.ipreservation.api.commands.ListIpReservationCmd;
import org.apache.cloudstack.ipreservation.api.commands.RemoveIpReservationCmd;
import org.apache.cloudstack.ipreservation.api.response.IpReservationResponse;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IpReservationServiceImpl extends ComponentLifecycleBase implements IpReservationService {
    public static final Logger logger = Logger.getLogger(IpReservationServiceImpl.class);

    @Inject
    IpReservationDao ipReservationDao;

    @Inject
    NetworkService networkService;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(AddIpReservationCmd.class);
        cmdList.add(ListIpReservationCmd.class);
        cmdList.add(RemoveIpReservationCmd.class);
        return cmdList;
    }

    @Override
    public void createReservation(AddIpReservationCmd cmd) {
        logger.debug("Creating reservation for network: " + cmd.getNetworkId() + " with range: " + cmd.getStartIp() + "-" + cmd.getEndIp());
        Network network = networkService.getNetwork(cmd.getNetworkId());
        validateAddReservation(cmd, network);
        IpReservationVO create = new IpReservationVO(cmd.getStartIp(), cmd.getEndIp(), network.getId());
        IpReservationVO created = ipReservationDao.persist(create);

        IpReservationResponse ipReservationResponse = generateListResponse(created, network);
        ipReservationResponse.setResponseName(cmd.getCommandName());
        cmd.setResponseObject(ipReservationResponse);
    }

    private static void validateAddReservation(AddIpReservationCmd cmd, Network network) {
        if (!NetUtils.validIpRange(cmd.getStartIp(), cmd.getEndIp())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Start and end are not valid range");
        }

        Pair<String, Integer> cidr = NetUtils.getCidr(network.getCidr());
        if (!NetUtils.sameSubnetCIDR(cidr.first(), cmd.getStartIp(), cidr.second())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Start is not within the network cidr");
        }

        if (!NetUtils.sameSubnetCIDR(cidr.first(), cmd.getEndIp(), cidr.second())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "End is not within the network cidr");
        }
    }

    @Override
    public void getReservations(ListIpReservationCmd cmd) {
        if (cmd.getNetworkId() != null) {
            logger.debug("Getting all reservations for network " + cmd.getNetworkId());
            Network network = networkService.getNetwork(cmd.getNetworkId());
            List<IpReservationVO> reservations = ipReservationDao.getIpReservationsForNetwork(network.getId());
            ListResponse<IpReservationResponse> response = generateListResponse(reservations, network);
            response.setResponseName(cmd.getCommandName());
            cmd.setResponseObject(response);
        } else {
            List<IpReservationDao.FullIpReservation> reservations = ipReservationDao.getAllIpReservations();
            List<IpReservationResponse> responses = reservations.stream()
                    .map(r -> new IpReservationResponse(r.id, r.startip, r.endip, r.networkid))
                    .collect(Collectors.toList());
            ListResponse<IpReservationResponse> response = new ListResponse<>();
            response.setResponses(responses);
            response.setResponseName(cmd.getCommandName());
            cmd.setResponseObject(response);
        }
    }

    @Override
    public void removeReservation(RemoveIpReservationCmd cmd) {
        IpReservationVO reservation = ipReservationDao.findByUuid(cmd.getId());
        boolean removed = ipReservationDao.remove(reservation.getId());
        SuccessResponse response = new SuccessResponse(cmd.getCommandName());
        response.setSuccess(removed);
        cmd.setResponseObject(response);
    }

    protected IpReservationResponse generateListResponse(IpReservationVO reservation, Network network) {
        return new IpReservationResponse(reservation.getUuid(), reservation.getStartIp(), reservation.getEndIp(), network.getUuid());
    }

    protected ListResponse<IpReservationResponse> generateListResponse(List<IpReservationVO> reservations, Network network) {
        List<IpReservationResponse> responses = reservations.stream()
                .map(reservation -> new IpReservationResponse(reservation.getUuid(), reservation.getStartIp(), reservation.getEndIp(), network.getUuid()))
                .collect(Collectors.toList());
        ListResponse<IpReservationResponse> response = new ListResponse<>();
        response.setResponses(responses);
        return response;
    }
}
