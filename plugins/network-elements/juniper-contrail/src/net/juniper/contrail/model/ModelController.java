package net.juniper.contrail.model;

import com.cloud.dc.dao.VlanDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.management.ContrailManager;

/**
 * Collection of state necessary for model object to update the Contrail API server.
 * 
 */
public class ModelController {
    ApiConnector _api;
    ContrailManager _manager;
    UserVmDao _vmDao;
    NetworkDao  _networkDao;
    NicDao _nicDao;
    VlanDao  _vlanDao;
    IPAddressDao _ipAddressDao;
    
    public ModelController(ContrailManager manager, ApiConnector api, UserVmDao vmDao, NetworkDao networkDao, 
            NicDao nicDao, VlanDao vlanDao, IPAddressDao ipAddressDao) {
        _manager = manager;
        assert api != null;
        _api = api;
        assert vmDao != null;
        _vmDao = vmDao;
        assert networkDao != null;
        _networkDao = networkDao;
        assert nicDao != null;
        _nicDao = nicDao;
        assert vlanDao != null;
        _vlanDao = vlanDao;
        assert ipAddressDao != null;
        _ipAddressDao = ipAddressDao;
    }
    ApiConnector getApiAccessor() {
        return _api;
    }
    ContrailManager getManager() {
        return _manager;
    }
    
    UserVmDao getVmDao() {
        return _vmDao;
    }
    
    NetworkDao getNetworkDao() {
        return _networkDao;
    }
    
    NicDao getNicDao() {
        return _nicDao;
    }
    
    VlanDao getVlanDao() {
        return _vlanDao;
    }

    IPAddressDao getIPAddressDao() {
        return _ipAddressDao;
    }
}
