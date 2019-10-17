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

import java.util.EnumSet;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.GenericDao;

public interface UserVmJoinDao extends GenericDao<UserVmJoinVO, Long> {

    UserVmResponse newUserVmResponse(ResponseView view, String objectName, UserVmJoinVO userVm, EnumSet<VMDetails> details, Account caller);

    UserVmResponse setUserVmResponse(ResponseView view, UserVmResponse userVmData, UserVmJoinVO uvo);

    List<UserVmJoinVO> newUserVmView(UserVm... userVms);

    List<UserVmJoinVO> searchByIds(Long... ids);

    List<UserVmJoinVO> listActiveByIsoId(Long isoId);
}
