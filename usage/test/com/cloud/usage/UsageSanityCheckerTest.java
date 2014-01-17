// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.usage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;

public class UsageSanityCheckerTest extends TestCase {

    @Test
    public void testCheckItemCountByPstmt() throws SQLException {
        // Prepare
        // Mock dependencies to exclude from the test
        String sqlTemplate1 = "SELECT * FROM mytable1";
        String sqlTemplate2 = "SELECT * FROM mytable2";

        Connection conn = Mockito.mock(Connection.class);
        PreparedStatement pstmt = Mockito.mock(PreparedStatement.class);
        ResultSet rs = Mockito.mock(ResultSet.class);

        Mockito.when(conn.prepareStatement(sqlTemplate1)).thenReturn(pstmt);
        Mockito.when(conn.prepareStatement(sqlTemplate2)).thenReturn(pstmt);
        Mockito.when(pstmt.executeQuery()).thenReturn(rs, rs);

        // First if: true -> 8
        // Second loop: true -> 16
        Mockito.when(rs.next()).thenReturn(true, true);
        Mockito.when(rs.getInt(1)).thenReturn(8, 8, 16, 16);

        // Prepare class under test
        UsageSanityChecker checker = new UsageSanityChecker();
        checker.conn = conn;
        checker.reset();
        checker.addCheckCase(sqlTemplate1, "item1");
        checker.addCheckCase(sqlTemplate2, "item2");

        // Execute
        checker.checkItemCountByPstmt();

        // Verify
        Pattern pattern = Pattern.compile(".*8.*item1.*\n.*16.*item2.*");
        Matcher matcher = pattern.matcher(checker.errors);
        assertTrue("Didn't create complete errors. It should create 2 errors: 8 item1 and 16 item2", matcher.find());
    }
}
