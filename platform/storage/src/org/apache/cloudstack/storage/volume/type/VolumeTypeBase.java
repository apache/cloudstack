package org.apache.cloudstack.storage.volume.type;

public class VolumeTypeBase implements VolumeType {
	protected String type = "Unknown";
	
	@Override
	public boolean equals(String type) {
		if (this.type.equalsIgnoreCase(type)) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return type;
	}
	
}
