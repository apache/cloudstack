package com.cloud.bridge.service.exception;

public class UnsupportedException extends RuntimeException {
	private static final long serialVersionUID = 8857313467347867680L;
	
	public UnsupportedException() {
	}
	
	public UnsupportedException(String message) {
		super(message);
	}
	
	public UnsupportedException(Throwable e) {
		super(e);
	}

	public UnsupportedException(String message, Throwable e) {
		super(message, e);
	}
}
