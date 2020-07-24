# -- coding: utf-8 --
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
from . import CsHelper
from .CsDatabag import CsCmdLine
import logging


class CsChain(object):

    def __init__(self):
        self.chain = {}
        self.last_added = ''
        self.count = {}

    def add(self, table, chain):
         if table not in list(self.chain.keys()):
            self.chain.setdefault(table, []).append(chain)
        else:
            self.chain[table].append(chain)
        if self.last_added != chain:
            self.last_added = chain
            self.count[chain] = 0

    def add_rule(self, chain):
        self.count[chain] += 1

    def get(self, table):
        if table not in list(self.chain.keys()):
            return {}
        return self.chain[table]

    def get_count(self, chain):
        return self.count[chain]

    def last(self):
        return self.last_added

    def has_chain(self, table, chain):
        if table not in list(self.chain.keys()):
            return False
        if chain not in self.chain[table]:
            return False
        return True


class CsTable(object):

    def __init__(self):
        self.table = []
        self.last_added = ''

    def add(self, name):
        if name not in self.table:
            self.table.append(name)
            self.last_added = name

    def get(self):
        return self.table

    def last(self):
        return self.last_added


class CsNetfilters(object):

    def __init__(self, load=True):
        self.rules = []
        self.table = CsTable()
        self.chain = CsChain()
        if load:
            self.get_all_rules()

    def get_all_rules(self):
        for i in CsHelper.execute("iptables-save"):
            if i.startswith('*'):  # Table
                self.table.add(i[1:])
            if i.startswith(':'):  # Chain
                self.chain.add(self.table.last(), i[1:].split(' ')[0])
            if i.startswith('-A'):  # Rule
                self.chain.add_rule(i.split()[1])
                rule = CsNetfilter()
                rule.parse(i)
                rule.set_table(self.table.last())
                rule.set_chain(i.split()[1])
                rule.set_count(self.chain.get_count(i.split()[1]))
                self.save(rule)

    def save(self, rule):
        self.rules.append(rule)

    def get(self):
        return self.rules

    def has_table(self, table):
        return table in self.table.get()

    def has_chain(self, table, chain):
        return self.chain.has_chain(table, chain)

    def has_rule(self, new_rule):
        for r in self.get():
            if new_rule == r:
                if new_rule.get_count() > 0:
                    continue
                r.mark_seen()
                return True
        return False

    def get_unseen(self):
        del_list = [x for x in self.rules if x.unseen()]
        for r in del_list:
            cmd = "iptables -t %s %s" % (r.get_table(), r.to_str(True))
            logging.debug("unseen cmd:  %s ", cmd)
            CsHelper.execute(cmd)
            # print "Delete rule %s from table %s" % (r.to_str(True), r.get_table())
            logging.info("Delete rule %s from table %s", r.to_str(True), r.get_table())

    def compare(self, list):
        """ Compare reality with what is needed """
        # PASS 1:  Ensure all chains are present
        for fw in list:
            new_rule = CsNetfilter()
            new_rule.parse(fw[2])
            new_rule.set_table(fw[0])
            self.add_chain(new_rule)

        ruleSet = set()
        # PASS 2: Create rules
        for fw in list:
            tupledFw = tuple(fw)
            if tupledFw in ruleSet:
                logging.debug("Already processed : %s", tupledFw)
                continue

            new_rule = CsNetfilter()
            new_rule.parse(fw[2])
            new_rule.set_table(fw[0])
            if isinstance(fw[1], int):
                new_rule.set_count(fw[1])

            rule_chain = new_rule.get_chain()

            logging.debug("Checking if the rule already exists: rule=%s table=%s chain=%s", new_rule.get_rule(), new_rule.get_table(), new_rule.get_chain())
            if self.has_rule(new_rule):
                logging.debug("Exists: rule=%s table=%s", fw[2], new_rule.get_table())
            else:
                # print "Add rule %s in table %s" % ( fw[2], new_rule.get_table())
                logging.info("Add: rule=%s table=%s", fw[2], new_rule.get_table())
                # front means insert instead of append
                cpy = fw[2]
                if fw[1] == "front":
                    cpy = cpy.replace('-A', '-I')
                if isinstance(fw[1], int):
                    # if the rule is for ACLs, we want to insert them in order, right before the DROP all
                    if rule_chain.startswith("ACL_INBOUND") or rule_chain.startswith("ACL_OUTBOUND"):
                        rule_count = self.chain.get_count(rule_chain) if self.chain.get_count(rule_chain) > 0 else 1
                        cpy = cpy.replace("-A %s" % new_rule.get_chain(), '-I %s %s' % (new_rule.get_chain(), rule_count))
                    else:
                        cpy = cpy.replace("-A %s" % new_rule.get_chain(), '-I %s %s' % (new_rule.get_chain(), fw[1]))
                ret = CsHelper.execute2("iptables -t %s %s" % (new_rule.get_table(), cpy))
                # There are some issues in this framework causing failures  .. like adding a chain without checking it is present causing
                # the failures. Also some of the rule like removeFromLoadBalancerRule is deleting rule and deleteLoadBalancerRule
                # trying to delete which causes the failure.
                # For now raising the log.
                # TODO: Need to fix in the framework.
                if ret.returncode != 0:
                    error = ret.communicate()[0]
                    logging.debug("iptables command got failed ... continuing")
                ruleSet.add(tupledFw)
                self.chain.add_rule(rule_chain)
        self.del_standard()
        self.get_unseen()

    def add_chain(self, rule):
        """ Add the given chain if it is not already present """
        if not self.has_chain(rule.get_table(), rule.get_chain()):
            if rule.get_chain():
                CsHelper.execute("iptables -t %s -N %s" % (rule.get_table(), rule.get_chain()))
            self.chain.add(rule.get_table(), rule.get_chain())

    def del_standard(self):
        """ Del rules that are there but should not be deleted
        These standard firewall rules vary according to the device type
        """
        type = CsCmdLine("cmdline").get_type()

        try:
            table = ''
            for i in open("/etc/iptables/iptables-%s" % type):
                if i.startswith('*'):  # Table
                    table = i[1:].strip()
                if i.startswith('-A'):  # Rule
                    self.del_rule(table, i.strip())
        except IOError:
            logging.debug("Exception in del_standard, returning")
            # Nothing can be done
            return

    def del_rule(self, table, rule):
        nr = CsNetfilter()
        nr.parse(rule)
        nr.set_table(table)
        self.delete(nr)

    def delete(self, rule):
        """ Delete a rule from the list of configured rules
        The rule will not actually be removed on the host """
        self.rules[:] = [x for x in self.rules if not x == rule]


