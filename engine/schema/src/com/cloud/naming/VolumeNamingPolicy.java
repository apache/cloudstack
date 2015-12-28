package com.cloud.naming;

import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;

public interface VolumeNamingPolicy extends ResourceNamingPolicy<Volume, VolumeVO>{

    String getDatadiskName();

    String getRootName(Long vmId);

    String getDatadiskName(long vmId);
}
