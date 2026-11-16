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

from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import wait_until
from marvin.lib.base import (Account,
                             Project,
                             UserData,
                             SslCertificate,
                             Template,
                             NetworkOffering,
                             ServiceOffering,
                             VirtualMachine,
                             Network,
                             VPC,
                             VpcOffering,
                             PublicIPAddress,
                             LoadBalancerRule)
from marvin.lib.common import (get_domain, get_zone, get_test_template)
from nose.plugins.attrib import attr

import os
import subprocess
import logging


_multiprocess_shared_ = True

DOMAIN = "test-ssl-offloading.cloudstack.org"
CONTENT = "Test page"
FULL_CHAIN = "/tmp/full_chain.crt"

CERT = {
    "privatekey": """-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCph7jsoMCQirRn
3obuvgnnefTXRQYd9tF9k2aCVkTiiisvC39px7MGdgvDXADhD9fmR7oyXVQlfNu0
rXjjgsVT3r4bv+DVi81YGXnuU7h10yCOZJt21i6QGHN1CS0/TAfg0UhlACCEYNRx
kB0klwUcj/jk/AKil1DoUGpvAm2gZsek/njb76/AeIfxc+Es4ZOPCVqQOHp6gI0q
t6KDMkUwv8fyzrpScygMUPVYrLmm6D0pn8yd3ihW07wGxMjND6UgOnao8t6H3LaM
Pe7eqSFzxunF9NFFjnUrKcHZZSledDM/37Kbqb/8T5f+4SwjioS1OdPCh8ApdiXq
HNUwYkALAgMBAAECggEAK5JiiQ7X7053B6s96uaVDRVfRGTNKa5iMXBNDHq3wbHZ
X4IJAVr+PE7ivxdKco3r45fT11X9ZpUsssdTJsZZiTDak69BTiFcaaRCnmqOIlpd
J7vb6TMrTIW8RvxQ0M/txm6DuNHLibqJX5a2pszZ13l5cwECfF9/v/XLJTTukCbu
6D/f3fBVFl1tM8y9saOEYLkdb4dILWY61bVSDNswgprz2EV1SFnk5jxz2FuBrM/Q
+7hINvjDcaRvcm59hRb1rkljv7S10VoNw/CFkU451csJkUe4vWZwB8lZK/XxLQG0
HEdS1zU1XY8H8Y1RCrxjGRyiiWsBtUThhWYlPrGCoQKBgQDkP09YAlKqXhT69Kx5
keg2i1jV2hA73zWbWXt9xp5jG5r3pl3m170DvKL93YIDnHtpTC56mlzGrzS7DSTN
p0buY9Qb3fkJxunCpPVFo0HMFkpeR77ax0v34NzSohlRLKFo5R2M1cmDfbVbnSSl
MB57FfRRMxzjrk+dJvjOeJsxjwKBgQC+JLb4B8CZjpurXYg3ySiRqFsCqkqob+kf
9dR+rWvcR6vMTEyha0hUlDvTikDepU2smYR4oPHfdcXF9lAJ7T02UmQDeizAqR68
u9e+yS0q3tdRnPPZmXJfaDCXG1hKMqF4YA5Vs0XAjleF3zHB+vBLrnlPpShtd/Mu
sWTpxICTxQKBgQDSr/n+pE5IQwYczOO0aFGwn5pF9L9NdPHXz5aleETV+TJn7WL6
ZiRsoaDWs7SCvtxQS2kP9RM0t5/2FeDmEMXx4aZ2fsSWGM3IxVo+iL+Aswa81n8/
Ff5y9lb/+29hNdBcsjk/ukwEG3Lf+UNNVAie15oppgPByzJkPwgmFsAy0wKBgHDX
/TZp82WuerhSw/rHiSoYjhqg0bnw4Ju1Gy0q4q5SYqTWS0wpDT4U0wSSMjlwRQ6/
9RxZ9/G0RXFc4tdhUkig0PY3VcPpGnLL0BhL8GBW69ZlnVpwdK4meV/UPKucLLPx
3dACmszSLSMn+LG0qVNg8mHQFJQS8eGuKcOKePw5AoGACuxtefROKdKOALh4lTi2
VOwPZ+1jxsm6lKNccIEvbUpe3UXPgNWpJiDX8mUcob4/NBLzmV3BUVKbG7Exbo5J
LoMfp7OsztWUFwt7YAvRfS8fHdhkEsxEf3T72ADieH5ZAuXFF+K0H3r6HtWPD4ws
mTJjGP4+Bl/dFakA5FJcjHg=
-----END PRIVATE KEY-----""",
    "certificate": """-----BEGIN CERTIFICATE-----
MIIFKjCCAxKgAwIBAgIUJ7BtN56KI8OuzbbM8SdtCLCB2UgwDQYJKoZIhvcNAQEL
BQAwXjELMAkGA1UEBhMCWFgxCzAJBgNVBAgMAlhYMQswCQYDVQQHDAJYWDETMBEG
A1UECgwKQ2xvdWRTdGFjazEPMA0GA1UECwwGQXBhY2hlMQ8wDQYDVQQDDAZBcGFj
aGUwHhcNMjUwNjIzMTMxMzA3WhcNMzUwNjIxMTMxMzA3WjBoMQswCQYDVQQGEwJY
WDELMAkGA1UECAwCWFgxCzAJBgNVBAcMAlhYMQ8wDQYDVQQKDAZBcGFjaGUxEzAR
BgNVBAsMCkNsb3VkU3RhY2sxGTAXBgNVBAMMECouY2xvdWRzdGFjay5vcmcwggEi
MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCph7jsoMCQirRn3obuvgnnefTX
RQYd9tF9k2aCVkTiiisvC39px7MGdgvDXADhD9fmR7oyXVQlfNu0rXjjgsVT3r4b
v+DVi81YGXnuU7h10yCOZJt21i6QGHN1CS0/TAfg0UhlACCEYNRxkB0klwUcj/jk
/AKil1DoUGpvAm2gZsek/njb76/AeIfxc+Es4ZOPCVqQOHp6gI0qt6KDMkUwv8fy
zrpScygMUPVYrLmm6D0pn8yd3ihW07wGxMjND6UgOnao8t6H3LaMPe7eqSFzxunF
9NFFjnUrKcHZZSledDM/37Kbqb/8T5f+4SwjioS1OdPCh8ApdiXqHNUwYkALAgMB
AAGjgdUwgdIwKwYDVR0RBCQwIoIQKi5jbG91ZHN0YWNrLm9yZ4IOY2xvdWRzdGFj
ay5vcmcwHQYDVR0OBBYEFCcq7jrdsqTD+Xi85DCqjYdL1gOqMIGDBgNVHSMEfDB6
oWKkYDBeMQswCQYDVQQGEwJYWDELMAkGA1UECAwCWFgxCzAJBgNVBAcMAlhYMRMw
EQYDVQQKDApDbG91ZFN0YWNrMQ8wDQYDVQQLDAZBcGFjaGUxDzANBgNVBAMMBkFw
YWNoZYIURVB9+qvRJyOnJnqmYOw467vW3vQwDQYJKoZIhvcNAQELBQADggIBACld
lEXgn/A4/kZQbLwwMxBvaoPDDaDaYVpPbOoPw7a8YkrL0rmPIc04PyX9GAqxdC+c
qaEXvmp3I+BdT13XGcBosXO8uEQ3kses9F3MhOHORPS2mJag7t4eLnNX/0CgKTlR
6yC2Gu7d3xPNJ+CKMxekdoF31StEFNAYI/La/q3D+IGsRCbrVu3xpPaw2XlXI7Ro
RU7yebVmQPSNc75bm8Ydo1cdYtz9h8PVnc+6ThhSrdS3jYScj9DrX5ZJaKuZqSlu
0ZqFXoBflme+cYB7nb9HqnIO67r9vzd2dTcErJVAk5jQqG5Y38d1tingDx1A5opU
z4BkXEbHNV6VXYUQ5VE0dXO2sNvXVJrstwMPE8d3EvbX/1gWj8kuymbskrCjySE4
4Yztkb0dsJkVU793lz3EV75DsXvj3gevK049nPv2Grt1+rTgFNa6NJnLvKIKk/mv
fWjxbK2b/AAJ1ci6xtw/vKmIWoEu6uEMIJmhfBwuP+VnVJWJbmYXpNW/L5g21B76
Fn8RuQa3mlm5lZrxEcJ/b6fF+2NPJwj7sh6l688VtNXoVSSyXUeV5HwqCv+YMjKn
CtwpEN/eNHMbrkJvgYwSoOzqhV/wpmNi28S7MOm66JMECHOXOhk/eX2chIEjiVna
MXhvr/Twfj2N4gNVtcgXkrk39HEYjk5+uF7SdNf4
-----END CERTIFICATE-----""",
    "certchain": """-----BEGIN CERTIFICATE-----
MIIFQzCCAysCFEVQffqr0ScjpyZ6pmDsOOu71t70MA0GCSqGSIb3DQEBCwUAMF4x
CzAJBgNVBAYTAlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxEzARBgNVBAoM
CkNsb3VkU3RhY2sxDzANBgNVBAsMBkFwYWNoZTEPMA0GA1UEAwwGQXBhY2hlMB4X
DTI1MDYxNjEwMjc1NloXDTMwMDYxNTEwMjc1NlowXjELMAkGA1UEBhMCWFgxCzAJ
BgNVBAgMAlhYMQswCQYDVQQHDAJYWDETMBEGA1UECgwKQ2xvdWRTdGFjazEPMA0G
A1UECwwGQXBhY2hlMQ8wDQYDVQQDDAZBcGFjaGUwggIiMA0GCSqGSIb3DQEBAQUA
A4ICDwAwggIKAoICAQCLiQmSjrht15R1F+r79m/LZN5hsfQBGp+dy+yrtsWfOOur
RdXAwgbLxxsyKMQKWCQxlRI7wdhqh0L0ZBrIr9MjltYqsqLAoLmgY4eG/f6G8YGr
O/rxzfwTLbCeaIseF/OMA6Sz125HXYp1bltYK4LsuC7tihZXbeVa5pUGs3Jwgcfx
LYm4eB42Hp7Eg05uL8LbwT/1AjcwoWkTewKAWXA83zgLRDFDbl1t0IPHI4cdVvia
BNwNbG49ZCF6OgmokSarQSe4Vbems1u9T9pAySXAVjEYBqFjKWyswpdr782uNLmB
lCGm0pDeJ9/WASxbTJr7k9H6ZpnaHr54DG6ZqennWMz8w6r2pf7bp/EGZ3mZQ4s3
5ylSP4cQt8CSSI8k2CflPGUyytUAiWlDS3qSyIuAOPKXDg7wIpcbwcu4VMeKnH0Z
x7Uu9j1UDZEZoSu6UI/VInTl47k1/ECD+AO9yBzZSv+pTQmO3/Im3CcxsTHmVd5s
Tl0CJ/jWNpo9DAMtmGvt6CBWBXGRsO2XNk7djRcq2CubiCpvODg+7CcR6CiZK73L
1aOisLiq3+ofiJSSXRRuKtJlkQ4eSPSbYWkNJcKmIhbCoYOdH/Pe3/+RHjvNc1kO
OUb+icmfzcMVAs3C5jybpazsfjDNQZXWAFx4FLDcqOVbrCwom+tMukw+hzlZnwID
AQABMA0GCSqGSIb3DQEBCwUAA4ICAQAdexoMwn+Ol1A3+NOHk9WJZX+t2Q8/9wWb
K+jSVleSfXXWsB1mC3fABVJQdCxtXCH3rpt4w7FK6aUes9VjqAHap4tt9WBq0Wqq
vvMURFHfllxEM31Z35kBOCSQY9xwpGV1rRh/zYs4h55YixomR3nXNZ9cI8xzlSCi
sMG0mv0y+yxPohKrZj3AzLYz/M11SimSoyRPIANI+cUg1nJXyQoHzVHWEp1Nj0HB
M/GW05cxsWea7f5YcAW1JQI3FOkpwb72fIZOtMDa4PO8IYWXJAeAc/chw745/MTi
Rvl2NT4RZBAcrSNbhCOzRPG/ZiG+ArQuCluZ9HHAXRBMTtlLk5DO4+XxZlyGpjwf
uKniK8dccy9uU0ho73p9SNDhXH0yb9Naj8vd9NWzCUYaaBXt/92cIyhaAHAVFxJu
o6jr2FLbnhSGF9EO/tHvF7LxZv1dnbInvlWHwoFQjwmoeB+e17lHBdPMnWnPKBZe
jA2VH/IzGCucWuWQhruummO5GT8Z6F4jBwvafBo+QARKPZgEBpx3LycXrpkYI3LT
GGOpGCxFt5tVZOEsC/jQ5rIljNSeTzWmzfNRn/yRUW97uWsrzcQIBAUtu/pQnyFQ
WCnC1ipCp1zhJsXAFUKuqEfLngXodOvC4tAOr76h11S57o5lN4506Poq2mWgAZe/
JZr9MEn1+w==
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
MIIFnzCCA4egAwIBAgIUcUNMqgWoDLsvMj0YmEudj60EG5swDQYJKoZIhvcNAQEL
BQAwXjELMAkGA1UEBhMCWFgxCzAJBgNVBAgMAlhYMQswCQYDVQQHDAJYWDETMBEG
A1UECgwKQ2xvdWRTdGFjazEPMA0GA1UECwwGQXBhY2hlMQ8wDQYDVQQDDAZBcGFj
aGUwIBcNMjUwNjE2MTAyNzM2WhgPMjEyNTA1MjMxMDI3MzZaMF4xCzAJBgNVBAYT
AlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxEzARBgNVBAoMCkNsb3VkU3Rh
Y2sxDzANBgNVBAsMBkFwYWNoZTEPMA0GA1UEAwwGQXBhY2hlMIICIjANBgkqhkiG
9w0BAQEFAAOCAg8AMIICCgKCAgEAwVQaePulUM523gKw168ToYp+gt05bXbu4Gg8
uaRDKhnRAX1sEgYwkQ36Q+iTDEM9sKRma8lMNMIqkZMQdk6sIGX6BL+6wUOb7mL0
5+I0yO9i8ooaGgNaeNvZftNIRlLsnPMGJaeom2/66XV4CsMqoZKaJ1H/I8N+bAeD
GvrBx+B4l9D3G390nQvot9JUzrJgGuLl0KDHapvhlR39cCgEfIii02uX1iy0qXlV
b+G1kLvpeC7T+lsJxondPJ69aO3lbDv/izyWw7qqBC57UhT/oKDxJmjQqklqzhgt
nM/p3YE7M0nkRi3LnRmsZBz7o1DRf+M29zypKzXVk1aJflL46AtLMmpDIzVrEB2M
q7o47rstXusYRYsBCqGTgdI1fV/CkDsZY5XkPZh2dsjZCHIS4P03OqFGsc6PQha2
+y2AhV1pvywkDl48kPKSukHfV1RtaPZUZtcQKztwHH+aFfo9mD8z0H2HcExdXKzd
jhRhI9ZSwFj3HEN9f5P8fS3lf5+fV7EEbG4NisieBj/UivW6QiTHpLD7wRLIUt2g
XgXNF0lfJzYHbIcxQ6kfC5McU2fu6mUC+p/pNN8G0POS3S2T55tEUqLL4N0SadQy
N1TZlTd2xTn+Hb6WlG0f5m97xGcNlGHKBvntFrHvOIfkEQ9ne3MlOO1Gjlintowo
fRGf15kCAwEAAaNTMFEwHQYDVR0OBBYEFM4WEQJpN9M07Q8CHq+5owG93Dj8MB8G
A1UdIwQYMBaAFM4WEQJpN9M07Q8CHq+5owG93Dj8MA8GA1UdEwEB/wQFMAMBAf8w
DQYJKoZIhvcNAQELBQADggIBABr5RKGc/rKx4fOgyXNJR4aCJCTtPZKs4AUCCBYz
cWOjJYGNvThOPSVx51nAD8+YP2BwkOIPfhl2u/+vQSSbz4OlauXLki2DUN8E2OFe
gJfxPDzWOfAo/QOJcyHwSlnIQjiZzG2lK3eyf5IFnfQILQzDvXaUYvMBkl2hb5Q7
44H6tRw78uuf/KsT4rY0bBFMN5DayjiyvoIDUvzCRqcb2KOi9DnZ7pXjduL7tO0j
PhlQ24B77LVUUAvydIGUzmbhGC2VvY1qE7uaYgYtgSUZ0zSjJrHjUjVLMzRouNP7
jpbBQRAcP4FDcOFZBHogunA0hxQdm0d8u3LqDYPNS0rpfW0ddU/72nfBX4bnoDEN
+anw4wOgFuUcoEThALWZ9ESVKxXQ9Fpvd6FRW8fLLqhXAuli1BqP1c1WRxagldYe
nPGm/FGZyJ2xOak9Uigi9NAQ/vX6CEfgcJgFZmCo8EKH0d4Ut72vGUcPqiUhT2EI
AFAd6drSyoUdXXniSMWky9Vrt+qtLuAD1nhHTv8ZPdItXokoiD6ea/4xrbUZn0qY
lLMDyfY76UVF0ruTR2Q6IdSq/zSggdwgkTooOW4XZcRf5l/ZnoeVQ1QH9C85SIKH
IKZwPeGUm+EntmpuCBDmQSHLRCGEThd64iOAjqLR6arLj4TBJzBrZsGHFJbm0OcI
dwa9
-----END CERTIFICATE-----""",
    "enabledrevocationcheck": False
}

