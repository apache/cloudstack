package org.apache.cloudstack.storage.snapshot;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.cloud.entity.api.SnapshotEntity;

import com.cloud.hypervisor.Hypervisor.HypervisorType;

public class SnapshotEntityImpl implements SnapshotEntity {

	@Override
	public String getUuid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCurrentState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDesiredState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getCreatedTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getLastUpdatedTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getOwner() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Method> getApplicableActions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getAccountId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getVolumeId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getCreated() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Status getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HypervisorType getHypervisorType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRecursive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public short getsnapshotType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDomainId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String reserveForBackup(int expiration) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void backup(String reservationToken) {
		// TODO Auto-generated method stub

	}

	@Override
	public void restore(String vm) {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, String> getDetails() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addDetail(String name, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delDetail(String name, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateDetail(String name, String value) {
		// TODO Auto-generated method stub
		
	}

}
