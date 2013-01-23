import factory
from marvin.integration.lib.newbase import CustomCertificate
class CustomCertificateFactory(factory.Factory):

    FACTORY_FOR = CustomCertificate

    certificate = None
    domainsuffix = None