# Install apache2 via userdata
USER_DATA="""I2Nsb3VkLWNvbmZpZwpydW5jbWQ6CiAgLSBzdWRvIGFwdC1nZXQgdXBkYXRlCiAgLSBzdWRvIGFw
dC1nZXQgaW5zdGFsbCAteSBhcGFjaGUyCiAgLSBzdWRvIHN5c3RlbWN0bCBlbmFibGUgYXBhY2hl
MgogIC0gc3VkbyBzeXN0ZW1jdGwgc3RhcnQgYXBhY2hlMgogIC0gZWNobyAiVGVzdCBwYWdlIiB8
c3VkbyB0ZWUgL3Zhci93d3cvaHRtbC90ZXN0Lmh0bWwK"""
#   #cloud-config
#   runcmd:
#   - sudo apt-get update
#   - sudo apt-get install -y apache2
#   - sudo systemctl enable apache2
#   - sudo systemctl start apache2
#   - echo "Test page" |sudo tee /var/www/html/test.html

class TestSslOffloading(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        testClient = super(TestSslOffloading, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls._cleanup = []

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.hypervisor = testClient.getHypervisorInfo()

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        # Save full chain as a file
        with open(FULL_CHAIN, "w", encoding="utf-8") as f:
            f.write(CERT["certchain"])

        # Register template if needed
        if cls.hypervisor.lower() == 'simulator':
            cls.template = get_test_template(
                cls.apiclient,
                cls.zone.id,
                cls.hypervisor)
        else:
            cls.template = Template.register(
                cls.apiclient,
                cls.services["test_templates_cloud_init"][cls.hypervisor.lower()],
                zoneid=cls.zone.id,
                hypervisor=cls.hypervisor,
            )
            cls.template.download(cls.apiclient)
            cls._cleanup.append(cls.template)

        if cls.template == FAILED:
            assert False, "get_test_template() failed to return template"

        # Create service offering
        cls.service_offering = ServiceOffering.create(
                                        cls.apiclient,
                                        cls.services["service_offerings"]["big"]    # 512MB memory
                                        )
        cls._cleanup.append(cls.service_offering)

        # Create network offering
        cls.services["isolated_network_offering"]["egress_policy"] = "true"
        cls.network_offering = NetworkOffering.create(cls.apiclient,
                                                      cls.services["isolated_network_offering"],
                                                      conservemode=True)
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls._cleanup.append(cls.network_offering)

        #Create an account, network, VM and IP addresses
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.user = cls.account.user[0]
        cls.userapiclient = cls.testClient.getUserApiClient(cls.user.username, cls.domain.name)

        cls.logger = logging.getLogger("TestSslOffloading")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        super(TestSslOffloading, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestSslOffloading, cls).tearDownClass()
        # Remove full chain file
        if os.path.exists(FULL_CHAIN):
            os.remove(FULL_CHAIN)

    def wait_for_service_ready(self, command, expected, retries=60):
        output = None
        self.logger.debug("======================================")
        self.logger.debug("Checking output of command '%s', expected result: '%s'" % (command, expected))
        def check_output():
            try:
                output = subprocess.check_output(command + ' 2>&1', shell=True).strip().decode('utf-8')
            except Exception as e:
                self.logger.debug("Failed to get output of command '%s': '%s'" % (command, e))
                if expected is None:
                    self.logger.debug("But it is expected")
                    return True, None
                return False, None
            self.logger.debug("Output of command '%s' is '%s'" % (command, output))
            if expected is None:
                self.logger.debug("But it is expected to be None")
                return False, None
            return (expected in output), None

        res, _ = wait_until(10, retries, check_output)
        if not res:
            self.fail("Failed to wait for http server to show content '%s'. The output is '%s'" % (expected, output))

    @attr(tags = ["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_ssl_offloading_isolated_network(self):
        """Test to create Load balancing rule with SSL offloading"""

        # Validate:
        # 1. Create isolated network and vm instance
        # 2. create LB with port 80 -> 80, verify the website (should get expected content)
        # 3. create LB with port 443 -> 80, verify the website (should not work)
        # 4. add cert to LB with port 443
        # 5. verify the website (should get expected content)
        # 6. remove cert from LB with port 443
        # 7. delete SSL certificate

        # Register Userdata
        self.userdata = UserData.register(self.apiclient,
                                         name="test-userdata",
                                         userdata=USER_DATA,
                                         account=self.account.name,
                                         domainid=self.account.domainid
                                         )

        # Upload SSL Certificate
        self.sslcert = SslCertificate.create(self.apiclient,
                                            CERT,
                                            name="test-ssl-certificate",
                                            account=self.account.name,
                                            domainid=self.account.domainid)

        # 1. Create network
        self.network = Network.create(self.apiclient,
                                      zoneid=self.zone.id,
                                      services=self.services["network"],
                                      domainid=self.domain.id,
                                      account=self.account.name,
                                      networkofferingid=self.network_offering.id)
        self.cleanup.append(self.network)

        self.services["virtual_machine"]["networkids"] = [str(self.network.id)]

        # Create vm instance
        self.vm_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            userdataid=self.userdata.userdata.id,
            serviceofferingid=self.service_offering.id
        )
        self.cleanup.append(self.vm_1)

        self.public_ip = PublicIPAddress.create(
            self.apiclient,
            self.account.name,
            self.zone.id,
            self.account.domainid,
            self.services["virtual_machine"],
            self.network.id)

        # 2. create LB with port 80 -> 80, verify the website (should get expected content).
        # firewall is open by default
        lb_http = {
            "name": "http",
            "alg": "roundrobin",
            "privateport": 80,
            "publicport": 80,
            "protocol": "tcp"
        }
        lb_rule_http = LoadBalancerRule.create(
            self.apiclient,
            lb_http,
            self.public_ip.ipaddress.id,
            accountid=self.account.name,
            domainid=self.domain.id,
            networkid=self.network.id
        )
        lb_rule_http.assign(self.apiclient, [self.vm_1])
        command = "curl -sL --connect-timeout 3 http://%s/test.html" % self.public_ip.ipaddress.ipaddress
        # wait 10 minutes until the webpage is available. it returns "503 Service Unavailable" if not available
        self.wait_for_service_ready(command, CONTENT, 60)

        # 3. create LB with port 443 -> 80, verify the website (should not work)
        # firewall is open by default
        lb_https = {
            "name": "https",
            "alg": "roundrobin",
            "privateport": 80,
            "publicport": 443,
            "protocol": "ssl"
        }
        lb_rule_https = LoadBalancerRule.create(
            self.apiclient,
            lb_https,
            self.public_ip.ipaddress.id,
            accountid=self.account.name,
            domainid=self.domain.id,
            networkid=self.network.id
        )
        lb_rule_https.assign(self.apiclient, [self.vm_1])

        command = "curl -L --connect-timeout 3 -k --resolve %s:443:%s https://%s/test.html" % (DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, None, 1)

        command = "curl -L --connect-timeout 3 --resolve %s:443:%s https://%s/test.html" % (DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, None, 1)

        # 4. add cert to LB with port 443
        lb_rule_https.assignCert(self.apiclient, self.sslcert.id)

        # 5. verify the website (should get expected content)
        command = "curl -L --connect-timeout 3 --resolve %s:443:%s https://%s/test.html" % (DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, None, 1)

        command = "curl -sL --connect-timeout 3 -k --resolve %s:443:%s https://%s/test.html" % (DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, CONTENT, 1)

        command = "curl -sL --connect-timeout 3 --cacert %s --resolve %s:443:%s https://%s/test.html" % (FULL_CHAIN, DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, CONTENT, 1)

        # 6. remove cert from LB with port 443
        lb_rule_https.removeCert(self.apiclient)

        # 7. delete SSL certificate
        self.sslcert.delete(self.apiclient)

    @attr(tags = ["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_02_ssl_offloading_project_vpc(self):
        """Test to create Load balancing rule with SSL offloading in VPC in user project"""

        # Validate:
        # 1. Create VPC, VPC tier and vm instance
        # 2. create LB with port 80 -> 80, verify the website (should get expected content)
        # 3. create LB with port 443 -> 80, verify the website (should not work)
        # 4. add cert to LB with port 443
        # 5. verify the website (should get expected content)
        # 6. remove cert from LB with port 443
        # 7. delete SSL certificate

        # Create project by user
        self.project = Project.create(
            self.userapiclient,
            self.services["project"]
        )
        self.cleanup.append(self.project)

        # Register Userdata by user
        self.userdata = UserData.register(self.userapiclient,
                                          name="test-user-userdata",
                                          userdata=USER_DATA,
                                          projectid=self.project.id
                                          )

        # Upload SSL Certificate by user
        self.sslcert = SslCertificate.create(self.userapiclient,
                                             CERT,
                                             name="test-user-ssl-certificate",
                                             projectid=self.project.id
                                             )

        # 1. Create VPC and VPC tier
        vpcOffering = VpcOffering.list(self.userapiclient, name="Default VPC offering")
        self.assertTrue(vpcOffering is not None and len(
            vpcOffering) > 0, "No VPC offerings found")

        self.vpc = VPC.create(
            apiclient=self.userapiclient,
            services=self.services["vpc_vpn"]["vpc"],
            vpcofferingid=vpcOffering[0].id,
            zoneid=self.zone.id,
            projectid=self.project.id
        )
        self.cleanup.append(self.vpc)

        networkOffering = NetworkOffering.list(
            self.userapiclient, name="DefaultIsolatedNetworkOfferingForVpcNetworks")
        self.assertTrue(networkOffering is not None and len(
            networkOffering) > 0, "No VPC based network offering")

        self.network = Network.create(
            apiclient=self.userapiclient,
            services=self.services["vpc_vpn"]["network_1"],
            networkofferingid=networkOffering[0].id,
            zoneid=self.zone.id,
            vpcid=self.vpc.id,
            projectid=self.project.id
        )
        self.cleanup.append(self.network)

        self.services["virtual_machine"]["networkids"] = [str(self.network.id)]

        # Create vm instance
        self.vm_2 = VirtualMachine.create(
            self.userapiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            userdataid=self.userdata.userdata.id,
            serviceofferingid=self.service_offering.id,
            projectid=self.project.id
        )
        self.cleanup.append(self.vm_2)

        self.public_ip = PublicIPAddress.create(
            self.userapiclient,
            zoneid=self.zone.id,
            services=self.services["virtual_machine"],
            networkid=self.network.id,
            vpcid=self.vpc.id,
            projectid=self.project.id
        )

        # 2. create LB with port 80 -> 80, verify the website (should get expected content).
        # firewall is open by default
        lb_http = {
            "name": "http",
            "alg": "roundrobin",
            "privateport": 80,
            "publicport": 80,
            "protocol": "tcp"
        }
        lb_rule_http = LoadBalancerRule.create(
            self.userapiclient,
            lb_http,
            self.public_ip.ipaddress.id,
            networkid=self.network.id,
            projectid=self.project.id
        )
        lb_rule_http.assign(self.userapiclient, [self.vm_2])
        command = "curl -sL --connect-timeout 3 http://%s/test.html" % self.public_ip.ipaddress.ipaddress
        # wait 10 minutes until the webpage is available. it returns "503 Service Unavailable" if not available
        self.wait_for_service_ready(command, CONTENT, 60)

        # 3. create LB with port 443 -> 80, verify the website (should not work)
        # firewall is open by default
        lb_https = {
            "name": "https",
            "alg": "roundrobin",
            "privateport": 80,
            "publicport": 443,
            "protocol": "ssl"
        }
        lb_rule_https = LoadBalancerRule.create(
            self.userapiclient,
            lb_https,
            self.public_ip.ipaddress.id,
            networkid=self.network.id,
            projectid=self.project.id
        )
        lb_rule_https.assign(self.userapiclient, [self.vm_2])

        command = "curl -L --connect-timeout 3 -k --resolve %s:443:%s https://%s/test.html" % (DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, None, 1)

        command = "curl -L --connect-timeout 3 --resolve %s:443:%s https://%s/test.html" % (DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, None, 1)

        # 4. add cert to LB with port 443
        lb_rule_https.assignCert(self.userapiclient, self.sslcert.id)

        # 5. verify the website (should get expected content)
        command = "curl -L --connect-timeout 3 --resolve %s:443:%s https://%s/test.html" % (DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, None, 1)

        command = "curl -sL --connect-timeout 3 -k --resolve %s:443:%s https://%s/test.html" % (DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, CONTENT, 1)

        command = "curl -sL --connect-timeout 3 --cacert %s --resolve %s:443:%s https://%s/test.html" % (FULL_CHAIN, DOMAIN, self.public_ip.ipaddress.ipaddress, DOMAIN)
        self.wait_for_service_ready(command, CONTENT, 1)

        # 6. remove cert from LB with port 443
        lb_rule_https.removeCert(self.userapiclient)

        # 7. delete SSL certificate
        self.sslcert.delete(self.userapiclient)
