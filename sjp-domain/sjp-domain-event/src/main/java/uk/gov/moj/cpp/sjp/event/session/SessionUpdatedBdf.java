package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.SessionType;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(SessionUpdatedBdf.EVENT_NAME)
public class SessionUpdatedBdf {

    public static final String EVENT_NAME = "sjp.events.session-updated-bdf";

    private final UUID sessionId;

    private final Optional<String> magistrate;

    private final Optional<String> courtHouseCode;

    private final Optional<String> courtHouseName;

    private final Optional<String> localJusticeAreaNationalCourtCode;

    private final Optional<SessionType> type;

    private final Optional<UUID> legalAdviserUserId;

    @JsonCreator
    public SessionUpdatedBdf(
            @JsonProperty("sessionId") final UUID sessionId,
            @JsonProperty("magistrate") final Optional<String> magistrate,
            @JsonProperty("courtHouseCode") final Optional<String> courtHouseCode,
            @JsonProperty("courtHouseName") final Optional<String> courtHouseName,
            @JsonProperty("localJusticeAreaNationalCourtCode") final Optional<String> localJusticeAreaNationalCourtCode,
            @JsonProperty("type") final Optional<SessionType> type,
            @JsonProperty("legalAdviserUserId") final Optional<UUID> legalAdviserUserId) {
        this.sessionId = sessionId;
        this.magistrate = magistrate;
        this.courtHouseCode = courtHouseCode;
        this.courtHouseName = courtHouseName;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
        this.type = type;
        this.legalAdviserUserId = legalAdviserUserId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Optional<String> getMagistrate() {
        return magistrate;
    }

    public Optional<String> getCourtHouseCode() {
        return courtHouseCode;
    }

    public Optional<String> getCourtHouseName() {
        return courtHouseName;
    }

    public Optional<String> getLocalJusticeAreaNationalCourtCode() {
        return localJusticeAreaNationalCourtCode;
    }

    public Optional<SessionType> getType() {
        return type;
    }

    public Optional<UUID> getLegalAdviserUserId() {
        return legalAdviserUserId;
    }
}
