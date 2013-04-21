# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import factory
import inspect

CREATORS = ["create", "deploy"]


class CloudStackBaseFactory(factory.Factory):
    ABSTRACT_FACTORY = True

    @classmethod
    def _build(cls, target_class, *args, **kwargs):
        if len(args) == 0:
            return target_class(kwargs)
        return target_class(*args, **kwargs)

    @classmethod
    def _create(cls, target_class, *args, **kwargs):
        if cls.apiclient:
            members = inspect.getmembers(target_class,
                predicate=inspect.ismethod)
            creators = filter(lambda x: x[0] in CREATORS, members)
            assert creators, "How do I bring this guy into existence?"
            assert inspect.ismethod(creators[0][1])
            creator = creators[0][1]
            return creator(cls.apiclient, factory=cls._build(target_class,
                *args, **kwargs))
        else:
            cls._build(target_class, *args, **kwargs)

    @classmethod
    def _adjust_kwargs(cls, **kwargs):
        if "apiclient" in kwargs:
            cls.apiclient = kwargs["apiclient"]
            clean_kwargs = dict((k, v) for k, v in kwargs.iteritems()
                if k != "apiclient")
            return clean_kwargs
        else:
            return kwargs