require 'spec_helper'

describe file('/etc/cloudstack-release') do
  it { should be_file }
  its(:content) { should match /Cloudstack Release [0-9]+(\.[0-9]+)+/ }
end
