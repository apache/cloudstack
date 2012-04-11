#!/usr/bin/env python
# Copyright 2012 Citrix Systems, Inc. Licensed under the
# Apache License, Version 2.0 (the "License"); you may not use this
# file except in compliance with the License. Citrix Systems, Inc.

from distutils.core import setup
from sys import version

if version < "2.7":
    print "Marvin needs at least python 2.7, found : \n%s"%version
else:
    try:
        import paramiko
    except ImportError:
        print "Marvin requires paramiko to be installed"
        raise
    
    setup(name="Marvin",
      version="0.1.0",
      description="Marvin - Python client for testing cloudstack",
      author="Edison Su",
      author_email="Edison.Su@citrix.com",
      maintainer="Prasanna Santhanam",
      maintainer_email="Prasanna.Santhanam@citrix.com",
      long_description="Marvin is the cloudstack testclient written around the python unittest framework",
      platforms=("Any",),
      url="http://jenkins.cloudstack.org:8080/job/marvin",
      packages=["marvin", "marvin.cloudstackAPI", "marvin.sandbox", "marvin.pymysql", "marvin.pymysql.constants", "marvin.pymysql.tests"],
      license="LICENSE.txt",
      requires=[
                "paramiko (>1.4)",
                "Python (>=2.7)"
                ]
     )
