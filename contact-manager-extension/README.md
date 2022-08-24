# user-manager extension.

Extension for `contact-manager` that is intended to be overlaid on the BackBase build artifact.

## Features

Overrides the `CreateContactRoute` route to validate contact information using finite before successfully adding a contact.

## Configuration

- `contact-manager-extension.last-name-validate-first-characters`: : First x number of characters in the last name to validate against, **default 3**
- `contact-manager-extension.finiteEntityIdentifierClaim`: : The name of the entity identifier claim, **default entityId**
