package com.backbase.dbs.contactmanager.extension;

import com.backbase.buildingblocks.backend.internalrequest.InternalRequest;
import com.backbase.buildingblocks.presentation.errors.BadRequestException;
import com.backbase.buildingblocks.presentation.errors.InternalServerErrorException;
import com.backbase.dbs.contactmanager.contact.dto.AccountInformation;
import com.backbase.dbs.contactmanager.contact.dto.Contact;
import com.backbase.dbs.contactmanager.contact.dto.InternalRequestWrapper;
import com.backbase.dbs.contactmanager.extension.config.ContactManagerExtensionConfiguration;
import com.finite.api.EntityApi;
import com.finite.api.model.EntityProfile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddContactRouteExtensionTest {

    AddContactRouteExtension addContactRouteExtension;
    public static final String ACCOUNT_NUMBER = "123456";
    public static final String LAST_NAME = "Davis";

    ContactManagerExtensionConfiguration extensionConfiguration = new ContactManagerExtensionConfiguration();
    @Mock
    EntityApi entityApi;
    @Mock
    InternalRequestWrapper<Contact> requestWrapper;
    @Mock
    InternalRequest<Contact> internalRequest;

    Contact requestBody;


    @BeforeEach
    void init() {
        extensionConfiguration.setLastNameValidateFirstCharacters(3);
        addContactRouteExtension = new AddContactRouteExtension(entityApi, extensionConfiguration);

        requestBody = Contact.builder()
                .accounts(List.of(AccountInformation.builder()
                        .accountNumber(ACCOUNT_NUMBER)
                        .build()))
                .name(LAST_NAME)
                .build();

        when(requestWrapper.getInternalRequest()).thenReturn(internalRequest);
        when(internalRequest.getData()).thenReturn(requestBody);
    }

    @Test
    public void testPreHook_happyPath() {
        when(entityApi.getEntityProfile(eq(ACCOUNT_NUMBER), isNull(), anyBoolean(), anyBoolean(), isNull(), isNull()))
                .thenReturn(new EntityProfile().lastname(LAST_NAME));

        addContactRouteExtension.preHook(requestWrapper, null);

        verify(entityApi, times(1))
                .getEntityProfile(eq(ACCOUNT_NUMBER), isNull(), anyBoolean(), anyBoolean(), isNull(), isNull());
    }

    @Test
    public void testPreHook_noAccountNumberInRequest_throwsBadRequestException() {
        requestBody = Contact.builder()
                .accounts(List.of(AccountInformation.builder()
                        .build()))
                .name(LAST_NAME)
                .build();
        when(requestWrapper.getInternalRequest()).thenReturn(internalRequest);
        when(internalRequest.getData()).thenReturn(requestBody);

        assertThrows(BadRequestException.class,
                () -> addContactRouteExtension.preHook(requestWrapper, null));
    }

    @Test
    void testPreHook_noNameInRequest_throwsBadRequestException() {
        requestBody = Contact.builder()
                .accounts(List.of(AccountInformation.builder()
                        .accountNumber(ACCOUNT_NUMBER)
                        .build()))
                .build();
        when(requestWrapper.getInternalRequest()).thenReturn(internalRequest);
        when(internalRequest.getData()).thenReturn(requestBody);


        assertThrows(BadRequestException.class,
                () -> addContactRouteExtension.preHook(requestWrapper, null));
    }

    @Test
    void testPreHook_entityProfileCallFails_throwsBadRequestException() {
        when(entityApi.getEntityProfile(eq(ACCOUNT_NUMBER), isNull(), anyBoolean(), anyBoolean(), isNull(), isNull()))
                .thenThrow(new RestClientException("stuff broke"));

        assertThrows(BadRequestException.class,
                () -> addContactRouteExtension.preHook(requestWrapper, null));
    }

    @Test
    void testPreHook_entityProfileHasNoLastName_throwsInternalServerErrorException() {
        when(entityApi.getEntityProfile(eq(ACCOUNT_NUMBER), isNull(), anyBoolean(), anyBoolean(), isNull(), isNull()))
                .thenReturn(new EntityProfile());

        assertThrows(InternalServerErrorException.class,
                () -> addContactRouteExtension.preHook(requestWrapper, null));
    }

    @Test
    void testPreHook_lastNameFailsValidation_throwsBadRequestException() {
        when(entityApi.getEntityProfile(eq(ACCOUNT_NUMBER), isNull(), anyBoolean(), anyBoolean(), isNull(), isNull()))
                .thenReturn(new EntityProfile().lastname("surely this isn't the correct last name"));

        assertThrows(BadRequestException.class,
                () -> addContactRouteExtension.preHook(requestWrapper, null));
    }

    @Test
    void testPreHook_lastNameValidation_shouldIgnoreCase() {
        when(entityApi.getEntityProfile(eq(ACCOUNT_NUMBER), isNull(), anyBoolean(), anyBoolean(), isNull(), isNull()))
                .thenReturn(new EntityProfile().lastname(LAST_NAME.toLowerCase()));

        addContactRouteExtension.preHook(requestWrapper, null);

        verify(entityApi, times(1))
                .getEntityProfile(eq(ACCOUNT_NUMBER), isNull(), anyBoolean(), anyBoolean(), isNull(), isNull());
    }
}