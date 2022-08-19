package com.backbase.dbs.contactmanager.extension;

import com.backbase.buildingblocks.backend.communication.extension.annotations.BehaviorExtension;
import com.backbase.buildingblocks.backend.communication.extension.annotations.PostHook;
import com.backbase.buildingblocks.backend.communication.extension.annotations.PreHook;
import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.dbs.contactmanager.contact.dto.AccountInformation;
import com.backbase.dbs.contactmanager.contact.dto.Contact;
import com.backbase.dbs.contactmanager.contact.dto.InternalRequestWrapper;
import com.backbase.dbs.contactmanager.contact.route.CreateContactRouteBuilder;
import com.backbase.dbs.contactmanager.extension.config.ContactManagerExtensionConfiguration;
import com.backbase.dbs.contactmanager.rest.spec.client.v2.model.ContactsPostRequestBody;
import com.finite.api.EntityApi;
import com.finite.api.model.EntityProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.internal.Internal;
import org.apache.camel.Exchange;
import org.codehaus.plexus.util.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Behavior Extension for the @see <a
 * href="https://developer.backbase.com/api/specs/contact-manager/contact-manager-client-api/2.19.1/operations/Contacts/postContacts">CreateContactRoute</a> Adds validation when
 * adding a contact
 */
@BehaviorExtension(
        // The name parameter is used as a configuration key to enable/disable this specific extension.
        // For example, "backbase.behavior-extensions.add-contact-route-extension.enabled=false".
        // (Extensions are enabled by default.)
        name = "add-contact-route-extension",
        routeId = CreateContactRouteBuilder.ROUTE_ID
)
@Slf4j
@RequiredArgsConstructor
@Service
public class AddContactRouteExtension {

    private final EntityApi entityApi;
    private final ContactManagerExtensionConfiguration extensionConfiguration;

    @PreHook
    public void preHook(InternalRequestWrapper<Contact> request, Exchange exchange) {
        var body = request.getInternalRequest().getData();
        var contactEntityId = body.getAccounts().stream()
                .findFirst()
                .map(AccountInformation::getAccountNumber)
                .orElseThrow(() -> new BadRequestException("Contact's account number is required"));
        var contactName = body.getName();

        if (StringUtils.isEmpty(contactName)) throw new BadRequestException("Contact's name is required");

        EntityProfile entityProfile;
        try {
            entityProfile = entityApi.getEntityProfile(contactEntityId, null, false, false, null, null);
        } catch (RestClientException rce) {
            log.error("Unable to get entity profile to validate contact being added!");
            throw new BadRequestException("Unable to validate contact with information provided", rce);
        }

        var entityLastName = entityProfile.getLastname();
        if (StringUtils.isEmpty(entityLastName)) throw new InternalServerErrorException("Unable to validate contact, entity has no last name field");

        int charactersToValidate = Math.min(entityLastName.length(), extensionConfiguration.getLastNameValidateFirstCharacters());

        var expected = StringUtils.substring(contactName, 0, charactersToValidate).toUpperCase();

        if (!entityLastName.toUpperCase().startsWith(expected)) {
            log.warn("Contact failed validation for input: {}", contactName);
            throw new BadRequestException("Contact failed validation");
        }
    }
}
