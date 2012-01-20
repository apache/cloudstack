#script to start multiple agents on one host
num=$1
port=8787
while [ $num -gt 0 ]
do
let "port=$port + $num"
java -Xrunjdwp:transport=dt_socket,address=$port,server=y,suspend=n  -cp ./'*' com.cloud.agent.AgentShell &
let "num=$num - 1"
done
