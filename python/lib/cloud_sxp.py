#!/usr/bin/env python
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
#============================================================================

"""
Input-driven parsing for s-expression (sxp) format.
Create a parser: pin = Parser();
Then call pin.input(buf) with your input.
Call pin.input_eof() when done.
Use pin.read() to see if a value has been parsed, pin.get_val()
to get a parsed value. You can call ready and get_val at any time -
you don't have to wait until after calling input_eof.

"""
from __future__ import generators

import sys
import types
import errno
import string
from StringIO import StringIO

__all__ = [
    "mime_type", 
    "ParseError", 
    "Parser", 
    "atomp", 
    "show", 
    "show_xml", 
    "elementp", 
    "name", 
    "attributes", 
    "attribute", 
    "children", 
    "child", 
    "child_at", 
    "child0", 
    "child1", 
    "child2", 
    "child3", 
    "child4", 
    "child_value",
    "has_id", 
    "with_id", 
    "child_with_id", 
    "elements",
    "merge",
    "to_string",
    "from_string",
    "all_from_string",
    "parse", 
    ]

mime_type = "application/sxp"

escapes = {
    'a': '\a',
    'b': '\b',
    't': '\t',
    'n': '\n',
    'v': '\v',
    'f': '\f',
    'r': '\r',
    '\\': '\\',
    '\'': '\'',
    '\"': '\"'}

k_list_open  = "("
k_list_close = ")"
k_attr_open  = "@"
k_eval       = "!"

escapes_rev = {}
for k in escapes:
    escapes_rev[escapes[k]] = k

class ParseError(StandardError):

    def __init__(self, parser, value):
        self.parser = parser
        self.value = value

    def __str__(self):
        return self.value

class ParserState:

    def __init__(self, fn, parent=None):
        self.parent = parent
        self.buf = ''
        self.val = []
        self.delim = None
        self.fn = fn

    def push(self, fn):
        return ParserState(fn, parent=self)
    
