acs-api-commands
================

Collection of API commands.xml of [Apache CloudStack](http://cloudstack.apache.org/) versions. used to generate
[Apache CloudStack Release Notes](http://docs.cloudstack.apache.org/projects/cloudstack-release-notes).

[How To Generate CloudStack API Documentation](https://cwiki.apache.org/confluence/display/CLOUDSTACK/How+To+Generate+CloudStack+API+Documentation)


Build API doc
-------------

```bash
mvn -Pdeveloper -Dnoredist clean install -DskipTests=true
mvn -Pdeveloper -Dnoredist clean install -pl :cloud-apidoc
```

Generate Diff
-------------

```bash
cd cloudstack
export COMMANDS=~/Documents/acs-api-commands
cp tools/apidoc/target/commands.xml $COMMANDS/4.9.0_commands.xml
mkdir $COMMANDS/diff-480-490
java -cp $HOME/.m2/repository/com/thoughtworks/xstream/xstream/1.4.9/xstream-1.4.9.jar:$HOME/.m2/repository/com/google/code/gson/gson/1.7.2/gson-1.7.2.jar:server/target/classes com.cloud.api.doc.ApiXmlDocReader -old $COMMANDS/4.8.0_commands.xml -new $COMMANDS/4.9.0_commands.xml -d $COMMANDS/diff-480-490
```

