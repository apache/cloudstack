package com.cloud.agent.storage;

import java.util.List;

import org.libvirt.StoragePool;

import com.cloud.agent.storage.KVMPhysicalDisk.PhysicalDiskFormat;
import com.cloud.storage.Storage.StoragePoolType;

public class LibvirtStoragePool implements KVMStoragePool {
	protected String uuid;
	protected String uri;
	protected long capacity;
	protected long used;
	protected String name;
	protected String localPath;
	protected PhysicalDiskFormat defaultFormat;
	protected StoragePoolType type;
	protected StorageAdaptor _storageAdaptor;
	protected StoragePool _pool;
	
	public LibvirtStoragePool(String uuid, String name, StoragePoolType type, StorageAdaptor adaptor, StoragePool pool) {
		this.uuid = uuid;
		this.name = name;
		this.type = type;
		this._storageAdaptor = adaptor;
		this.capacity = 0;
		this.used = 0;
		this._pool = pool;
		
	}
	
	public void setCapacity(long capacity) {
		this.capacity = capacity;
	}
	
	@Override
	public long getCapacity() {
		return this.capacity;
	}
	
	public void setUsed(long used) {
		this.used = used;
	}
	
	@Override
	public long getUsed() {
		return this.used;
	}
	
	public StoragePoolType getStoragePoolType() {
		return this.type;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getUuid() {
		return this.uuid;
	}
	
	public String uri() {
		return this.uri;
	}
	
	@Override
	public PhysicalDiskFormat getDefaultFormat() {
		if (getStoragePoolType() == StoragePoolType.CLVM) {
			return PhysicalDiskFormat.RAW;
		} else {
			return PhysicalDiskFormat.QCOW2;
		}
	}
	
	@Override
	public KVMPhysicalDisk createPhysicalDisk(String name, PhysicalDiskFormat format, long size) {
		return this._storageAdaptor.createPhysicalDisk(name, this, format, size);
	}
	
	@Override
	public KVMPhysicalDisk createPhysicalDisk(String name, long size) {
		return this._storageAdaptor.createPhysicalDisk(name, this, this.getDefaultFormat(), size);
	}
	
	@Override
	public KVMPhysicalDisk getPhysicalDisk(String volumeUuid) {
		return this._storageAdaptor.getPhysicalDisk(volumeUuid, this);
	}
	
	@Override
	public boolean deletePhysicalDisk(String uuid) {
		return this._storageAdaptor.deletePhysicalDisk(uuid, this);
	}
	
	@Override
	public List<KVMPhysicalDisk> listPhysicalDisks() {
		return this._storageAdaptor.listPhysicalDisks(this.uuid, this);
	}

	@Override
	public boolean refresh() {
		return this._storageAdaptor.refresh(this);
	}

	@Override
	public boolean isExternalSnapshot() {
		if (this.type == StoragePoolType.Filesystem) {
			return false;
		}
	
		return true;
	}

	@Override
	public String getLocalPath() {
		return this.localPath;
	}
	
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	@Override
	public StoragePoolType getType() {
		return this.type;
	}
	
	public StoragePool getPool() {
		return this._pool;
	}

	@Override
	public boolean delete() {
		return this._storageAdaptor.deleteStoragePool(this);
	}
}
