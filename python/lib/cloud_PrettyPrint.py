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
# Copyright (C) 2004, 2005 Mike Wray <mike.wray@hp.com>
# Copyright (C) 2005 XenSource Ltd
#============================================================================

"""General pretty-printer, including support for SXP.

"""
import sys
import types
import StringIO
#from xen.xend import sxp
import cloud_sxp as sxp

class PrettyItem:

    def __init__(self, width):
        self.width = width

    def insert(self, block):
        block.addtoline(self)

    def get_width(self):
        return self.width

    def output(self, _):
        print '***PrettyItem>output>', self
        pass

    def prettyprint(self, _):
        print '***PrettyItem>prettyprint>', self
        return self.width

class PrettyString(PrettyItem):

    def __init__(self, x):
        PrettyItem.__init__(self, len(x))
        self.value = x

    def output(self, out):
        out.write(self.value)

    def prettyprint(self, line):
        line.output(self)

    def show(self, out):
        print >> out, ("(string (width %d) '%s')" % (self.width, self.value))

class PrettySpace(PrettyItem):

    def output(self, out):
        out.write(' ' * self.width)

    def prettyprint(self, line):
        line.output(self)

    def show(self, out):
        print >> out, ("(space (width %d))" % self.width)
        
class PrettyBreak(PrettyItem):

    def __init__(self, width, indent):
        PrettyItem.__init__(self, width)
        self.indent = indent
        self.space = 0
        self.active = 0

    def output(self, out):
        out.write(' ' * self.width)

    def prettyprint(self, line):
        if line.breaks(self.space):
            self.active = 1
            line.newline(self.indent)
        else:
            line.output(self)

    def show(self, out):
        print >> out, ("(break (width %d) (indent %d) (space %d) (active %d))"
                       % (self.width, self.indent, self.space, self.active))

class PrettyNewline(PrettySpace):

    def insert(self, block):
        block.newline()
        block.addtoline(self)

    def prettyprint(self, line):
        line.newline(0)
        line.output(self)

    def show(self, out):
        print >> out, ("(nl (width %d))" % self.width)

class PrettyLine(PrettyItem):
    def __init__(self):
        PrettyItem.__init__(self, 0)
        self.content = []

    def write(self, x):
        self.content.append(x)

    def end(self):
        width = 0
        lastwidth = 0
        lastbreak = None
        for x in self.content:
            if isinstance(x, PrettyBreak):
                if lastbreak:
                    lastbreak.space = (width - lastwidth)
                lastbreak = x
                lastwidth = width
            width += x.get_width()
        if lastbreak:
            lastbreak.space = (width - lastwidth)
        self.width = width
 
    def prettyprint(self, line):
        for x in self.content:
            x.prettyprint(line)

    def show(self, out):
        print >> out, '(LINE (width %d)' % self.width
        for x in self.content:
            x.show(out)
        print >> out, ')'

class PrettyBlock(PrettyItem):

    def __init__(self, all=0, parent=None):
        PrettyItem.__init__(self, 0)

        self.lines = []
        self.parent = parent
        self.indent = 0
        self.all = all
        self.broken = 0
        self.newline()

    def add(self, item):
        item.insert(self)

    def end(self):
        self.width = 0
        for l in self.lines:
            l.end()
            if self.width < l.width:
                self.width = l.width

    def breaks(self, _):
        return self.all and self.broken

    def newline(self):
        self.lines.append(PrettyLine())

    def addtoline(self, x):
        self.lines[-1].write(x)

    def prettyprint(self, line):
        self.indent = line.used
        line.block = self
        if not line.fits(self.width):
            self.broken = 1
        for l in self.lines:
            l.prettyprint(line)
        line.block = self.parent

    def show(self, out):
        print >> out, ('(BLOCK (width %d) (indent %d) (all %d) (broken %d)' %
                       (self.width, self.indent, self.all, self.broken))
        for l in self.lines:
            l.show(out)
        print >> out, ')'

class Line:

    def __init__(self, out, width):
        self.block = None
        self.out = out
        self.width = width
        self.used = 0
        self.space = self.width

    def newline(self, indent):
        indent += self.block.indent
        self.out.write('\n')
        self.out.write(' ' * indent)
        self.used = indent
        self.space = self.width - self.used

    def fits(self, n):
        return self.space - n >= 0

    def breaks(self, n):
        return self.block.breaks(n) or not self.fits(n)

    def output(self, x):
        n = x.get_width()
        self.space -= n
        self.used += n
        if self.space < 0:
            self.space = 0
        x.output(self.out)

class PrettyPrinter:
    """A prettyprinter based on what I remember of Derek Oppen's
    prettyprint algorithm from TOPLAS way back.
    """

    def __init__(self, width=40):
        self.width = width
        self.block = None
        self.top = None

    def write(self, x):
        self.block.add(PrettyString(x))

    def add(self, item):
        self.block.add(item)

    def addbreak(self, width=1, indent=4):
        self.add(PrettyBreak(width, indent))

    def addspace(self, width=1):
        self.add(PrettySpace(width))

    def addnl(self, indent=0):
        self.add(PrettyNewline(indent))

    def begin(self, all=0):
        block = PrettyBlock(all=all, parent=self.block)
        self.block = block

    def end(self):
        self.block.end()
        if self.block.parent:
            self.block.parent.add(self.block)
        else:
            self.top = self.block
        self.block = self.block.parent

    def prettyprint(self, out=sys.stdout):
        self.top.prettyprint(Line(out, self.width))

class SXPPrettyPrinter(PrettyPrinter):
    """An SXP prettyprinter.
    """
    
    def pstring(self, x):
        io = StringIO.StringIO()
        sxp.show(x, out=io)
        io.seek(0)
        val = io.getvalue()
        io.close()
        return val

    def pprint(self, l):
        if isinstance(l, types.ListType):
            self.begin(all=1)
            self.write('(')
            i = 0
            for x in l:
                if(i): self.addbreak()
                self.pprint(x)
                i += 1
            self.addbreak(width=0, indent=0)
            self.write(')')
            self.end()
        else:
            self.write(self.pstring(l))

def prettyprint(sxpr, out=sys.stdout, width=80):
    """Prettyprint an SXP form.

    sxpr	s-expression
    out		destination
    width	maximum output width
    """
    if isinstance(sxpr, types.ListType):
        pp = SXPPrettyPrinter(width=width)
        pp.pprint(sxpr)
        pp.prettyprint(out=out)
    else:
        sxp.show(sxpr, out=out)
    print >> out

def prettyprintstring(sxpr, width=80):
    """Prettyprint an SXP form to a string.

    sxpr	s-expression
    width	maximum output width
    """
    io = StringIO.StringIO()
    prettyprint(sxpr, out=io, width=width)
    io.seek(0)
    val = io.getvalue()
    io.close()
    return val

def main():
    pin = sxp.Parser()
    while 1:
        buf = sys.stdin.read(100)
        pin.input(buf)
        if buf == '': break
    l = pin.get_val()
    prettyprint(l, width=80)

if __name__ == "__main__":
    main()
    
