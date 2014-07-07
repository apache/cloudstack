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
//
package com.cloud.baremetal.manager;

import com.cloud.baremetal.database.BaremetalRctDao;
import com.cloud.baremetal.database.BaremetalRctVO;
import com.cloud.baremetal.networkservice.BaremetalRctResponse;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.google.gson.Gson;
import org.apache.cloudstack.api.AddBaremetalRctCmd;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 5/8/14.
 */
public class BaremetalVlanManagerImpl extends ManagerBase implements BaremetalVlanManager {
    private Gson gson = new Gson();

    @Inject
    private BaremetalRctDao rctDao;

    @Override
    public BaremetalRctResponse addRct(AddBaremetalRctCmd cmd) {
        try {
            URL url = new URL(cmd.getRctUrl());
            RestTemplate rest = new RestTemplate();
            String rctStr = rest.getForObject(url.toString(), String.class);

            // validate it's right format
            BaremetalRct rct = gson.fromJson(rctStr, BaremetalRct.class);
            QueryBuilder<BaremetalRctVO> sc = QueryBuilder.create(BaremetalRctVO.class);
            sc.and(sc.entity().getUrl(), SearchCriteria.Op.EQ, cmd.getRctUrl());
            BaremetalRctVO vo =  sc.find();
            if (vo == null) {
                vo = new BaremetalRctVO();
                vo.setRct(gson.toJson(rct));
                vo.setUrl(cmd.getRctUrl());
                vo = rctDao.persist(vo);
            } else {
                vo.setRct(gson.toJson(rct));
                rctDao.update(vo.getId(), vo);
            }

            BaremetalRctResponse rsp = new BaremetalRctResponse();
            rsp.setUrl(vo.getUrl());
            rsp.setId(vo.getUuid());
            return rsp;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("%s is not a legal http url", cmd.getRctUrl()));
        }
    }

    @Override
    public String getName() {
        return "Baremetal Vlan Manager";
    }


    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmds = new ArrayList<Class<?>>();
        cmds.add(AddBaremetalRctCmd.class);
        return cmds;
    }
}
