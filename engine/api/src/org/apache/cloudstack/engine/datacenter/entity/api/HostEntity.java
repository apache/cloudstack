package org.apache.cloudstack.engine.datacenter.entity.api;

import com.cloud.hypervisor.Hypervisor.HypervisorType;

public interface HostEntity extends DataCenterResourceEntity {
	
    /**
     * @return total amount of memory.
     */
    Long getTotalMemory();

    /**
     * @return # of cores in a machine.  Note two cpus with two cores each returns 4.
     */
    Integer getCpus();

    /**
     * @return speed of each cpu in mhz.
     */
    Long getSpeed();

    /**
     * @return the pod.
     */
    Long getPodId();

    /**
     * @return availability zone.
     */
    long getDataCenterId();

    /**
     * @return type of hypervisor
     */
    HypervisorType getHypervisorType();
    
    /**
     * @return the mac address of the host.
     */
    String getGuid();

    Long getClusterId();

}

