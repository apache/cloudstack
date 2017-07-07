acs-api-commands
================

Collection of API commands.xml of [Apache CloudStack](http://cloudstack.apache.org/) versions. used to generate
[Apache CloudStack Release Notes](http://docs.cloudstack.apache.org/projects/cloudstack-release-notes).

[How To Generate CloudStack API Documentation](https://cwiki.apache.org/confluence/display/CLOUDSTACK/How+To+Generate+CloudStack+API+Documentation)


Install NonOSS Dependencies
---------------------------

```bash
$ cd /tmp
$ git clone https://github.com/rhtyd/cloudstack-nonoss.git nonoss
$ cd nonoss && bash -x install-non-oss.sh
```

Build API doc
-------------

```bash
$ cd /path/to/cloudstack
$ git fetch <upstream>
$ git checkout master
$ git checkout <release_commit>
$ mvn -Pdeveloper -Dnoredist clean install -DskipTests=true
$ mvn -Pdeveloper -Dnoredist clean install -pl :cloud-apidoc
```

Generate Diff
-------------

```bash
$ cd /path/to/cloudstack
$ export COMMANDS=/path/to/acs-api-commands
$ export OLD_RELEASE=4.9.0
$ export NEW_RELEASE=4.10.0
$ cp tools/apidoc/target/commands.xml $COMMANDS/${NEW_RELEASE}_commands.xml
$ mkdir $COMMANDS/diff-${OLD_RELEASE//.}-${NEW_RELEASE//.}
$ java -cp $HOME/.m2/repository/com/thoughtworks/xstream/xstream/1.4.9/xstream-1.4.9.jar:$HOME/.m2/repository/com/google/code/gson/gson/1.7.2/gson-1.7.2.jar:server/target/classes com.cloud.api.doc.ApiXmlDocReader -old $COMMANDS/${OLD_RELEASE}_commands.xml -new $COMMANDS/${NEW_RELEASE}_commands.xml -d $COMMANDS/diff-${OLD_RELEASE//.}-${NEW_RELEASE//.}
```

