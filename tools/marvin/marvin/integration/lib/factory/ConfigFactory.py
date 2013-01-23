import factory
from marvin.integration.lib.newbase import Config
class ConfigFactory(factory.Factory):

    FACTORY_FOR = Config

    hostname = None
    queryfilter = None
    searchbase = None
