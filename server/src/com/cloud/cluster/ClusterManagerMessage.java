package com.cloud.cluster;

import java.util.List;

public class ClusterManagerMessage {
	public static enum MessageType { nodeAdded, nodeRemoved, nodeIsolated };

	MessageType _type;
	List<ManagementServerHostVO> _nodes;
	
	public ClusterManagerMessage(MessageType type) {
		_type = type;
	}
	
	public ClusterManagerMessage(MessageType type, List<ManagementServerHostVO> nodes) {
		_type = type;
		_nodes = nodes;
	}
	
	public MessageType getMessageType() {
		return _type;
	}
	
	public List<ManagementServerHostVO> getNodes() {
		return _nodes;
	}
}
