package com.cloud.network.security;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.StandardMBean;

public class SecurityManagerMBeanImpl extends StandardMBean implements SecurityGroupManagerMBean, RuleUpdateLog {
    SecurityGroupManagerImpl2 _sgMgr;
    boolean _monitoringEnabled = false;
    //keep track of last scheduled, last update sent and last seqno sent per vm. Make it available over JMX
    Map<Long, Date> _scheduleTimestamps = new ConcurrentHashMap<Long, Date>(4000, 100, 64);
    Map<Long, Date> _updateTimestamps = new ConcurrentHashMap<Long, Date>(4000, 100, 64);
    
    
    protected SecurityManagerMBeanImpl(SecurityGroupManagerImpl2 securityGroupManager) {
        super(SecurityGroupManagerMBean.class, false);
        this._sgMgr = securityGroupManager;
    }




    @Override
    public int getQueueSize() {
       return this._sgMgr.getQueueSize();
    }
    
    @Override
    public void logUpdateDetails(Long vmId, Long seqno) {
        if (_monitoringEnabled) {
            _updateTimestamps.put(vmId, new Date());
        }
       
    }
    
    @Override
    public void logScheduledDetails(Set<Long> vmIds) {
        if (_monitoringEnabled) {
            for (Long vmId : vmIds) {
                _scheduleTimestamps.put(vmId, new Date());
            }
        }
    }
    
    @Override
    public void enableUpdateMonitor(boolean enable) {
        _monitoringEnabled = enable;
        if (!enable) {
            _updateTimestamps.clear();
            _scheduleTimestamps.clear();
        }
    }


    @Override
    public Map<Long, Date> getScheduledTimestamps() {
        return _scheduleTimestamps;
    }

    @Override
    public Map<Long, Date> getLastUpdateSentTimestamps() {
        return _updateTimestamps;
    }


    @Override
    public List<Long> getVmsInQueue() {
        return _sgMgr.getWorkQueue().getVmsInQueue();
    }




    @Override
    public void disableSchedulerForVm(Long vmId) {
        _sgMgr.disableSchedulerForVm(vmId,  true);
        
    }

    @Override
    public void enableSchedulerForVm(Long vmId) {
        _sgMgr.disableSchedulerForVm(vmId,  false);
        
    }
    
    @Override
    public Long[] getDisabledVmsForScheduler() {
        return _sgMgr.getDisabledVmsForScheduler();
    }




    @Override
    public void enableSchedulerForAllVms() {
        _sgMgr.enableAllVmsForScheduler();
        
    }

}
