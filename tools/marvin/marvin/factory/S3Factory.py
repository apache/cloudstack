import factory
from marvin.base import S3
class S3Factory(factory.Factory):
    FACTORY_FOR = S3.S3

    accesskey = None
    bucket = None
    secretkey = None
