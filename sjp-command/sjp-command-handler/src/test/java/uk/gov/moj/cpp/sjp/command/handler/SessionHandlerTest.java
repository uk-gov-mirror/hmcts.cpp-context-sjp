package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.ResetAocpSession;
import uk.gov.moj.cpp.sjp.event.session.SessionEnded;
import uk.gov.moj.cpp.sjp.event.session.SessionUpdatedBdf;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SessionHandlerTest {

    private static final String START_SESSION_COMMAND = "sjp.command.start-session";
    private static final String END_SESSION_COMMAND = "sjp.command.end-session";
    private static final String RESET_SESSION_COMMAND = "sjp.command.reset-aocp-session";
    private static final String UPDATE_SESSION_BDF_COMMAND = "sjp.command.update-session-bdf";
    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream sessionEventStream;

    @Mock
    private Session session;

    @Spy
    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter converter;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            MagistrateSessionStarted.class,
            MagistrateSessionEnded.class,
            DelegatedPowersSessionStarted.class,
            DelegatedPowersSessionEnded.class,
            ResetAocpSession.class,
            SessionUpdatedBdf.class);

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @InjectMocks
    private SessionHandler sessionHandler;

    private UUID sessionId, userId;
    private String courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode;
    private ZonedDateTime startedAt;
    private ZonedDateTime endedAt;
    private Optional<DelegatedPowers> legalAdviser;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        sessionId = randomUUID();
        userId = randomUUID();
        courtHouseCode = "B01LY";
        courtHouseName = "Coventry Magistrates' Court";
        localJusticeAreaNationalCourtCode = "2924";
        startedAt = clock.now();
        endedAt = clock.now();
        legalAdviser = Optional.of(DelegatedPowers.delegatedPowers().withFirstName("Erica").withLastName("Wilson").withUserId(randomUUID()).build());
    }

    @Test
    public void shouldHandleSessionCommands() {
        assertThat(SessionHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(allOf(
                        method("startSession").thatHandles(START_SESSION_COMMAND),
                        method("endSession").thatHandles(END_SESSION_COMMAND),
                        method("resetAocpSessionRequest").thatHandles(RESET_SESSION_COMMAND),
                        method("updateSessionBdf").thatHandles(UPDATE_SESSION_BDF_COMMAND)
                )));
    }

    @Test
    public void shouldStartMagistrateSession() throws EventStreamException {

        final String magistrate = randomAlphanumeric(20);
        final JsonObject legalAdviserJsonObject = buildLegalAdviserJsonObject(legalAdviser.get());

        final JsonEnvelope startSessionCommand = envelopeFrom(metadataWithRandomUUID(START_SESSION_COMMAND).withUserId(userId.toString()),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("magistrate", magistrate)
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("legalAdviser", legalAdviserJsonObject)
                        .add("prosecutors", createArrayBuilder().add("TFL").add("DVL").build())
                        .build());

        final MagistrateSessionStarted sessionStartedEvent = new MagistrateSessionStarted(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate, legalAdviser, Arrays.asList("TFL", "DVL"));

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.startMagistrateSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, magistrate, legalAdviser, Arrays.asList("TFL", "DVL"))).thenReturn(Stream.of(sessionStartedEvent));

        sessionHandler.startSession(startSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(startSessionCommand)
                                        .withName(MagistrateSessionStarted.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.userId", equalTo(userId.toString())),
                                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                                        withJsonPath("$.magistrate", equalTo(magistrate)),
                                        withJsonPath("$.startedAt", equalTo(ZonedDateTimes.toString(startedAt))),
                                        withJsonPath("$.legalAdviser.firstName", equalTo(legalAdviser.get().getFirstName())),
                                        withJsonPath("$.legalAdviser.lastName", equalTo(legalAdviser.get().getLastName())),
                                        withJsonPath("$.legalAdviser.userId", equalTo(legalAdviser.get().getUserId().toString())),
                                        withJsonPath("$.prosecutors[0]", equalTo("TFL")),
                                        withJsonPath("$.prosecutors[1]", equalTo("DVL"))
                                ))))));
    }

    @Test
    public void shouldStartDelegatedPowersSession() throws EventStreamException {

        final JsonEnvelope startSessionCommand = envelopeFrom(metadataWithRandomUUID(START_SESSION_COMMAND).withUserId(userId.toString()),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("prosecutors", createArrayBuilder().add("TFL").add("DVL").build())
                        .build());

        final DelegatedPowersSessionStarted sessionStartedEvent = new DelegatedPowersSessionStarted(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, Arrays.asList("TFL", "DVL"));

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.startDelegatedPowersSession(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, startedAt, Arrays.asList("TFL", "DVL"))).thenReturn(Stream.of(sessionStartedEvent));

        sessionHandler.startSession(startSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(startSessionCommand)
                                        .withName(DelegatedPowersSessionStarted.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.userId", equalTo(userId.toString())),
                                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                                        withJsonPath("$.startedAt", equalTo(ZonedDateTimes.toString(startedAt))),
                                        withJsonPath("$.prosecutors[0]", equalTo("TFL")),
                                        withJsonPath("$.prosecutors[1]", equalTo("DVL")),
                                        withoutJsonPath("$.magistrate")
                                ))))));
    }

    @Test
    public void shouldEndDelegatedPowersSession() throws EventStreamException {
        shouldEndSession(new DelegatedPowersSessionEnded(sessionId, endedAt));
    }

    @Test
    public void shouldEndMagistrateSession() throws EventStreamException {
        shouldEndSession(new MagistrateSessionEnded(sessionId, endedAt));
    }

    @Test
    public void shouldResetSession() throws EventStreamException {
        final JsonObject payload = null;
        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("sjp.command.reset-aocp-session");
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataBuilder, payload);

        when(eventSource.getStreamById(any())).thenReturn(sessionEventStream);

        sessionHandler.resetAocpSessionRequest(envelope);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(envelope.metadata()).withName(ResetAocpSession.class.getAnnotation(Event.class).value()),
                                payloadIsJson(
                                        withJsonPath("$.resetAt", is(clock.now().truncatedTo(ChronoUnit.MILLIS).toString())))))));

    }


    @Test
    public void shouldUpdateSessionBdfWithAllFields() throws EventStreamException {

        final String magistrate = randomAlphanumeric(20);
        final UUID legalAdviserUserId = randomUUID();

        final JsonEnvelope updateSessionCommand = envelopeFrom(metadataWithRandomUUID(UPDATE_SESSION_BDF_COMMAND),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("magistrate", magistrate)
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("type", SessionType.MAGISTRATE.name())
                        .add("legalAdviserUserId", legalAdviserUserId.toString())
                        .build());

        final SessionUpdatedBdf sessionUpdatedBdf = new SessionUpdatedBdf(sessionId, Optional.of(magistrate), Optional.of(courtHouseCode), Optional.of(courtHouseName),
                Optional.of(localJusticeAreaNationalCourtCode), Optional.of(SessionType.MAGISTRATE), Optional.of(legalAdviserUserId));

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.updateSessionBdf(sessionId, Optional.of(magistrate), Optional.of(courtHouseCode), Optional.of(courtHouseName),
                Optional.of(localJusticeAreaNationalCourtCode), Optional.of(SessionType.MAGISTRATE), Optional.of(legalAdviserUserId)))
                .thenReturn(Stream.of(sessionUpdatedBdf));

        sessionHandler.updateSessionBdf(updateSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(updateSessionCommand)
                                        .withName(SessionUpdatedBdf.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.magistrate", equalTo(magistrate)),
                                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                                        withJsonPath("$.type", equalTo(SessionType.MAGISTRATE.name())),
                                        withJsonPath("$.legalAdviserUserId", equalTo(legalAdviserUserId.toString()))
                                ))))));
    }

    @Test
    public void shouldUpdateSessionBdfWithOnlyMagistrate() throws EventStreamException {

        final String magistrate = randomAlphanumeric(20);

        final JsonEnvelope updateSessionCommand = envelopeFrom(metadataWithRandomUUID(UPDATE_SESSION_BDF_COMMAND),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("magistrate", magistrate)
                        .build());

        final SessionUpdatedBdf sessionUpdatedBdf = new SessionUpdatedBdf(sessionId, Optional.of(magistrate), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.updateSessionBdf(sessionId, Optional.of(magistrate), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty()))
                .thenReturn(Stream.of(sessionUpdatedBdf));

        sessionHandler.updateSessionBdf(updateSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(updateSessionCommand)
                                        .withName(SessionUpdatedBdf.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                                        withJsonPath("$.magistrate", equalTo(magistrate)),
                                        withoutJsonPath("$.courtHouseCode"),
                                        withoutJsonPath("$.courtHouseName"),
                                        withoutJsonPath("$.localJusticeAreaNationalCourtCode"),
                                        withoutJsonPath("$.type"),
                                        withoutJsonPath("$.legalAdviserUserId")
                                ))))));
    }

    private void shouldEndSession(final SessionEnded sessionEnded) throws EventStreamException {
        final JsonEnvelope endSessionCommand = envelopeFrom(metadataWithRandomUUID(END_SESSION_COMMAND),
                createObjectBuilder().add("sessionId", sessionEnded.getSessionId().toString()).build());

        when(eventSource.getStreamById(sessionEnded.getSessionId())).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);
        when(session.endSession(sessionEnded.getSessionId(), sessionEnded.getEndedAt())).thenReturn(Stream.of(sessionEnded));

        sessionHandler.endSession(endSessionCommand);

        assertThat(sessionEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(endSessionCommand)
                                        .withName(sessionEnded.getClass().getAnnotation(Event.class).value()),
                                payloadIsJson(allOf(
                                        withJsonPath("$.sessionId", equalTo(sessionEnded.getSessionId().toString())),
                                        withJsonPath("$.endedAt", equalTo(ZonedDateTimes.toString(sessionEnded.getEndedAt())))
                                ))))));
    }

    private JsonObject buildLegalAdviserJsonObject(final DelegatedPowers legalAdviser) {
        return createObjectBuilder()
                .add("firstName", legalAdviser.getFirstName())
                .add("lastName", legalAdviser.getLastName())
                .add("userId", legalAdviser.getUserId().toString())
                .build();
    }

}
