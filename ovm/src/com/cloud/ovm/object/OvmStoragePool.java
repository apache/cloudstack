package com.cloud.ovm.object;

import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

import com.cloud.utils.Pair;

public class OvmStoragePool extends OvmObject {
	public static final String NFS = "OVSSPNFS";
	public static final String OCFS2 = "OVSSPOCFS2";
	public static final String PARTITION = "OVSSPPartition";

	public static class Details {
		public String uuid;
		public String type;
		public String path;
		public String mountPoint;
		public Long totalSpace;
		public Long freeSpace;
		public Long usedSpace;
		
		public String toJson() {
			return Coder.toJson(this);
		}
	}
	
	/**
	 * @param c: connection
	 * @param d: includes three fields {uuid, type, path}
	 * @throws XmlRpcException
	 */
	public static void create(Connection c, Details d) throws XmlRpcException {
		Object[] params = {d.toJson()};
		c.callTimeoutInSec("OvmStoragePool.create", params, 3600);
	}
	
	/**
	 * 
	 * @param c: connection
	 * @param uuid: uuid of primary storage
	 * @return: Details with full fields
	 * @throws XmlRpcException
	 */
	public static Details getDetailsByUuid(Connection c, String uuid) throws XmlRpcException {
		Object[] params = {uuid};
		String res = (String)c.call("OvmStoragePool.getDetailsByUuid", params);
		return Coder.fromJson(res, Details.class);
	}
	
	/**
	 * 
	 * @param c: Connection
	 * @param uuid: Pool uuid
	 * @param from: secondary storage download path
	 * @return: <destenation path, size of template>
	 * @throws XmlRpcException 
	 */
	public static Pair<String, Long> downloadTemplate(Connection c, String uuid, String from) throws XmlRpcException {
		Object[] params = {uuid, from};
		String res = (String) c.callTimeoutInSec("OvmStoragePool.downloadTemplate", params, 3600);
		Map pair = Coder.mapFromJson(res);
		return new Pair<String, Long>((String)pair.get("installPath"), Long.parseLong((String)pair.get("templateSize")));
	}
	
	public static void prepareOCFS2Nodes(Connection c, String clusterName, String nodes) throws XmlRpcException {
	    Object[] params = {clusterName, nodes};
	    c.call("OvmStoragePool.prepareOCFS2Nodes", params);
	}
	
	public static Map<String, String> createTemplateFromVolume(Connection c, String secStorageMountPath, String installPath, String volumePath, int timeout) throws XmlRpcException {
		Object[] params = {secStorageMountPath, installPath, volumePath};
		String res = (String) c.callTimeoutInSec("OvmStoragePool.createTemplateFromVolume", params, timeout);
		Map info = Coder.mapFromJson(res);
		return info;		
	}
	
	public static String copyVolume(Connection c, String secStorageMountPath, String volumeFolderOnSecStorage, String volumePath, String storagePoolUuid, Boolean toSec, int timeout) throws XmlRpcException {
		Object[] params = {secStorageMountPath, volumeFolderOnSecStorage, volumePath, storagePoolUuid, toSec};
		String res = (String) c.callTimeoutInSec("OvmStoragePool.copyVolume", params, timeout);
		Map info = Coder.mapFromJson(res);
		return (String) info.get("installPath");
	}
}
