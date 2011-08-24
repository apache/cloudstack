#!/usr/bin/env python

from datetime import datetime
import sys
import matplotlib.cbook as cbook
import matplotlib.image as image
import matplotlib.pyplot as plt

def formGraph(list_timedelta):
    """ Draw a graph of the performance of host add response """
    plt.plot(list(map(lambda x:x.seconds, list_timedelta)))
    plt.ylabel("time(s) to add host")
    plt.xlabel("number of hosts")
    plt.title("Add Host Performance")
    plt.grid(True)
    plt.show()


if __name__=='__main__':
    time_file=open(sys.argv[1], 'r')
    timelist=[]
    diffs=[]
    for line in time_file.readlines():
        try:
            timelist.append(datetime.strptime(line.strip(), "%d %b %Y %H:%M:%S"))
        except ValueError:
            print "Unable to parse:",line

    stime=timelist[:-1]
    btime=timelist[1:]

    diffs=list(map(lambda x,y: y - x,stime,btime))
    
    formGraph(diffs)

    