class Parser:

    def __init__(self):
        self.error = sys.stderr
        self.reset()

    def reset(self):
        self.val = []
        self.eof = 0
        self.err = 0
        self.line_no = 0
        self.char_no = 0
        self.state = None

    def push_state(self, fn):
        self.state = self.state.push(fn)

    def pop_state(self):
        val = self.state
        self.state = self.state.parent
        if self.state and self.state.fn == self.state_start:
            # Return to start state - produce the value.
            self.val += self.state.val
            self.state.val = []
        return val

    def in_class(self, c, s):
        return s.find(c) >= 0
        
    def in_space_class(self, c):
        return self.in_class(c, ' \t\n\v\f\r')

    def is_separator(self, c):
        return self.in_class(c, '{}()<>[]!;')

    def in_comment_class(self, c):
        return self.in_class(c, '#')

    def in_string_quote_class(self, c):
        return self.in_class(c, '"\'')

    def in_printable_class(self, c):
        return self.in_class(c, string.printable)

    def set_error_stream(self, error):
        self.error = error

    def has_error(self):
        return self.err > 0

    def at_eof(self):
        return self.eof

    def input_eof(self):
        self.eof = 1
        self.input_char(-1)

    def input(self, buf):
        if not buf or len(buf) == 0:
            self.input_eof()
        else:
            for c in buf:
                self.input_char(c)

    def input_char(self, c):
        if self.at_eof():
            pass
        elif c == '\n':
            self.line_no += 1
            self.char_no = 0
        else:
           self.char_no += 1 

        if self.state is None:
            self.begin_start(None)
        self.state.fn(c)

    def ready(self):
        return len(self.val) > 0

    def get_val(self):
        v = self.val[0]
        self.val = self.val[1:]
        return v

    def get_all(self):
        return self.val

    def begin_start(self, c):
        self.state = ParserState(self.state_start)

    def end_start(self):
        self.val += self.state.val
        self.pop_state()
    
    def state_start(self, c):
        if self.at_eof():
            self.end_start()
        elif self.in_space_class(c):
            pass
        elif self.in_comment_class(c):
            self.begin_comment(c)
        elif c == k_list_open:
            self.begin_list(c)
        elif c == k_list_close:
            raise ParseError(self, "syntax error: "+c)
        elif self.in_string_quote_class(c):
            self.begin_string(c)
        elif self.in_printable_class(c):
            self.begin_atom(c)
        elif c == chr(4):
            # ctrl-D, EOT: end-of-text.
            self.input_eof()
        else:
            raise ParseError(self, "invalid character: code %d" % ord(c))

    def begin_comment(self, c):
        self.push_state(self.state_comment)
        self.state.buf += c

    def end_comment(self):
        self.pop_state()
    
    def state_comment(self, c):
        if c == '\n' or self.at_eof():
            self.end_comment()
        else:
            self.state.buf += c

    def begin_string(self, c):
        self.push_state(self.state_string)
        self.state.delim = c

    def end_string(self):
        val = self.state.buf
        self.state.parent.val.append(val)
        self.pop_state()
        
    def state_string(self, c):
        if self.at_eof():
            raise ParseError(self, "unexpected EOF")
        elif c == self.state.delim:
            self.end_string()
        elif c == '\\':
            self.push_state(self.state_escape)
        else:
            self.state.buf += c

    def state_escape(self, c):
        if self.at_eof():
            raise ParseError(self, "unexpected EOF")
        d = escapes.get(c)
        if d:
            self.state.parent.buf += d
            self.pop_state()
        elif c == 'x':
            self.state.fn = self.state_hex
            self.state.val = 0
        elif c in string.octdigits:
            self.state.fn = self.state_octal
            self.state.val = 0
            self.input_char(c)
        else:
            # ignore escape if it doesn't match anything we know
            self.state.parent.buf += '\\'
            self.pop_state()

    def state_octal(self, c):
        def octaldigit(c):
            self.state.val *= 8
            self.state.val += ord(c) - ord('0')
            self.state.buf += c
            if self.state.val < 0 or self.state.val > 0xff:
                raise ParseError(self, "invalid octal escape: out of range " + self.state.buf)
            if len(self.state.buf) == 3:
               octaldone()
               
        def octaldone():
            d = chr(self.state.val)
            self.state.parent.buf += d
            self.pop_state()
            
        if self.at_eof():
            raise ParseError(self, "unexpected EOF")
        elif '0' <= c <= '7':
            octaldigit(c)
        elif len(self.state.buf):
            octaldone()
            self.input_char(c)

    def state_hex(self, c):
        def hexdone():
            d = chr(self.state.val)
            self.state.parent.buf += d
            self.pop_state()
            
        def hexdigit(c, d):
            self.state.val *= 16
            self.state.val += ord(c) - ord(d)
            self.state.buf += c
            if self.state.val < 0 or self.state.val > 0xff:
                raise ParseError(self, "invalid hex escape: out of range " + self.state.buf)
            if len(self.state.buf) == 2:
                hexdone()
            
        if self.at_eof():
            raise ParseError(self, "unexpected EOF")
        elif '0' <= c <= '9':
            hexdigit(c, '0')
        elif 'A' <= c <= 'F':
            hexdigit(c, 'A')
        elif 'a' <= c <= 'f':
            hexdigit(c, 'a')
        elif len(buf):
            hexdone()
            self.input_char(c)

    def begin_atom(self, c):
        self.push_state(self.state_atom)
        self.state.buf = c

    def end_atom(self):
        val = self.state.buf
        self.state.parent.val.append(val)
        self.pop_state()
    
    def state_atom(self, c):
        if self.at_eof():
            self.end_atom()
        elif (self.is_separator(c) or
              self.in_space_class(c) or
              self.in_comment_class(c)):
            self.end_atom()
            self.input_char(c)
        else:
            self.state.buf += c

    def begin_list(self, c):
        self.push_state(self.state_list)

    def end_list(self):
        val = self.state.val
        self.state.parent.val.append(val)
        self.pop_state()

    def state_list(self, c):
        if self.at_eof():
            raise ParseError(self, "unexpected EOF")
        elif c == k_list_close:
            self.end_list()
        else:
            self.state_start(c)

def atomp(sxpr):
    """Check if an sxpr is an atom.
    """
    if sxpr.isalnum() or sxpr == '@':
        return 1
    for c in sxpr:
        if c in string.whitespace: return 0
        if c in '"\'\\(){}[]<>$#&%^': return 0
        if c in string.ascii_letters: continue
        if c in string.digits: continue
        if c in '.-_:/~': continue
        return 0
    return 1
    
def show(sxpr, out=sys.stdout):
    """Print an sxpr in bracketed (lisp-style) syntax.
    """
    if isinstance(sxpr, (types.ListType, types.TupleType)):
        out.write(k_list_open)
        i = 0
        for x in sxpr:
            if i: out.write(' ')
            show(x, out)
            i += 1
        out.write(k_list_close)
    elif isinstance(sxpr, (types.IntType, types.FloatType)):
        out.write(str(sxpr))
    elif isinstance(sxpr, types.StringType) and atomp(sxpr):
        out.write(sxpr)
    else:
        out.write(repr(str(sxpr)))

