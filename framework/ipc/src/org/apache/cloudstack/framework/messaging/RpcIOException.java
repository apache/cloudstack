package org.apache.cloudstack.framework.messaging;

public class RpcIOException extends RpcException {

	private static final long serialVersionUID = -6108039302920641533L;
	
	public RpcIOException() {
		super();
	}
	
	public RpcIOException(String message) {
		super(message);
	}
	
	public RpcIOException(String message, Throwable cause) {
		super(message, cause);
	}
}
