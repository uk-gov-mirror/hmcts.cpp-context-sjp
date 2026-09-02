package uk.gov.moj.cpp.sjp.command.handler;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getBoolean;


import java.util.List;
import java.util.stream.Collectors;
import javax.json.JsonString;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.event.session.ResetAocpSession;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class SessionHandler {

    private static final String SESSION_ID = "sessionId";
    private static final String COURT_HOUSE_CODE = "courtHouseCode";
    private static final String COURT_HOUSE_NAME = "courtHouseName";
    private static final String MAGISTRATE = "magistrate";
    private static final String LEGAL_ADVISER = "legalAdviser";
    private static final String LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE = "localJusticeAreaNationalCourtCode";

    @Inject
    private Clock clock;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("sjp.command.start-session")
    public void startSession(final JsonEnvelope startSessionCommand) throws EventStreamException {
        final JsonObject startSession = startSessionCommand.payloadAsJsonObject();

        final UUID userId = UUID.fromString(startSessionCommand.metadata().userId().orElseThrow(() ->
                new IllegalStateException(format("Envelope with id %s does not contains user id", startSessionCommand.metadata().id()))));
        final UUID sessionId = UUID.fromString(startSession.getString(SESSION_ID));
        final String courtHouseCode = startSession.getString(COURT_HOUSE_CODE);
        final String courtHouseName = startSession.getString(COURT_HOUSE_NAME);
        final String localJusticeAreaNationalCourtCode = startSession.getString(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE);
        final Optional<String> magistrate = ofNullable(startSession.getString(MAGISTRATE, null));
        final Optional<Boolean> isAocpSession = getBoolean(startSession, "isAocpSession");
        final List<String> prosecutors = startSession.getJsonArray("prosecutors").stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .collect(Collectors.toList());

        if (isAocpSession.isPresent() && Boolean.TRUE.equals(isAocpSession.get())) {
            applyToSessionAggregate(startSessionCommand, session -> session.startAocpSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, clock.now(), prosecutors));
        }else {
            applyToSessionAggregate(startSessionCommand, session -> magistrate
                    .map(providedMagistrate -> {
                        final Optional<DelegatedPowers> legalAdviser = getLegalAdviserFromSession(startSession);
                        return session.startMagistrateSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, clock.now(), providedMagistrate, legalAdviser, prosecutors);
                    })
                    .orElseGet(() -> session.startDelegatedPowersSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, clock.now(), prosecutors)));
        }
    }

    @Handles("sjp.command.end-session")
    public void endSession(final JsonEnvelope endSessionCommand) throws EventStreamException {
        final UUID sessionId = UUID.fromString(endSessionCommand.payloadAsJsonObject().getString(SESSION_ID));
        applyToSessionAggregate(endSessionCommand, session -> session.endSession(sessionId, clock.now()));
    }

    @Handles("sjp.command.update-session-bdf")
    public void updateSessionBdf(final JsonEnvelope updateSessionCommand) throws EventStreamException {
        final JsonObject updateSession = updateSessionCommand.payloadAsJsonObject();
        final UUID sessionId = UUID.fromString(updateSession.getString(SESSION_ID));
        final Optional<String> magistrate = JsonObjects.getString(updateSession, MAGISTRATE);
        final Optional<String> courtHouseCode = JsonObjects.getString(updateSession, COURT_HOUSE_CODE);
        final Optional<String> courtHouseName = JsonObjects.getString(updateSession, COURT_HOUSE_NAME);
        final Optional<String> localJusticeAreaNationalCourtCode = JsonObjects.getString(updateSession, LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE);
        final Optional<SessionType> type = JsonObjects.getString(updateSession, "type").map(SessionType::valueOf);
        final Optional<UUID> legalAdviserUserId = JsonObjects.getString(updateSession, "legalAdviserUserId").map(UUID::fromString);

        applyToSessionAggregate(updateSessionCommand, session ->
                session.updateSessionBdf(sessionId, magistrate, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, type, legalAdviserUserId));
    }

    @Handles("sjp.command.reset-aocp-session")
    public void resetAocpSessionRequest(final JsonEnvelope command) throws EventStreamException {

        final EventStream eventStream = eventSource.getStreamById(UUID.randomUUID());

        final Stream<JsonEnvelope> newEvents =
                Stream.of(new ResetAocpSession(clock.now()))
                        .map(toEnvelopeWithMetadataFrom(command));

        eventStream.append(newEvents);
    }

    private void applyToSessionAggregate(JsonEnvelope sessionCommand, final Function<Session, Stream<Object>> function) throws EventStreamException {
        final UUID sessionId = UUID.fromString(sessionCommand.payloadAsJsonObject().getString(SESSION_ID));
        final EventStream eventStream = eventSource.getStreamById(sessionId);
        final Session session = aggregateService.get(eventStream, Session.class);

        final Stream<Object> events = function.apply(session);

        eventStream.append(events.map(enveloper.withMetadataFrom(sessionCommand)));
    }

    private Optional<DelegatedPowers> getLegalAdviserFromSession(final JsonObject session) {
        if (JsonObjects.getJsonObject(session, LEGAL_ADVISER).isPresent()) {
            final JsonObject legalAdviserJsonObject = session.getJsonObject(LEGAL_ADVISER);
            return ofNullable(convertToObject(legalAdviserJsonObject, DelegatedPowers.class));
        } else {
            return empty();
        }
    }

    private <T> T convertToObject(final JsonObject jsonObject, final Class<T> clazz) {
        return this.jsonObjectToObjectConverter.convert(jsonObject, clazz);
    }
}
