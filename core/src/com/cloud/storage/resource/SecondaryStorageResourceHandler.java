/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.storage.resource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public interface SecondaryStorageResourceHandler {
	Answer executeRequest(Command cmd);
}
