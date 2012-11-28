/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class Future2<T> {
	
	private Callable<T> callable;
	private Callable<T> success;
	private Callable<T> failed;
	private ExecutorService executor = Executors.newFixedThreadPool(2);
	private Future<T> f;
	
	public  class Func<T> implements Callable<T> {
		private Callable<T> f;
		private Callable<T> fs;
		public T func() throws Exception {
			return f.call();
		}
		
		public T success() throws Exception {
			return fs.call();
		}
		
		public Func (Callable<T> f, Callable<T> s) {
			this.f = f;
			this.fs = s;
		}
		
		
		@Override
		public T call() throws Exception {
			func();
			success();
			return null;
		}
		
	}
	public Future2 (Callable<T> callable) {
		this.callable = callable;
	}
	
	public void onSuccess(Callable<T> s) {
		this.success = s;
	}
	
	public void go() {
		Func<T> ft = new Func<T>(this.callable, this.success);
		f = executor.submit(ft);
	}
	
	public T get() {
		try {
			return this.f.get();
		} catch (InterruptedException e) {
			return null;
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}
	
	public void shutdown() {
		this.executor.shutdown();
	}
	
	public static void main(String[] args) {
		Callable<String> fun = new Callable<String> () {

			@Override
			public String call() throws Exception {
				System.out.println("execing");
				return "test";
			}
			
		};
		Future2<String> f2 = new Future2<String>(fun);
		f2.onSuccess(new Callable<String>() {

			@Override
			public String call() throws Exception {
				Thread.sleep(1000);
				System.out.println("success");
				return null;
			}
		});
		
		 f2.go();
		//f2.get();
		f2.shutdown();
	}
}
