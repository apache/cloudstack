package com.cloud.storage;

import com.cloud.user.Account;

public class CreateSnapshotPayload {
	private Long snapshotPolicyId;
	private Long snapshotId;
	private Account account;

	public Long getSnapshotPolicyId() {
		return snapshotPolicyId;
	}

	public void setSnapshotPolicyId(Long snapshotPolicyId) {
		this.snapshotPolicyId = snapshotPolicyId;
	}

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

}
