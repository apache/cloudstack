acs-api-commands
================

Collection of API commands.xml of [Apache CloudStack](http://cloudstack.apache.org/) versions. used to generate
[Apache CloudStack Release Notes](http://docs.cloudstack.apache.org/projects/cloudstack-release-notes).

[How To Generate CloudStack API Documentation](https://cwiki.apache.org/confluence/display/CLOUDSTACK/How+To+Generate+CloudStack+API+Documentation)


Build API doc
-------------

```bash
cd cloudstack
git fetch <upstream>
git checkout master
git checkout <release_commit>
mvn -Pdeveloper -Dnoredist clean install -DskipTests=true
mvn -Pdeveloper -Dnoredist clean install -pl :cloud-apidoc
```

Generate Diff
-------------

```bash
cd cloudstack
export COMMANDS=/path/to/acs-api-commands
export OLD_RELEASE=4.9.0
export NEW_RELEASE=4.10.0
cp tools/apidoc/target/commands.xml $COMMANDS/$NEW_RELEASE_commands.xml
mkdir $COMMANDS/diff-${OLD_RELEASE//.}-${NEW_RELEASE//.}
java -cp $HOME/.m2/repository/com/thoughtworks/xstream/xstream/1.4.9/xstream-1.4.9.jar:$HOME/.m2/repository/com/google/code/gson/gson/1.7.2/gson-1.7.2.jar:server/target/classes com.cloud.api.doc.ApiXmlDocReader -old $COMMANDS/$OLD_RELEASE_commands.xml -new $COMMANDS/$NEW_RELEASE_commands.xml -d $COMMANDS/diff-${OLD_RELEASE//.}-${NEW_RELEASE//.}
```

