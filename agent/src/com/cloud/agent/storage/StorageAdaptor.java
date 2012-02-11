package com.cloud.agent.storage;

import java.util.List;

import org.libvirt.StoragePool;

import com.cloud.agent.storage.KVMPhysicalDisk.PhysicalDiskFormat;
import com.cloud.storage.Storage.StoragePoolType;

public interface StorageAdaptor {

	public KVMStoragePool getStoragePool(String uuid);

	public KVMPhysicalDisk getPhysicalDisk(String volumeUuid,
			KVMStoragePool pool);

	public KVMStoragePool createStoragePool(String name, String host,
			String path, StoragePoolType type);

	public boolean deleteStoragePool(String uuid);

	public KVMPhysicalDisk createPhysicalDisk(String name, KVMStoragePool pool,
			PhysicalDiskFormat format, long size);

	public boolean deletePhysicalDisk(String uuid, KVMStoragePool pool);

	public KVMPhysicalDisk createDiskFromTemplate(KVMPhysicalDisk template,
			String name, PhysicalDiskFormat format, long size,
			KVMStoragePool destPool);

	public KVMPhysicalDisk createTemplateFromDisk(KVMPhysicalDisk disk,
			String name, PhysicalDiskFormat format, long size,
			KVMStoragePool destPool);

	public List<KVMPhysicalDisk> listPhysicalDisks(String storagePoolUuid,
			KVMStoragePool pool);

	public KVMPhysicalDisk copyPhysicalDisk(KVMPhysicalDisk disk, String name,
			KVMStoragePool destPools);

	public KVMPhysicalDisk createDiskFromSnapshot(KVMPhysicalDisk snapshot,
			String snapshotName, String name, KVMStoragePool destPool);

	public KVMStoragePool getStoragePoolByUri(String uri);

	public KVMPhysicalDisk getPhysicalDiskFromURI(String uri);

	public boolean refresh(KVMStoragePool pool);

	public boolean deleteStoragePool(KVMStoragePool pool);

	public boolean createFolder(String uuid, String path);

}
