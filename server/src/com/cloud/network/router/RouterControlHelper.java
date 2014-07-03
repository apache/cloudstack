package com.cloud.network.router;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

public class RouterControlHelper {

    private static final Logger logger = Logger.getLogger(RouterControlHelper.class);

    @Inject
    private DomainRouterDao routerDao = null;

    @Inject
    private NetworkDao networkDao;

    @Inject
    private NicDao nicDao;

    public String getRouterControlIp(final long routerId) {
        String routerControlIpAddress = null;
        final List<NicVO> nics = nicDao.listByVmId(routerId);
        for (final NicVO n : nics) {
            final NetworkVO nc = networkDao.findById(n.getNetworkId());
            if (nc != null && nc.getTrafficType() == TrafficType.Control) {
                routerControlIpAddress = n.getIp4Address();
                // router will have only one control ip
                break;
            }
        }

        if (routerControlIpAddress == null) {
            logger.warn("Unable to find router's control ip in its attached NICs!. routerId: " + routerId);
            final DomainRouterVO router = routerDao.findById(routerId);
            return router.getPrivateIpAddress();
        }

        return routerControlIpAddress;
    }

    public String getRouterIpInNetwork(final long networkId, final long instanceId) {
        return nicDao.getIpAddress(networkId, instanceId);
    }
}