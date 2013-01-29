# -*- coding: utf-8 -*-
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

try:
    from pygments import highlight
    from pygments.console import ansiformat
    from pygments.formatter import Formatter
    from pygments.formatters import Terminal256Formatter
    from pygments.lexer import bygroups, include, RegexLexer
    from pygments.token import *

    import sys
except ImportError, e:
    print e


MONKEY_COLORS = {
    Token:          '',
    Whitespace:     'reset',
    Text:           'reset',

    Name:           'green',
    Operator:       'teal',
    Operator.Word:  'lightgray',
    String:         'purple',

    Keyword:        '_red_',
    Error:          'red',
    Literal:        'yellow',
    Number:         'blue',
}


def get_colorscheme():
    return MONKEY_COLORS


class MonkeyLexer(RegexLexer):
    keywords = ['[a-z]*id', '^[a-z A-Z]*:']
    attributes = ['[Tt]rue', '[Ff]alse']
    params = ['[a-z]*[Nn]ame', 'type', '[Ss]tate']

    uuid_rgx = r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'
    date_rgx = r'[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:]{8}-[0-9]{4}'

    def makelistre(lis):
        return r'(' + r'|'.join(lis) + r')'

    tokens = {
        'root': [
            (r' ', Whitespace),
            (date_rgx, Number),
            (uuid_rgx, Literal),
            (r'(?:\b\d+\b(?:-\b\d+|%)?)', Number),
            (r'^[-=]*\n', Operator.Word),
            (r'Error', Error),
            (makelistre(keywords), Keyword),
            (makelistre(attributes), Literal),
            (makelistre(params) + r'( = )(.*)', bygroups(Name, Operator,
                                                         String)),
            (makelistre(params), Name),
            (r'(^[a-zA-Z]* )(=)', bygroups(Name, Operator)),
            (r'\S+', Text),
        ]
    }

    def analyse_text(text):
        npos = text.find('\n')
        if npos < 3:
            return False
        return text[0] == '[' and text[npos - 1] == ']'


class MonkeyFormatter(Formatter):
    def __init__(self, **options):
        Formatter.__init__(self, **options)
        self.colorscheme = get_colorscheme()

    def format(self, tokensource, outfile):
        self.encoding = outfile.encoding
        return Formatter.format(self, tokensource, outfile)

    def format_unencoded(self, tokensource, outfile):
        for ttype, value in tokensource:
            color = self.colorscheme.get(ttype)
            while color is None:
                ttype = ttype[:-1]
                color = self.colorscheme.get(ttype)
            if color:
                spl = value.split('\n')
                for line in spl[:-1]:
                    if line:
                        outfile.write(ansiformat(color, line))
                    outfile.write('\n')
                if spl[-1]:
                    outfile.write(ansiformat(color, spl[-1]))
            else:
                outfile.write(value)


def monkeyprint(text):
    fmter = MonkeyFormatter()
    lexer = MonkeyLexer()
    lexer.encoding = 'utf-8'
    fmter.encoding = 'utf-8'
    highlight(text, lexer, fmter, sys.stdout)
