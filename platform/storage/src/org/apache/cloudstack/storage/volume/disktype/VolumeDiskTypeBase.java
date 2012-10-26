package org.apache.cloudstack.storage.volume.disktype;

public class VolumeDiskTypeBase implements VolumeDiskType {
	protected String type = "Unknown";

	@Override
	public boolean equals(String diskType) {
		if (getType().equalsIgnoreCase(diskType)) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return getType();
	}
	
	protected String getType() {
		return this.type;
	}
}
