# Makefile to compile the Xen Java SDK bindings

BINDINGJAVAFILES := $(wildcard com/xensource/xenapi/*.java)
BINDINGCLASSFILES := $(BINDINGJAVAFILES:.java=.class)
BINDINGJAR := xenserver-5.6.100-1.jar

CLASSPATH := :../cloud-xmlrpc-client-3.1.3.jar:../cloud-xmlrpc-common-3.1.3.jar:../cloud-ws-commons-util-1.0.2.jar
EXAMPLECLASSPATH := :$(BINDINGJAR)$(CLASSPATH)

.PHONY: all
all: $(BINDINGJAR) tests
	@ :

# Target to compile the automated tests
.PHONY: tests
tests: $(patsubst %.java,%.class,$(wildcard *.java))
	@ :

.PHONY: docs
docs:
	javadoc -classpath ${CLASSPATH} -d doc -link http://java.sun.com/javase/6/docs/api com/xensource/xenapi/*.java

%.class: %.java
	javac -cp ${CLASSPATH} $^

$(BINDINGJAR): $(BINDINGCLASSFILES)
	jar -cvf $@ com/xensource/xenapi/*.class

.PHONY: cleanclass
cleanclass:
	rm -f com/xensource/xenapi/*.class
	rm -f *.class

.PHONY: cleanjar
cleanjar:
	rm -f $(BINDINGJAR)

.PHONY: cleandocs
cleandocs:
	rm -rf doc

.PHONY: clean
clean: cleanjar cleanclass cleandocs
	@ :
