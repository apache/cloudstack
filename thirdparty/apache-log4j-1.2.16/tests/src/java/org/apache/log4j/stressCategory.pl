# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This perl script all combinations of tree structures based on the "args"
# array.

# For example, if args = ("a", "b", "c"), it will output:
#
# stressTest  a b c
# stressTest  a b a.c
# stressTest  a b b.c
# stressTest  a a.b c
# stressTest  a a.b a.c
# stressTest  a a.b a.b.c

$prefix = "stressTest";

@tree = ("");

@args = ("a", "b");
permute(0, @tree);

@args = ("a", "b", "c");
permute(0, @tree);

@args = ("a", "b", "c", "d");
permute(0, @tree);

@args = ("a", "b", "c", "d", "e");
permute(0, @tree);

@args = ("a", "b", "c", "d", "e", "f");
permute(0, @tree);


sub permute() {
  my ($i, @t)  = @_;
  #print "Tree is @t\n";
  #print "i is  $i \n";
  
  if($i == $#args + 1) {
    print "$prefix @t\n";
    return;
  }
  
  foreach $j (@t) {
    #print "J is $j \n";
    #print "args[$i]=$args[$i]\n";
    if($j eq "") {
      $next = "$args[$i]";
    }
    else {
      $next = "$j.$args[$i]";
    }
	
    permute($i+1, (@t, $next));
  }
  
}
