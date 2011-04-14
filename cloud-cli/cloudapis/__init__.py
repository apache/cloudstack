
  #
  # Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
  # 
  # This software is licensed under the GNU General Public License v3 or later.
  # 
  # It is free software: you can redistribute it and/or modify
  # it under the terms of the GNU General Public License as published by
  # the Free Software Foundation, either version 3 of the License, or any later version.
  # This program is distributed in the hope that it will be useful,
  # but WITHOUT ANY WARRANTY; without even the implied warranty of
  # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  # GNU General Public License for more details.
  # 
  # You should have received a copy of the GNU General Public License
  # along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #
 

'''
Created on Aug 2, 2010

@author: rudd-o
'''

import os,pkgutil

def get_all_apis():
    apis = []
    for x in pkgutil.walk_packages([os.path.dirname(__file__)]):
        loader = x[0].find_module(x[1])
        try: module = loader.load_module("cloudapis." + x[1])
        except ImportError: continue
        apis.append(module)
    return apis

def lookup_api(api_name):
    api = None
    matchingapi = [ x for x in get_all_apis() if api_name.replace("-","_") == x.__name__.split(".")[-1] ]
    if not matchingapi: api = None
    else: api = matchingapi[0]
    if api: api = getattr(api,"implementor")
    return api

