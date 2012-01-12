'''
/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
'''
from optparse import OptionParser
import sys
import os, os.path
import fnmatch

class CopyRightDetecter(object):
    def isCopyRightLine(self, txt):
        return False

class KeyWordCopyRightDetecter(CopyRightDetecter):
    keywords = ['Cloud.com', 'Copyright', '(C)', '2011', 'Citrix', 'Systems', 'Inc', 'All', 'rights', 'reserved', 'This', 'software', 'is', 'licensed', 'under', 'the', 'GNU', 'General', 'Public', 'License', 'v3', 'or', 'later', 'It', 'is', 'free', 'software:', 'you', 'can', 'redistribute', 'it', 'and/or', 'modify', 'it', 'under', 'the', 'terms', 'of', 'the', 'GNU', 'General', 'Public', 'License', 'as', 'published', 'by', 'the', 'Free', 'Software', 'Foundation', 'either', 'version', '3', 'of', 'the', 'License', 'or', 'any', 'later', 'version', 'This', 'program', 'is', 'distributed', 'in', 'the', 'hope', 'that', 'it', 'will', 'be', 'useful', 'but', 'WITHOUT', 'ANY', 'WARRANTY;', 'without', 'even', 'the', 'implied', 'warranty', 'of', 'MERCHANTABILITY', 'or', 'FITNESS', 'FOR', 'A', 'PARTICULAR', 'PURPOSE', 'See', 'the', 'GNU', 'General', 'Public', 'License', 'for', 'more', 'details', 'You', 'should', 'have', 'received', 'a', 'copy', 'of', 'the', 'GNU', 'General', 'Public', 'License', 'along', 'with', 'this', 'program', 'If', 'not', 'see', '<http://www.gnu.org/licenses/>']
    
    def isCopyRightLine(self, txt):
        words = [ c.strip().strip('.').strip('\n').strip(',') for c in txt.split(" ") ]
        total = len(words)
        if total == 0: return False
        
        numKeyWord = 0
        for w in words:
            if w in self.keywords: numKeyWord+=1
            
        if float(numKeyWord)/float(total) > float(1)/float(3): return True
        return False

copyRightDetectingFactory = {"KeyWord":KeyWordCopyRightDetecter.__name__}


class Logger(object):
    @staticmethod
    def info(msg):
        sys.stdout.write("INFO: %s"%msg)
        sys.stdout.write("\n")
        sys.stdout.flush()
    
    @staticmethod
    def debug(msg):
        sys.stdout.write("DEBUG: %s"%msg)
        sys.stdout.write("\n")
        sys.stdout.flush()
    
    @staticmethod
    def warn(msg):
        sys.stdout.write("WARNNING: %s"%msg)
        sys.stdout.write("\n")
        sys.stdout.flush()
    
    @staticmethod
    def error(msg):
        sys.stderr.write("ERROR: %s"%msg)
        sys.stderr.write("\n")
        sys.stderr.flush()
    

class Adder(object):
    defaultTxt = '''Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
    
This software is licensed under the GNU General Public License v3 or later.

It is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
'''
    copyRightTxt = None
    targetFile = None
    fileBody = None
    decter = None
    
    COMMENT_NOTATION = ""
    
    def __init__(self):                
        self.decter = eval(copyRightDetectingFactory["KeyWord"])()
    
    def setTargetFile(self, fpath):
        self.targetFile = fpath
        if not os.path.exists(self.targetFile):
            raise Exception("Cannot find %s"%self.targetFile)
    
    def setCopyRightTxt(self, txt):
        self.copyRightTxt = txt.split("\n")
    
    def checkParams(self):
        assert self.targetFile != None, "Target file not set"
        if self.copyRightTxt == None:
            self.copyRightTxt = self.defaultTxt.split("\n")
        
    def pasteCopyRight(self):
        self.fileBody = self.copyRightTxt + self.fileBody
        file(self.targetFile, 'w').write("".join(self.fileBody))
        Logger.info("Added copyright header to %s"%self.targetFile)
    
    def composeCopyRight(self):
        l = self.copyRightTxt
        l = [ self.COMMENT_NOTATION + " " + line + "\n" for line in l ]
        self.copyRightTxt = l
    
    def getFileBody(self):
        self.fileBody = file(self.targetFile).readlines()
    
    def isCommentLine(self, line):
        if line.lstrip().startswith(self.COMMENT_NOTATION):
            return True
        return False
    
    def removeOldCopyRight(self):
        newBody = []
        removed = False
        for line in self.fileBody:
            if self.isCommentLine(line) and self.decter.isCopyRightLine(line): removed = True; continue
            newBody.append(line)
            
        self.fileBody = newBody
        if removed:
            Logger.info("Removed old copyright header of %s"%self.targetFile)
    
    def cleanBlankComment(self):
        newBody = []
        for l in self.fileBody:
            if self.isCommentLine(l) and l.strip(self.COMMENT_NOTATION).strip().strip('\n') == "":
                continue
            newBody.append(l)
        self.fileBody = newBody
                
    def doWork(self):
        self.checkParams()
        self.getFileBody()
        self.removeOldCopyRight()
        self.composeCopyRight()
        self.cleanBlankComment()
        self.pasteCopyRight()
        
    
