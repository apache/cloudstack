actions :create, :delete
default_action(:create)

attribute(:device)
attribute(:object)
attribute(:cidrs)
attribute(:index)
attribute(:bdev)

attr_accessor :exists
attr_accessor :up
attr_accessor :contrack
attr_accessor :configured
attr_accessor :device
