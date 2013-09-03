## Idea Stack

### Bugs

- **marvin.sync and xml compilation produce different versions of cloudstackAPI**
- marvin build now requires inflect which will cause -Pdeveloper profile to break for the first time
- Entities should include @docstring for optional arguments in their actions() methods. **kwargs is confusing
- Dissociate the grammar list to make it extensible via a properties file
- Handle APIs that need parameters but dont have a required args list because multiple sets of args form a required list
	- eg: disableAccount (either provide id (account) or accoutname and domainid)
- XML precache required for factory and base generation [CLOUDSTACK-4589](https://issues.apache.org/jira//browse/CLOUDSTACK-4589)
- Remove marvin dependency with apidoc build. Provide precache json [CLOUDSTACK-4589](https://issues.apache.org/jira//browse/CLOUDSTACK-4589)
- Better sync functionality
- Bump up version to 0.2.0
- Improved cleanup support

### Features
- Export deployment to JSON [CLOUDSTACK-4590](https://issues.apache.org/jira//browse/CLOUDSTACK-4590)
- nose2 and unittest2 support [CLOUDSTACK-4591](https://issues.apache.org/jira//browse/CLOUDSTACK-4591)
- Use distutils
- Python pip repository for cloudstack-marvin
- DSL for marvin using Behave [CLOUDSTACK-1952](https://issues.apache.org/jira/browse/CLOUDSTACK-1952)