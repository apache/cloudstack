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
package org.apache.cloudstack.api.view;

import java.util.EnumSet;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.InstanceGroupResponse;
import org.apache.cloudstack.api.response.ProjectAccountResponse;
import org.apache.cloudstack.api.response.ProjectInvitationResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.view.dao.DomainRouterJoinDao;
import org.apache.cloudstack.api.view.dao.EventJoinDao;
import org.apache.cloudstack.api.view.dao.InstanceGroupJoinDao;
import org.apache.cloudstack.api.view.dao.ProjectAccountJoinDao;
import org.apache.cloudstack.api.view.dao.ProjectInvitationJoinDao;
import org.apache.cloudstack.api.view.dao.ProjectJoinDao;
import org.apache.cloudstack.api.view.dao.ResourceTagJoinDao;
import org.apache.cloudstack.api.view.dao.SecurityGroupJoinDao;
import org.apache.cloudstack.api.view.dao.UserAccountJoinDao;
import org.apache.cloudstack.api.view.dao.UserVmJoinDao;
import org.apache.cloudstack.api.view.vo.DomainRouterJoinVO;
import org.apache.cloudstack.api.view.vo.EventJoinVO;
import org.apache.cloudstack.api.view.vo.InstanceGroupJoinVO;
import org.apache.cloudstack.api.view.vo.ProjectAccountJoinVO;
import org.apache.cloudstack.api.view.vo.ProjectInvitationJoinVO;
import org.apache.cloudstack.api.view.vo.ProjectJoinVO;
import org.apache.cloudstack.api.view.vo.ResourceTagJoinVO;
import org.apache.cloudstack.api.view.vo.SecurityGroupJoinVO;
import org.apache.cloudstack.api.view.vo.UserAccountJoinVO;
import org.apache.cloudstack.api.view.vo.UserVmJoinVO;


import com.cloud.configuration.ConfigurationService;
import com.cloud.event.Event;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.security.SecurityGroup;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectAccount;
import com.cloud.projects.ProjectInvitation;
import com.cloud.projects.ProjectService;
import com.cloud.server.ResourceTag;
import com.cloud.server.TaggedResourceService;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserAccount;

import com.cloud.uservm.UserVm;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.vm.InstanceGroup;


/**
 * @author minc
 *
 */
public class DBViewUtils {

    private static DomainRouterJoinDao _domainRouterJoinDao;
    private static SecurityGroupJoinDao _securityGroupJoinDao;
    private static UserVmJoinDao _userVmJoinDao;
    private static ResourceTagJoinDao _tagJoinDao;
    private static EventJoinDao _eventJoinDao;
    private static InstanceGroupJoinDao _vmGroupJoinDao;
    private static UserAccountJoinDao _userAccountJoinDao;
    private static ProjectJoinDao _projectJoinDao;
    private static ProjectAccountJoinDao _projectAccountJoinDao;
    private static ProjectInvitationJoinDao _projectInvitationJoinDao;

    static {
        ComponentLocator locator = ComponentLocator.getLocator("management-server");
        _domainRouterJoinDao = locator.getDao(DomainRouterJoinDao.class);
        _userVmJoinDao = locator.getDao(UserVmJoinDao.class);
        _securityGroupJoinDao = locator.getDao(SecurityGroupJoinDao.class);
        _tagJoinDao = locator.getDao(ResourceTagJoinDao.class);
        _vmGroupJoinDao = locator.getDao(InstanceGroupJoinDao.class);
        _eventJoinDao = locator.getDao(EventJoinDao.class);
        _userAccountJoinDao = locator.getDao(UserAccountJoinDao.class);
        _projectJoinDao = locator.getDao(ProjectJoinDao.class);
        _projectAccountJoinDao = locator.getDao(ProjectAccountJoinDao.class);
        _projectInvitationJoinDao = locator.getDao(ProjectInvitationJoinDao.class);
    }


    public static DomainRouterResponse newDomainRouterResponse(DomainRouterJoinVO vr, Account caller) {
        return _domainRouterJoinDao.newDomainRouterResponse(vr, caller);
    }

    public static DomainRouterResponse fillRouterDetails(DomainRouterResponse vrData, DomainRouterJoinVO vr){
         return _domainRouterJoinDao.setDomainRouterResponse(vrData, vr);
    }

    public static List<DomainRouterJoinVO> newDomainRouterView(VirtualRouter vr){
        return _domainRouterJoinDao.newDomainRouterView(vr);
    }

