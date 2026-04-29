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
package groovy.org.apache.cloudstack.ldap

import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.SearchResult

class BasicNamingEnumerationImpl implements NamingEnumeration {

	private LinkedList<String> items = new LinkedList<SearchResult>();

	public void add(SearchResult item) {
		items.add(item)
	}

	@Override
	public void close() throws NamingException {
	}

	@Override
	public boolean hasMore() throws NamingException {
		return hasMoreElements();
	}

	@Override
	public boolean hasMoreElements() {
		return items.size != 0;
	}

	@Override
	public Object next() throws NamingException {
		return nextElement();
	}

	@Override
	public Object nextElement() {
		SearchResult result = items.getFirst();
		items.removeFirst();
		return result;
	}
}
