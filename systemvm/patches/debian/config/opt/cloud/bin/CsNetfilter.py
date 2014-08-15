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

    def __init__(self):
        self.rules = []
        self.table = CsTable()
        self.chain = CsChain()
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

    def hasTable(self, table):
        return table in self.table.get()

    def hasChain(self, table, chain):
        return chain in self.chain.get(table)

    def has_rule(self, new_rule):
        for r in self.get():
            if new_rule.equals(r):
                return True
        return False

    def compare(self, list):
        """ Compare reality with what is needed """
        for fw in list:
            new_rule = CsNetfilter()
            new_rule.parse(fw[2])
            new_rule.set_table(fw[0])
            if self.has_rule(new_rule):
                logging.debug("rule %s exists in table %s", fw[2], new_rule.get_table())
            else:
                logging.info("Add rule %s in table %s", fw[2], new_rule.get_table())
                print "Add rule %s in table %s" % (fw[2], new_rule.get_table())

                CsHelper.execute("iptables -t %s %s" % (new_rule.get_table(), fw[2]))
             
class CsNetfilter(object):
     
    def __intt(self):
        self.rule = {}
        self.table = ''
        self.chain = ''

    def parse(self, rule):
        self.rule = self.__convert_to_dict(rule)

    def __convert_to_dict(self, rule):
        rule = rule.replace('! -', '!_-')
        # -m can appear twice in a string
        rule = rule.replace('-m state', '-m2 state')
        bits = rule.split(' ')
        rule = dict(zip(bits[0::2], bits[1::2]))
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

    def to_str(self):
        return ', '.join("%s=%r" % (key,val) for (key,val) in self.rule.iteritems())

    def equals(self, rule):
        if rule.get_table() != self.get_table():
            return False
        if rule.get_chain() != self.get_chain():
            return False
        for r in rule.get_rule():
            if not r in self.get_rule().keys():
                return False
            if rule.get_rule()[r] != self.get_rule()[r]:
               return False
        return True

