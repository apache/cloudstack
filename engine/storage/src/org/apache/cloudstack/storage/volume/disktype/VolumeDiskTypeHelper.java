package org.apache.cloudstack.storage.volume.disktype;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class VolumeDiskTypeHelper {
	
	static private List<VolumeDiskType> diskTypes;
	static final private VolumeDiskType defaultType = new Unknown();
	
	@Inject
	public void setDiskTypes(List<VolumeDiskType> diskTypes) {
		VolumeDiskTypeHelper.diskTypes = diskTypes;
	}
	
	public static VolumeDiskType getDiskType(String type) {
		for (VolumeDiskType diskType : diskTypes) {
			if (diskType.equals(type)) {
				return diskType;
			}
		}
		
		return VolumeDiskTypeHelper.defaultType;
	}
}
