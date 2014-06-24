package org.apache.cloudstack.resourcedetail.dao;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDao;
import org.apache.cloudstack.resourcedetail.SnapshotPolicyDetailVO;

public interface SnapshotPolicyDetailsDao extends GenericDao<SnapshotPolicyDetailVO, Long>, ResourceDetailsDao<SnapshotPolicyDetailVO> {
}
