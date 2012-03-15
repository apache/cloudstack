package com.cloud.gate.model;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;

import com.cloud.bridge.model.MHost;
import com.cloud.bridge.model.MHostMount;
import com.cloud.bridge.model.SBucket;
import com.cloud.bridge.model.SHost;
import com.cloud.bridge.model.SMeta;
import com.cloud.bridge.model.SObject;
import com.cloud.bridge.util.CloudSessionFactory;
import com.cloud.bridge.util.QueryHelper;
import com.cloud.gate.testcase.BaseTestCase;

public class ModelTestCase extends BaseTestCase {
    protected final static Logger logger = Logger.getLogger(ModelTestCase.class);

    public void testSHost() {
		SHost host;
		
		// create the record
		Session session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			host = new SHost();
			host.setHost("localhost");
			host.setExportRoot("/");
			host.setUserOnHost("root");
			host.setUserPassword("password");
			session.saveOrUpdate(host);
			txn.commit();
		} finally {
			session.close();
		}
		Assert.assertTrue(host.getId() != 0);
		
		// retrive the record
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			host = (SHost)session.get(SHost.class, (long)host.getId());
			txn.commit();
			
			Assert.assertTrue(host.getHost().equals("localhost"));
			Assert.assertTrue(host.getUserOnHost().equals("root"));
			Assert.assertTrue(host.getUserPassword().equals("password"));
			
