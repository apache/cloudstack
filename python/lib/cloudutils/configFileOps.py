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
import re
import tempfile
import shutil
from utilities import bash
class configFileOps:
    class entry:
        def __init__(self, name, value, op, separator):
            self.name = name
            self.value = value
            self.state = "new"
            self.op = op
            self.separator = separator
        def setState(self, state):
            self.state = state 
        def getState(self):
            return self.state

    def __init__(self, fileName, cfg=None):
        self.fileName = fileName
        self.entries = []
        self.backups = []
        
        if cfg is not None:
            cfg.cfoHandlers.append(self)

    def addEntry(self, name, value, separator="="):
        e = self.entry(name, value, "add", separator)
        self.entries.append(e)
    
    def rmEntry(self, name, value, separator="="):
        entry = self.entry(name, value, "rm", separator)
        self.entries.append(entry)
    
    def getEntry(self, name, separator="="):
        try:
            ctx = file(self.fileName).read(-1)
            match = re.search("^" + name + ".*", ctx, re.MULTILINE)
            if match is None:
                return ""
            line = match.group(0).split(separator, 1)
            return line[1]
        except:
            return ""

    def save(self):
        fp = open(self.fileName, "r")
        newLines = []
        for line  in fp.readlines():
            matched = False
            for entry in self.entries:
                if entry.op == "add":
                    if entry.separator == "=":
                        matchString = "^\ *" + entry.name + ".*"
                    elif entry.separator == " ":
                        matchString = "^\ *" + entry.name + "\ *" + entry.value
                else:
                    if entry.separator == "=":
                        matchString = "^\ *" + entry.name + "\ *=\ *" + entry.value
                    else:
                        matchString = "^\ *" + entry.name + "\ *" + entry.value
                
                match = re.match(matchString, line)
                if match is not None:
                    if entry.op == "add" and entry.separator == "=":
                        newline = "\n" + entry.name + "=" + entry.value + "\n"
                        entry.setState("set")
                        newLines.append(newline)
                        self.backups.append([line, newline])
                        matched = True
                        break
                    elif entry.op == "rm":
                        entry.setState("set")
                        self.backups.append([line, None])
                        matched = True
                        break  
                    
            if not matched: 
                newLines.append(line)

        for entry in self.entries:
            if entry.getState() != "set":
                if entry.op == "add":
                    newline = entry.name + entry.separator + entry.value + "\n"
                    newLines.append(newline)
                    self.backups.append([None, newline])
                    entry.setState("set")

        fp.close()
        
        file(self.fileName, "w").writelines(newLines)

    def replace_line(self, startswith,stanza,always_add=False):
        lines = [ s.strip() for s in file(self.fileName).readlines() ]
        newlines = []
        replaced = False
        for line in lines:
            if re.search(startswith, line):
                if stanza is not None:
                    newlines.append(stanza)
                    self.backups.append([line, stanza])
                replaced = True
            else: newlines.append(line)
        if not replaced and always_add:
            newlines.append(stanza)
            self.backups.append([None, stanza])
        newlines = [ s + '\n' for s in newlines ]
        file(self.fileName,"w").writelines(newlines)

    def replace_or_add_line(self, startswith,stanza):
        return self.replace_line(startswith,stanza,always_add=True)

    def add_lines(self, lines, addToBackup=True):
        fp = file(self.fileName).read(-1) 
        sh = re.escape(lines)
        match = re.search(sh, fp, re.MULTILINE) 
        if match is not None:
            return
    
        fp += lines
        file(self.fileName, "w").write(fp)
        self.backups.append([None, lines])
        
    def replace_lines(self, src, dst, addToBackup=True):
        fp = file(self.fileName).read(-1) 
        sh = re.escape(src)
        if dst is None:
            dst = ""
        repl,nums = re.subn(sh, dst, fp)
        if nums <=0:
            return
        file(self.fileName, "w").write(repl)
        if addToBackup:
            self.backups.append([src, dst])

    def append_lines(self, match_lines, append_lines):
        fp = file(self.fileName).read(-1)
        sh = re.escape(match_lines)
        match = re.search(sh, fp, re.MULTILINE)
        if match is None:
            return

        sh = re.escape(append_lines)
        if re.search(sh, fp, re.MULTILINE) is not None:
            return

        newlines = []
        for line in file(self.fileName).readlines():
            if re.search(match_lines, line) is not None:
                newlines.append(line + append_lines)
                self.backups.append([line, line + append_lines])
            else:
                newlines.append(line)

        file(self.fileName, "w").writelines(newlines)
            
    def backup(self):
        for oldLine, newLine in self.backups:
            if newLine is None:
                self.add_lines(oldLine, False)
            else:
                self.replace_lines(newLine, oldLine, False)
