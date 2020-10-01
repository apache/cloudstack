package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.dao.NetworkDao;
import org.apache.cloudstack.network.tungsten.service.TungstenService;

public class CreateTungstenNetworkCommand extends TungstenCommand {

    private final long networkId;
    private transient NetworkDao networkDao;
    private transient TungstenService tungstenService;
    private transient DataCenterDao dataCenterDao;

    public CreateTungstenNetworkCommand(long networkId, NetworkDao networkDao, TungstenService tungstenService, DataCenterDao dataCenterDao) {
        this.networkId = networkId;
        this.networkDao = networkDao;
        this.dataCenterDao = dataCenterDao;
        this.tungstenService = tungstenService;
    }

    public long getNetworkId() {
        return networkId;
    }

    public TungstenService getTungstenService() {
        return tungstenService;
    }

    public NetworkDao getNetworkDao() {
        return networkDao;
    }

    public DataCenterDao getDataCenterDao() {
        return dataCenterDao;
    }
}
