Facter.add(:xen_hostuuid) do
  setcode do
    uuid=Facter::Util::Resolution.exec('xe host-list |grep uuid|awk \'{print $5}\'')
  end
end