def show_xml(sxpr, out=sys.stdout):
    """Print an sxpr in XML syntax.
    """
    if isinstance(sxpr, (types.ListType, types.TupleType)):
        element = name(sxpr)
        out.write('<%s' % element)
        for attr in attributes(sxpr):
            out.write(' %s=%s' % (attr[0], attr[1]))
        out.write('>')
        i = 0
        for x in children(sxpr):
            if i: out.write(' ')
            show_xml(x, out)
            i += 1
        out.write('</%s>' % element)
    elif isinstance(sxpr, types.StringType) and atomp(sxpr):
        out.write(sxpr)
    else:
        out.write(str(sxpr))

def elementp(sxpr, elt=None):
    """Check if an sxpr is an element of the given type.

    sxpr sxpr
    elt  element type
    """
    return (isinstance(sxpr, (types.ListType, types.TupleType))
            and len(sxpr)
            and (None == elt or sxpr[0] == elt))

def name(sxpr):
    """Get the element name of an sxpr.
    If the sxpr is not an element (i.e. it's an atomic value) its name
    is None.

    sxpr

    returns name (None if not an element).
    """
    val = None
    if isinstance(sxpr, types.StringType):
        val = sxpr
    elif isinstance(sxpr, (types.ListType, types.TupleType)) and len(sxpr):
        val = sxpr[0]
    return val

def attributes(sxpr):
    """Get the attribute list of an sxpr.

    sxpr

    returns attribute list
    """
    val = []
    if isinstance(sxpr, (types.ListType, types.TupleType)) and len(sxpr) > 1:
        attr = sxpr[1]
        if elementp(attr, k_attr_open):
            val = attr[1:]
    return val

def attribute(sxpr, key, val=None):
    """Get an attribute of an sxpr.

    sxpr sxpr
    key  attribute key
    val  default value (default None)

    returns attribute value
    """
    for x in attributes(sxpr):
        if x[0] == key:
            val = x[1]
            break
    return val

def children(sxpr, elt=None):
    """Get children of an sxpr.

    sxpr sxpr
    elt  optional element type to filter by

    returns children (filtered by elt if specified)
    """
    val = []
    if isinstance(sxpr, (types.ListType, types.TupleType)) and len(sxpr) > 1:
        i = 1
        x = sxpr[i]
        if elementp(x, k_attr_open):
            i += 1
        val = sxpr[i : ]
    if elt:
        def iselt(x):
            return elementp(x, elt)
        val = filter(iselt, val)
    return val

def child(sxpr, elt, val=None):
    """Get the first child of the given element type.

    sxpr sxpr
    elt  element type
    val  default value
    """
    for x in children(sxpr):
        if elementp(x, elt):
            val = x
            break
    return val

def child_at(sxpr, index, val=None):
    """Get the child at the given index (zero-based).

    sxpr  sxpr
    index index
    val   default value
    """
    kids = children(sxpr)
    if len(kids) > index:
        val = kids[index]
    return val

def child0(sxpr, val=None):
    """Get the zeroth child.
    """
    return child_at(sxpr, 0, val)

def child1(sxpr, val=None):
    """Get the first child.
    """
    return child_at(sxpr, 1, val)

def child2(sxpr, val=None):
    """Get the second child.
    """
    return child_at(sxpr, 2, val)

def child3(sxpr, val=None):
    """Get the third child.
    """
    return child_at(sxpr, 3, val)

def child4(sxpr, val=None):
    """Get the fourth child.
    """
    return child_at(sxpr, 4, val)

def child_value(sxpr, elt, val=None):
    """Get the value of the first child of the given element type.
    Assumes the child has an atomic value.

    sxpr sxpr
    elt  element type
    val  default value
    """
    kid = child(sxpr, elt)
    if kid:
        val = child_at(kid, 0, val)
    return val

def has_id(sxpr, id):
    """Test if an s-expression has a given id.
    """
    return attribute(sxpr, 'id') == id

def with_id(sxpr, id, val=None):
    """Find the first s-expression with a given id, at any depth.

    sxpr   s-exp or list
    id     id
    val    value if not found (default None)

    return s-exp or val
    """
    if isinstance(sxpr, (types.ListType, types.TupleType)):
        for n in sxpr:
            if has_id(n, id):
                val = n
                break
            v = with_id(n, id)
            if v is None: continue
            val = v
            break
    return val

