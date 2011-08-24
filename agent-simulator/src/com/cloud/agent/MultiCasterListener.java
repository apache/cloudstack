/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent;
import java.net.*;

public interface MultiCasterListener {
	public void onMultiCasting(byte[] data, int off, int len, InetAddress addrFrom);
}
