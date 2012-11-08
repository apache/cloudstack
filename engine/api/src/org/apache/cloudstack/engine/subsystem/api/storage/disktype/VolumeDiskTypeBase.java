package org.apache.cloudstack.engine.subsystem.api.storage.disktype;

public class VolumeDiskTypeBase implements VolumeDiskType {
	protected String type = "Unknown";

	@Override
	public boolean equals(Object that) {
		if (this == that) {
			return true;
		}
		if (that instanceof String) {
			if (getType().equalsIgnoreCase((String)that)) {
				return true;
			}
		} else if (that instanceof VolumeDiskTypeBase) {
			VolumeDiskTypeBase th = (VolumeDiskTypeBase)that;
			if (this.getType().equalsIgnoreCase(th.getType())) {
				return true;
			}
		} else {
			return false;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getType();
	}
	
	protected String getType() {
		return this.type;
	}
}
