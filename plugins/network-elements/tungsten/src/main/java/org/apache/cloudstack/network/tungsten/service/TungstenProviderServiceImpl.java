package org.apache.cloudstack.network.tungsten.service;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.TungstenProvider;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenProvidersCmd;
import org.apache.cloudstack.network.tungsten.resource.TungstenResource;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TungstenProviderServiceImpl implements TungstenProviderService {

    private static final Logger s_logger = Logger.getLogger(TungstenProviderServiceImpl.class);

    @Inject
    TungstenProviderDao _tungstenProviderDao;
    @Inject
    TungstenService _tungstenService;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    DataCenterDao _zoneDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    HostDetailsDao _hostDetailsDao;
    @Inject
    HostDao _hostDao;

    @Override
    public TungstenProvider addProvider(CreateTungstenProviderCmd cmd) {
        TungstenProviderVO element = _tungstenProviderDao.findByNspId(cmd.getNspId());
        if (element != null) {
            s_logger.debug("There is already a tungsten provider with service network provider id " + cmd.getNspId());
            return null;
        }

        TungstenProviderVO tungstenProvider;
        final Long physicalNetworkId = cmd.getPhysicalNetworkId();
        final String name = cmd.getName();
        final String hostname = cmd.getHostname();
        final String port = cmd.getPort();
        final String vrouter = cmd.getVrouter();
        final String vrouterPort = cmd.getVrouterPort();

        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find physical network with ID: " + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        final PhysicalNetworkServiceProviderVO ntwkSvcProvider =
                _physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(), Network.Provider.Tungsten.getName());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + Network.Provider.Tungsten.getName() + " is not enabled in the physical network: " +
                    physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() + " is in shutdown state in the physical network: " +
                    physicalNetworkId + "to add this device");
        }

        TungstenResource tungstenResource = new TungstenResource();

        DataCenterVO zone = _zoneDao.findById(physicalNetwork.getDataCenterId());
        String zoneName;
        if (zone != null) {
            zoneName = zone.getName();
        } else {
            zoneName = String.valueOf(zoneId);
        }

        Map<String, String> params = new HashMap<String, String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", zoneName);
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "TungstenDevice - " + cmd.getName());
        params.put("hostname", cmd.getHostname());
        params.put("port", cmd.getPort());
        params.put("vrouter", cmd.getVrouter());
        params.put("vrouterPort", cmd.getVrouterPort());

        Map<String, Object> hostdetails = new HashMap<String, Object>();
        hostdetails.putAll(params);

        try {
            tungstenResource.configure(cmd.getHostname(), hostdetails);
            final Host host = _resourceMgr.addHost(zoneId, tungstenResource, Host.Type.L2Networking, params);
            if (host != null) {
                tungstenProvider = Transaction.execute(new TransactionCallback<TungstenProviderVO>() {
                    @Override
                    public TungstenProviderVO doInTransaction(TransactionStatus status) {
                        TungstenProviderVO tungstenProviderVO = new TungstenProviderVO(cmd.getNspId(), physicalNetworkId, name, host.getId(),
                                port, hostname, vrouter, vrouterPort);
                        _tungstenProviderDao.persist(tungstenProviderVO);
                        _tungstenService.init();

                        DetailVO detail = new DetailVO(host.getId(), "tungstendeviceid", String.valueOf(tungstenProviderVO.getId()));
                        _hostDetailsDao.persist(detail);

                        return tungstenProviderVO;
                    }
                });
            } else {
                throw new CloudRuntimeException("Failed to add Tungsten provider due to internal error.");
            }
        } catch (ConfigurationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        return tungstenProvider;
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
        TungstenProviderVO tungstenProviderVO = _tungstenProviderDao.findByUuid(cmd.getTungstenProviderUuid());
        if (tungstenProviderVO == null)
            throw new InvalidParameterValueException("Tungsten provider not found with the uuid: " + cmd.getTungstenProviderUuid());

        HostVO tungstenHost = _hostDao.findById(tungstenProviderVO.getHostId());
        Long hostId = tungstenHost.getId();

        tungstenHost.setResourceState(ResourceState.Maintenance);
        _hostDao.update(hostId, tungstenHost);
        _resourceMgr.deleteHost(hostId, false, false);

        _tungstenProviderDao.deleteProviderByUuid(tungstenProviderVO.getUuid());
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

}
