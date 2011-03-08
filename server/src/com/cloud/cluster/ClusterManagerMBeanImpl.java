package com.cloud.cluster;

import java.util.Date;
import java.util.TimeZone;

import javax.management.StandardMBean;

import com.cloud.utils.DateUtil;

public class ClusterManagerMBeanImpl extends StandardMBean implements ClusterManagerMBean {
	private ManagementServerHostVO _mshostVo;
	
	public ClusterManagerMBeanImpl(ManagementServerHostVO mshostVo) {
		super(ClusterManagerMBean.class, false);
		
		_mshostVo = mshostVo;
	}
	
	public long getMsid() {
		return _mshostVo.getMsid();
	}
	
	public String getLastUpdateTime() {
		Date date = _mshostVo.getLastUpdateTime();
		return DateUtil.getDateDisplayString(TimeZone.getDefault(), date);
	}
	
	public String getClusterNodeIP() {
		return _mshostVo.getServiceIP();
	}
	
	public String getVersion() {
		return _mshostVo.getVersion();
	}
}
