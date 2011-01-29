# Simple Makefile for creating a native binary and a legacy jar file with gcj.

GCJ=gcj
GCJ_FLAGS=-g -O2

#JAVAC=$(GCJ) -C
#JAVAC_FLAGS=$(GCJ_FLAGS)
JAVAC=jikes-classpath
JAVAC_FLAGS=-g -O -source 1.5

JAR=fastjar

PROGRAM=snark
MAINCLASS=org.klomp.snark.Snark

SOURCES= \
org/klomp/snark/BitField.java \
org/klomp/snark/ConnectionAcceptor.java \
org/klomp/snark/CoordinatorListener.java \
org/klomp/snark/HttpAcceptor.java \
org/klomp/snark/MetaInfo.java \
org/klomp/snark/Message.java \
org/klomp/snark/Peer.java \
org/klomp/snark/PeerID.java \
org/klomp/snark/PeerAcceptor.java \
org/klomp/snark/PeerCheckerTask.java \
org/klomp/snark/PeerConnectionIn.java \
org/klomp/snark/PeerConnectionOut.java \
org/klomp/snark/PeerListener.java \
org/klomp/snark/PeerMonitorTask.java \
org/klomp/snark/PeerCoordinator.java \
org/klomp/snark/PeerState.java \
org/klomp/snark/Request.java \
org/klomp/snark/Snark.java \
org/klomp/snark/SnarkShutdown.java \
org/klomp/snark/ShutdownListener.java \
org/klomp/snark/Storage.java \
org/klomp/snark/StorageListener.java \
org/klomp/snark/Tracker.java \
org/klomp/snark/TrackerClient.java \
org/klomp/snark/TrackerInfo.java \
org/klomp/snark/bencode/BEValue.java \
org/klomp/snark/bencode/BEncoder.java \
org/klomp/snark/bencode/BDecoder.java \
org/klomp/snark/bencode/InvalidBEncodingException.java

STATIC_SOURCES=$(SOURCES) org/klomp/snark/StaticSnark.java
STATIC_MAINCLASS=org.klomp.snark.StaticSnark

GNOME_SOURCES=$(SOURCES) org/klomp/snark/SnarkGnome.java \
	org/klomp/snark/GnomeInfoWindow.java \
	org/klomp/snark/GnomePeerList.java
GNOME_MAINCLASS=org.klomp.snark.SnarkGnome

$(PROGRAM): $(SOURCES)
	$(GCJ) $(GCJ_FLAGS) --main=$(MAINCLASS) -o $(PROGRAM) $(SOURCES)

$(PROGRAM)-static: $(STATIC_SOURCES)
	$(GCJ) $(GCJ_FLAGS) -static --main=$(STATIC_MAINCLASS) \
		-o $(PROGRAM)-static $(STATIC_SOURCES)

$(PROGRAM)-gnome: $(GNOME-SOURCES)
	$(JAVAC) $(JAVAC_FLAGS) \
		-classpath /usr/share/java/gtk2.8.jar:/usr/share/java/gnome2.12.jar \
		-d dist/classes $(GNOME_SOURCES)

$(PROGRAM).jar: $(PROGRAM)-classes Manifest
	$(JAR) cfm $(PROGRAM).jar Manifest -C dist/classes/ .

Manifest:
	echo "Main-Class: $(MAINCLASS)" > Manifest

classes:
	mkdir dist
	mkdir dist/classes

$(PROGRAM)-classes: classes $(SOURCES)
	$(JAVAC) $(JAVAC_FLAGS) -d dist/classes $(SOURCES)

all: $(PROGRAM) $(PROGRAM)-static $(PROGRAM).jar

clean:
	rm -rf dist Manifest $(PROGRAM) $(PROGRAM)-static $(PROGRAM).jar

.PHONY : all clean $(PROGRAM)-classes
