package com.cloud.network;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface OvsProvider extends InternalIdentity, Identity {
	public boolean isEnabled();

	public long getNspId();
}
