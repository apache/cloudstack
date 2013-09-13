Feature: Deploy a VirtualMachine

Scenario: Deploy a VirtualMachine
  Given: we have an advanced zone
  when: I deploy a virtualmachine
  then: virtualmachine is deployed
