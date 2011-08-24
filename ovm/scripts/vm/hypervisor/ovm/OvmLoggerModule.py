'''
Created on May 19, 2011

@author: frank
'''
import logging

class OvmLogger(object):
    '''
    classdocs
    '''


    def __init__(self, className):
        '''
        Constructor
        '''
        self.className = className
        self.logger = logging.getLogger(className)
    
    def info(self, func, msg=None):
        assert callable(func), "%s is not a function"%func
        fmt = "[%s.%s]: "%(self.className, func.__name__)
        self.logger.info("%s%s"%(fmt,msg))
    
    def debug(self, func, msg=None):
        assert callable(func), "%s is not a function"%func
        fmt = "[%s.%s]: "%(self.className, func.__name__)
        self.logger.debug("%s%s"%(fmt,msg))
    
    def error(self, func, msg=None):
        assert callable(func), "%s is not a function"%func
        fmt = "[%s.%s]: "%(self.className, func.__name__)
        self.logger.error("%s%s"%(fmt,msg))
    
    def warning(self, func, msg=None):
        assert callable(func), "%s is not a function"%func
        fmt = "[%s.%s]: "%(self.className, func.__name__)
        self.logger.warning("%s%s"%(fmt,msg))