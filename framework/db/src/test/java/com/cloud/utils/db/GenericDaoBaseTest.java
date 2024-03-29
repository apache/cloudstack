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
package com.cloud.utils.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityExistsException;

@RunWith(MockitoJUnitRunner.class)
public class GenericDaoBaseTest {
    @Mock
    ResultSet resultSet;
    @Mock
    SQLException mockedSQLException;

    private static final DbTestDao dbTestDao = new DbTestDao();
    private static final String INTEGRITY_CONSTRAINT_VIOLATION = "23000";
    private static final int DUPLICATE_ENTRY_ERRO_CODE = 1062;

    GenericDaoBase genericDaoBaseMock = Mockito.mock(GenericDaoBase.class, Mockito.CALLS_REAL_METHODS);

    @Before
    public void prepareTests() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn(false);
        Mockito.when(resultSet.getBoolean(1)).thenReturn(false);
        Mockito.when(resultSet.getObject(1)).thenReturn((short) 1);
        Mockito.when(resultSet.getShort(1)).thenReturn((short) 1);
        Mockito.when(resultSet.getObject(1)).thenReturn(0.1f);
        Mockito.when(resultSet.getFloat(1)).thenReturn(0.1f);
        Mockito.when(resultSet.getObject(1)).thenReturn(0.1d);
        Mockito.when(resultSet.getDouble(1)).thenReturn(0.1d);
        Mockito.when(resultSet.getObject(1)).thenReturn(1l);
        Mockito.when(resultSet.getLong(1)).thenReturn(1l);
        Mockito.when(resultSet.getInt(1)).thenReturn(1);
        Mockito.when(resultSet.getObject(1)).thenReturn((byte) 1);
        Mockito.when(resultSet.getByte(1)).thenReturn((byte) 1);
    }

    @Test
    public void getObjectBoolean() throws SQLException {
        Assert.assertFalse(GenericDaoBase
                .getObject(Boolean.class, resultSet, 1));
        Mockito.verify(resultSet).getBoolean(1);
    }

    @Test
    public void getObjectPrimitiveBoolean() throws SQLException {
        Assert.assertFalse(GenericDaoBase
                .getObject(boolean.class, resultSet, 1));
        Mockito.verify(resultSet).getBoolean(1);
    }

    @Test
    public void getObjectPrimitiveShort() throws SQLException {
        Assert.assertEquals(Short.valueOf((short) 1),
                GenericDaoBase.getObject(short.class, resultSet, 1));
        Mockito.verify(resultSet).getShort(1);
    }

    @Test
    public void getObjectShort() throws SQLException {
        Assert.assertEquals(Short.valueOf((short) 1),
                GenericDaoBase.getObject(Short.class, resultSet, 1));
        Mockito.verify(resultSet).getShort(1);
    }

    @Test
    public void getObjectFloat() throws SQLException {
        Assert.assertEquals(0.1f,
                GenericDaoBase.getObject(Float.class, resultSet, 1), 0.1);
        Mockito.verify(resultSet).getFloat(1);
    }

    @Test
    public void getObjectPrimitiveFloat() throws SQLException {
        Assert.assertEquals(0.1f,
                GenericDaoBase.getObject(float.class, resultSet, 1), 0.1);
        Mockito.verify(resultSet).getFloat(1);
    }

    @Test
    public void getObjectPrimitiveDouble() throws SQLException {
        Assert.assertEquals(0.1d,
                GenericDaoBase.getObject(double.class, resultSet, 1), 0.1);
        Mockito.verify(resultSet).getDouble(1);
    }

    @Test
    public void getObjectDouble() throws SQLException {
        Assert.assertEquals(0.1d,
                GenericDaoBase.getObject(Double.class, resultSet, 1), 0.1);
        Mockito.verify(resultSet).getDouble(1);
    }

    @Test
    public void getObjectLong() throws SQLException {
        Assert.assertEquals((Long) 1l,
                GenericDaoBase.getObject(Long.class, resultSet, 1));
        Mockito.verify(resultSet).getLong(1);
    }

    @Test
    public void getObjectPrimitiveLong() throws SQLException {
        Assert.assertEquals((Long) 1l,
                GenericDaoBase.getObject(long.class, resultSet, 1));
        Mockito.verify(resultSet).getLong(1);
    }

    @Test
    public void getObjectPrimitiveInt() throws SQLException {
        Assert.assertEquals((Integer) 1,
                GenericDaoBase.getObject(int.class, resultSet, 1));
        Mockito.verify(resultSet).getInt(1);
    }

    @Test
    public void getObjectInteger() throws SQLException {
        Assert.assertEquals((Integer) 1,
                GenericDaoBase.getObject(Integer.class, resultSet, 1));
        Mockito.verify(resultSet).getInt(1);
    }

    @Test
    public void getObjectPrimitiveByte() throws SQLException {
        Assert.assertTrue((byte) 1 == GenericDaoBase.getObject(byte.class,
                resultSet, 1));
        Mockito.verify(resultSet).getByte(1);
    }

    @Test
    public void handleEntityExistsExceptionTestNoMatchForEntityExists() {
        Mockito.when(mockedSQLException.getErrorCode()).thenReturn(123);
        Mockito.when(mockedSQLException.getSQLState()).thenReturn("123");
        GenericDaoBase.handleEntityExistsException(mockedSQLException);
    }

    @Test
    public void handleEntityExistsExceptionTestIntegrityConstraint() {
        Mockito.when(mockedSQLException.getErrorCode()).thenReturn(123);
        Mockito.when(mockedSQLException.getSQLState()).thenReturn(INTEGRITY_CONSTRAINT_VIOLATION);
        GenericDaoBase.handleEntityExistsException(mockedSQLException);
    }

    @Test
    public void handleEntityExistsExceptionTestIntegrityConstraintNull() {
        Mockito.when(mockedSQLException.getErrorCode()).thenReturn(123);
        Mockito.when(mockedSQLException.getSQLState()).thenReturn(null);
        GenericDaoBase.handleEntityExistsException(mockedSQLException);
    }

    @Test
    public void handleEntityExistsExceptionTestDuplicateEntryErrorCode() {
        Mockito.when(mockedSQLException.getErrorCode()).thenReturn(DUPLICATE_ENTRY_ERRO_CODE);
        Mockito.when(mockedSQLException.getSQLState()).thenReturn("123");
        GenericDaoBase.handleEntityExistsException(mockedSQLException);
    }

    @Test(expected = EntityExistsException.class)
    public void handleEntityExistsExceptionTestExpectEntityExistsException() {
        Mockito.when(mockedSQLException.getErrorCode()).thenReturn(DUPLICATE_ENTRY_ERRO_CODE);
        Mockito.when(mockedSQLException.getSQLState()).thenReturn(INTEGRITY_CONSTRAINT_VIOLATION);
        GenericDaoBase.handleEntityExistsException(mockedSQLException);
    }

    @Test
    public void checkCountOfRecordsAgainstTheResultSetSizeTestCountHigherThanResultSetSize() {
        int count = 10;
        int resultSetSize = 5;

        int result = genericDaoBaseMock.checkCountOfRecordsAgainstTheResultSetSize(count, resultSetSize);

        Assert.assertEquals(count, result);
    }

    @Test
    public void checkCountOfRecordsAgainstTheResultSetSizeTestCountEqualToResultSetSize() {
        int count = 10;
        int resultSetSize = 10;

        int result = genericDaoBaseMock.checkCountOfRecordsAgainstTheResultSetSize(count, resultSetSize);

        Assert.assertEquals(count, result);
        Assert.assertEquals(resultSetSize, result);
    }

    @Test
    public void checkCountOfRecordsAgainstTheResultSetSizeTestCountSmallerThanResultSetSize() {
        int count = 5;
        int resultSetSize = 10;

        int result = genericDaoBaseMock.checkCountOfRecordsAgainstTheResultSetSize(count, resultSetSize);

        Assert.assertEquals(resultSetSize, result);
    }

    @Test
    public void addJoinsTest() {
        StringBuilder joinString = new StringBuilder();
        Collection<JoinBuilder<SearchCriteria<?>>> joins = new ArrayList<>();

        Attribute attr1 = new Attribute("table1", "column1");
        Attribute attr2 = new Attribute("table2", "column2");
        Attribute attr3 = new Attribute("table3", "column1");
        Attribute attr4 = new Attribute("table4", "column2");
        Attribute attr5 = new Attribute("table3", "column1");
        Attribute attr6 = new Attribute("XYZ");

        joins.add(new JoinBuilder<>("", dbTestDao.createSearchCriteria(), attr1, attr2, JoinBuilder.JoinType.INNER));
        joins.add(new JoinBuilder<>("", dbTestDao.createSearchCriteria(),
                new Attribute[]{attr3, attr5}, new Attribute[]{attr4, attr6}, JoinBuilder.JoinType.INNER, JoinBuilder.JoinCondition.OR));
        dbTestDao.addJoins(joinString, joins);

        Assert.assertEquals(" INNER JOIN table2 ON table1.column1=table2.column2 " +
                " INNER JOIN table4 ON table3.column1=table4.column2 OR table3.column1=? ", joinString.toString());
    }

    @Test
    public void multiJoinSameTableTest() {
        StringBuilder joinString = new StringBuilder();
        Collection<JoinBuilder<SearchCriteria<?>>> joins = new ArrayList<>();

        Attribute tAc1 = new Attribute("tableA", "column1");
        Attribute tAc2 = new Attribute("tableA", "column2");
        Attribute tAc3 = new Attribute("tableA", "column3");
        Attribute tBc2 = new Attribute("tableB", "column2");
        Attribute tCc3 = new Attribute("tableC", "column3");
        Attribute tDc4 = new Attribute("tableD", "column4");
        Attribute tDc5 = new Attribute("tableD", "column5");
        Attribute attr = new Attribute(123);

        joins.add(new JoinBuilder<>("tableA1Alias", dbTestDao.createSearchCriteria(), tBc2, tAc1, JoinBuilder.JoinType.INNER));
        joins.add(new JoinBuilder<>("tableA2Alias", dbTestDao.createSearchCriteria(), tCc3, tAc2, JoinBuilder.JoinType.INNER));
        joins.add(new JoinBuilder<>("tableA3Alias", dbTestDao.createSearchCriteria(),
                new Attribute[]{tDc4, tDc5}, new Attribute[]{tAc3, attr}, JoinBuilder.JoinType.INNER, JoinBuilder.JoinCondition.AND));
        dbTestDao.addJoins(joinString, joins);

        Assert.assertEquals(" INNER JOIN tableA tableA1Alias ON tableB.column2=tableA1Alias.column1 " +
                " INNER JOIN tableA tableA2Alias ON tableC.column3=tableA2Alias.column2 " +
                " INNER JOIN tableA tableA3Alias ON tableD.column4=tableA3Alias.column3 AND tableD.column5=? ", joinString.toString());
    }
}
