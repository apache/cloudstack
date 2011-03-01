XEN_ROOT = ../../..
BLKTAP_ROOT := ..
include $(XEN_ROOT)/tools/Rules.mk

ifeq ($(LVM_UTIL_TEST),y)
TEST              := lvm-util
endif

CFLAGS            += -Werror
CFLAGS            += -Wno-unused
CFLAGS            += -I../include
CFLAGS            += -D_GNU_SOURCE

ifeq ($(CONFIG_X86_64),y)
CFLAGS            += -fPIC
endif

# Get gcc to generate the dependencies for us.
CFLAGS            += -Wp,-MD,.$(@F).d
DEPS               = .*.d

LVM-OBJS          := lvm-util.o

all: build

build: $(TEST) $(LVM-OBJS)

install: all

lvm-util: lvm-util.o
	$(CC) $(CFLAGS) -DLVM_UTIL $(LDFLAGS) -o lvm-util lvm-util.c

clean:
	rm -rf *.o *~ $(DEPS) $(IBIN)

.PHONY: all build clean install lvm-util

-include $(DEPS)
