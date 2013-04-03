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
package com.cloud.api.query.dao;

import java.util.List;

import org.apache.cloudstack.api.response.ObjectStoreResponse;
import com.cloud.api.query.vo.ImageDataStoreJoinVO;
import com.cloud.storage.ObjectStore;
import com.cloud.utils.db.GenericDao;

public interface ImageDataStoreJoinDao extends GenericDao<ImageDataStoreJoinVO, Long> {

    ObjectStoreResponse newObjectStoreResponse(ImageDataStoreJoinVO os);

    ObjectStoreResponse setObjectStoreResponse(ObjectStoreResponse response, ImageDataStoreJoinVO os);

    List<ImageDataStoreJoinVO> newObjectStoreView(ObjectStore os);

    List<ImageDataStoreJoinVO> searchByIds(Long... spIds);

}
