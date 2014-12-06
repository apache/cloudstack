acs-api-commands
================

Collection of API commands.xml of Apache CloudStack versions. used to generate
Apache CloudStack Release Notes.


Build API doc
-------------

```bash
mvn -Pdeveloper -Dnoredist clean install -DskipTests=true
mvn -Pdeveloper -Dnoredist clean install -pl :cloud-apidoc
```

```bash
cd cloudstack
java -cp ~/.m2/repository/com/thoughtworks/xstream/xstream/1.4.3/xstream-1.4.3.jar:server/target/classes com.cloud.api.doc.ApiXmlDocReader -old ~/Documents/acs-api-commands/4.4.2_commands.xml -new ~/Documents/acs-api-commands/4.5.0_commands.xml -d ~/Documents/acs-api-commands/diff-442-450
```