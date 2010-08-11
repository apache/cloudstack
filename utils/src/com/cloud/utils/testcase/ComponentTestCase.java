/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.utils.testcase;

import java.lang.annotation.Annotation;

import com.cloud.utils.component.ComponentLocator;

public class ComponentTestCase extends Log4jEnabledTestCase {
	protected void setUp() {
		super.setUp();
		
		Annotation[] annotations = getClass().getAnnotations();
		if(annotations != null) {
			for(Annotation annotation : annotations) {
				if(annotation instanceof ComponentSetup) {
					ComponentLocator.getLocator(
						((ComponentSetup)annotation).managerName(),
						((ComponentSetup)annotation).setupXml(),
						((ComponentSetup)annotation).log4j()
					);
					
					break;
				}
			}
		}
	}
}

