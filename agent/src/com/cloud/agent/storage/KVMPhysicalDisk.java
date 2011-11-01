package com.cloud.agent.storage;

public class KVMPhysicalDisk {
	private String path;
	private String name;
	private KVMStoragePool pool;
	public static enum PhysicalDiskFormat {
		RAW,
		QCOW2
	}
	private PhysicalDiskFormat format;
	private long size;
	private long virtualSize;
	
	public KVMPhysicalDisk(String path, String name, KVMStoragePool pool) {
		this.path = path;
		this.name = name;
		this.pool = pool;
	}
	
	public void setFormat(PhysicalDiskFormat format) {
		this.format = format;
	}
	
	public PhysicalDiskFormat getFormat() {
		return this.format;
	}
	
	public void setSize(long size) {
		this.size = size;
	}
	
	public long getSize() {
		return this.size;
	}
	
	public void setVirtualSize(long size) {
		this.virtualSize = size;
	}
	
	public long getVirtualSize() {
		return this.virtualSize;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public KVMStoragePool getPool() {
		return this.pool;
	}
}
