package com.cloud.utils.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GenericDaoBaseTest {
    @Mock
    ResultSet resultSet;

    @Test
    public void getObjectBoolean() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn(false);
        Mockito.when(resultSet.getBoolean(1)).thenReturn(false);
        Assert.assertFalse(GenericDaoBase
                .getObject(Boolean.class, resultSet, 1));
        Mockito.verify(resultSet).getBoolean(1);
    }

    @Test
    public void getObjectPrimitiveBoolean() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn(false);
        Mockito.when(resultSet.getBoolean(1)).thenReturn(false);
        Assert.assertFalse(GenericDaoBase
                .getObject(boolean.class, resultSet, 1));
        Mockito.verify(resultSet).getBoolean(1);
    }

    @Test
    public void getObjectPrimitiveShort() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn((short) 1);
        Mockito.when(resultSet.getShort(1)).thenReturn((short) 1);
        Assert.assertEquals(Short.valueOf((short) 1),
                GenericDaoBase.getObject(short.class, resultSet, 1));
        Mockito.verify(resultSet).getShort(1);
    }

    @Test
    public void getObjectShort() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn((short) 1);
        Mockito.when(resultSet.getShort(1)).thenReturn((short) 1);
        Assert.assertEquals(Short.valueOf((short) 1),
                GenericDaoBase.getObject(Short.class, resultSet, 1));
        Mockito.verify(resultSet).getShort(1);
    }

    @Test
    public void getObjectFloat() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn(0.1f);
        Mockito.when(resultSet.getFloat(1)).thenReturn(0.1f);
        Assert.assertEquals(0.1f,
                GenericDaoBase.getObject(Float.class, resultSet, 1));
        Mockito.verify(resultSet).getFloat(1);
    }

    @Test
    public void getObjectPrimitiveFloat() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn(0.1f);
        Mockito.when(resultSet.getFloat(1)).thenReturn(0.1f);
        Assert.assertEquals(0.1f,
                GenericDaoBase.getObject(float.class, resultSet, 1));
        Mockito.verify(resultSet).getFloat(1);
    }

    @Test
    public void getObjectPrimitiveDouble() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn(0.1d);
        Mockito.when(resultSet.getDouble(1)).thenReturn(0.1d);
        Assert.assertEquals(0.1d,
                GenericDaoBase.getObject(double.class, resultSet, 1));
        Mockito.verify(resultSet).getDouble(1);
    }

    @Test
    public void getObjectDouble() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn(0.1d);
        Mockito.when(resultSet.getDouble(1)).thenReturn(0.1d);
        Assert.assertEquals(0.1d,
                GenericDaoBase.getObject(Double.class, resultSet, 1));
        Mockito.verify(resultSet).getDouble(1);
    }

    @Test
    public void getObjectLong() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn(1l);
        Mockito.when(resultSet.getLong(1)).thenReturn(1l);
        Assert.assertEquals((Long) 1l,
                GenericDaoBase.getObject(Long.class, resultSet, 1));
        Mockito.verify(resultSet).getLong(1);
    }

    @Test
    public void getObjectPrimitiveLong() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn(1l);
        Mockito.when(resultSet.getLong(1)).thenReturn(1l);
        Assert.assertEquals((Long) 1l,
                GenericDaoBase.getObject(long.class, resultSet, 1));
        Mockito.verify(resultSet).getLong(1);
    }

    @Test
    public void getObjectPrimitiveByte() throws SQLException {
        Mockito.when(resultSet.getObject(1)).thenReturn((byte) 1);
        Mockito.when(resultSet.getByte(1)).thenReturn((byte) 1);
        Assert.assertTrue((byte) 1 == GenericDaoBase.getObject(byte.class,
                resultSet, 1));
        Mockito.verify(resultSet).getByte(1);
    }

}
