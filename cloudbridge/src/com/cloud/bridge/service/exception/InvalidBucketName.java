package com.cloud.bridge.service.exception;

public class InvalidBucketName extends RuntimeException {

	private static final long serialVersionUID = -5727022800215753266L;

	public InvalidBucketName() {
	}
	
	public InvalidBucketName(String message) {
		super(message);
	}
	
	public InvalidBucketName(Throwable e) {
		super(e);
	}
	
	public InvalidBucketName(String message, Throwable e) {
		super(message, e);
	}
}
