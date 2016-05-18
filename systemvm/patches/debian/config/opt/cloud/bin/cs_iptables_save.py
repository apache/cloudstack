#!/usr/bin/python
#
# -*- coding: utf-8 -*-
#
"""
iptables_converter.py:
    convert iptables commands within a script
    into a correspondig iptables-save script

    default filename to read is rules, to read some other
        file, append: -s filename

    output is written to stdout for maximum flexibilty

Author:     Johannes Hubertz <johannes@hubertz.de>
Date:       2015-03-17
version:    0.9.8
License:    GNU General Public License version 3 or later

Have Fun!
"""
from __future__ import print_function

try:
    from collections import UserDict
except ImportError:
    from UserDict import UserDict
import re
import sys
import logging


class ConverterError():
    """on accidential case of error show given reason"""

    def __init__(self, message):
        """message to stdout to compatible testings 2.7 and 3.4"""
        print (message)
        sys.exit(1)


class Chains(UserDict):
    """this is for one type of tables"""

    def __init__(self, name, tables):
        """init Chains object"""
        UserDict.__init__(self)
        self.name = name
        self.tables = tables
        self.predef = tables
        self.reset()  # name, tables)

    def put_into_fgr(self, content):
        """fill this line into this tabular"""
        self.length += 1
        cha = "filter"
        # act = ""
        liste = content.split()
        action = liste[0]
        if "-t" in action:
            liste.pop(0)  # remove 1st: -t
            fname = liste.pop(0)
            legals = ["filter", "nat", "raw", "mangle"]
            if fname not in legals:
                msg = "Valid is one of %s, got: %s" % (legals, fname)
                raise ValueError(msg)
            action = liste[0]
            content = ""                       # rebuild content from here
            for elem in liste:
                content = content + elem + " "
            if len(liste) > 1:
                chain_name = liste[1]
        if "-F" in action:
            self.reset()
            return
        if "-P" in action:
            liste.pop(0)
            cha = liste.pop(0)
            new = liste.pop(0)
            if new not in ["ACCEPT", "DROP", "REJECT"]:
                msg = "Illegal policy: % s" % (new)
                raise ValueError(msg)
            self.poli[cha] = new
            return
        if "-X" in action:
            predef = ['INPUT', 'FORWARD', 'OUTPUT',
                      'PREROUTING', 'POSTROUTING']
            rem_chain_name = liste.pop(1)
            if rem_chain_name in predef:
                msg = "Cannot remove predefined chain"
                raise ValueError(msg)
            if rem_chain_name in self.data:
                self.data[rem_chain_name] = []        # empty list
                self.poli[rem_chain_name] = "-"       # empty policy, no need
                self.data.pop(rem_chain_name)
            return
        if "-N" in action:
            new_chain_name = liste.pop(1)
            existing = self.data.keys()
            if new_chain_name in existing:
                logging.debug("Chain %s already exists" % new_chain_name)
                return
            self.data[new_chain_name] = []        # empty list
            self.poli[new_chain_name] = "-"       # empty policy, no need
            return
        if "-I" in action:  # or "-A" in action:
            chain_name = liste[1]
            existing = self.data.keys()
            if chain_name not in existing:
                self.data[chain_name] = []
                self.poli[chain_name] = "-"
            kette = self.data[chain_name]
            kette.insert(0, content.replace("-I", "-A"))
            self.data[chain_name] = kette
            return
        if "-A" in action:  # or "-I" in action:
            chain_name = liste[1]
            existing = self.data.keys()
            if chain_name not in existing:
                self.data[chain_name] = []
                self.poli[chain_name] = "-"
            kette = self.data[chain_name]
            kette.append(content)
            self.data[chain_name] = kette
            return
        msg = "Unknown filter command in input:", content
        raise ValueError(msg)

    def reset(self):  # name, tables):
        """
        name is one of filter, nat, raw, mangle,
        tables is a list of tables in that table-class
        """
        self.poli = {}               # empty dict
        self.length = 0
        self.policy = "-"
        for tabular in self.tables:
            self.data[tabular] = []
            self.poli[tabular] = "ACCEPT"


class Tables(UserDict):
    """
    some chaingroups in tables are predef: filter, nat, mangle, raw
    """

    def __init__(self, rules):
        """init Tables Object is easy going"""
        UserDict.__init__(self)
        self.reset(rules)

    def reset(self, rules):
        """all predefined Chains aka lists are setup as new here"""
        filter = Chains("filter", ["INPUT", "FORWARD", "OUTPUT"])

        mang = ["PREROUTING", "INPUT", "FORWARD", "OUTPUT", "POSTROUTING", ]
        mangle = Chains("mangle", mang)

        # kernel 2.6.32 has no INPUT in NAT!
        nat = Chains("nat", ["PREROUTING", "OUTPUT", "POSTROUTING"])

        raw = Chains("raw", ["PREROUTING", "OUTPUT", ])

        self.data["filter"] = filter
        self.data["mangle"] = mangle
        self.data["nat"] = nat
        self.data["raw"] = raw
        if rules is not None:
            self.read_file(rules)

    def table_printout(self):
        """printout nonempty tabulars in fixed sequence"""
        with open("/tmp/rules.save", 'w') as f:
            for key in ["raw", "nat", "mangle", "filter"]:
                len = self.data[key].length
                if len > -1:
                    print("*%s" % (self.data[key].name), file=f)
                    for chain in self.data[key].keys():
                        poli = self.data[key].poli[chain]
                        print(":%s %s [0:0]" % (chain, poli), file=f)
                    for chain in self.data[key].values():
                        for elem in chain:
                            print(elem, file=f)
                    print("COMMIT", file=f)

    def put_into_tables(self, line):
        """put line into matching Chains-object"""
        liste = line.split()
        liste.pop(0)                        # we always know, it's iptables
        rest = ""
        for elem in liste:                  # remove redirects and the like
            if ">" not in elem:
                rest = rest + elem + " "    # string again with single blanks
        action = liste.pop(0)               # action is one of {N,F,A,I, etc.}
        fam = "filter"
        if "-t nat" in line:                # nat filter group
            fam = "nat"
        elif "-t mangle" in line:           # mangle filter group
            fam = "mangle"
        elif "-t raw" in line:              # raw filter group
            fam = "raw"
        fam_dict = self.data[fam]           # select the group dictionary
        fam_dict.put_into_fgr(rest)         # do action thers

    def read_file(self, rules):
        """read file into Tables-object"""
        self.linecounter = 0
        self.tblctr = 0
        for zeile in rules:
            line = str(zeile.strip())
            self.linecounter += 1
            if line.startswith('#'):
                continue
            for element in ['\$', '\(', '\)', ]:
                if re.search(element, line):
                    m1 = "Line %d:\n%s\nplain files only, " % \
                         (self.linecounter, line)
                    if element in ['\(', '\)', ]:
                        m2 = "unable to convert shell functions, abort"
                    else:
                        m2 = "unable to resolve shell variables, abort"
                    msg = m1 + m2
                    raise ConverterError(msg)
            for muster in ["^/sbin/iptables ", "^iptables "]:
                if re.search(muster, line):
                    self.tblctr += 1
                    self.put_into_tables(line)
