# -- coding: utf-8 --
import CsHelper
from pprint import pprint
import logging

class CsChain(object):

    def __init__(self):
        self.chain = {}
        self.last_added = ''

    def add(self, table, chain):
        if not table in self.chain.keys():
            self.chain.setdefault(table, []).append( chain )
        else:
            self.chain[table].append(chain)
        self.last_added = chain

    def get(self, table):
        return self.chain[table]

    def last(self):
        return self.last_added

    def has_chain(self, table, chain):
        if not table in self.chain.keys():
            return False
        if not chain in self.chain[table]:
            return False
        return True

class CsTable(object):

    def __init__(self):
        self.table = []
        self.last_added = ''

    def add(self, name):
        if not name in self.table:
            self.table.append(name)
            self.last_added = name

    def get(self):
        return self.table

    def last(self):
        return self.last_added

class CsNetfilters(object):

    def __init__(self, load = True):
        self.rules = []
        self.table = CsTable()
        self.chain = CsChain()
        if load:
            self.get_all_rules()

    def get_all_rules(self):
        for i in CsHelper.execute("iptables-save"):
            if i.startswith('*'): # Table
                self.table.add(i[1:])
            if i.startswith(':'): # Chain
                self.chain.add(self.table.last(), i[1:].split(' ')[0])
            if i.startswith('-A'): # Rule
                rule = CsNetfilter()
                rule.parse(i)
                rule.set_table(self.table.last())
                self.save(rule)

    def save(self,rule):
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
                r.mark_seen()
                return True
        return False

    def get_unseen(self):
        del_list = [x for x in self.rules if x.unseen()]
        for r in del_list:
            cmd = "iptables -t %s %s" % (r.get_table(), r.to_str(True))
            CsHelper.execute(cmd)
            logging.info("Delete rule %s from table %s", r.to_str(True), r.get_table())

    def compare(self, list):
        """ Compare reality with what is needed """
        for c in self.chain.get("filter"):
            # Ensure all inbound chains have a default drop rule
            if c.startswith("ACL_INBOUND"):
                list.append(["filter", "", "-A %s -j DROP" % c])
        print list
        for fw in list:
            new_rule = CsNetfilter()
            new_rule.parse(fw[2])
            new_rule.set_table(fw[0])
            self.add_chain(new_rule)
            if self.has_rule(new_rule):
                logging.debug("rule %s exists in table %s", fw[2], new_rule.get_table())
            else:
                logging.info("Add rule %s in table %s", fw[2], new_rule.get_table())
                # front means insert instead of append
                cpy = fw[2]
                if fw[1] == "front":
                    cpy = cpy.replace('-A', '-I')

                CsHelper.execute("iptables -t %s %s" % (new_rule.get_table(), cpy))
        self.del_standard()
        self.get_unseen()


    def add_chain(self, rule):
        """ Add the given chain if it is not already present """
        if not self.has_chain(rule.get_table(), rule.get_chain()):
           CsHelper.execute("iptables -t %s -N %s" % (rule.get_table(), rule.get_chain()))
           self.chain.add(rule.get_table(), rule.get_chain())

    def del_standard(self):
        """ Del rules that are there but should not be deleted 
        from the host but that configure does not actually manage """
        self.del_rule("mangle", "-m udp --dport 68 -A OUTPUT -p udp -j CHECKSUM")

        self.del_rule("filter", "-d 224.0.0.18/32 -A INPUT -j ACCEPT")
        self.del_rule("filter", "-d 225.0.0.50/32 -A INPUT -j ACCEPT")
        self.del_rule("filter", "-A INPUT -p icmp -j ACCEPT")
        self.del_rule("filter", "-i lo -A INPUT -j ACCEPT")
        self.del_rule("filter", "-A INPUT -m tcp -i eth0 -m state --dport 3922 -p tcp --state NEW -j ACCEPT")
        self.del_rule("filter", "-j ACCEPT -A INPUT --state RELATED,ESTABLISHED -m state")
        self.del_rule("filter", "-j ACCEPT -A FORWARD --state RELATED,ESTABLISHED -m state")

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
        self.seen  = False

    def parse(self, rule):
        self.rule = self.__convert_to_dict(rule)

    def unseen(self):
        return self.seen == False

    def mark_seen(self):
        self.seen = True

    def __convert_to_dict(self, rule):
        rule = unicode(rule.lstrip())
        rule = rule.replace('! -', '!_-')
        rule = rule.replace('-p all', '')
        rule = rule.replace('  ', ' ')
        # -m can appear twice in a string
        rule = rule.replace('-m state', '-m2 state')
        bits = rule.split(' ')
        rule = dict(zip(bits[0::2],bits[1::2]))
        if "-A" in rule.keys():
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

    def get_chain(self):
        return self.chain

    def get_rule(self):
        return self.rule

    def to_str(self, delete = False):
        """ Convert the rule back into aynactically correct iptables command """
        # Order is important 
        order = ['-A', '-s', '-d', '!_-d', '-i', '-p', '-m', '-m2', '--state', 
                '--dport', '--destination-port', '-o', '-j', '--set-xmark',
                 '--to-source', '--to-destination']
        str = ''
        for k in order:
            if k in self.rule.keys():
                printable = k.replace('-m2', '-m')
                printable = printable.replace('!_-', '! -')
                if delete:
                    printable = printable.replace('-A', '-D')
                if str == '':
                    str = "%s %s" % (printable, self.rule[k])
                else:
                    str = "%s %s %s" % (str, printable, self.rule[k])
        return str

    def __eq__(self, rule):
        if rule.get_table() != self.get_table():
            return False
        if rule.get_chain() != self.get_chain():
            return False
        if len(rule.get_rule().items()) != len(self.get_rule().items()):
            return False
        common = set(rule.get_rule().items()) & set(self.get_rule().items())
        if len(common) != len(rule.get_rule()):
            return False
        return True

