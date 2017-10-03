# Use VERSION and RELEASE_CANDIDATE from the parent Makefile
# but if we do a local make docs in this directory just use
# these. The main Makefile ones are the ones to keep updated.
CPVER ?= 4.9
HSVER ?= 6.2
RC ?= 1

HTML = $(patsubst %.txt,%.html,$(shell find . -name \*.txt))

.PHONY: all clean

all: $(HTML)

clean: 
	rm -f $(HTML) 

%.html: %.txt
ifneq ("$(wildcard /etc/fedora-release)","")
	# Only check spelling on fedora and only if have tty
	-tty -s && aspell --dont-backup -p ./aspell.dict -c $(<F)
endif
	asciidoc -a "cpver=$(CPVER)" -a "hsver=$(HSVER)" -a "rc=$(RC)" -a icons -a data-uri -a toc2 -a toclevels=4 -d book -n $(<F)


# TODO: Add -f cloudstack.conf for CloudStack.
