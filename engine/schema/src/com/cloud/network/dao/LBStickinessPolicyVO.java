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
package com.cloud.network.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import com.cloud.network.rules.StickinessPolicy;
import com.cloud.utils.Pair;

@Entity
@Table(name = ("load_balancer_stickiness_policies"))
@PrimaryKeyJoinColumn(name = "load_balancer_id", referencedColumnName = "id")
public class LBStickinessPolicyVO implements StickinessPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "load_balancer_id")
    private long loadBalancerId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "method_name")
    private String methodName;

    @Column(name = "params")
    private String paramsInDB;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "revoke")
    private boolean revoke = false;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    protected LBStickinessPolicyVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    /*  get the params in Map format and converts in to string format and stores in DB
     *  paramsInDB represent the string stored in database :
     *  Format :  param1=value1&param2=value2&param3=value3&
     *  Example for App cookie method:  "name=cookapp&length=12&holdtime=3h" . Here 3 parameters name,length and holdtime with corresponding values.
     *  getParams function is used to get in List<Pair<string,String>> Format.
     *           - API response use Map format
     *           - In database plain String with DB_PARM_DELIMITER
     *           - rest of the code uses List<Pair<string,String>>
     */
    public LBStickinessPolicyVO(long loadBalancerId, String name, String methodName, Map paramList, String description) {
        this.loadBalancerId = loadBalancerId;
        this.name = name;
        this.methodName = methodName;
        StringBuilder sb = new StringBuilder("");

        if (paramList != null) {
            Iterator<HashMap<String, String>> iter = paramList.values().iterator();
            while (iter.hasNext()) {
                HashMap<String, String> paramKVpair = iter.next();
                String paramName = paramKVpair.get("name");
                String paramValue = paramKVpair.get("value");
                sb.append(paramName + "=" + paramValue + "&");
            }
        }
        paramsInDB = sb.toString();
        this.description = description;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public List<Pair<String, String>> getParams() {
        List<Pair<String, String>> paramsList = new ArrayList<Pair<String, String>>();
        String[] params = paramsInDB.split("[=&]");

        for (int i = 0; i < (params.length - 1); i = i + 2) {
            paramsList.add(new Pair<String, String>(params[i], params[i + 1]));
        }
        return paramsList;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getLoadBalancerId() {
        return loadBalancerId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public boolean isRevoke() {
        return revoke;
    }

    public void setRevoke(boolean revoke) {
        this.revoke = revoke;
    }

    @Override
    public String getUuid() {
        return this.uuid;
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
}
