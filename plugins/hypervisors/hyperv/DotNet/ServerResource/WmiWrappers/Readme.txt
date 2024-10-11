Classes were generated on a hyper-v server using the Visual Studio GUI, but you can do the same using
mgmtclassgen.exe.  Below are some examples:

mgmtclassgen.exe Msvm_ComputerSystem /N root\virtualization /L CS /O CloudStack.Plugin.WmiWrappers /P ComputerSystem.cs
mgmtclassgen.exe Msvm_VirtualSystemManagementService /N root\virtualization /L CS /O CloudStack.Plugin.WmiWrappers /P VirtualSystemManagementService.cs
mgmtclassgen.exe Msvm_VirtualSystemGlobalSettingData /N root\virtualization /L CS /O CloudStack.Plugin.WmiWrappers /P VirtualSystemGlobalSettingData.cs


BUT, you have to tweak the generated code, because it does not deal with NULL method parameters properly.
E.g. when a method completes immediately, the returned out parameters include a "Job" property that has a NULL value.
The generated code will attempt to call ToString() on this NULL value.

ALSO, you have to tweak the generated code to expose useful details such as the WMI name for the class.
E.g. the generated code creates a wrapper called class ComputerSystem for WMI objects of class Msvm_ComputerSystem.
Thus, there is a mismatch in the class name and the corresponding WMI class, and the WMI class name is a private
constant static member.  The tweak involves making this member public.
