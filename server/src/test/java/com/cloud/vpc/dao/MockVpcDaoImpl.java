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
package com.cloud.vpc.dao;

import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.Vpc.State;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@DB()
public class MockVpcDaoImpl extends GenericDaoBase<VpcVO, Long> implements VpcDao {

    /* (non-Javadoc)
     * @see com.cloud.network.vpc.Dao.VpcDao#getVpcCountByOfferingId(long)
     */
    @Override
    public int getVpcCountByOfferingId(long offId) {
        return 100;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpc.Dao.VpcDao#getActiveVpcById(long)
     */
    @Override
    public Vpc getActiveVpcById(long vpcId) {
        Vpc vpc = findById(vpcId);
        if (vpc != null && vpc.getState() == Vpc.State.Enabled) {
            return vpc;
        }

        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpc.Dao.VpcDao#listByAccountId(long)
     */
    @Override
    public List<? extends Vpc> listByAccountId(long accountId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpc.Dao.VpcDao#listInactiveVpcs()
     */
    @Override
    public List<VpcVO> listInactiveVpcs() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.vpc.Dao.VpcDao#countByAccountId(long)
     */
    @Override
    public long countByAccountId(long accountId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public VpcVO persist(VpcVO vpc, Map<String, List<String>> serviceProviderMap) {
        return null;
    }

    @Override
    public void persistVpcServiceProviders(long vpcId, Map<String, List<String>> serviceProviderMap) {
        return;
    }

    @Override
    public VpcVO findById(Long id) {
        VpcVO vo = null;
        if (id.longValue() == 1) {
            vo = new VpcVO(1, "new vpc", "new vpc", 1, 1, 1, "0.0.0.0/0", "vpc domain", false, false, false, null, null, null, null);
        } else if (id.longValue() == 2) {
            vo = new VpcVO(1, "new vpc", "new vpc", 1, 1, 1, "0.0.0.0/0", "vpc domain", false, false, false, null, null, null, null);
            vo.setState(State.Inactive);
        }

        vo = setId(vo, id);

        return vo;
    }

    private VpcVO setId(VpcVO vo, long id) {
        VpcVO voToReturn = vo;
        Class<?> c = voToReturn.getClass();
        try {
            Field f = c.getDeclaredField("id");
            f.setAccessible(true);
            f.setLong(voToReturn, id);
        } catch (NoSuchFieldException ex) {
            logger.warn(ex);
            return null;
        } catch (IllegalAccessException ex) {
            logger.warn(ex);
            return null;
        }

        return voToReturn;
    }

    @Override
    public boolean remove(Long id) {
        return true;
    }

    @Override
    public boolean update(Long id, VpcVO vo) {
        return true;
    }

}
