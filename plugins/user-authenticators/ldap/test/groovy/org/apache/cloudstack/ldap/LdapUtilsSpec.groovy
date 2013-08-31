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

import org.apache.cloudstack.ldap.LdapUtils

import javax.naming.directory.Attribute
import javax.naming.directory.Attributes

class LdapUtilsSpec extends spock.lang.Specification {
    def "Testing than an attribute is not successfully returned"() {
	given: "You have an attributes object with some attribute"
		def attributes = Mock(Attributes)
		attributes.get("uid") >> null

		when: "You get the attribute"
		String foundValue = LdapUtils.getAttributeValue(attributes, "uid")

		then: "Its value equals uid"
		foundValue == null
    }

    def "Testing than an attribute is successfully returned"() {
        given: "You have an attributes object with some attribute"
        def attributes = Mock(Attributes)
        def attribute = Mock(Attribute)
        attribute.getId() >> name
        attribute.get() >> value
        attributes.get(name) >> attribute

        when: "You get the attribute"
        String foundValue = LdapUtils.getAttributeValue(attributes, name)

        then: "Its value equals uid"
        foundValue == value

        where:
        name    | value
        "uid"   | "rmurphy"
        "email" | "rmurphy@test.com"
    }

    def "Testing that a Ldap Search Filter is correctly escaped"() {
		given: "You have some input from a user"

		expect: "That the input is escaped"
		LdapUtils.escapeLDAPSearchFilter(input) == result

		where: "The following inputs are given "
		input                                       | result
		"Hi This is a test #çà"                     | "Hi This is a test #çà"
		"Hi (This) = is * a \\ test # ç à ô \u0000" | "Hi \\28This\\29 = is \\2a a \\5c test # ç à ô \\00"
    }
}
