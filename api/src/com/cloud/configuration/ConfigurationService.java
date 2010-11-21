package com.cloud.configuration;

import com.cloud.api.commands.CreateCfgCmd;
import com.cloud.api.commands.CreateDiskOfferingCmd;
import com.cloud.api.commands.CreatePodCmd;
import com.cloud.api.commands.CreateServiceOfferingCmd;
import com.cloud.api.commands.CreateVlanIpRangeCmd;
import com.cloud.api.commands.CreateZoneCmd;
import com.cloud.api.commands.DeleteDiskOfferingCmd;
import com.cloud.api.commands.DeletePodCmd;
import com.cloud.api.commands.DeleteServiceOfferingCmd;
import com.cloud.api.commands.DeleteVlanIpRangeCmd;
import com.cloud.api.commands.DeleteZoneCmd;
import com.cloud.api.commands.UpdateCfgCmd;
import com.cloud.api.commands.UpdateDiskOfferingCmd;
import com.cloud.api.commands.UpdatePodCmd;
import com.cloud.api.commands.UpdateServiceOfferingCmd;
import com.cloud.api.commands.UpdateZoneCmd;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.dc.Vlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;

public interface ConfigurationService {
    
    /**
     * Updates a configuration entry with a new value
     * @param cmd - the command wrapping name and value parameters
     * @return updated configuration object if successful
     * @throws InvalidParameterValueException
     */
    Configuration updateConfiguration(UpdateCfgCmd cmd) throws InvalidParameterValueException;
    
    /**
     * Persists a config value via the API call
     * @return newly created Config object
     */
    Configuration addConfig(CreateCfgCmd cmd);
    
    /**
     * Create a service offering through the API
     * @param cmd the command object that specifies the name, number of cpu cores, amount of RAM, etc. for the service offering
     * @return the newly created service offering if successful, null otherwise
     */
    ServiceOffering createServiceOffering(CreateServiceOfferingCmd cmd);
    
    /**
     * Updates a service offering
     * @param serviceOfferingId
     * @param userId
     * @param name
     * @param displayText
     * @param offerHA
     * @param useVirtualNetwork
     * @param tags
     * @return updated service offering
     */
    ServiceOffering updateServiceOffering(UpdateServiceOfferingCmd cmd);
    
    /**
     * Deletes a service offering
     * @param userId
     * @param serviceOfferingId
     */
    boolean deleteServiceOffering(DeleteServiceOfferingCmd cmd);
    
    /**
     * Updates a disk offering
     * @param cmd - the command specifying diskOfferingId, name, description, tags
     * @return updated disk offering
     * @throws 
     */
    DiskOffering updateDiskOffering(UpdateDiskOfferingCmd cmd);
    
    /**
     * Deletes a disk offering
     * @param cmd - the command specifying disk offering id
     * @return true or false
     * @throws 
     */
    boolean deleteDiskOffering(DeleteDiskOfferingCmd cmd);
    
    /**
     * Creates a new disk offering
     * @param domainId
     * @param name
     * @param description
     * @param numGibibytes
     * @param mirrored
     * @param size
     * @return ID
     */
    DiskOffering createDiskOffering(CreateDiskOfferingCmd cmd);
    
    /**
     * Creates a new pod based on the parameters specified in the command object
     * @param cmd the command object that specifies the name, zone, gateway, cidr, and ip range for the pod
     * @return the new pod if successful, null otherwise
     * @throws 
     * @throws 
     */
    Pod createPod(CreatePodCmd cmd);

    /**
     * Edits a pod in the database. Will not allow you to edit pods that are being used anywhere in the system.
     * @param userId
     * @param podId
     * @param newPodName
     * @param cidr
     * @param startIp
     * @param endIp
     * @return Pod
     * @throws  
     * @throws  
     */
    Pod editPod(UpdatePodCmd cmd);
    
    /**
     * Deletes a pod from the database. Will not allow you to delete pods that are being used anywhere in the system.
     * @param cmd - the command containing podId
     * @return true or false
     * @throws , 
     */
    boolean deletePod(DeletePodCmd cmd);
    
    /**
     * Creates a new zone
     * @param cmd
     * @return the zone if successful, null otherwise
     * @throws 
     * @throws 
     */
    DataCenter createZone(CreateZoneCmd cmd);
    
    /**
     * Edits a zone in the database. Will not allow you to edit DNS values if there are VMs in the specified zone.
     * @param UpdateZoneCmd
     * @return Updated zone
     */
    DataCenter editZone(UpdateZoneCmd cmd);

    /**
     * Deletes a zone from the database. Will not allow you to delete zones that are being used anywhere in the system.
     * @param userId
     * @param zoneId
     */
    boolean deleteZone(DeleteZoneCmd cmd);
    
    
    /**
     * Adds a VLAN to the database, along with an IP address range. Can add three types of VLANs: (1) zone-wide VLANs on the virtual public network (2) pod-wide direct attached VLANs (3) account-specific direct attached VLANs
     * @param userId
     * @param vlanType - either "DomR" (VLAN for a virtual public network) or "DirectAttached" (VLAN for IPs that will be directly attached to UserVMs)
     * @param zoneId
     * @param accountId
     * @param podId
     * @param add
     * @param vlanId
     * @param gateway
     * @param startIP
     * @param endIP
     * @throws 
     * @return The new Vlan object
     */
    Vlan createVlanAndPublicIpRange(CreateVlanIpRangeCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException;

    boolean deleteVlanIpRange(DeleteVlanIpRangeCmd cmd);
}
