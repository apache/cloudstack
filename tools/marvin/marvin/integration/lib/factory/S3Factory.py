import factory
from marvin.integration.lib.newbase import S3
class S3Factory(factory.Factory):

    FACTORY_FOR = S3

    accesskey = None
    bucket = None
    secretkey = None


    FACTORY_FOR = S3
