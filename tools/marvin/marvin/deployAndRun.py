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

from tcExecuteEngine import TestCaseExecuteEngine
import sys
import os
import traceback
import time
from argparse import ArgumentParser
from marvinInit import MarvinInit
from marvin.codes import (SUCCESS,
                          FAILED,
                          EXCEPTION,
                          UNKNOWN_ERROR
                          )


parser = None


def printAndExit():
    '''
    Prints pretty message for parser and exit
    '''
    global parser
    if parser is not None:
        parser.print_usage()
        exit(1)


def parseAndCheck():
    '''
    Parses,reads the options and verifies for the config file
    '''
    global parser
    parser = ArgumentParser()

    parser.add_argument("-d", "--tcpath", dest="tcpath",
                        help="the test case directory or file path")
    parser.add_argument("-c", "--config", action="store",
                        default="./datacenterCfg", dest="config",
                        help="the path where the json config file generated,\
                        by default is ./datacenterCfg")
    parser.add_argument("-l", "--load", dest="load", action="store_true",
                        help="only load config, do not deploy,\
                        it will only run testcase")
    parser.add_argument("-n", "--num", dest="number",
                        help="how many times you want to run the tests")

    options = parser.parse_args()
    cfg_file = options.config
    tc_path = options.tcpath
    load_flag = options.load
    num_iter = 1 if options.number is None else int(options.number)

    '''
    Check if the config file is None or not and exit accordingly
    '''
    if cfg_file is None:
        printAndExit()
    return {"cfg_file": cfg_file,
            "load_flag": load_flag,
            "tc_path": tc_path,
            "num_iter": num_iter}


def startMarvin(cfg_file, load_flag):
    '''
    Initialize the Marvin
    '''
    try:
        obj_marvininit = MarvinInit(cfg_file, load_flag)
        if obj_marvininit.init() == SUCCESS:
            testClient = obj_marvininit.getTestClient()
            tcRunLogger = obj_marvininit.getLogger()
            parsedConfig = obj_marvininit.getParsedConfig()
            debugStream = obj_marvininit.getDebugFile()
            return {"tc_client": testClient,
                    "tc_runlogger": tcRunLogger,
                    "tc_parsedcfg": parsedConfig,
                    "tc_debugstream": debugStream}
        else:
            print "\nMarvin Initialization Failed"
            exit(1)
    except Exception, e:
            print "\n Exception occurred while starting Marvin %s" % str(e)
            exit(1)


def runTCs(num_iter, inp1, inp2):
    '''
    Run Test Cases based upon number of iterations
    '''
    n = 0
    while(n < num_iter):
        engine = TestCaseExecuteEngine(inp2["tc_client"],
                                       inp2["tc_parsedcfg"],
                                       inp2["tc_runlogger"],
                                       inp2["tc_debugstream"])
        if inp1["tc_file"] is not None:
            engine.loadTestsFromFile(inp1["tc_file"])
        else:
            engine.loadTestsFromDir(inp1["tc_dir"])
        engine.run()
        n = n + 1


def checkTCPath(tc_path):
    '''
    Verifies if the tc_path is a folder or file and its existence
    '''
    ret = {"tc_file": None, "tc_dir": None}
    check = True
    if tc_path is None:
        printAndExit()
    else:
        if os.path.isfile(tc_path):
            ret["tc_file"] = tc_path
        elif os.path.isdir(tc_path):
            ret["tc_dir"] = tc_path
        else:
            check = False
    if check is False:
        print"\nTC Path is Invalid.So Exiting"
        exit(1)

    return ret

if __name__ == "__main__":

    '''
    1. Parse and Check
    '''
    out1 = parseAndCheck()
    print "\nStep1 :Parsing Options And Check Went Fine"

    '''
    2. Start Marvin
    '''
    out2 = startMarvin(out1["cfg_file"], out1["load_flag"])
    print "\nStep2: Marvin Initialization Went Fine"

    '''
    3. Check TC folder or Module and Path existence
    '''
    out3 = checkTCPath(out1["tc_path"])
    print "\nStep3: TC Path Check Went Fine"

    '''
    4. Run TCs
    '''
    runTCs(out1["num_iter"], out3, out2)
    print "\nStep4: TC Running Finished"
