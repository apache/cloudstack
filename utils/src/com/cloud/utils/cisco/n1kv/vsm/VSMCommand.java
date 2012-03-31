// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.cloud.utils.cisco.n1kv.vsm;

public class VSMCommand {
	String XML_command;
	
	public void setXMLCommand(String xmlCmd) {
		this.XML_command = xmlCmd;
		return;
	}
	
	public String getXMLCommand() {
		return this.XML_command;
	}
	
	// we probably should put in other functions here
	// that help construct the XML command. Or probably
	// we're better off putting them in the layers that
	// create an object of this class.
}