package org.apache.cloudstack.service;

import com.cloud.host.DetailVO;
import com.cloud.network.NsxProvider;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.element.NsxProviderVO;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.command.AddNsxControllerCmd;
import org.apache.cloudstack.api.response.NsxControllerResponse;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.List;

public class NsxProviderServiceImpl implements NsxProviderService {

    @Inject
    NsxProviderDao nsxProviderDao;

    @Override
    public NsxProvider addProvider(AddNsxControllerCmd cmd) {
        NsxProviderVO nsxProvider = Transaction.execute((TransactionCallback<NsxProviderVO>) status -> {
            NsxProviderVO nsxProviderVO = new NsxProviderVO(cmd.getZoneId(), cmd.getName(), cmd.getHostname(),
                    cmd.getUsername(),  cmd.getPassword(),
                    cmd.getTier0Gateway(), cmd.getEdgeCluster());
            nsxProviderDao.persist(nsxProviderVO);

            return nsxProviderVO;
        });
        return null;
    }

    @Override
    public NsxControllerResponse createNsxControllerResponse(NsxProvider tungstenProvider) {
        return null;
    }

    @Override
    public List<BaseResponse> listTungstenProvider(Long zoneId) {
        return null;
    }

    @Override
    public List<Class<?>> getCommands() {
        return null;
    }
}
