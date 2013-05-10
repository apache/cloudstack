package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

public class SnapshotObjectTO implements DataTO {
	private String path;
	private VolumeObjectTO volume;
	private String parentSnapshotPath;
	private DataStoreTO dataStore;
	private String vmName;
	private String name;
	private HypervisorType hypervisorType;
	private long id;
	
	public SnapshotObjectTO() {
	    
	}
	
	public SnapshotObjectTO(SnapshotInfo snapshot) {
	    this.path = snapshot.getPath();
	    this.setId(snapshot.getId());
	    this.volume = (VolumeObjectTO)snapshot.getBaseVolume().getTO();
	    this.setVmName(snapshot.getBaseVolume().getAttachedVmName());
	    if (snapshot.getParent() != null) {
	        this.parentSnapshotPath = snapshot.getParent().getPath();
	    }
	    this.dataStore = snapshot.getDataStore().getTO();
	    this.setName(snapshot.getName());
	    this.hypervisorType = snapshot.getHypervisorType();
	}
	
	@Override
	public DataObjectType getObjectType() {
		return DataObjectType.SNAPSHOT;
	}

	@Override
	public DataStoreTO getDataStore() {
		return this.dataStore;
	}

	@Override
	public String getPath() {
		return this.path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}

    public VolumeObjectTO getVolume() {
        return volume;
    }

    public void setVolume(VolumeObjectTO volume) {
        this.volume = volume;
    }

    public String getParentSnapshotPath() {
        return parentSnapshotPath;
    }

    public void setParentSnapshotPath(String parentSnapshotPath) {
        this.parentSnapshotPath = parentSnapshotPath;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
}
