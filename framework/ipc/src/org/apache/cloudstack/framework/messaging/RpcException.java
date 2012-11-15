package org.apache.cloudstack.framework.messaging;

public class RpcException extends Exception {
	private static final long serialVersionUID = -3164514701087423787L;

	public RpcException() {
		super();
	}
	
	public RpcException(String message) {
		super(message);
	}
	
	public RpcException(String message, Throwable cause) {
		super(message, cause);
	}
}
