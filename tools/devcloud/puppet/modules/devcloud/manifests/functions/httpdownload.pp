 define devcloud::functions::httpdownload () {
  $file="${name['basedir']}/${name['basefile']}"

  exec {
    "getfileifnotexist${name}":
      command => "/usr/bin/wget ${name['url']}/${file}  -O ${name['local_dir']}/${file}",
      timeout => 0,
      unless  => "test -f ${name['local_dir']}/${file}",
      require => [ File["${name['local_dir']}/${name['base_dir']}/"],
                   Exec["get_md5sums"] ];


    "getfileifnotmatch${name}":
      command => "/usr/bin/wget ${name['url']}/${file} -O ${name['local_dir']}/${file}",
      timeout => 0,
      unless  => "/usr/local/bin/compare.sh ${file} ${name['working_dir']} ",
      require => [  Exec["getfileifnotexist${name}"], File["/usr/local/bin/compare.sh"] ]
    }

}
