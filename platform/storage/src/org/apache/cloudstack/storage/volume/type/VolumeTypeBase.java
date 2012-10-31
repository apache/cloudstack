package org.apache.cloudstack.storage.volume.type;

public class VolumeTypeBase implements VolumeType {
	protected String type = "Unknown";
	
	@Override
	public boolean equals(Object that) {
		if (this == that) {
			return true;
		}
		if (that instanceof String) {
			if (this.toString().equalsIgnoreCase((String)that)) {
				return true;
			}
		} else if (that instanceof VolumeTypeBase) {
			VolumeTypeBase th = (VolumeTypeBase)that;
			if (this.toString().equalsIgnoreCase(th.toString())) {
				return true;
			}
		} else {
			return false;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return type;
	}
	
}
