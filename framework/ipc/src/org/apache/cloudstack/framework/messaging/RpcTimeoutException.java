package org.apache.cloudstack.framework.messaging;

public class RpcTimeoutException extends RpcException {

	private static final long serialVersionUID = -3618654987984665833L;

	public RpcTimeoutException() {
		super();
	}
	
	public RpcTimeoutException(String message) {
		super(message);
	}
}
