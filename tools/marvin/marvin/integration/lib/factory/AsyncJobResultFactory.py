import factory
from marvin.integration.lib.newbase import AsyncJobResult
class AsyncJobResultFactory(factory.Factory):

    FACTORY_FOR = AsyncJobResult

    jobid = None
