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
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine;

public interface UserVmJoinDao extends GenericDao<UserVmJoinVO, Long> {

    UserVmResponse newUserVmResponse(ResponseView view, String objectName, UserVmJoinVO userVm, Set<VMDetails> details, Boolean accumulateStats, Boolean showUserData,
            Account caller);

    UserVmResponse setUserVmResponse(ResponseView view, UserVmResponse userVmData, UserVmJoinVO uvo);

    List<UserVmJoinVO> newUserVmView(UserVm... userVms);

    List<UserVmJoinVO> newUserVmView(VirtualMachine... vms);

    List<UserVmJoinVO> searchByIds(Long... ids);

    List<UserVmJoinVO> listActiveByIsoId(Long isoId);

    List<UserVmJoinVO> listByAccountServiceOfferingTemplateAndNotInState(long accountId, List<VirtualMachine.State> states,
            List<Long> offeringIds, List<Long> templateIds);
}
