package com.cloud.storage;

public class ResizeVolumePayload {
	public final Long newSize;
	public final boolean shrinkOk;
	public final String instanceName;
	public final long[] hosts;
	public ResizeVolumePayload(Long newSize, boolean shrinkOk, String instanceName, long[] hosts) {
		this.newSize = newSize;
		this.shrinkOk = shrinkOk;
		this.instanceName = instanceName;
		this.hosts = hosts;
	}
}
