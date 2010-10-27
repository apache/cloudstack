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

