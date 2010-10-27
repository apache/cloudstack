'''Implements the Amazon API'''


import boto.ec2
import os
from cloudtool.utils import describe,OptionConflictError,OptionValueError
raise ImportError

class AmazonAPI:
    
    @describe("access_key", "Amazon access key")
    @describe("secret_key", "Amazon secret key")
    @describe("region", "Amazon region")
    @describe("endpoint", "Amazon endpoint")
    def __init__(self,
                 access_key=os.environ.get("AWS_ACCESS_KEY_ID",None),
                 secret_key=os.environ.get("AWS_SECRET_ACCESS_KEY",None),
                 region=None,
                 endpoint=None):
        if not access_key: raise OptionValueError,"you need to specify an access key"
        if not secret_key: raise OptionValueError,"you need to specify a secret key"
        if region and endpoint:
            raise OptionConflictError,("mutually exclusive with --endpoint",'--region')
        self.__dict__.update(locals())
        
    def _get_regions(self):
        return boto.ec2.regions(aws_access_key_id=self.access_key,aws_secret_access_key=self.secret_key)

    def _get_region(self,name):
        try: return [ x for x in self._get_regions() if x.name == name ][0]
        except IndexError: raise KeyError,name

    def _connect(self):
        if self.region:
            region = self._get_region(self.region)
            self.connection =  region.connect(
                               aws_access_key_id=self.access_key,
                               aws_secret_access_key=self.secret_key
                               )
        else:
            self.connection =  boto.ec2.connection.EC2Connection(
                               host=self.endpoint,
                               aws_access_key_id=self.access_key,
                               aws_secret_access_key=self.secret_key
                               )
    def list_regions(self):
        """Lists all regions"""
        regions = self._get_regions()
        for r in regions: print r

    def get_all_images(self):
        """Lists all images"""
        self._connect()
        images = self.connection.get_all_images()
        for i in images: print i

    def get_region(self):
        """Gets the region you're connecting to"""
        self._connect()
        print self.connection.region


implementor = AmazonAPI