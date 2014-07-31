#!/bin/bash

set -e
set -x

# script invoked by Test-Kitchen shell provisioner to further
# customize the VM prior to running tests

function setup_networking() {
  # for internet access
  if [[ ! `grep eth1 /etc/network/interfaces` ]]; then
    cat >>/etc/network/interfaces <<END

iface eth1 inet dhcp
auto eth1
END
  fi
  ifup eth1
}

function install_packages() {
  export DEBIAN_FRONTEND=noninteractive
  export DEBIAN_PRIORITY=critical

  local apt_get="apt-get -q -y --force-yes"

  ${apt_get} update

  if [[ ! `which curl` ]]; then
    ${apt_get} install curl
  fi
  if [[ ! `which patch` ]]; then
    ${apt_get} install patch
  fi
  if [[ ! `which git` ]]; then
    ${apt_get} install git
  fi
}

function install_chef() {
  if [[ ! -f '/opt/chef/embedded/bin/gem' ]]; then
    curl -L https://www.opscode.com/chef/install.sh | bash
  fi
}
function add_junit_reports_to_serverspec() {
  local gem="/opt/chef/embedded/bin/gem"
  local gem_install="${gem} install --no-rdoc --no-ri"

  ${gem_install} rspec rspec_junit_formatter

  if [[ ! -d 'serverspec' ]]; then
    git clone https://github.com/serverspec/serverspec.git
    (cd serverspec; git submodule update --init --recursive)
  fi
  cd serverspec
  git reset --hard
  cat >serverspec.patch <<END
diff -u serverspec.gemspec serverspec-spec3.gemspec
--- serverspec.gemspec
+++ serverspec.gemspec
@@ -19,7 +19,7 @@ Gem::Specification.new do |spec|
   spec.require_paths = ["lib"]

   spec.add_runtime_dependency "net-ssh"
-  spec.add_runtime_dependency "rspec", "~> 2.99"
+  spec.add_runtime_dependency "rspec", [">= 2.99", '< 4.0']
   spec.add_runtime_dependency "rspec-its"
   spec.add_runtime_dependency "highline"
   spec.add_runtime_dependency "specinfra", "~> 1.22"
END
  patch -p0 <serverspec.patch

  ${gem} build serverspec.gemspec
  ${gem_install} serverspec-*.gem
  cd ..


  if [[ ! -d 'busser-serverspec' ]]; then
    git clone https://github.com/test-kitchen/busser-serverspec.git
  fi
  cd busser-serverspec
  git reset --hard
  cat >busser-serverspec.patch <<END
diff -u lib/busser/serverspec/runner.rb lib/busser/serverspec/runner-spec3.rb
--- lib/busser/serverspec/runner.rb
+++ lib/busser/serverspec/runner.rb
@@ -42,7 +42,7 @@ RSpec::Core::RakeTask.new(:spec) do |t|
   end

   t.rspec_path = rspec_bin if rspec_bin
-  t.rspec_opts = ['--color', '--format documentation']
+  t.rspec_opts = ['--color', '--format documentation', '--format RspecJunitFormatter', '--out /tmp/rspec.xml']
   t.ruby_opts = "-I#{base_path}"
   t.pattern = "#{base_path}/**/*_spec.rb"
 end
END

  patch -p0 <busser-serverspec.patch
  ${gem} build busser-serverspec.gemspec
  ${gem_install} busser-serverspec-*.gem
  cd ..
}

function main() {
  setup_networking
  install_packages
  install_chef
  add_junit_reports_to_serverspec
}

# we only run main() if not source-d
return 2>/dev/null || main