			logger.info("Retrived record, host:" + host.getHost() 
					+ ", user: " + host.getUserOnHost() 
					+ ", password: " + host.getUserPassword());
			
		} finally {
			session.close();
		}
		
		// delete the record
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			host = (SHost)session.get(SHost.class, (long)host.getId());
			session.delete(host);
			txn.commit();
		} finally {
			session.close();
		}
		
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			host = (SHost)session.get(SHost.class, (long)host.getId());
			txn.commit();
			
			Assert.assertTrue(host == null);
		} finally {
			session.close();
		}
    }
    
    public void testSBucket() {
    	SHost host;
    	SBucket bucket;
    	Session session;
    	
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			host = new SHost();
			host.setHost("localhost");
			host.setUserOnHost("root");
			host.setUserPassword("password");
			host.setExportRoot("/");
			
			bucket = new SBucket();
			bucket.setName("Bucket");
			bucket.setOwnerCanonicalId("OwnerId-dummy");
			bucket.setCreateTime(new Date());
			
			host.getBuckets().add(bucket);
			bucket.setShost(host);
			
			session.save(host);
			session.save(bucket);
			txn.commit();
		} finally {
			session.close();
		}
		
		long bucketId = bucket.getId();
		
		// load bucket
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			bucket = (SBucket)session.get(SBucket.class, bucketId);
			txn.commit();
			
			Assert.assertTrue(bucket.getShost().getHost().equals("localhost"));
			Assert.assertTrue(bucket.getName().equals("Bucket"));
			Assert.assertTrue(bucket.getOwnerCanonicalId().equals("OwnerId-dummy"));
		} finally {
			session.close();
		}
		
		// delete the bucket
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			bucket = (SBucket)session.get(SBucket.class, bucketId);
			session.delete(bucket);
			
			host = (SHost)session.get(SHost.class, host.getId());
			session.delete(host);
			txn.commit();
		} finally {
			session.close();
		}

		// verify the deletion
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			bucket = (SBucket)session.get(SBucket.class, bucketId);
			txn.commit();
			
			Assert.assertTrue(bucket == null);
		} finally {
			session.close();
		}
    }
    
    public void testSObject() {
    	SHost host;
    	SBucket bucket;
    	Session session;
    	SObject sobject;
    	
    	// setup 
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			host = new SHost();
			host.setHost("localhost");
			host.setUserOnHost("root");
			host.setUserPassword("password");
			host.setExportRoot("/");
			
			bucket = new SBucket();
			bucket.setName("Bucket");
			bucket.setOwnerCanonicalId("OwnerId-dummy");
			bucket.setCreateTime(new Date());
			bucket.setShost(host);
			host.getBuckets().add(bucket);
			
			sobject = new SObject();
			sobject.setNameKey("ObjectNameKey");
			sobject.setOwnerCanonicalId("OwnerId-dummy");
			sobject.setCreateTime(new Date());
			sobject.setBucket(bucket);
			bucket.getObjectsInBucket().add(sobject);
			
			session.save(host);
			session.save(bucket);
			session.save(sobject);
			txn.commit();
			
		} finally {
			session.close();
		}
		
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			sobject = (SObject)session.get(SObject.class, sobject.getId());
			txn.commit();
			Assert.assertTrue(sobject.getBucket().getName().equals("Bucket"));
			Assert.assertTrue(sobject.getNameKey().equals("ObjectNameKey"));
			Assert.assertTrue(sobject.getOwnerCanonicalId().equals("OwnerId-dummy"));
		} finally {
			session.close();
		}

		// test delete cascade
		session = CloudSessionFactory.getInstance().openSession();
		try {
			Transaction txn = session.beginTransaction();
			bucket = (SBucket)session.get(SBucket.class, bucket.getId());
			session.delete(bucket);
			
			host = (SHost)session.get(SHost.class, host.getId());
			session.delete(host);
			txn.commit();
		} finally {
			session.close();
		}
    }
    
    public void testMeta() {
		Session session;

		session = CloudSessionFactory.getInstance().openSession();
    	try {
			Transaction txn = session.beginTransaction();
    		SMeta meta = new SMeta();
    		meta.setTarget("SObject");
    		meta.setTargetId(1);
    		meta.setName("param1");
    		meta.setValue("value1");
    		session.save(meta);
    		
    		logger.info("Meta 1: " + meta.getId());
    		
    		meta = new SMeta();
    		meta.setTarget("SObject");
    		meta.setTargetId(1);
    		meta.setName("param2");
    		meta.setValue("value2");
    		session.save(meta);
    		
    		logger.info("Meta 2: " + meta.getId());
    		
    		txn.commit();
    	} finally {
    		session.close();
    	}
    	
    	session = CloudSessionFactory.getInstance().openSession();
    	try {
			Transaction txn = session.beginTransaction();
    		Query query = session.createQuery("from SMeta where target=? and targetId=?");
    		QueryHelper.bindParameters(query, new Object[] {
    			"SObject", new Long(1)
    		});
    		List<SMeta> l = QueryHelper.executeQuery(query); 
    		txn.commit();
    		
    		for(SMeta meta: l) {
    			logger.info("" + meta.getName() + "=" + meta.getValue());
    		}
    	} finally {
    		session.close();
    	}
    	
    	session = CloudSessionFactory.getInstance().openSession();
    	try {
			Transaction txn = session.beginTransaction();
    		Query query = session.createQuery("delete from SMeta where target=?");
    		QueryHelper.bindParameters(query, new Object[] {"SObject"});
    		query.executeUpdate();
    		txn.commit();
    	} finally {
    		session.close();
    	}
    }
    
    public void testHosts() {
		Session session;
		SHost shost;
		MHost mhost;
		MHostMount hostMount;

    	session = CloudSessionFactory.getInstance().openSession();
    	try {
    		Transaction txn = session.beginTransaction();
    		shost = new SHost();
    		shost.setHost("Storage host1");
    		shost.setUserOnHost("root");
    		shost.setUserPassword("password");
			shost.setExportRoot("/");
    		session.save(shost);
    		
    		mhost = new MHost();
    		mhost.setHostKey("1");
    		mhost.setHost("management host1");
    		mhost.setVersion("v1");
    		session.save(mhost);

    		hostMount = new MHostMount();
    		hostMount.setMhost(mhost);
    		hostMount.setShost(shost);
    		hostMount.setMountPath("/mnt");
    		session.save(hostMount);
    		txn.commit();
    	} finally {
    		session.close();
    	}
    	
    	session = CloudSessionFactory.getInstance().openSession();
    	try {
    		Transaction txn = session.beginTransaction();
    		mhost = (MHost)session.createQuery("from MHost where hostKey=?").
    			setLong(0, new Long(1)).uniqueResult();
    		
    		if(mhost != null) {
	    		Iterator it = mhost.getMounts().iterator();
	    		while(it.hasNext()) {
	    			MHostMount mount = (MHostMount)it.next();
	    			Assert.assertTrue(mount.getMountPath().equals("/mnt"));
	    			
	    			logger.info(mount.getMountPath());
	    		}
    		}
    		txn.commit();
    	} finally {
    		session.close();
    	}
    	
    	session = CloudSessionFactory.getInstance().openSession();
    	try {
    		Transaction txn = session.beginTransaction();
    		mhost = (MHost)session.createQuery("from MHost where hostKey=?").
    			setLong(0, new Long(1)).uniqueResult();
    		if(mhost != null)
    			session.delete(mhost);
    		
    		shost = (SHost)session.createQuery("from SHost where host=?").
			setString(0, "Storage host1").uniqueResult();
    		if(shost != null)
    			session.delete(shost);
    		txn.commit();
    	} finally {
    		session.close();
    	}
    }
}
