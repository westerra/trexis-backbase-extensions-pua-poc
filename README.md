## treXis Backbase Extensions
The treXis Backbase extensions repository is a single project containing all Backbase extentions that is then embedded as dependencies in other services.

This repository is utilizing Backbase depedencies, which require configuration of your local maven environment to have access to Backbase depedencies.  Consult Backbase Support for your credentials.

Perform these commands to build this repository :
```
mvn clean install
```

### Versions and Upgrade
The service is developed using the Backbase Service SDK.  The service contains the dependency to the correct service SDK used to package the service, update the dependency to upgrade this to the SDK matching your deployment.

### Exclude yourself as a contact
If you don't want to allow yourself as a contact then you must need accountNumber mapper in client (Bb-web-client) with these values :
```
In Identity ->  Clients -> Bb-web-client -> Mappers -> Create
Name                : accountNumber
Mapper Type         : User Attribute
User Attribute      : accountNumber
Token Claim Name    : accountNumber
Claim JSON Type     : String
Add to ID token     : ON
Add to access token : ON
Add to userinfo     : ON
Multivalued         : OFF
Aggregate attribute values : OFF
```

### About treXis Co-Develop
The treXis co-development program allow treXis customers and partners to clone/fork code repositories from the treXis Bitbucket repository.  A list of all accelerators are published on the https://experts.trexis.net.

The <a href="license.txt">license.txt</a> file describe the developer licence agreement for using this repository.