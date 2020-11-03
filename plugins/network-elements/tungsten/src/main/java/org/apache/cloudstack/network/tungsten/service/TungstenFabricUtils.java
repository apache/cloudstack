package org.apache.cloudstack.network.tungsten.service;

import com.cloud.agent.AgentManager;
import com.cloud.network.Network;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class TungstenFabricUtils {

    private static final Logger s_logger = Logger.getLogger(TungstenFabricUtils.class);

    @Inject
    AgentManager agentMgr;
    @Inject
    TungstenProviderDao tungstenProviderDao;

    public TungstenAnswer sendTungstenCommand(TungstenCommand cmd, Network network) throws IllegalArgumentException {

        TungstenProviderVO tungstenProviderVO = tungstenProviderDao.findByZoneId(network.getDataCenterId());
        if (tungstenProviderVO == null) {
            s_logger.error("No tungsten provider have been found!");
            throw new IllegalArgumentException("Failed to find a tungsten provider");
        }

        TungstenAnswer answer = (TungstenAnswer) agentMgr.easySend(tungstenProviderVO.getHostId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("Tungsten API Command failed");
            throw new IllegalArgumentException("Failed API call to Tungsten Network plugin");
        }

        return answer;
    }
}
