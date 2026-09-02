package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.session.AocpSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.AocpSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.SessionUpdatedBdf;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.repository.SessionRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class SessionListenerTest {

    private static final String TFL = "TFL";
    private static final String DVL = "DVL";
    @Mock
    private Session existingSession;

    @Mock
    private SessionRepository sessionRepository;

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private SessionListener sessionListener;

    @Spy
    private Clock clock = new StoppedClock(new UtcClock().now());

    @Captor
    private ArgumentCaptor<Session> sessionCaptor;

    private final UUID sessionId = randomUUID();
    private final UUID userId = randomUUID();
    private final String courtHouseCode = "B01LY";
    private final String courtHouseName = "Hendon Magistrates' Court";
    private final String localJusticeAreaNationalCourtCode = "2571";
    private final String magistrate = "John Smith";
    private final UUID legalAdviserUserId = UUID.fromString("a085e359-6069-4694-8820-7810e7dfe762");
    private final String legalAdviserFirstName = "Erica";
    private final String legalAdviserLastName = "Wilson";
    private ZonedDateTime startedAt;
    private ZonedDateTime endedAt;

    @BeforeEach
    public void setup() {
        startedAt = clock.now();
        endedAt = startedAt.plusMinutes(1);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleDelegatedPowersSessionStartedForBackwardCompatibility() {

        final JsonEnvelope delegatedPowersSessionStarted = envelopeFrom(metadataWithRandomUUID(DelegatedPowersSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("userId", userId.toString())
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("startedAt", startedAt.format(ISO_DATE_TIME))
                        .build());

        sessionListener.handleDelegatedPowersSessionStarted(delegatedPowersSessionStarted);

        verify(sessionRepository).save(sessionCaptor.capture());

        final Session session = sessionCaptor.getValue();

        assertThat(session.getSessionId(), equalTo(sessionId));
        assertThat(session.getUserId(), equalTo(userId));
        assertThat(session.getCourtHouseCode(), equalTo(courtHouseCode));
        assertThat(session.getCourtHouseName(), equalTo(courtHouseName));
        assertThat(session.getLocalJusticeAreaNationalCourtCode(), equalTo(localJusticeAreaNationalCourtCode));
        assertThat(session.getStartedAt(), equalTo(startedAt));
        assertThat(session.getType(), equalTo(SessionType.DELEGATED_POWERS));
        assertThat(session.getMagistrate().isPresent(), equalTo(false));
        assertThat(session.getEndedAt().isPresent(), equalTo(false));
    }

    @Test
    public void shouldHandleDelegatedPowersSessionStarted() {

        final JsonEnvelope delegatedPowersSessionStarted = envelopeFrom(metadataWithRandomUUID(DelegatedPowersSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("userId", userId.toString())
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("startedAt", startedAt.format(ISO_DATE_TIME))
                        .add("prosecutors", createArrayBuilder().add(TFL).add(DVL).build())
                        .build());

        sessionListener.handleDelegatedPowersSessionStarted(delegatedPowersSessionStarted);

        verify(sessionRepository).save(sessionCaptor.capture());

        final Session session = sessionCaptor.getValue();

        assertThat(session.getSessionId(), equalTo(sessionId));
        assertThat(session.getUserId(), equalTo(userId));
        assertThat(session.getCourtHouseCode(), equalTo(courtHouseCode));
        assertThat(session.getCourtHouseName(), equalTo(courtHouseName));
        assertThat(session.getLocalJusticeAreaNationalCourtCode(), equalTo(localJusticeAreaNationalCourtCode));
        assertThat(session.getStartedAt(), equalTo(startedAt));
        assertThat(session.getType(), equalTo(SessionType.DELEGATED_POWERS));
        assertThat(session.getMagistrate().isPresent(), equalTo(false));
        assertThat(session.getEndedAt().isPresent(), equalTo(false));
        assertThat(session.getProsecutors().get(0), equalTo(TFL));
        assertThat(session.getProsecutors().get(1), equalTo(DVL));
    }

    @Test
    public void shouldHandleMagistrateSessionStartedForBackwardCompatibility() {

        final JsonEnvelope magistrateSessionStarted = envelopeFrom(metadataWithRandomUUID(MagistrateSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("userId", userId.toString())
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("startedAt", startedAt.format(ISO_DATE_TIME))
                        .add("magistrate", magistrate)
                        .add("legalAdviser", createObjectBuilder()
                                .add("userId", legalAdviserUserId.toString())
                                .add("firstName", legalAdviserFirstName)
                                .add("lastName", legalAdviserLastName))
                        .build());

        sessionListener.handleMagistrateSessionStarted(magistrateSessionStarted);

        verify(sessionRepository).save(sessionCaptor.capture());

        final Session session = sessionCaptor.getValue();

        assertThat(session.getSessionId(), equalTo(sessionId));
        assertThat(session.getUserId(), equalTo(userId));
        assertThat(session.getCourtHouseCode(), equalTo(courtHouseCode));
        assertThat(session.getCourtHouseName(), equalTo(courtHouseName));
        assertThat(session.getLocalJusticeAreaNationalCourtCode(), equalTo(localJusticeAreaNationalCourtCode));
        assertThat(session.getStartedAt(), equalTo(startedAt));
        assertThat(session.getType(), equalTo(SessionType.MAGISTRATE));
        assertThat(session.getMagistrate().get(), equalTo(magistrate));
        assertThat(session.getEndedAt().isPresent(), equalTo(false));
        assertThat(session.getLegalAdviserUserId(), equalTo(legalAdviserUserId));
    }

    @Test
    public void shouldHandleMagistrateSessionStarted() {

        final JsonEnvelope magistrateSessionStarted = envelopeFrom(metadataWithRandomUUID(MagistrateSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("userId", userId.toString())
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("startedAt", startedAt.format(ISO_DATE_TIME))
                        .add("magistrate", magistrate)
                        .add("legalAdviser", createObjectBuilder()
                                .add("userId", legalAdviserUserId.toString())
                                .add("firstName", legalAdviserFirstName)
                                .add("lastName", legalAdviserLastName))
                        .add("prosecutors", createArrayBuilder().add(TFL).add(DVL).build())
                        .build());

        sessionListener.handleMagistrateSessionStarted(magistrateSessionStarted);

        verify(sessionRepository).save(sessionCaptor.capture());

        final Session session = sessionCaptor.getValue();

        assertThat(session.getSessionId(), equalTo(sessionId));
        assertThat(session.getUserId(), equalTo(userId));
        assertThat(session.getCourtHouseCode(), equalTo(courtHouseCode));
        assertThat(session.getCourtHouseName(), equalTo(courtHouseName));
        assertThat(session.getLocalJusticeAreaNationalCourtCode(), equalTo(localJusticeAreaNationalCourtCode));
        assertThat(session.getStartedAt(), equalTo(startedAt));
        assertThat(session.getType(), equalTo(SessionType.MAGISTRATE));
        assertThat(session.getMagistrate().get(), equalTo(magistrate));
        assertThat(session.getEndedAt().isPresent(), equalTo(false));
        assertThat(session.getLegalAdviserUserId(), equalTo(legalAdviserUserId));
        assertThat(session.getProsecutors().get(0), equalTo(TFL));
        assertThat(session.getProsecutors().get(1), equalTo(DVL));
    }

    @Test
    public void shouldHandleDelegatedPowersSessionEndedEvent() {

        final JsonEnvelope delegatedPowersSessionEnded = envelopeFrom(metadataWithRandomUUID(DelegatedPowersSessionEnded.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("endedAt", endedAt.format(ISO_DATE_TIME))
                        .build());

        when(sessionRepository.findBy(sessionId)).thenReturn(existingSession);

        sessionListener.handleDelegatedPowersSessionEnded(delegatedPowersSessionEnded);

        verify(existingSession).setEndedAt(endedAt);
    }

    @Test
    public void shouldHandleMagistrateSessionEndedEvent() {

        final JsonEnvelope magistrateSessionEnded = envelopeFrom(metadataWithRandomUUID(MagistrateSessionEnded.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("endedAt", endedAt.format(ISO_DATE_TIME))
                        .build());

        when(sessionRepository.findBy(sessionId)).thenReturn(existingSession);

        sessionListener.handleMagistrateSessionEnded(magistrateSessionEnded);

        verify(existingSession).setEndedAt(endedAt);
    }

    @Test
    public void shouldHandlesQuery() {
        assertThat(SessionListener.class, isHandlerClass(Component.EVENT_LISTENER)
                .with(allOf(
                        method("handleDelegatedPowersSessionStarted").thatHandles(DelegatedPowersSessionStarted.EVENT_NAME),
                        method("handleMagistrateSessionStarted").thatHandles(MagistrateSessionStarted.EVENT_NAME),
                        method("handleDelegatedPowersSessionEnded").thatHandles(DelegatedPowersSessionEnded.EVENT_NAME),
                        method("handleMagistrateSessionEnded").thatHandles(MagistrateSessionEnded.EVENT_NAME),
                        method("handleSessionUpdatedBdf").thatHandles(SessionUpdatedBdf.EVENT_NAME)
                )));
    }


    @Test
    public void shouldHandleAocpStartedEventForBackwardCompatibility() {

        final JsonEnvelope aocpSessionStarted = envelopeFrom(metadataWithRandomUUID(AocpSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("userId", userId.toString())
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("startedAt", startedAt.format(ISO_DATE_TIME))
                        .build());

        sessionListener.handleAocpSessionStarted(aocpSessionStarted);

        verify(sessionRepository).save(sessionCaptor.capture());

        final Session session = sessionCaptor.getValue();

        assertThat(session.getSessionId(), equalTo(sessionId));
        assertThat(session.getUserId(), equalTo(userId));
        assertThat(session.getCourtHouseCode(), equalTo(courtHouseCode));
        assertThat(session.getCourtHouseName(), equalTo(courtHouseName));
        assertThat(session.getLocalJusticeAreaNationalCourtCode(), equalTo(localJusticeAreaNationalCourtCode));
        assertThat(session.getStartedAt(), equalTo(startedAt));
        assertThat(session.getType(), equalTo(SessionType.AOCP));
        assertThat(session.getEndedAt().isPresent(), equalTo(false));
    }

    @Test
    public void shouldHandleAocpStartedEvent() {

        final JsonEnvelope aocpSessionStarted = envelopeFrom(metadataWithRandomUUID(AocpSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("userId", userId.toString())
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("startedAt", startedAt.format(ISO_DATE_TIME))
                        .add("prosecutors", createArrayBuilder().add(TFL).add(DVL).build())
                        .build());

        sessionListener.handleAocpSessionStarted(aocpSessionStarted);

        verify(sessionRepository).save(sessionCaptor.capture());

        final Session session = sessionCaptor.getValue();

        assertThat(session.getSessionId(), equalTo(sessionId));
        assertThat(session.getUserId(), equalTo(userId));
        assertThat(session.getCourtHouseCode(), equalTo(courtHouseCode));
        assertThat(session.getCourtHouseName(), equalTo(courtHouseName));
        assertThat(session.getLocalJusticeAreaNationalCourtCode(), equalTo(localJusticeAreaNationalCourtCode));
        assertThat(session.getStartedAt(), equalTo(startedAt));
        assertThat(session.getType(), equalTo(SessionType.AOCP));
        assertThat(session.getEndedAt().isPresent(), equalTo(false));
        assertThat(session.getProsecutors().get(0), equalTo(TFL));
        assertThat(session.getProsecutors().get(1), equalTo(DVL));
    }

    @Test
    public void shouldHandleSessionUpdatedBdfEventWithAllFields() {

        final JsonEnvelope sessionUpdatedBdf = envelopeFrom(metadataWithRandomUUID(SessionUpdatedBdf.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("magistrate", magistrate)
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .add("type", SessionType.MAGISTRATE.name())
                        .add("legalAdviserUserId", legalAdviserUserId.toString())
                        .build());

        when(sessionRepository.findBy(sessionId)).thenReturn(existingSession);

        sessionListener.handleSessionUpdatedBdf(sessionUpdatedBdf);

        verify(existingSession).setMagistrate(magistrate);
        verify(existingSession).setCourtHouseCode(courtHouseCode);
        verify(existingSession).setCourtHouseName(courtHouseName);
        verify(existingSession).setLocalJusticeAreaNationalCourtCode(localJusticeAreaNationalCourtCode);
        verify(existingSession).setType(SessionType.MAGISTRATE);
        verify(existingSession).setLegalAdviserUserId(legalAdviserUserId);
    }

    @Test
    public void shouldNotUpdateFieldsAbsentFromSessionUpdatedBdfEvent() {

        final JsonEnvelope sessionUpdatedBdf = envelopeFrom(metadataWithRandomUUID(SessionUpdatedBdf.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("magistrate", magistrate)
                        .build());

        when(sessionRepository.findBy(sessionId)).thenReturn(existingSession);

        sessionListener.handleSessionUpdatedBdf(sessionUpdatedBdf);

        verify(existingSession).setMagistrate(magistrate);
        verify(existingSession, never()).setCourtHouseCode(anyString());
        verify(existingSession, never()).setCourtHouseName(anyString());
        verify(existingSession, never()).setLocalJusticeAreaNationalCourtCode(anyString());
        verify(existingSession, never()).setType(any(SessionType.class));
        verify(existingSession, never()).setLegalAdviserUserId(any(UUID.class));
    }

    @Test
    public void shouldHandleAocpEndedEvent() {

        final JsonEnvelope magistrateSessionEnded = envelopeFrom(metadataWithRandomUUID(AocpSessionEnded.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("endedAt", endedAt.format(ISO_DATE_TIME))
                        .build());

        when(sessionRepository.findBy(sessionId)).thenReturn(existingSession);

        sessionListener.handleAocpSessionEnded(magistrateSessionEnded);

        verify(existingSession).setEndedAt(endedAt);
    }

}
