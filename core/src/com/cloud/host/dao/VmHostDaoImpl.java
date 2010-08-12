/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.host.dao;

import com.cloud.host.VmHostVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;

public class VmHostDaoImpl extends GenericDaoBase<VmHostVO, Long> implements VmHostDao {
    
    @Override
    public void freeVncPort(long hostId, int vncPort) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        VmHostVO vo = lock(hostId, true);
        if (vo == null) {
            txn.commit();
            return;
        }
        
        vo.setVncPorts(VmHostVO.markPortAvailable(vo.getVncPorts(), vncPort));
        vo.setStartAt(vncPort % 63);
        
        update(hostId, vo);
        
        txn.commit();
    }
    
    @Override
    public void setVncPort(long hostId, int vncPort) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        VmHostVO vo = lock(hostId, true);
        if (vo == null) {
            txn.commit();
            return;
        }
        
        vo.setVncPorts(VmHostVO.markPortUsed(vo.getVncPorts(), vncPort));
        vo.setStartAt((vncPort + 1) % 63);
        
        update(hostId, vo);
        
        txn.commit();
    }
    
    @Override
    public Integer getAvailableVncPort(long hostId) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        VmHostVO vo  = lock(hostId, true);
        if (vo == null) {
            txn.commit();
            return null;
        }
        
        Integer port = findAvailablePort(vo);
        if (port == null) {
            txn.commit();
            return null;
        }
        
        vo.setVncPorts(VmHostVO.markPortUsed(vo.getVncPorts(), port));
        vo.setStartAt((port + 1) % 63);
        
        update(hostId, vo);
        txn.commit();
        
        return port;
    }
    
    protected Integer findAvailablePort(VmHostVO vo) {
        long ports = vo.getVncPorts();
        int start = vo.getStartAt();
        for (int i = 0; i < 63; i++) {
            int port = (i + start) % 63;
            if ((ports & (0x01l << port)) == 0) {
                return port;
            }
        }
        return null;
    }
    
}
