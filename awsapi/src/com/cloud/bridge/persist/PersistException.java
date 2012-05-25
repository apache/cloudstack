/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.persist;

/**
 * @author Kelven Yang
 */
public class PersistException extends RuntimeException {
	private static final long serialVersionUID = -7137918292537610367L;

	public PersistException() {
	}
	
	public PersistException(String message) {
		super(message);
	}
	
	public PersistException(Throwable e) {
		super(e);
	}
	
	public PersistException(String message, Throwable e) {
		super(message, e);
	}
}
