CP=. 
for file in lib/*.jar
do
  CP=${CP}:$file
done

java -cp $CP:./cloud-tool.jar com.cloud.gate.tool.CloudS3CmdTool $@
