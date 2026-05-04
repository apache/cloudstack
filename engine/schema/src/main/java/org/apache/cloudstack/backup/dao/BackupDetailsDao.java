// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.backup.dao;

import org.apache.cloudstack.backup.BackupDetailVO;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDao;

import com.cloud.utils.db.GenericDao;

public interface BackupDetailsDao extends GenericDao<BackupDetailVO, Long>, ResourceDetailsDao<BackupDetailVO> {

    String END_OF_CHAIN = "end_of_chain";

    String CURRENT = "current";

    String IMAGE_STORE_ID = "image_store_id";

    String PARENT_ID = "parent_id";

    String ISOLATED = "isolated";

    String SCREENSHOT_PATH = "screenshot_path";

    String BACKUP_HASH = "backup_hash";

    void removeDetailsExcept(long backupId, String exception);
}
