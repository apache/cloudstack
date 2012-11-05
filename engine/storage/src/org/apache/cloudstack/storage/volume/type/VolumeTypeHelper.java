package org.apache.cloudstack.storage.volume.type;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class VolumeTypeHelper {
	static private List<VolumeType> types;
	private static VolumeType defaultType = new Unknown();
	
	@Inject
	public void setTypes(List<VolumeType> types) {
		VolumeTypeHelper.types = types;
	}
	
	public static VolumeType getType(String type) {
		for (VolumeType ty : types) {
			if (ty.equals(type)) {
				return ty;
			}
		}
		return VolumeTypeHelper.defaultType;
	}
	
}