    public static UserVmResponse newUserVmResponse(String objectName, UserVmJoinVO userVm, EnumSet<VMDetails> details, Account caller) {
        return _userVmJoinDao.newUserVmResponse(objectName, userVm, details, caller);
    }

    public static UserVmResponse fillVmDetails(UserVmResponse vmData, UserVmJoinVO vm){
         return _userVmJoinDao.setUserVmResponse(vmData, vm);
    }

    public static List<UserVmJoinVO> newUserVmView(UserVm... userVms){
        return _userVmJoinDao.newUserVmView(userVms);
    }

    public static SecurityGroupResponse newSecurityGroupResponse(SecurityGroupJoinVO vsg, Account caller) {
        return _securityGroupJoinDao.newSecurityGroupResponse(vsg, caller);
    }

    public static SecurityGroupResponse fillSecurityGroupDetails(SecurityGroupResponse vsgData, SecurityGroupJoinVO sg){
         return _securityGroupJoinDao.setSecurityGroupResponse(vsgData, sg);
    }

    public static List<SecurityGroupJoinVO> newSecurityGroupView(SecurityGroup sg){
        return _securityGroupJoinDao.newSecurityGroupView(sg);
    }

    public static List<SecurityGroupJoinVO> findSecurityGroupViewById(Long sgId){
        return _securityGroupJoinDao.searchByIds(sgId);
    }

    public static ResourceTagResponse newResourceTagResponse(ResourceTagJoinVO vsg, boolean keyValueOnly) {
        return _tagJoinDao.newResourceTagResponse(vsg, keyValueOnly);
    }

    public static ResourceTagJoinVO newResourceTagView(ResourceTag sg){
        return _tagJoinDao.newResourceTagView(sg);
    }

    public static ResourceTagJoinVO findResourceTagViewById(Long tagId){
        List<ResourceTagJoinVO> tags = _tagJoinDao.searchByIds(tagId);
        if ( tags != null && tags.size() > 0 ){
            return tags.get(0);
        }
        else{
            return null;
        }
    }

    public static EventResponse newEventResponse(EventJoinVO ve) {
        return _eventJoinDao.newEventResponse(ve);
    }

    public static EventJoinVO newEventView(Event e){
        return _eventJoinDao.newEventView(e);
    }

    public static InstanceGroupResponse newInstanceGroupResponse(InstanceGroupJoinVO ve) {
        return _vmGroupJoinDao.newInstanceGroupResponse(ve);
    }

    public static InstanceGroupJoinVO newInstanceGroupView(InstanceGroup e){
        return _vmGroupJoinDao.newInstanceGroupView(e);
    }

    public static UserResponse newUserResponse(UserAccountJoinVO usr) {
        return _userAccountJoinDao.newUserResponse(usr);
    }

    public static UserAccountJoinVO newUserView(User usr){
        return _userAccountJoinDao.newUserView(usr);
    }

    public static UserAccountJoinVO newUserView(UserAccount usr){
        return _userAccountJoinDao.newUserView(usr);
    }

    public static ProjectResponse newProjectResponse(ProjectJoinVO proj) {
        return _projectJoinDao.newProjectResponse(proj);
    }

    public static ProjectResponse fillProjectDetails(ProjectResponse rsp, ProjectJoinVO proj){
         return _projectJoinDao.setProjectResponse(rsp,proj);
    }

    public static List<ProjectJoinVO> newProjectView(Project proj){
        return _projectJoinDao.newProjectView(proj);
    }

    public static List<UserAccountJoinVO> findUserViewByAccountId(Long accountId){
        return _userAccountJoinDao.searchByAccountId(accountId);
    }

    public static ProjectAccountResponse newProjectAccountResponse(ProjectAccountJoinVO proj) {
        return _projectAccountJoinDao.newProjectAccountResponse(proj);
    }

    public static ProjectAccountJoinVO newProjectAccountView(ProjectAccount proj) {
        return _projectAccountJoinDao.newProjectAccountView(proj);
    }

    public static ProjectInvitationResponse newProjectInvitationResponse(ProjectInvitationJoinVO proj) {
        return _projectInvitationJoinDao.newProjectInvitationResponse(proj);
    }

    public static ProjectInvitationJoinVO newProjectInvitationView(ProjectInvitation proj) {
        return _projectInvitationJoinDao.newProjectInvitationView(proj);
    }

}
