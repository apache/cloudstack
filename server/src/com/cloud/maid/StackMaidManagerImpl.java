package com.cloud.maid;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.cluster.ClusterManager;
import com.cloud.maid.dao.StackMaidDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;

@Local(value = { StackMaidManager.class })
public class StackMaidManagerImpl implements StackMaidManager {
	private static final Logger s_logger = Logger.getLogger(StackMaidManagerImpl.class);

	private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 3; 	// 3 seconds
    private static final int GC_INTERVAL = 10000;				// 10 seconds
	
	private String _name;
	
	@Inject
	private StackMaidDao _maidDao;
	
	@Inject
	private ClusterManager _clusterMgr;
	
    private final ScheduledExecutorService _heartbeatScheduler =
        Executors.newScheduledThreadPool(1, new NamedThreadFactory("StackMaidMgr-Heartbeat"));
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		
		if (s_logger.isInfoEnabled())
			s_logger.info("Start configuring StackMaidManager : " + name);
		
		StackMaid.init(_clusterMgr.getId());
		return true;
	}
	
	private void cleanupLeftovers(List<StackMaidVO> l) {
		for(StackMaidVO maid : l) {
			StackMaid.doCleanup(maid);
			_maidDao.delete(maid.getId());
		}
	}
	
	@DB
	private Runnable getGCTask() {
		return new Runnable() {
			public void run() {
				GlobalLock scanLock = GlobalLock.getInternLock("StackMaidManagerGC");
				try {
					if(scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
						try {
							reallyRun();
						} finally {
							scanLock.unlock();
						}
					}
				} finally {
					scanLock.releaseRef();
				}
			}
			
			public void reallyRun() {
				try {
					Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - 3600000);
					List<StackMaidVO> l = _maidDao.listLeftoversByCutTime(cutTime);
					cleanupLeftovers(l);
				} catch(Throwable e) {
					s_logger.error("Unexpected exception when trying to execute queue item, ", e);
				}
			}
		};
	}
	
    public boolean start() {
    	try {
    		List<StackMaidVO> l = _maidDao.listLeftoversByMsid(_clusterMgr.getId());
    		cleanupLeftovers(l);
    	} catch(Throwable e) {
    		s_logger.error("Unexpected exception " + e.getMessage(), e);
    	}

    	_heartbeatScheduler.scheduleAtFixedRate(getGCTask(), GC_INTERVAL, GC_INTERVAL, TimeUnit.MILLISECONDS);
    	return true;
    }
    
    public boolean stop() {
    	return true;
    }
	
    public String getName() {
    	return _name;
    }
}
