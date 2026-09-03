package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;

public class IndependentContactNumberConverter {

    /**
     * Builds the {@code personDetails.contact} object carried on
     * {@code public.events.hearing.hearing-resulted}.
     *
     * <p>Blank values are omitted rather than forwarded. SJP's own
     * {@code contact-details.json} declares home/mobile as unconstrained strings, so an empty
     * string is accepted inbound and persisted (the online-plea flow does exactly this). But
     * core-domain {@code commonContactNumber.json} constrains home and mobile with
     * {@code definitions.json#/definitions/phone} ({@code ^[0-9()\-\.\s]+$}) and both emails with
     * the email pattern - all four require at least one character. Forwarding {@code ""} therefore
     * fails schema validation in every consumer of the event (progression, listing and results),
     * which rejects the whole message rather than just the field.
     *
     * <p>An empty contact value carries no information, so omitting it loses nothing.
     */
    public ContactNumber getContact(final ContactDetails contactDetails) {
        return ContactNumber.contactNumber()
                .withWork(trimToNull(contactDetails.getBusiness()))
                .withMobile(trimToNull(contactDetails.getMobile()))
                .withHome(trimToNull(contactDetails.getHome()))
                .withPrimaryEmail(trimToNull(contactDetails.getEmail()))
                .withSecondaryEmail(trimToNull(contactDetails.getEmail2()))
                .build();
    }
}