def child_with_id(sxpr, id, val=None):
    """Find the first child with a given id.

    sxpr   s-exp or list
    id     id
    val    value if not found (default None)

    return s-exp or val
    """
    if isinstance(sxpr, (types.ListType, types.TupleType)):
        for n in sxpr:
            if has_id(n, id):
                val = n
                break
    return val

def elements(sxpr, ctxt=None):
    """Generate elements (at any depth).
    Visit elements in pre-order.
    Values generated are (node, context)
    The context is None if there is no parent, otherwise
    (index, parent, context) where index is the node's index w.r.t its parent,
    and context is the parent's context.

    sxpr   s-exp

    returns generator
    """
    yield (sxpr, ctxt)
    i = 0
    for n in children(sxpr):
        if isinstance(n, (types.ListType, types.TupleType)):
            # Calling elements() recursively does not generate recursively,
            # it just returns a generator object. So we must iterate over it.
            for v in elements(n, (i, sxpr, ctxt)):
                yield v
        i += 1

def merge(s1, s2):
    """Merge sxprs s1 and s2.
    Returns an sxpr containing all the fields from s1 and s2, with
    entries in s1 overriding s2. Recursively merges fields.

    @param s1 sxpr
    @param s2 sxpr
    @return merged sxpr
    """
    if s1 is None:
        val = s2
    elif s2 is None:
        val = s1
    elif elementp(s1):
        name1 = name(s1)
        (m1, v1) = child_map(s1)
        (m2, v2) = child_map(s2)
        val = [name1]
        for (k1, f1) in m1.items():
            merge_list(val, f1, m2.get(k1, []))
        for (k2, f2) in m2.items():
            if k2 in m1: continue
            val.extend(f2)
        val.extend(v1)
    else:
        val = s1
    return val

def merge_list(sxpr, l1, l2):
    """Merge element lists l1 and l2 into sxpr.
    The lists l1 and l2 are all element with the same name.
    Values from l1 are merged with values in l2 and stored in sxpr.
    If one list is longer than the other the excess values are used
    as they are.

    @param sxpr to merge into
    @param l1 sxpr list
    @param l2 sxpr list
    @return modified sxpr
    """
    n1 = len(l1)
    n2 = len(l2)
    nmin = min(n1, n2)
    for i in range(0, nmin):
        sxpr.append(merge(l1[i], l2[i]))
    for i in range(nmin, n1):
        sxpr.append(l1[i])
    for i in range(nmin, n2):
        sxpr.append(l2[i])
    return sxpr

def child_map(sxpr):
    """Get a dict of the elements in sxpr and a list of its values.
    The dict maps element name to the list of elements with that name,
    and the list is the non-element children.

    @param sxpr
    @return (dict, list)
    """
    m = {}
    v = []
    for x in children(sxpr):
        if elementp(x):
            n = name(x)
            l = m.get(n, [])
            l.append(x)
            m[n] = l
        else:
            v.append(x)
    return (m, v)

def to_string(sxpr):
    """Convert an sxpr to a string.

    sxpr sxpr
    returns string
    """
    io = StringIO()
    show(sxpr, io)
    io.seek(0)
    val = io.getvalue()
    io.close()
    return val

def from_string(s):
    """Create an sxpr by parsing a string.

    s string
    returns sxpr
    """
    if s == '':
        return []

    io = StringIO(s)
    vals = parse(io)
    if vals is []:
        return None
    else:
        return vals[0]
    

def all_from_string(s):
    """Create an sxpr list by parsing a string.

    s string
    returns sxpr list
    """
    io = StringIO(s)
    vals = parse(io)
    return vals

def parse(io):
    """Completely parse all input from 'io'.

    io	input file object
    returns list of values, None if incomplete
    raises ParseError on parse error
    """
    pin = Parser()
    while 1:
        buf = io.readline()
        pin.input(buf)
        if len(buf) == 0:
            break
    if pin.ready():
        val = pin.get_all()
    else:
        val = None
    return val
   

if __name__ == '__main__':
    print ">main"
    pin = Parser()
    while 1:
        buf = sys.stdin.read(1024)
        #buf = sys.stdin.readline()
        pin.input(buf)
        while pin.ready():
            val = pin.get_val()
            print
            print '****** val=', val
        if len(buf) == 0:
            break

