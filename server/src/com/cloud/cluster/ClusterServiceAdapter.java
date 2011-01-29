package com.cloud.cluster;

import java.rmi.RemoteException;

import com.cloud.cluster.ClusterService;
import com.cloud.utils.component.Adapter;

public interface ClusterServiceAdapter extends Adapter {
	public ClusterService getPeerService(String strPeer) throws RemoteException;
	public String getServiceEndpointName(String strPeer);
	public int getServicePort();
}
