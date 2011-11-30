/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.network;

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
            while (iter.hasNext())  {
                HashMap<String, String>  paramKVpair =  iter.next();
                String paramName = paramKVpair.get("name");
                String paramValue =  paramKVpair.get("value");
                sb.append(paramName + "=" + paramValue + "&");
            }
        } 
        paramsInDB = sb.toString();
        this.description = description;
        this.uuid = UUID.randomUUID().toString();
    }

    public List<Pair<String, String>> getParams() {
        List<Pair<String, String>> paramsList = new ArrayList<Pair<String, String>>();
        String[] params = paramsInDB.split("[=&]");
 
        for (int i = 0; i < (params.length - 1); i = i + 2) {
            paramsList.add(new Pair<String, String>(params[i], params[i + 1]));
        }
        return paramsList;
    }
    
    public long getId() {
        return id;
    }

    public long getLoadBalancerId() {
        return loadBalancerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMethodName() {
        return methodName;
    }
    
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
}
