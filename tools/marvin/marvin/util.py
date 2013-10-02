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

from marvin.entity.template import Template
from marvin.entity.zone import Zone
from marvin.entity.serviceoffering import ServiceOffering
from marvin.entity.domain import Domain


def get_domain(apiclient):
    "Returns a default `ROOT` domain"

    domains = Domain.list(
        apiclient=apiclient,
    )
    if isinstance(domains, list) and len(domains) > 0:
        return domains[0]
    else:
        raise Exception("Failed to find any domains")


def get_zone(apiclient):
    "Returns the default enabled zone"

    zones = Zone.list(
        apiclient=apiclient,
    )
    if isinstance(zones, list) and len(zones) > 0:
        for zone in zones:
            if zone.allocationstate == 'Enabled':
                return zone
        else:
            raise Exception("No active zones found for deployment")
    else:
        raise Exception("Failed to find specified zone.")


def get_service_offering(apiclient, storagetype='shared', scope=None):
    """Returns the service offering that is available in the zone

    @param: `storagetype` is assumed to be `shared storage`
    @param: `scope` zone-wide or cluster-wide. defaults to cluster
    """
    serviceofferings = ServiceOffering.list(
        apiclient=apiclient,
        name='Small Instance'
    )
    if isinstance(serviceofferings, list) and len(serviceofferings) > 0:
        for service in serviceofferings:
            if service.storagetype == storagetype:
                return service
    raise Exception("No service offering for storagetype %s available")


def get_template(apiclient, description=None):
    "Returns a featured template with a specific description"
    templates = Template.list(
        apiclient=apiclient,
        templatefilter='featured'
    )

    if isinstance(templates, list) and len(templates) > 0:
        for template in templates:
            if template.isready:
                return template
        else:
            raise Exception(
                "None of the templates are ready in your deployment")
    else:
        raise Exception(
            "Failed to find ready and featured template of : %s" % description)
