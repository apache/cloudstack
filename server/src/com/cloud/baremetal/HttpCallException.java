/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.baremetal;

import com.cloud.utils.SerialVersionUID;

public class HttpCallException extends Exception {
	private static final long serialVersionUID= SerialVersionUID.HttpCallException;
	public HttpCallException(String msg) {
		super(msg);
	}
}
