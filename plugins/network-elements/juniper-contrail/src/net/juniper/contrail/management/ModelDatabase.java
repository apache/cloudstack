package net.juniper.contrail.management;

import java.util.TreeSet;

import com.cloud.network.Networks.TrafficType;

import net.juniper.contrail.model.ModelObjectBase;
import net.juniper.contrail.model.ServiceInstanceModel;
import net.juniper.contrail.model.VirtualMachineModel;
import net.juniper.contrail.model.VirtualNetworkModel;

public class ModelDatabase {
    TreeSet<ServiceInstanceModel> _serviceInstanceTable;
    TreeSet<VirtualMachineModel> _vmTable;
    TreeSet<VirtualNetworkModel> _vnTable;
    
    ModelDatabase() {
        initDb();
    }

    public void initDb() {
        _serviceInstanceTable = new TreeSet<ServiceInstanceModel>(new ModelObjectBase.UuidComparator());
        _vmTable = new TreeSet<VirtualMachineModel>(new ModelObjectBase.UuidComparator());
        _vnTable = new TreeSet<VirtualNetworkModel>(new ModelObjectBase.UuidComparator());
    }
    
    public TreeSet<ServiceInstanceModel> getServiceInstances() {
        return _serviceInstanceTable;
    }
    
    public ServiceInstanceModel lookupServiceInstance(String uuid) {
        ServiceInstanceModel siKey = new ServiceInstanceModel(uuid);
        ServiceInstanceModel current = _serviceInstanceTable.ceiling(siKey);
        if  (current != null && current.getUuid().equals(uuid)) {
            return current;
        }
        return null;
    }
    
    public TreeSet<VirtualMachineModel> getVirtualMachines() {
        return _vmTable;
    }
    
    public VirtualMachineModel lookupVirtualMachine(String uuid) {
        VirtualMachineModel vmKey = new VirtualMachineModel(null, uuid);
        VirtualMachineModel current = _vmTable.ceiling(vmKey);
        if (current != null && current.getUuid().equals(uuid)) {
            return current;
        }
        return null;
    }
    
    public TreeSet<VirtualNetworkModel> getVirtualNetworks() {
        return _vnTable;
    }
    
    public VirtualNetworkModel lookupVirtualNetwork(String uuid, String name, TrafficType ttype) {
        VirtualNetworkModel vnKey = new VirtualNetworkModel(null, uuid, name, ttype);
        VirtualNetworkModel current = _vnTable.ceiling(vnKey);
        if (current != null) {
            if (ttype == TrafficType.Management || ttype == TrafficType.Storage
                    || ttype == TrafficType.Control) {
                if (current.getName().equals(name)) {
                    return current;
                }
            } else if (current.getUuid().equals(uuid)) {
                return current;
            } 
        }
        return null;
    }
}
