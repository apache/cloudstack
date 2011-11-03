package com.cloud.agent.storage;

import java.util.List;

import com.cloud.agent.storage.KVMPhysicalDisk.PhysicalDiskFormat;
import com.cloud.storage.Storage.StoragePoolType;

public interface KVMStoragePool {
	public KVMPhysicalDisk createPhysicalDisk(String name, PhysicalDiskFormat format, long size);
	public KVMPhysicalDisk createPhysicalDisk(String name, long size);
	public KVMPhysicalDisk getPhysicalDisk(String volumeUuid);
	public boolean deletePhysicalDisk(String uuid);
	public List<KVMPhysicalDisk> listPhysicalDisks();
	public String getUuid();
	public long getCapacity();
	public long getUsed();
	public boolean refresh();
	public boolean isExternalSnapshot();
	public String getLocalPath();
	public StoragePoolType getType();
	public boolean delete();
	PhysicalDiskFormat getDefaultFormat();
}
