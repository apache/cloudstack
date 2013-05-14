# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import unittest
import HTMLTestRunner
import xmlrunner


global DRIVER
global MS_ip

# Import test cases

##################################
from Login_and_Accounts import *
from Service_Offering import *

from TemplatesAndISO import *
from VM_lifeCycle import *

###################################


# Following are BVT Tests
# serialize the test cases


suite = unittest.TestSuite() # setup new test suite


####################################################################################################

# Following logs admin user in and creates test account then logs admin user out and logs in as test to run tests.
# You should leave this as is for all the tests.

suite.addTest(unittest.makeSuite(login)) #Login Admin    

time.sleep(5)
suite.addTest(unittest.makeSuite(createAcc)) # Create an Account test. We will use test account for all our tests

time.sleep(5)
suite.addTest(unittest.makeSuite(logout)) #Logout Admin    

time.sleep(5)
suite.addTest(unittest.makeSuite(login_test)) # Login Test



####################################################################################################



time.sleep(5)
suite.addTest(unittest.makeSuite(Disk_offering_Add))

time.sleep(5)
suite.addTest(unittest.makeSuite(Disk_offering_Edit))

time.sleep(5)
suite.addTest(unittest.makeSuite(Disk_offering_Delete))

time.sleep(5)
suite.addTest(unittest.makeSuite(Compute_offering_Add))

time.sleep(5)
suite.addTest(unittest.makeSuite(Compute_offering_Edit))

time.sleep(5)
suite.addTest(unittest.makeSuite(Compute_offering_Delete))


# time.sleep(5)
# suite.addTest(unittest.makeSuite(deployVM))

# time.sleep(5)
# suite.addTest(unittest.makeSuite(stopVM))

# time.sleep(5)
# suite.addTest(unittest.makeSuite(startVM))

# time.sleep(5)
# suite.addTest(unittest.makeSuite(destroyVM))

# time.sleep(5)
# suite.addTest(unittest.makeSuite(restoreVM)) 


# time.sleep(5)
# suite.addTest(unittest.makeSuite(Template_Add)) 

# time.sleep(5)
# suite.addTest(unittest.makeSuite(Template_Edit)) 

# time.sleep(5)
# suite.addTest(unittest.makeSuite(Template_Delete)) 


####################################################################################################

# Following logs test user out and logs back in as Admin and tears down the test account.
# You should leave this as is for all the tests.

suite.addTest(unittest.makeSuite(logout)) #Logout test
time.sleep(5)
suite.addTest(unittest.makeSuite(login)) #Login Admin
time.sleep(5)
suite.addTest(unittest.makeSuite(tearAcc))  # Delete Account test

####################################################################################################



# If XML reports compatible with junit's XML output are desired then leave folowing code as is.
# If HTML reports are desired follow instructions


#Comment following line for HTML and uncomment for XML
runner = xmlrunner.XMLTestRunner(output='test-reports')

#Comment following line for XML and uncomment for HTML
#runner = HTMLTestRunner.HTMLTestRunner() 

#header is required for displaying the website
#Comment following line for XML and uncomment for HTML
#print "Content-Type: text/html\n" 

# Leave following as is for either XML or HTML
runner.run(suite)



