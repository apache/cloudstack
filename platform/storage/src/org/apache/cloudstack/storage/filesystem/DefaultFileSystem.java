package org.apache.cloudstack.storage.filesystem;

import org.apache.cloudstack.platform.subsystem.api.storage.DataObject;
import org.apache.cloudstack.platform.subsystem.api.storage.DataStore;
import org.apache.cloudstack.platform.subsystem.api.storage.FileSystem;

public class DefaultFileSystem implements FileSystem {

	public DataObject create(DataObject obj) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataObject copy(DataObject Obj, DataStore destStore) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataObject copy(DataObject obj, DataObject destObj) {
		// TODO Auto-generated method stub
		return null;
	}

	public DataObject move(DataObject srcObj, DataObject destObj) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean delete(DataObject obj) {
		// TODO Auto-generated method stub
		return false;
	}

	public long getStats(DataObject obj) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getFileType() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isWritable(DataObject obj) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean contains(DataObject obj) {
		// TODO Auto-generated method stub
		return false;
	}

	public DataObject ioctl(DataObject obj, Object... objects) {
		// TODO Auto-generated method stub
		return null;
	}

}
