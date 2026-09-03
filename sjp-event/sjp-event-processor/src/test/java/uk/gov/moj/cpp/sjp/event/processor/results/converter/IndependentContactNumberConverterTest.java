package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.HOME_PHONE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.MOBILE_PHONE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.PRIMARY_EMAIL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.SECONDARY_EMAIL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.WORK_PHONE;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IndependentContactNumberConverterTest {

    @InjectMocks
    IndependentContactNumberConverter independentContactNumberConverter;

    @Mock
    ContactDetails contactDetails;


    @Test
    public void shouldConvertIndependentContactNumber() {

        when(contactDetails.getBusiness()).thenReturn(WORK_PHONE);
        when(contactDetails.getHome()).thenReturn(HOME_PHONE);
        when(contactDetails.getMobile()).thenReturn(MOBILE_PHONE);
        when(contactDetails.getEmail()).thenReturn(PRIMARY_EMAIL);
        when(contactDetails.getEmail2()).thenReturn(SECONDARY_EMAIL);

        ContactNumber contactNumber = independentContactNumberConverter.getContact(contactDetails);

        assertThat(contactNumber.getWork(), is(WORK_PHONE));
        assertThat(contactNumber.getHome(), is(HOME_PHONE));
        assertThat(contactNumber.getMobile(), is(MOBILE_PHONE));
        assertThat(contactNumber.getPrimaryEmail(), is(PRIMARY_EMAIL));
        assertThat(contactNumber.getSecondaryEmail(), is(SECONDARY_EMAIL));
    }



    /*
     * Blank phone/email values must be omitted, not forwarded.
     *
     * core-domain commonContactNumber.json constrains home and mobile with
     *   definitions.json#/definitions/phone -> "^[0-9()\\-\\.\\s]+$"
     * and both emails with the email pattern. All four require at least one
     * character, so an empty string fails validation in every consumer of
     * public.events.hearing.hearing-resulted (progression, listing, results).
     */

    @Test
    public void shouldOmitMobileWhenBlank() {
        when(contactDetails.getMobile()).thenReturn("");

        assertThat(independentContactNumberConverter.getContact(contactDetails).getMobile(), is(nullValue()));
    }

    @Test
    public void shouldOmitHomeWhenBlank() {
        when(contactDetails.getHome()).thenReturn("");

        assertThat(independentContactNumberConverter.getContact(contactDetails).getHome(), is(nullValue()));
    }

    @Test
    public void shouldOmitMobileAndHomeWhenWhitespaceOnly() {
        when(contactDetails.getMobile()).thenReturn("   ");
        when(contactDetails.getHome()).thenReturn("\t");

        final ContactNumber contactNumber = independentContactNumberConverter.getContact(contactDetails);

        assertThat(contactNumber.getMobile(), is(nullValue()));
        assertThat(contactNumber.getHome(), is(nullValue()));
    }

    @Test
    public void shouldOmitEmailsWhenBlank() {
        when(contactDetails.getEmail()).thenReturn("");
        when(contactDetails.getEmail2()).thenReturn("  ");

        final ContactNumber contactNumber = independentContactNumberConverter.getContact(contactDetails);

        assertThat(contactNumber.getPrimaryEmail(), is(nullValue()));
        assertThat(contactNumber.getSecondaryEmail(), is(nullValue()));
    }

    @Test
    public void shouldOmitWorkWhenBlank() {
        when(contactDetails.getBusiness()).thenReturn("");

        assertThat(independentContactNumberConverter.getContact(contactDetails).getWork(), is(nullValue()));
    }

    @Test
    public void shouldTrimSurroundingWhitespaceFromPopulatedValues() {
        when(contactDetails.getMobile()).thenReturn("  07894278021  ");

        assertThat(independentContactNumberConverter.getContact(contactDetails).getMobile(), is("07894278021"));
    }

    @Test
    public void shouldReproduceTheOnlinePleaPayloadThatBreaksDownstreamConsumers() {
        // Exactly what sjp.command.plead-online-aocp-eligible-template.json sends:
        //   {"email": "test@test.com", "home": "", "mobile": ""}
        when(contactDetails.getEmail()).thenReturn("test@test.com");
        when(contactDetails.getHome()).thenReturn("");
        when(contactDetails.getMobile()).thenReturn("");

        final ContactNumber contactNumber = independentContactNumberConverter.getContact(contactDetails);

        assertThat(contactNumber.getPrimaryEmail(), is("test@test.com"));
        assertThat(contactNumber.getHome(), is(nullValue()));
        assertThat(contactNumber.getMobile(), is(nullValue()));
    }
}
