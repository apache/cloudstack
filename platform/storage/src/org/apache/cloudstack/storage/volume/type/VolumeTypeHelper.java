package org.apache.cloudstack.storage.volume.type;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class VolumeTypeHelper {
	@Inject
	private List<VolumeType> types;
	private VolumeType defaultType = new Unknown();
	
	public VolumeType getType(String type) {
		for (VolumeType ty : types) {
			if (ty.equals(type)) {
				return ty;
			}
		}
		return defaultType;
	}
	
}
