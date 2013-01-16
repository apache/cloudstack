package org.apache.cloudstack.engine.cloud.entity.api.db;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.cloudstack.api.InternalIdentity;


@Entity
@Table(name = "vm_network_map")
public class VMNetworkMapVO implements InternalIdentity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name="network_id")
    private long networkId;

   
    /**
     * There should never be a public constructor for this class. Since it's
     * only here to define the table for the DAO class.
     */
    protected VMNetworkMapVO() {
    }

    public VMNetworkMapVO(long vmId, long networkId) {
        this.vmId = vmId;
        this.networkId = networkId;
    }
    
    
    public long getId() {
        return id;
    }

    public long getVmId() {
        return vmId;
    }

    
    public long getNetworkId() {
        return networkId;
    }
  
}
