package com.cloud.cluster;

import com.cloud.utils.component.Manager;

public interface ClusterFenceManager extends Manager {
	public static final int SELF_FENCING_EXIT_CODE = 219;
}
