package com.cloud.storage.image;

import java.util.List;

import com.cloud.host.HostVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;

public class ImageManagerImpl implements ImageManager {
	@Inject
	VMTemplateDao _vmTemplDao;
	@Override
	public VMTemplateVO getImageById(Long imageId) {
		return _vmTemplDao.findById(imageId);
	}
	
	@Override
    public Pair<String, String> getAbsoluteIsoPath(long templateId, long dataCenterId) {
        String isoPath = null;

        List<HostVO> storageHosts = _resourceMgr.listAllHostsInOneZoneByType(Host.Type.SecondaryStorage, dataCenterId);
        if (storageHosts != null) {
            for (HostVO storageHost : storageHosts) {
                VMTemplateHostVO templateHostVO = _vmTemplateHostDao.findByHostTemplate(storageHost.getId(), templateId);
                if (templateHostVO != null) {
                    isoPath = storageHost.getStorageUrl() + "/" + templateHostVO.getInstallPath();
                    return new Pair<String, String>(isoPath, storageHost.getStorageUrl());
                }
            }
        }
        s_logger.warn("Unable to find secondary storage in zone id=" + dataCenterId);
        return null;
    }
}
