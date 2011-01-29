The Hunting of the Snark Project - BitTorrent Application Suite
0.5 - The Beaver's Lesson (27 June 2003)

  "It's a Snark!" was the sound that first came to their ears,
     And seemed almost too good to be true.
  Then followed a torrent of laughter and cheers:
     Then the ominous words "It's a Boo-"

  -- from The Hunting Of The Snark by Lewis Carroll

Snark is a client for downloading and sharing files distributed with
the BitTorrent protocol. It is mainly used for exploring the BitTorrent
protocol and experimenting with the the GNU Compiler for Java (gcj).
But it can also be used as a regular BitTorrent Client.

Snark can also act as a torrent creator, micro http server for delivering
metainfo.torrent files and has an integrated Tracker for making sharing of
files as easy as possible.

When you give the option --share Snark will automatically
create a .torrent file, start a very simple webserver to distribute
the metainfo.torrent file and a local tracker that other BitTorrent
clients can connect to.

Distribution
------------

  Copyright (C) 2003 Mark J. Wielaard

  Snark is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

Requirements/Installation
-------------------------

The GNU Compiler for java (gcj) version 3.3 or later.
(Earlier versions have a faulty SHA message digest implementation.)
On Debian GNU/Linux based distributions just install the gcj-3.3 package.
Edit the GCJ variable in the Makefile if your gcj binary is not gcj-3.3.

Typing 'make' will create the native snark binary and a snark.jar file
for use with traditional java byte code interpreters.

It is possible to compile the sources with other java compilers
like jikes or kjc to produce the snark.jar file.  Edit the JAVAC and
JAVAC_FLAGS variables on top of the Makefile for this.  And type
'make snark.jar' to create a jar file that can be used by traditional
java bytecode interpreters like kaffe: 'kaffe -jar snark.jar'.
You will need at least version 1.1 of kaffe for all functionality to work
correctly ('--share' does not work with older versions).

When trying out the experimental Gnome frontend you also need the java-gnome
bindings. On Debian GNU/Linux systems install the package libgnome0-java.
You can try it out by typing 'make snark-gnome' and then run 'snark-gnome.sh'
like you would with the normal command line client.

Running
-------

To use the program start it with:

snark [--debug [level]] [--no-commands] [--port <port>]
      [--share (<ip>|<host>)] (<url>|<file>|<dir>)
  --debug       Shows some extra info and stacktraces.
    level       How much debug details to show
                (defaults to 3, with --debug to 4, highest level is 6).
  --no-commands Don't read interactive commands or show usage info.
  --port        The port to listen on for incomming connections
                (if not given defaults to first free port between 6881-6889).
  --share       Start torrent tracker on <ip> address or <host> name.
  <url>         URL pointing to .torrent metainfo file to download/share.
  <file>        Either a local .torrent metainfo file to download
                or (with --share) a file to share.
  <dir>         A directory with files to share (needs --share).

Since this is an early beta release there are probably still some bugs
in the program. To help find them run the program with the --debug
option which shows more information on what it going on. You can also give
the level of debug output you want. Zero will give (almost) no output at all.
Everything above debug level 4 is probably to much (only really useful to
see what goes on on the protocol/network level).

Examples

- To simple start downloading/sharing a file.
  Either download the .torrent file to disk and start snark with:
  ./snark somefile.torrent

  Or give it the complete URL:
  ./snark http://somehost.example.com/cd-images/bbc-lnx.iso.torrent

- To start seeding/sharing a local file:
  ./snark --share my-host.example.com some-file

  Snark will respond with:
  Listening on port: 6881
  Trying to create metainfo torrent for 'some-file'
  Creating torrent piece hashes: ++++++++++
  Torrent available on http://my-host.example.com:6881/metainfo.torrent

  You can now point other people to the above URL so they can share
  the file with their own BitTorrent client.

Commands

While the program is running in text mode you can currently give the
following commands: 'info', 'list' and 'quit'.

Interactive commands are disabled when the '--no-commands' flag is given.
This is sometimes desireable for running snark in the background.

More information
----------------

- The Evolution of Cooperation - Robert Axelrod
  ISBN 0-465-02121-2

- The BitTorrent protocol description:
  <http://bitconjurer.org/BitTorrent/protocol.html>

- The GNU Compiler for Java (gcj):
  <http://gcc.gnu.org/java/>

- java-gnome bindings : <http://java-gnome.sourceforge.net/>

- The Hunting of the Snark - Lewis Carroll

Comments welcome

	- Mark Wielaard <mark@klomp.org>
