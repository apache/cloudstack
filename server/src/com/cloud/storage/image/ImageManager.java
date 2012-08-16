package com.cloud.storage.image;

import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.Pair;

public interface ImageManager {

	VMTemplateVO getImageById(Long imageId);

	Pair<String, String> getAbsoluteIsoPath(long templateId, long dataCenterId);

}
