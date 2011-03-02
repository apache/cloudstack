SUBDIRS-y :=
SUBDIRS-y += lvm
SUBDIRS-y += vhd

%.o: %.c
	$(CC) $(CPPFLAGS) $(CFLAGS) -c -o $@ $<

all:
	for subdir in $(SUBDIRS-y); do \
		$(MAKE) -C $$subdir all; \
	done


clean:
	for subdir in $(SUBDIRS-y); do \
		$(MAKE) -C $$subdir clean; \
	done

	rm -rf *.a *.so *.o *.rpm $(LIB) *~

.PHONY: all clean
