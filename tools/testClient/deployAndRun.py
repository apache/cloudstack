import deployDataCenter
import TestCaseExecuteEngine
from optparse import OptionParser
import os
if __name__ == "__main__":
    parser = OptionParser()
  
    parser.add_option("-c", "--config", action="store", default="./datacenterCfg", dest="config", help="the path where the json config file generated, by default is ./datacenterCfg")
    parser.add_option("-d", "--directory", dest="testCaseFolder", help="the test case directory")
    parser.add_option("-r", "--result", dest="result", help="test result log file")
    parser.add_option("-t", dest="testcaselog", help="test case log file")
    (options, args) = parser.parse_args()
    if options.testCaseFolder is None:
        parser.print_usage()
        exit(1)
        
    testResultLogFile = None
    if options.result is not None:
        testResultLogFile = options.result
    
    testCaseLogFile = None
    if options.testcaselog is not None:
        testCaseLogFile = options.testcaselog
    deploy = deployDataCenter.deployDataCenters(options.config)    
    deploy.loadCfg()
    
    testcaseEngine = TestCaseExecuteEngine.TestCaseExecuteEngine(deploy.testClient, options.testCaseFolder, testCaseLogFile, testResultLogFile)
    testcaseEngine.run()