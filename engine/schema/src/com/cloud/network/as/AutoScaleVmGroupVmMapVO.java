package com.cloud.network.as;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = ("autoscale_vmgroup_vm_map"))
public class AutoScaleVmGroupVmMapVO implements InternalIdentity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private long id;

	@Column(name = "vmgroup_id")
	private long vmGroupId;

	@Column(name = "instance_id")
	private long instanceId;

	public AutoScaleVmGroupVmMapVO() {
	}

	public AutoScaleVmGroupVmMapVO(long vmGroupId, long instanceId) {
		this.vmGroupId = vmGroupId;
		this.instanceId = instanceId;
	}

	public long getId() {
		return id;
	}

	public long getVmGroupId() {
		return vmGroupId;
	}

	public long getInstanceId() {
		return instanceId;
	}

}
