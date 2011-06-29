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
package com.cloud.utils.db;

import java.sql.Connection;
import java.sql.PreparedStatement;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.Test;

public class Merovingian2Test extends TestCase {
    static final Logger s_logger = Logger.getLogger(Merovingian2Test.class);
    
    @Override
    protected void setUp() throws Exception {
        Connection conn = Transaction.getStandaloneConnection();
        PreparedStatement pstmt = conn.prepareStatement("DELETE FROM op_lock");
        pstmt.executeUpdate();
        
        pstmt.close();
        conn.close();
    }

    @Test
    public void testLockAndRelease() {
        Merovingian2 m = Merovingian2.createLockMaster(1234);
        
        s_logger.info("Testing first acquire");
        boolean result = m.acquire("first"+1234, 5);
        Assert.assertTrue(result);
        
        s_logger.info("Testing acquire of different lock");
        result = m.acquire("second"+1234, 5);
        Assert.assertTrue(result);
        
        s_logger.info("Testing reacquire of the same lock");
        result = m.acquire("first"+1234, 5);
        Assert.assertTrue(result);
        
        int count = m.owns("first"+1234);
        Assert.assertEquals(count, 2);
        
        count = m.owns("second"+1234);
        Assert.assertEquals(count, 1);
        
        s_logger.info("Testing release of the first lock");
        result = m.release("first"+1234);
        Assert.assertTrue(result);
        
        count = m.owns("first"+1234);
        Assert.assertEquals(count, 1);
        
        s_logger.info("Testing release of the second lock");
        result = m.release("second"+1234);
        Assert.assertTrue(result);
        
        result = m.release("first"+1234);
        Assert.assertTrue(result);
    }
    
    
}
