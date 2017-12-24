#!/usr/bin/python
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import logging
import re
import shutil
import os


class LineEdit(object):
    """Helper for LineEditingFile that keeps track of one edit."""
    def __init__(self, search, sub, *sub_args, **kwargs):
        if len(sub_args) > 0:
            sub = sub % sub_args
        flags = kwargs.get('flags', 0)
        self.pattern = re.compile(search, flags=flags)
        self.sub = sub
        self.count = kwargs.get('count', 0)                            # max subs to make
        self.subs = 0                                                  # subs made so far


class LineEditingFile(object):
    """
    Atomic, conservative, by-line editing of configuration files.

    Will not touch the file if there are no changes to do.
    Reasonably efficient for large files, though files with a long time
    before their first match will use memory.


    Given a vhosts file such as:
    >>> with open('doctest-vhosts.conf', 'w') as f:
    ...   f.write('''
    ... Listen foo:80
    ... <VirtualHost foo:80>
    ...   DocRoot /var/www
    ... </VirtualHost>
    ...
    ... Listen other:80
    ... <VirtualHost other:80>
    ...   DocRoot /var/www
    ... </VirtualHost>
    ... ''')
    ...

    To replace the hostname for the first virtualhost entry:
    >>> new_hostname = 'fooooo'
    >>> with LineEditingFile('doctest-vhosts.conf') as f:
    ...   f.replace(r'<VirtualHost .*?:80>', '<VirtualHost %s:80>', new_hostname, count=1, flags=re.I)
    ...   f.replace(r'Listen .*?:80', 'Listen %s:80', new_hostname, count=1, flags=re.I)
    ...

    Be careful with the matches!
    A second invocation of the same rule will edit the second vhost:
    >>> new_hostname = 'fooooo'
    >>> with LineEditingFile('doctest-vhosts.conf') as f:
    ...   f.replace(r'<VirtualHost .*?:80>', '<VirtualHost %s:80>', new_hostname, count=1, flags=re.I)
    ...

    To move all hosts from port 80 to port 8080:
    >>> with LineEditingFile('doctest-vhosts.conf') as f:
    ...   f.replace(r'<VirtualHost (.*?):80>', '<VirtualHost \\\\1:8080>', flags=re.I)
    ...   f.replace(r'Listen (.*?):80', 'Listen \\\\1:80', flags=re.I)
    ...

    (please note in this example there's a double escape of the backreference
    \\\\1, to make the example work with doctest)

    Since this example already matched all files, a second invocation does nothing:
    >>> with LineEditingFile('doctest-vhosts.conf') as f:
    ...   f.replace(r'<VirtualHost (.*?):80>', '<VirtualHost \\\\1:8080>', flags=re.I)
    ...

    It's also acceptable to not make any edits at all:
    >>> with LineEditingFile('doctest-vhosts.conf') as f:
    ...   pass
    ...

    You don't _have_ to use a with statement:
    >>> f = LineEditingFile('doctest-vhosts.conf')
    >>> f.replace(r'DocRoot /var/www', 'DocRoot /var/www/html', flags=re.I)
    >>> changes = f.commit()
    >>> print changes
    2
    >>>

    Cleanup of the example vhosts.conf:
    >>> # noinspection PyBroadException
    >>> try:
    ...   os.unlink('doctest-vhosts.conf')
    ...   os.unlink('doctest-vhosts.conf.bak')
    ...   os.unlink('doctest-vhosts.conf.new')
    ... except:
    ...   pass
    ...
    """

    def __init__(self, filename):
        self.filename = filename
        self.changed = False
        self.edits = []

    def __enter__(self):
        return self

    def replace(self, search, sub, *sub_args, **kwargs):
        edit = LineEdit(search, sub, *sub_args, **kwargs)
        self.edits.append(edit)

    # noinspection PyUnusedLocal
    def __exit__(self, exc, value, traceback):
        if exc is not None:
            return False                                               # return false results in re-raise

        self.commit()

    def commit(self):
        changes = 0
        changed_file = None
        changed_filename = self.filename + '.new'
        try:
            lines = []
            backup_filename = self.filename + '.bak'
            # noinspection PyUnusedLocal
            stat = None
            with open(self.filename, 'r') as orig:
                stat = os.fstat(orig.fileno())
                for line in orig:
                    changed_line = line
                    for edit in self.edits:
                        remaining_count = 0
                        if edit.count != 0:
                            remaining_count = edit.count - edit.subs
                            if remaining_count < 0:
                                raise Exception("Made too many edits")
                            elif remaining_count == 0:
                                continue
                        changed_line, subs = edit.pattern.subn(
                            edit.sub, line, remaining_count)
                        if changed_line != line:
                            if changed_file is None:
                                logging.debug("Editing file %s" % self.filename)
                            logging.debug("  - %s" % line[:-1])
                            logging.debug("  + %s" % changed_line[:-1])
                            changes += subs
                            edit.subs += subs
                    if changes == 0:                                   # buffer until we find a change
                        lines.append(changed_line)
                    elif changed_file is None:                         # found first change, flush buffer
                        changed_file = open(changed_filename, 'w')
                        if hasattr(os, 'fchmod'):
                            os.fchmod(changed_file.fileno(),           # can cause OSError which aborts
                                      stat.st_mode)
                        if hasattr(os, 'fchown'):
                            os.fchown(changed_file.fileno(),           # can cause OSError which aborts
                                      stat.st_uid, stat.st_gid)
                        changed_file.writelines(lines)
                        changed_file.write(changed_line)
                        del lines                                      # reclaim buffer memory
                    else:                                              # already flushed, just write
                        changed_file.write(changed_line)

            if changes == 0:
                logging.info("No edits need for file %s" %
                             self.filename)
            else:
                changed_file.close()
                changed_file = None
                if os.path.exists(backup_filename):                    # back up the original
                    os.unlink(backup_filename)
                shutil.copy(self.filename, backup_filename)
                os.rename(changed_filename, self.filename)             # the swap
                logging.info("Edited file %s (%d changes)" %
                             (self.filename, changes))
        finally:
            if changed_file is not None:                               # failed, clean up
                changed_file.close()
                os.unlink(changed_filename)
        return changes

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    import doctest
    doctest.testmod()