class CsNetfilter(object):

    def __init__(self):
        self.rule = {}
        self.table = ''
        self.chain = ''
        self.seen = False
        self.count = 0

    def parse(self, rule):
        self.rule = self.__convert_to_dict(rule)

    def unseen(self):
        return self.seen is False

    def mark_seen(self):
        self.seen = True

    def __convert_to_dict(self, rule):
        rule = str(rule.lstrip())
        rule = rule.replace('! -', '!_-')
        rule = rule.replace('-p all', '')
        rule = rule.replace('  ', ' ')
        rule = rule.replace('bootpc', '68')
        # Ugly hack no.23 split this or else I will have an odd number of parameters
        rule = rule.replace('--checksum-fill', '--checksum fill')
        # -m can appear twice in a string
        rule = rule.replace('-m state', '-m2 state')
        rule = rule.replace('ESTABLISHED,RELATED', 'RELATED,ESTABLISHED')
        bits = rule.split(' ')
        rule = dict(list(zip(bits[0::2], bits[1::2])))
        if "-A" in list(rule.keys()):
            self.chain = rule["-A"]
        return rule

    def set_table(self, table):
        if table == '':
            table = "filter"
        self.table = table

    def get_table(self):
        return self.table

    def set_chain(self, chain):
        self.chain = chain

    def set_count(self, count=0):
        self.count = count

    def get_count(self):
        return self.count

    def get_chain(self):
        return self.chain

    def get_rule(self):
        return self.rule

    def to_str(self, delete=False):
        """ Convert the rule back into aynactically correct iptables command """
        # Order is important
        order = ['-A', '-s', '-d', '!_-d', '-i', '!_-i', '-p', '-m', '-m2', '--icmp-type', '--state',
                 '--dport', '--destination-port', '-o', '!_-o', '-j', '--set-xmark', '--checksum',
                 '--to-source', '--to-destination', '--mark']
        str = ''
        for k in order:
            if k in list(self.rule.keys()):
                printable = k.replace('-m2', '-m')
                printable = printable.replace('!_-', '! -')
                if delete:
                    printable = printable.replace('-A', '-D')
                if str == '':
                    str = "%s %s" % (printable, self.rule[k])
                else:
                    str = "%s %s %s" % (str, printable, self.rule[k])
        str = str.replace("--checksum fill", "--checksum-fill")
        return str

    def __eq__(self, rule):
        if rule.get_table() != self.get_table():
            return False
        if rule.get_chain() != self.get_chain():
            return False
        if len(list(rule.get_rule().items())) != len(list(self.get_rule().items())):
            return False
        common = set(rule.get_rule().items()) & set(self.get_rule().items())
        if len(common) != len(rule.get_rule()):
            return False
        return True
