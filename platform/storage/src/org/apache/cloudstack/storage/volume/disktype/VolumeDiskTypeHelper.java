package org.apache.cloudstack.storage.volume.disktype;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class VolumeDiskTypeHelper {
	@Inject
	protected List<VolumeDiskType> diskTypes;
	protected VolumeDiskType defaultType = new Unknown();
	
	public VolumeDiskType getDiskType(String type) {
		for (VolumeDiskType diskType : diskTypes) {
			if (diskType.equals(type)) {
				return diskType;
			}
		}
		
		return defaultType;
	}
}
