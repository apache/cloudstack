import CsHelper
from pprint import pprint

class CsChain(object):

    def __init__(self):
        self.chain = {}

    def add(self, table, chain):
        if not table in self.chain.keys():
            self.chain.setdefault(table, []).append( chain )
        else:
            self.chain[table].append(chain)

    def get(self, table):
        return self.chain[table]

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
                self.save(rule)

    def save(self,rule):
        self.rules.append(rule)

    def get(self):
        return self.rules

    def hasTable(self, table):
        return table in self.table.get()

    def hasChain(self, table, chain):
        return chain in self.chain.get(table)

class CsNetfilter(object):

    def parse(self, rule):
        rule.replace('! -', '!_-')
        bits = rule.split(' ')
        self.rule = dict(zip(bits[0::2], bits[1::2])).iteritems()

if __name__ == "__main__":

    t = CsNetfilters()
    print t.hasTable('mangle');
    print t.hasChain('mangle', 'PREROUTING');
