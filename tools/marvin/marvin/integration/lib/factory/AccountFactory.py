import factory
from marvin.integration.lib.newbase import Account
class AccountFactory(factory.Factory):

    FACTORY_FOR = Account

    accounttype = None
    email = None
    firstname = None
    lastname = None
    password = None
    username = None
