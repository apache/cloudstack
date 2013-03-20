#!/usr/bin/perl -w
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

#############################################################
# This script connects to the system vm socket and writes the
# authorized_keys and cmdline data to it. The system VM then
# reads it from /dev/vport0p1 in cloud_early_config
#############################################################

use strict;
use Getopt::Std;
use IO::Socket;
$|=1;

my $opts = {};
getopt('pn',$opts);
my $name = $opts->{n};
my $cmdline = $opts->{p};
my $sockfile = "/var/lib/libvirt/qemu/$name.agent";
my $pubkeyfile = "/root/.ssh/id_rsa.pub.cloud";

if (! -S $sockfile) {
  print "ERROR: $sockfile socket not found\n";
  exit 1;
}

if (! -f $pubkeyfile) {
  print "ERROR: ssh public key not found on host at $pubkeyfile\n";
  exit 1;
}

open(FILE,$pubkeyfile) or die "ERROR: unable to open $pubkeyfile - $^E";
my $key = <FILE>;
close FILE;

$cmdline =~ s/%/ /g;
my $msg = "pubkey:" . $key . "\ncmdline:" . $cmdline;

my $socket = IO::Socket::UNIX->new(Peer=>$sockfile,Type=>SOCK_STREAM)
    or die "ERROR: unable to connect to $sockfile - $^E\n";
print $socket "$msg\r\n";
close $socket;

