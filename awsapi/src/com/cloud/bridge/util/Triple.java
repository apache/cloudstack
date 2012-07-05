// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.bridge.util;

/** 
 * Reusable class whose instances encode any triple (or 3-tuple) of values of types T1, T2 and T3
 * Provide getters: getFirst(), getSecond(), getThird()
 * Provide setters: setFirst(val), setSecond(val), setThird(val)
 * @param <T1>
 * @param <T2>
 * @param <T3>
 */
public class Triple <T1, T2, T3> {
	T1 first;
	T2 second;
	T3 third;

	public Triple(T1 t1, T2 t2, T3 t3) {
		first = t1;
		second = t2;
		third = t3;
	}
	
	public T1 getFirst() {
		return first;
	}
	
	public Triple<T1, T2, T3> setFirst(T1 t1) {
		first = t1;
		return this;
	}
	
	public T2 getSecond() {
		return second;
	}
	
	public Triple<T1, T2, T3> setSecond(T2 t2) {
		second = t2;
		return this;
	}
	
	public T3 getThird() {
		return third;
	}
	
	public Triple<T1, T2, T3> setThird(T3 t3) {
		third = t3;
		return this;
	}
}
