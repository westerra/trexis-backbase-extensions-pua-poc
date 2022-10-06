# user-manager extension.

Extension for `contact-manager` that is intended to be overlaid on the BackBase build artifact.

## Features

Overrides the `CreateContactRoute` route to validate contact information using finite before successfully adding a contact.

## Configuration

- `contact-manager-extension.last-name-validate-first-characters`: : First x number of characters in the last name to validate against, **default 3**
- `contact-manager-extension.finiteEntityIdentifierClaim`: : The name of the entity identifier claim, **default entityId**

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