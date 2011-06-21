#!/opt/perl5/bin/perl -w
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
use strict;
use Socket;
use IO::Socket;
use Sys::Hostname;

main();
exit(0);

sub main()
{
    my $MAX_MSG_SIZE = 16384; # 16KBytes should be enough...

    my $svrport = 8300;
    my $svriaddr = gethostbyname(hostname());
    my $svrproto = getprotobyname('udp');
    my $svrpaddr = sockaddr_in($svrport, $svriaddr);

    socket(SOCKET, PF_INET, SOCK_DGRAM, $svrproto)   || die "socket: $!";
    bind(SOCKET, $svrpaddr)                          || die "bind: $!";

    my $rin = '';
    vec($rin, fileno(SOCKET), 1) = 1;

    # timeout after 10.0 seconds
    # at some time, I'm going to add signal handlers so that signals can be
    # sent to cause logfile rollover, or tidy exit...then the timeout will
    # come in useful..

    my $exit = 0;
    while (!$exit)
    {
        my $rout = $rin;

        # select(readvec, writevec, exceptionvec, timeout)
        # : returns # of selected filehandles, modifies
        #   vector parameters so that set bits indicate
        #   filehandles which are readable, writable or have
        #   an exception state

        my $nSelected = select($rout, undef, undef, 10.0);
        if ($nSelected == 0)
        {
          # timedout : go back to start of loop
          next;
        }

        my $msgData = '';
        my $clientpaddr = recv(SOCKET, $msgData, $MAX_MSG_SIZE, 0);
        if (!$clientpaddr)
        {
          die "recv: $!";
        }

        my ($clientport, $clientiaddr) = sockaddr_in($clientpaddr);
        my $clienthost = gethostbyaddr($clientiaddr, AF_INET);
        if (!$clienthost)
        {
          # unable to determine name for client : show raw ip address
          $clienthost = inet_ntoa($clientiaddr);
        }

        print "$clienthost:$msgData\n";
    }
}


