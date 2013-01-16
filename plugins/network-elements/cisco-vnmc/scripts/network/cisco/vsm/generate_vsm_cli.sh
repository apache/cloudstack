for ((vlanid=2000; vlanid <=2020; vlanid++)); 
do   
  #sed "s/vlanid/$vlanid/g" vsm_asa_inside_profiles;
  sed "s/vlanid/$vlanid/g" vservice_node ; 
done

