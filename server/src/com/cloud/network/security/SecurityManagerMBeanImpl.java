package com.cloud.network.security;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.StandardMBean;

import com.cloud.utils.Ternary;

public class SecurityManagerMBeanImpl extends StandardMBean implements SecurityGroupManagerMBean, RuleUpdateLog {
    SecurityGroupManagerImpl2 _sgMgr;
    boolean _monitoringEnabled = false;
    //keep track of last scheduled, last update sent and last seqno sent per vm. Make it available over JMX
    Map<Long, Ternary<Date, Date, Long>> _updateDetails = new ConcurrentHashMap<Long, Ternary<Date,Date,Long>>(4000, 100, 64);
    
    
    protected SecurityManagerMBeanImpl(SecurityGroupManagerImpl2 securityGroupManager) {
        super(SecurityGroupManagerMBean.class, false);
        this._sgMgr = securityGroupManager;
    }


    @Override
    public Map<Long, Ternary<Date, Date, Long>> getVmUpdateDetails() {
        return _updateDetails;
    }

    @Override
    public int getQueueSize() {
       return this._sgMgr.getQueueSize();
    }
    
    @Override
    public void logUpdateDetails(Long vmId, Long seqno) {
        if (_monitoringEnabled) {
            Ternary<Date, Date, Long> detail = _updateDetails.get(vmId);
            if (detail == null) {
                detail = new Ternary<Date, Date, Long>(new Date(), new Date(), seqno);
            }
            detail.second(new Date());
            detail.third(seqno);
            _updateDetails.put(vmId, detail);
        }
       
    }
    
    @Override
    public void logScheduledDetails(Set<Long> vmIds) {
        if (_monitoringEnabled) {
            for (Long vmId : vmIds) {
                Ternary<Date, Date, Long> detail = _updateDetails.get(vmId);
                if (detail == null) {
                    detail = new Ternary<Date, Date, Long>(new Date(), null, 0L);
                }
                detail.first(new Date());
                _updateDetails.put(vmId, detail);
            }
        }
    }
    
    @Override
    public void enableUpdateMonitor(boolean enable) {
        _monitoringEnabled = enable;
        if (!enable) {
            _updateDetails.clear();
        }
    }

}
