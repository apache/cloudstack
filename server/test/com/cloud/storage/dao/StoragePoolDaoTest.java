package com.cloud.storage.dao;

import junit.framework.TestCase;

import com.cloud.storage.StoragePoolStatus;
import com.cloud.utils.component.ComponentLocator;

public class StoragePoolDaoTest extends TestCase {
    
    public void testCountByStatus() {
        StoragePoolDaoImpl dao = ComponentLocator.inject(StoragePoolDaoImpl.class);
        long count = dao.countPoolsByStatus(StoragePoolStatus.Up);
        System.out.println("Found " + count + " storage pools");
    }
}
