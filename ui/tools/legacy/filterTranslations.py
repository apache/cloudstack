# coding: utf-8

import json
import os
import sys

def loadJson(lfile):
    ldata = lfile.read()
    cont = ldata.split("var dictionary =")
    if len(cont) != 2:
        print "Unexpected format for file " + lfile + ". Expected `var dictionary =` from old source code"
        exit(1)

    trans = cont[1].strip().replace("\n", "")
    if trans[-1] == ";":
        trans = trans[0: -1]

    try:
        return json.loads(trans)
    except expression as identifier:
        print "Something went wrong in parsing old files. Perhaps incorrect formatting?"
        exit(1)

def loadTranslations(l10repo):
    with open("fieldsFromOldLayout.json") as outfile:
        oldLayout = json.load(outfile)

    fieldsFromOldLayout = oldLayout["allFields"]
    actionsFromOldLayout = oldLayout["actions"]

    with open("manualNeededLabels.json") as outfile:
        manualNeededLabels = json.load(outfile)

    newTranslations = {}
    for r, d, f in os.walk(l10repo):
        for file in f:
            print file
            if '.js' in file:
                with open(os.path.join(r, file)) as oldfile:
                    oldTrans = loadJson(oldfile)
                    print len(oldTrans.keys())
                    newTrans = {}
                    for apikey in fieldsFromOldLayout:
                        currLabel = fieldsFromOldLayout[apikey]["labels"][0] # Just use the first label for now in case multiple labels exist
                        if currLabel in oldTrans:
                            newTrans[apikey] = oldTrans[currLabel]
                    for label in manualNeededLabels:
                        if label in oldTrans:
                            newTrans[manualNeededLabels[label]] = oldTrans[label]
                        else:
                            newTrans[manualNeededLabels[label]] = manualNeededLabels[label]

                    for a in actionsFromOldLayout:
                        actions = actionsFromOldLayout[a]
                        for action in actions:
                            if not "label" in action:
                                continue
                            curLabel = action["label"]
                            if curLabel in oldTrans:
                                newTrans[curLabel] = oldTrans[curLabel]
                            else:
                                print "Not found translation for " + curLabel

                            if "keys" in action:
                                curKeys = action["keys"]
                                for key in curKeys:
                                    curLabel = curKeys[key]["label"]
                                    if curLabel in oldTrans:
                                        newTrans[key] = oldTrans[curLabel]
                                    else:
                                        print "Not found translation for " + curLabel


                    newTranslations[file] = newTrans

    for file in newTranslations:
        with open("../src/locales/" + file[:-3] + ".json", "w") as newT:
            json.dump(newTranslations[file], newT, sort_keys=True, indent=4)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print "Usage: fileTranslations.py $CLOUDSTACK_CODE_REPO"
        exit(1)

    cldstk = sys.argv[1]
    l10repo = os.path.join(cldstk, "ui/l10n")
    if not os.path.isdir(l10repo):
        print "Invalid translations location sent: " + l10repo + " does not exists"
        exit(1)

    loadTranslations(l10repo)
    exit(0)