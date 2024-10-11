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
package com.cloud.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.DbUtil;
import com.cloud.utils.db.TransactionLegacy;

@RunWith(MockitoJUnitRunner.class)
public class DbUtilTest {

    @Mock
    Connection connection;

    @Mock
    PreparedStatement preparedStatement;

    @Mock
    Statement statement;

    @Mock
    ResultSet resultSet;

    @Mock
    DataSource dataSource;

    DataSource backup;

    Map<String, Connection> connectionMapBackup = null;

    Map<String, Connection> connectionMap = null;

    @Table(name = "test_table")
    static class Testbean {
        String noAnnotation;
        @Column()
        String withAnnotation;
        @Column(name = "surprise")
        String withAnnotationAndName;
    }

    @Test
    public void getColumnName() throws SecurityException, NoSuchFieldException {
        // if no annotation, then the field name
        Assert.assertEquals("noAnnotation", DbUtil.getColumnName(Testbean.class.getDeclaredField("noAnnotation")));
        // there is annotation with name, take the name
        Assert.assertEquals("surprise", DbUtil.getColumnName(Testbean.class.getDeclaredField("withAnnotationAndName")));
    }

    @Test
    @Ignore
    public void getColumnNameWithAnnotationButWithoutNameAttribute() throws SecurityException, NoSuchFieldException {
        // there is annotation, but no name defined, fallback to field name
        // this does not work this way, it probably should
        Assert.assertEquals("withAnnotation", DbUtil.getColumnName(Testbean.class.getDeclaredField("withAnnotation")));

    }

    static class IsPersistableTestBean {
        static final String staticFinal = "no";
        final String justFinal = "no";
        transient String transientField;
        transient static String strange = "";
        String instanceField;
    }

    @Test
    public void isPersistable() throws SecurityException, NoSuchFieldException {
        Assert.assertFalse(DbUtil.isPersistable(IsPersistableTestBean.class.getDeclaredField("staticFinal")));
        Assert.assertFalse(DbUtil.isPersistable(IsPersistableTestBean.class.getDeclaredField("justFinal")));
        Assert.assertFalse(DbUtil.isPersistable(IsPersistableTestBean.class.getDeclaredField("transientField")));
        Assert.assertFalse(DbUtil.isPersistable(IsPersistableTestBean.class.getDeclaredField("strange")));
        Assert.assertTrue(DbUtil.isPersistable(IsPersistableTestBean.class.getDeclaredField("instanceField")));
    }

    class Bar {

    }

    @Test
    public void getTableName() {
        Assert.assertEquals("test_table", DbUtil.getTableName(Testbean.class));
        Assert.assertEquals("Bar", DbUtil.getTableName(Bar.class));
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field globalLocks = DbUtil.class.getDeclaredField("s_connectionForGlobalLocks");
        globalLocks.setAccessible(true);
        connectionMapBackup = (Map<String, Connection>)globalLocks.get(null);
        connectionMap = new HashMap<String, Connection>();
        globalLocks.set(null, connectionMap);

        Field dsField = TransactionLegacy.class.getDeclaredField("s_ds");
        dsField.setAccessible(true);
        backup = (DataSource)dsField.get(null);
        dsField.set(null, dataSource);
    }

    @After
    public void cleanup() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field globalLocks = DbUtil.class.getDeclaredField("s_connectionForGlobalLocks");
        globalLocks.setAccessible(true);
        globalLocks.set(null, connectionMapBackup);

        Field dsField = TransactionLegacy.class.getDeclaredField("s_ds");
        dsField.setAccessible(true);
        dsField.set(null, backup);
    }

    @Test
    public void getGlobalLock() throws SQLException {
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        Mockito.when(resultSet.first()).thenReturn(true);
        Mockito.when(resultSet.getInt(1)).thenReturn(1);
        Assert.assertTrue(DbUtil.getGlobalLock("TEST", 600));

        Mockito.verify(connection).prepareStatement(ArgumentMatchers.anyString());
        Mockito.verify(preparedStatement).close();
        Mockito.verify(resultSet).close();
    }

    @Test
    public void getGlobalLockTimeout() throws SQLException {
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        Mockito.when(resultSet.first()).thenReturn(true);
        Mockito.when(resultSet.getInt(1)).thenReturn(0);
        Assert.assertFalse(DbUtil.getGlobalLock("TEST", 600));

        Mockito.verify(connection).prepareStatement(ArgumentMatchers.anyString());
        Mockito.verify(preparedStatement).close();
        Mockito.verify(resultSet).close();
        Mockito.verify(connection).close();

        // if any error happens, the connection map must be cleared
        Assert.assertTrue(connectionMap.isEmpty());
    }

    @Test
    public void closeNull() {
        DbUtil.closeStatement((Statement)null);
        DbUtil.closeConnection((Connection)null);
        DbUtil.closeResultSet((ResultSet)null);
        // no exception should be thrown
    }

    @Test
    public void closeConnection() throws IOException, SQLException {
        DbUtil.closeConnection(connection);
        Mockito.verify(connection).close();
    }

    @Test
    public void closeConnectionFail() throws IOException, SQLException {
        Mockito.doThrow(new SQLException("it is all right")).when(connection).close();
        DbUtil.closeConnection(connection);
        Mockito.verify(connection).close();
    }

    @Test
    public void closeStatement() throws IOException, SQLException {
        DbUtil.closeStatement(statement);
        Mockito.verify(statement).close();
    }

    @Test
    public void closeStatementFail() throws IOException, SQLException {
        Mockito.doThrow(new SQLException("it is all right")).when(statement).close();
        DbUtil.closeStatement(statement);
        Mockito.verify(statement).close();
    }

    @Test
    public void closeResultSet() throws IOException, SQLException {
        DbUtil.closeResultSet(resultSet);
        Mockito.verify(resultSet).close();
    }

    @Test
    public void closeResultSetFail() throws IOException, SQLException {
        Mockito.doThrow(new SQLException("it is all right")).when(resultSet).close();
        DbUtil.closeResultSet(resultSet);
        Mockito.verify(resultSet).close();
    }

    @Test
    @Ignore
    //can not be performed since assertion embedded in this branch of execution
        public
        void releaseGlobalLockNotexisting() throws SQLException {
        Assert.assertFalse(DbUtil.releaseGlobalLock("notexisting"));
        Mockito.verify(dataSource, Mockito.never()).getConnection();
    }

    @Test
    public void releaseGlobalLock() throws SQLException {
        Mockito.when(connection.prepareStatement(ArgumentMatchers.anyString())).thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery()).thenReturn(resultSet);
        Mockito.when(resultSet.first()).thenReturn(true);
        Mockito.when(resultSet.getInt(1)).thenReturn(1);
        connectionMap.put("testLock", connection);
        Assert.assertTrue(DbUtil.releaseGlobalLock("testLock"));

        Mockito.verify(resultSet).close();
        Mockito.verify(preparedStatement).close();
        Mockito.verify(connection).close();
        Assert.assertFalse(connectionMap.containsKey("testLock"));
    }

}
