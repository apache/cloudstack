for ((vlanid=2002; vlanid <=2030; vlanid++)); 
do   
  #sed "s/vlanid/$vlanid/g" vsm_asa_inside_profiles;
  sed "s/vlanid/$vlanid/g" vservice_node ; 
done

