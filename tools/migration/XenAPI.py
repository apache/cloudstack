#============================================================================
# This library is free software; you can redistribute it and/or
# modify it under the terms of version 2.1 of the GNU Lesser General Public
# License as published by the Free Software Foundation.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#============================================================================
# Copyright (C) 2006-2007 XenSource Inc.
#============================================================================
#
# Parts of this file are based upon xmlrpclib.py, the XML-RPC client
# interface included in the Python distribution.
#
# Copyright (c) 1999-2002 by Secret Labs AB
# Copyright (c) 1999-2002 by Fredrik Lundh
#
# By obtaining, using, and/or copying this software and/or its
# associated documentation, you agree that you have read, understood,
# and will comply with the following terms and conditions:
#
# Permission to use, copy, modify, and distribute this software and
# its associated documentation for any purpose and without fee is
# hereby granted, provided that the above copyright notice appears in
# all copies, and that both that copyright notice and this permission
# notice appear in supporting documentation, and that the name of
# Secret Labs AB or the author not be used in advertising or publicity
# pertaining to distribution of the software without specific, written
# prior permission.
#
# SECRET LABS AB AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD
# TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANT-
# ABILITY AND FITNESS.  IN NO EVENT SHALL SECRET LABS AB OR THE AUTHOR
# BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY
# DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
# WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
# ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
# OF THIS SOFTWARE.
# --------------------------------------------------------------------

import gettext
import xmlrpclib
import httplib
import socket

translation = gettext.translation('xen-xm', fallback = True)

API_VERSION_1_1 = '1.1'
API_VERSION_1_2 = '1.2'

class Failure(Exception):
    def __init__(self, details):
        self.details = details

    def __str__(self):
        try:
            return str(self.details)
        except Exception, exn:
            import sys
            print >>sys.stderr, exn
            return "Xen-API failure: %s" % str(self.details)

    def _details_map(self):
        return dict([(str(i), self.details[i])
                     for i in range(len(self.details))])


_RECONNECT_AND_RETRY = (lambda _ : ())

class UDSHTTPConnection(httplib.HTTPConnection):
    """HTTPConnection subclass to allow HTTP over Unix domain sockets. """
    def connect(self):
        path = self.host.replace("_", "/")
        self.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.sock.connect(path)

class UDSHTTP(httplib.HTTP):
    _connection_class = UDSHTTPConnection

class UDSTransport(xmlrpclib.Transport):
    def __init__(self, use_datetime=0):
        self._use_datetime = use_datetime
        self._extra_headers=[]
    def add_extra_header(self, key, value):
        self._extra_headers += [ (key,value) ]
    def make_connection(self, host):
        return UDSHTTP(host)
    def send_request(self, connection, handler, request_body):
        connection.putrequest("POST", handler)
        for key, value in self._extra_headers:
            connection.putheader(key, value)
        
class Session(xmlrpclib.ServerProxy):
    """A server proxy and session manager for communicating with xapi using
    the Xen-API.

    Example:

    session = Session('http://localhost/')
    session.login_with_password('me', 'mypassword')
    session.xenapi.VM.start(vm_uuid)
    session.xenapi.session.logout()
    """

    def __init__(self, uri, transport=None, encoding=None, verbose=0,
                 allow_none=1):
        xmlrpclib.ServerProxy.__init__(self, uri, transport, encoding,
                                       verbose, allow_none)
        self.transport = transport
        self._session = None
        self.last_login_method = None
        self.last_login_params = None
        self.API_version = API_VERSION_1_1


    def xenapi_request(self, methodname, params):
        if methodname.startswith('login'):
            self._login(methodname, params)
            return None
        elif methodname == 'logout' or methodname == 'session.logout':
            self._logout()
            return None
        else:
            retry_count = 0
            while retry_count < 3:
                full_params = (self._session,) + params
                result = _parse_result(getattr(self, methodname)(*full_params))
                if result == _RECONNECT_AND_RETRY:
                    retry_count += 1
                    if self.last_login_method:
                        self._login(self.last_login_method,
                                    self.last_login_params)
                    else:
                        raise xmlrpclib.Fault(401, 'You must log in')
                else:
                    return result
            raise xmlrpclib.Fault(
                500, 'Tried 3 times to get a valid session, but failed')


    def _login(self, method, params):
        result = _parse_result(getattr(self, 'session.%s' % method)(*params))
        if result == _RECONNECT_AND_RETRY:
            raise xmlrpclib.Fault(
                500, 'Received SESSION_INVALID when logging in')
        self._session = result
        self.last_login_method = method
        self.last_login_params = params
        self.API_version = self._get_api_version()

    def _logout(self):
        try:
            if self.last_login_method.startswith("slave_local"):
                return _parse_result(self.session.local_logout(self._session))
            else:
                return _parse_result(self.session.logout(self._session))
        finally:
            self._session = None
            self.last_login_method = None
            self.last_login_params = None
            self.API_version = API_VERSION_1_1

    def _get_api_version(self):
        pool = self.xenapi.pool.get_all()[0]
        host = self.xenapi.pool.get_master(pool)
        major = self.xenapi.host.get_API_version_major(host)
        minor = self.xenapi.host.get_API_version_minor(host)
        return "%s.%s"%(major,minor)

    def __getattr__(self, name):
        if name == 'handle':
            return self._session
        elif name == 'xenapi':
            return _Dispatcher(self.API_version, self.xenapi_request, None)
        elif name.startswith('login') or name.startswith('slave_local'):
            return lambda *params: self._login(name, params)
        else:
            return xmlrpclib.ServerProxy.__getattr__(self, name)

def xapi_local():
    return Session("http://_var_xapi_xapi/", transport=UDSTransport())

def _parse_result(result):
    if type(result) != dict or 'Status' not in result:
        raise xmlrpclib.Fault(500, 'Missing Status in response from server' + result)
    if result['Status'] == 'Success':
        if 'Value' in result:
            return result['Value']
        else:
            raise xmlrpclib.Fault(500,
                                  'Missing Value in response from server')
    else:
        if 'ErrorDescription' in result:
            if result['ErrorDescription'][0] == 'SESSION_INVALID':
                return _RECONNECT_AND_RETRY
            else:
                raise Failure(result['ErrorDescription'])
        else:
            raise xmlrpclib.Fault(
                500, 'Missing ErrorDescription in response from server')


# Based upon _Method from xmlrpclib.
class _Dispatcher:
    def __init__(self, API_version, send, name):
        self.__API_version = API_version
        self.__send = send
        self.__name = name

    def __repr__(self):
        if self.__name:
            return '<XenAPI._Dispatcher for %s>' % self.__name
        else:
            return '<XenAPI._Dispatcher>'

    def __getattr__(self, name):
        if self.__name is None:
            return _Dispatcher(self.__API_version, self.__send, name)
        else:
            return _Dispatcher(self.__API_version, self.__send, "%s.%s" % (self.__name, name))

    def __call__(self, *args):
        return self.__send(self.__name, args)