class SqlAdder(Adder):
    def __init__(self):
        super(SqlAdder, self).__init__()
        self.COMMENT_NOTATION = "#"
            

copyRightAdderFactory = {".sql":SqlAdder.__name__}           
class CopyRightAdder(object):
    parser = None
    options = None
    args = None
    targetFiles = None
    excludeFiles = None
    copyRightFileTxt = None
    rootDir = None
    
    def errAndExit(self, msg):
        Logger.error(msg)
        Logger.info("addcopyright -h for help")
        sys.exit(1)
        
    def __init__(self):
        usage = '''Usage: addcopyright [file_name_pattern] [--exculdes=file_name_pattern] [--file=copyright_file] [--root=root_dir_of_files_to_add_header]
Examples:
    addcopyright
    addcopyright *.sql
    addcopyright *.sql --excludes="*.sql~"
    addcopyright *.sql --file=/root/Citrix.copyright
    addcopyright *.sql --file=/root/Citrix.copyright --root=~/cloudstack-oss
        '''
        self.parser = OptionParser(usage=usage)
        self.parser.add_option("", "--excludes", action="store", type="string", dest="excludes", default="",
                               help="Exclude these files when adding copyright header")
        self.parser.add_option("", "--file", action="store", type="string", dest="copyRightFile", default="",
                               help="Path to copyright header file. Default to Citrix copyright header")
        self.parser.add_option("", "--root", action="store", type="string", dest="rootDir", default="",
                               help="Root folder where files being added copyright header locate. Default to current directory")
        (self.options, self.args) = self.parser.parse_args()
        if len(self.args) > 1:
            self.errAndExit("Invalid arguments:%s" % self.args)
        
        if len(self.args) == 1:
            self.targetFiles = self.args[0]
        if self.options.excludes != "":
            self.excludeFiles = self.options.excludes
        if self.options.copyRightFile != "":
            self.copyRightFileTxt = file(self.options.copyRightFile).read()
        if self.options.rootDir != "":
            self.rootDir = os.path.expanduser(self.options.rootDir)
            if not os.path.isdir(self.rootDir): raise Exception("Cannot find directory %s"%self.rootDir)
        else:
            self.rootDir = os.getcwd()

    def createConcreteAdder(self, filename):
        (x, ext) = os.path.splitext(filename)
        if not copyRightAdderFactory.has_key(ext): return None
        return eval(copyRightAdderFactory[ext])()
    
    def run(self):
        for root, dirs, files in os.walk(self.rootDir):
            for f in files:
                fpath = os.path.join(root, f)
                if self.excludeFiles != None and fnmatch.fnmatch(f, self.excludeFiles):
                    Logger.info("Skipping excluded file %s" % fpath)
                    continue
                if self.targetFiles != None and not fnmatch.fnmatch(f, self.targetFiles):
                    Logger.info("Skipping %s not matching our file name pattern" % fpath)
                    continue
                
                adder = self.createConcreteAdder(f)
                if adder == None:
                    Logger.warn("Cannot find a proper copyright Adder for %s, skip it" % fpath)
                    continue
                
                adder.setTargetFile(fpath)
                if self.copyRightFileTxt != None:
                    adder.setCopyRightTxt(self.copyRightFileTxt)
                adder.doWork()
                
        
if __name__ == '__main__':
    task = CopyRightAdder()
    task.run()