The publican.cfg file in this directory should point to the main book file that will build 100% of the content in the CloudStack documentation XML source files. Swap this .cfg file up to the cloudstack-docs directory to build everything.

For example, to build the English language total documentation set, which includes everything in the cloudstack-docs/en-US directory, use this publican.cfg file with a command like:

publican build --formats=html --langs=en-US