

IBIN               = vhd-util vhd-update

CFLAGS            += -m32
CFLAGS            += -Werror
CFLAGS            += -Wno-unused
CFLAGS            += -I../include
CFLAGS            += -D_GNU_SOURCE

ifeq ($(CONFIG_X86_64),y)
CFLAGS            += -fPIC
endif

LIBS              += -luuid

# Get gcc to generate the dependencies for us.
CFLAGS            += -Wp,-MD,.$(@F).d
DEPS               = .*.d

all: libs build

build: $(IBIN)


vhd-util: vhd-util.o $(LIBS_DEPENDS)
	$(CC) $(CFLAGS) -o vhd-util vhd-util.o lib/libvhd.a $(LDFLAGS) $(LIBS)

vhd-update: vhd-update.o $(LIBS_DEPENDS)
	$(CC) $(CFLAGS) -o vhd-update vhd-update.o lib/libvhd.a  $(LDFLAGS) $(LIBS)

libs: 
	$(MAKE) -C lib all

clean: 
	rm -rf *.o *~ $(DEPS) $(IBIN)
	$(MAKE) -C lib clean

PHONY: all clean vhd-util vhd-update lib

-include $(DEPS)
