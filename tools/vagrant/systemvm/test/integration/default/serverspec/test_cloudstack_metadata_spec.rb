require 'serverspec'

include Serverspec::Helper::Exec
include Serverspec::Helper::DetectOS

RSpec.configure do |c|
  c.before :all do
    c.path = '/sbin:/usr/sbin'
  end
end

describe file('/etc/cloudstack-release') do
  it { should be_file }
  its(:content) { should match /Cloudstack Release [0-9]+(\.[0-9]+)+/ }
end
