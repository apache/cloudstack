package org.apache.cloudstack.network.tungsten.service;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenProvidersCmd;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class TungstenProviderServiceImpl implements TungstenProviderService {

    private static final Logger s_logger = Logger.getLogger(TungstenProviderServiceImpl.class);

    @Inject
    TungstenProviderDao _tungstenProviderDao;
    @Inject
    TungstenService _tungstenService;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;

    @Override
    public TungstenProvider addProvider(CreateTungstenProviderCmd cmd) throws ConfigurationException {
        checkProviderConnection(cmd);
        TungstenProviderVO element = _tungstenProviderDao.findByNspId(cmd.getNspId());
        if (element != null) {
            s_logger.debug("There is already a tungsten provider with service network provider id " + cmd.getNspId());
            return null;
        }
        element = new TungstenProviderVO(cmd.getNspId(), cmd.getName(), cmd.getPort(), cmd.getHostname(), cmd.getVrouter(), cmd.getVrouterPort());
        _tungstenProviderDao.persist(element);
        _tungstenService.init();
        return element;
    }

    @Override
    public TungstenProviderVO getTungstenProvider() {
        PhysicalNetworkServiceProviderVO provider = _physicalNetworkServiceProviderDao.findByProviderName(Network.Provider.Tungsten.getName());
        TungstenProviderVO tungstenProvider = _tungstenProviderDao.findByNspId(provider.getId());
        return tungstenProvider;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TUNGSTEN_DELETE_PROVIDER, eventDescription = "Delete tungsten provider", async = true)
    public void deleteTungstenProvider(DeleteTungstenProviderCmd cmd) {
        if (_tungstenProviderDao.findByUuid(cmd.getTungstenProviderUuid()) == null)
            throw new InvalidParameterValueException("Tungsten provider not found with the uuid: " + cmd.getTungstenProviderUuid());
        _tungstenProviderDao.deleteProviderByUuid(cmd.getTungstenProviderUuid());
        disableTungstenNsp();
    }

    @Override
    public List<TungstenProviderVO> listProviders(ListTungstenProvidersCmd cmd) {
        if (cmd.getTungstenProviderUuid() != null) {
            List<TungstenProviderVO> tungstenProviders = new ArrayList<TungstenProviderVO>();
            TungstenProviderVO tungstenProvider = _tungstenProviderDao.findById(cmd.getTungstenProviderUuid());
            if (tungstenProvider != null) {
                tungstenProviders.add(tungstenProvider);
            }
            return tungstenProviders;
        }
        return _tungstenProviderDao.listAll();
    }

    @Override
    public void disableTungstenNsp() {
        PhysicalNetworkServiceProviderVO networkServiceProvider = _physicalNetworkServiceProviderDao.findByProviderName(Network.Provider.Tungsten.getName());
        networkServiceProvider.setState(PhysicalNetworkServiceProvider.State.Disabled);
        _physicalNetworkServiceProviderDao.update(networkServiceProvider.getId(), networkServiceProvider);
    }

    public void checkProviderConnection(CreateTungstenProviderCmd cmd) {
        try {
            URL url = new URL("http://" + cmd.getHostname() + ":" + cmd.getPort());
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();

            if (huc.getResponseCode() != 200) {
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR,
                        "There is not a tungsten provider using hostname: " + cmd.getHostname() + " and port: " + cmd.getPort());
            }
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR,
                    "There is not a tungsten provider using hostname: " + cmd.getHostname() + " and port: " + cmd.getPort());
        }
    }
}
