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
package com.cloud.network.as;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.net.NetUtils;

@Entity
@Table(name = "autoscale_vmprofiles")
@Inheritance(strategy = InheritanceType.JOINED)
public class AutoScaleVmProfileVO implements AutoScaleVmProfile, Identity, InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    protected long id;

    @Column(name = "uuid")
    protected String uuid;

    @Column(name = "zone_id", updatable = true, nullable = false)
    protected Long zoneId;

    @Column(name = "domain_id", updatable = true)
    private long domainId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "autoscale_user_id")
    private Long autoscaleUserId;

    @Column(name = "service_offering_id", updatable = true, nullable = false)
    private Long serviceOfferingId;

    @Column(name = "template_id", updatable = true, nullable = false, length = 17)
    private Long templateId;

    @Column(name = "other_deploy_params", updatable = true, length = 1024)
    private String otherDeployParams;

    @Column(name = "expunge_vm_grace_period", updatable = true)
    private Integer expungeVmGracePeriod = NetUtils.DEFAULT_AUTOSCALE_EXPUNGE_VM_GRACE_PERIOD;

    @Column(name = "counter_params", updatable = true)
    private String counterParams;

    @Column(name = "user_data", updatable = true, nullable = true, length = 1048576)
    @Basic(fetch = FetchType.LAZY)
    private String userData;

    @Column(name = "user_data_id", nullable = true)
    private Long userDataId = null;

    @Column(name = "user_data_details", updatable = true, length = 4096)
    private String userDataDetails;

    @Column(name = GenericDao.REMOVED_COLUMN)
    protected Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    protected Date created;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    public AutoScaleVmProfileVO() {
    }

    public AutoScaleVmProfileVO(long zoneId, long domainId, long accountId, long serviceOfferingId, long templateId,
                                Map<String, HashMap<String, String>> otherDeployParamsMap, Map counterParamList,
                                String userData, Integer expungeVmGracePeriod, Long autoscaleUserId) {
        uuid = UUID.randomUUID().toString();
        this.zoneId = zoneId;
        this.domainId = domainId;
        this.accountId = accountId;
        this.serviceOfferingId = serviceOfferingId;
        this.templateId = templateId;
        this.autoscaleUserId = autoscaleUserId;
        if (expungeVmGracePeriod != null) {
            this.expungeVmGracePeriod = expungeVmGracePeriod;
        }
        this.userData = userData;
        setCounterParamsForUpdate(counterParamList);
        setOtherDeployParamsForUpdate(otherDeployParamsMap);
    }

    @Override
    public String toString() {
        return new StringBuilder("AutoScaleVMProfileVO[").append("id").append(id).append("-").append("templateId").append("-").append(templateId).append("]").toString();
    }

    @Override
    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    @Override
    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    @Override
    public String getOtherDeployParams() {
        return otherDeployParams;
    }

    @Override
    public List<Pair<String, String>> getOtherDeployParamsList() {
        List<Pair<String, String>> paramsList = new ArrayList<>();
        if (otherDeployParams != null) {
            String[] params = otherDeployParams.split("[=&]");
            for (int i = 0; i < (params.length - 1); i = i + 2) {
                paramsList.add(new Pair<>(params[i], params[i + 1]));
            }
        }
        return paramsList;
    }

    public void setOtherDeployParams(String otherDeployParams) {
        this.otherDeployParams = otherDeployParams;
    }

    public void setOtherDeployParamsForUpdate(Map<String, HashMap<String, String>> otherDeployParamsMap) {
        if (MapUtils.isNotEmpty(otherDeployParamsMap)) {
            List<String> params = new ArrayList<>();
            for (HashMap<String, String> paramKVpair : otherDeployParamsMap.values()) {
                String paramName = paramKVpair.get("name");
                String paramValue = paramKVpair.get("value");
                params.add(paramName + "=" + paramValue);
            }
            setOtherDeployParams(StringUtils.join(params, "&"));
        }
    }

    @Override
    public List<Pair<String, String>> getCounterParams() {
        List<Pair<String, String>> paramsList = new ArrayList<>();
        if (counterParams != null) {
            String[] params = counterParams.split("[=&]");
            for (int i = 0; i < (params.length - 1); i = i + 2) {
                paramsList.add(new Pair<>(params[i], params[i + 1]));
            }
        }
        return paramsList;
    }

    public String getCounterParamsString() {
        return this.counterParams;
    }

    public void setCounterParams(String counterParam) {
        this.counterParams = counterParam;
    }

    public void setCounterParamsForUpdate(Map counterParamList) {
        StringBuilder sb = new StringBuilder("");
        boolean isFirstParam = true;
        if (counterParamList != null) {
            Iterator<HashMap<String, String>> iter = counterParamList.values().iterator();
            while (iter.hasNext()) {
                HashMap<String, String> paramKVpair = iter.next();
                if (!isFirstParam) {
                    sb.append("&");
                }
                String paramName = paramKVpair.get("name");
                String paramValue = paramKVpair.get("value");
                sb.append(paramName + "=" + paramValue);
                isFirstParam = false;
            }
        }
        /*
         * setCounterParams(String counterParam)'s String param is caught by UpdateBuilder and stored in an internal
         * list.
         * Which is used later to update the db. The variables in a VO object is not used to update the db.
         * Hence calling the function which is intercepted.
         */
        setCounterParams(sb.toString());
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    @Override
    public String getUserData() {
        return userData;
    }

    @Override
    public Long getUserDataId() {
        return userDataId;
    }

    public void setUserDataId(Long userDataId) {
        this.userDataId = userDataId;
    }

    @Override
    public String getUserDataDetails() {
        return userDataDetails;
    }

    public void setUserDataDetails(String userDataDetails) {
        this.userDataDetails = userDataDetails;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setAutoscaleUserId(Long autoscaleUserId) {
        this.autoscaleUserId = autoscaleUserId;
    }

    @Override
    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Integer getExpungeVmGracePeriod() {
        return expungeVmGracePeriod;
    }

    public void setExpungeVmGracePeriod(Integer expungeVmGracePeriod) {
        this.expungeVmGracePeriod = expungeVmGracePeriod;
    }

    @Override
    public Long getAutoScaleUserId() {
        return autoscaleUserId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }

    @Override
    public Class<?> getEntityType() {
        return AutoScaleVmProfile.class;
    }

    @Override
    public String getName() {
        return null;
    }

}
