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

import deployDataCenter
import TestCaseExecuteEngine
import NoseTestExecuteEngine
from optparse import OptionParser
import os

if __name__ == "__main__":

    parser = OptionParser() #TODO: deprecate and use the argparse module
  
    parser.add_option("-c", "--config", action="store", default="./datacenterCfg", dest="config", help="the path where the json config file generated, by default is ./datacenterCfg")
    parser.add_option("-d", "--directory", dest="testCaseFolder", help="the test case directory")
    parser.add_option("-r", "--result", dest="result", help="test result log file")
    parser.add_option("-t", "--client", dest="testcaselog", help="test case log file")
    parser.add_option("-l", "--load", dest="load", action="store_true", help="only load config, do not deploy, it will only run testcase")
    parser.add_option("-f", "--file", dest="module", help="run tests in the given file")
    parser.add_option("-n", "--nose", dest="nose", action="store_true", help="run tests using nose")
    parser.add_option("-x", "--xml", dest="xmlrunner", help="use the xml runner to generate xml reports and path to store xml files")
    (options, args) = parser.parse_args()
    
    testResultLogFile = None
    if options.result is not None:
        testResultLogFile = options.result
    
    testCaseLogFile = None
    if options.testcaselog is not None:
        testCaseLogFile = options.testcaselog
    deploy = deployDataCenter.deployDataCenters(options.config)    
    if options.load:
        deploy.loadCfg()
    else:
        deploy.deploy()
        
    format = "text"
    xmlDir = None
    if options.xmlrunner is not None:
        xmlDir = options.xmlrunner
        format = "xml"
    
    if options.testCaseFolder is None:
        if options.module is None:
            parser.print_usage()
            exit(1)
        else:
            if options.nose:
                engine = NoseTestExecuteEngine.NoseTestExecuteEngine(deploy.testClient, testCaseLogFile, testResultLogFile, format, xmlDir)
                engine.runTestsFromFile(options.module)
            else:
                engine = TestCaseExecuteEngine.TestCaseExecuteEngine(deploy.testClient, testCaseLogFile, testResultLogFile, format, xmlDir)
                engine.loadTestsFromFile(options.module)
                engine.run()
    else:
        if options.nose:
            engine = NoseTestExecuteEngine.NoseTestExecuteEngine(deploy.testClient, clientLog=testCaseLogFile, resultLog=testResultLogFile, workingdir=options.testCaseFolder, format=format, xmlDir=xmlDir)
            engine.runTests()
        else:
           engine = TestCaseExecuteEngine.TestCaseExecuteEngine(deploy.testClient, testCaseLogFile, testResultLogFile, format, xmlDir)
           engine.loadTestsFromDir(options.testCaseFolder)
           engine.run()
