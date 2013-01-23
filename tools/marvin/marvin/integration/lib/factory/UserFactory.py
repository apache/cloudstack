import factory
from marvin.integration.lib.newbase import User
class UserFactory(factory.Factory):

    FACTORY_FOR = User

    account = None
    email = None
    firstname = None
    lastname = None
    password = None
    username = None
