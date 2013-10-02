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

import os
import sys
from argparse import ArgumentParser
from generate.xmltoapi import codeGenerator
from generate.apitoentity import generate


def get_api_cmds():
    """ Returns the API cmdlet instances

    @return: instances of all the API commands exposed by CloudStack
    """
    namespace = {}
    execfile('cloudstackAPI/__init__.py', namespace)
    api_classes = __import__(
        'cloudstackAPI', globals().update(namespace), fromlist=['*'], level=-1)

    cmdlist = map(
        lambda f: getattr(api_classes, f),
        filter(
            lambda t: t.startswith('__') is False,
            dir(api_classes)
        )
    )
    cmdlist = filter(
        lambda g: g is not None,
        cmdlist
    )
    clslist = map(
        lambda g: getattr(g, g.__name__.split('.')[-1] + 'Cmd'),
        filter(
            lambda h: h.__name__.split('.')[-1] not in [
                'baseCmd', 'baseResponse', 'cloudstackAPIClient'],
            cmdlist
        )
    )
    cmdlets = map(lambda t: t(), clslist)
    return cmdlets

if __name__ == "__main__":
    parser = ArgumentParser()
    parser.add_argument("-o", "--output", dest="output",
                        help="The path to the generated code entities, default\
 is .")
    parser.add_argument("-s", "--specfile", dest="spec",
                        help="The path and name of the api spec xml file,\
 default is /etc/cloud/cli/commands.xml")
    parser.add_argument("-e", "--endpoint", dest="endpoint",
                        help="The endpoint mgmt server (with open 8096) where\
 apis are discovered, default is localhost")
    parser.add_argument("-y", "--entity", dest="entity", action="store_true",
                        help="Generate entity based classes")

    options = parser.parse_args()

    folder = "."
    if options.output is not None:
        folder = options.output
    apiModule = folder + "/cloudstackAPI"
    if not os.path.exists(apiModule):
        try:
            os.mkdir(apiModule)
        except:
            print "Failed to create folder %s, due to %s" % (apiModule,
                                                             sys.exc_info())
            print parser.print_help()
            exit(2)

    apiSpecFile = "/etc/cloud/cli/commands.xml"
    if options.spec is not None:
        apiSpecFile = options.spec
        if not os.path.exists(apiSpecFile):
            print "the spec file %s does not exists" % apiSpecFile
            print parser.print_help()
            exit(1)

    cg = codeGenerator(apiModule)
    if options.spec is not None:
        cg.generateCodeFromXML(apiSpecFile)
    elif options.endpoint is not None:
        endpointUrl = 'http://%s:8096/client/api?command=listApis&\
response=json' % options.endpoint
        cg.generateCodeFromJSON(endpointUrl)

    if options.entity:
        generate(get_api_cmds())
