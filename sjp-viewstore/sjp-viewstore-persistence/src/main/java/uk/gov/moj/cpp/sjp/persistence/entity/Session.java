package uk.gov.moj.cpp.sjp.persistence.entity;

import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;


import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.JoinColumn;
import uk.gov.moj.cpp.sjp.domain.SessionType;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "session")
public class Session implements Serializable {

    private static final long serialVersionUID = -252959194824297763L;

    @Id
    @Column(name = "id")
    private UUID sessionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "legal_adviser_user_id")
    private UUID legalAdviserUserId;

    @Column(name = "court_house_code")
    private String courtHouseCode;

    @Column(name = "court_house_name")
    private String courtHouseName;

    @Column(name = "local_justice_area_national_court_code")
    private String localJusticeAreaNationalCourtCode;

    @Column(name = "magistrate")
    private String magistrate;

    @Column(name = "started_at")
    private ZonedDateTime startedAt;

    @Column(name = "ended_at")
    private ZonedDateTime endedAt;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private SessionType type;

    @ElementCollection
    @CollectionTable(
            name = "session_prosecutors",
            joinColumns = @JoinColumn(name = "session_id")
    )
    @Column(name = "prosecutor", nullable = true)
    private List<String> prosecutors = new ArrayList<>();

    public Session() {
        //for JPA
    }

    public Session(final UUID sessionId, final UUID userId, final String courtHouseCode, final String courtHouseName, final String localJusticeAreaNationalCourtCode, final String magistrate, final ZonedDateTime startedAt, final List<String> prosecutors) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.courtHouseCode = courtHouseCode;
        this.courtHouseName = courtHouseName;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
        this.startedAt = startedAt;
        this.magistrate = magistrate;
        this.type = Objects.isNull(magistrate) ? DELEGATED_POWERS : MAGISTRATE;
        this.prosecutors = prosecutors;
    }

    public Session(final UUID sessionId, final UUID userId, final UUID legalAdviserUserId, final String courtHouseCode, final String courtHouseName, final String localJusticeAreaNationalCourtCode, final String magistrate, final ZonedDateTime startedAt, final ZonedDateTime endedAt, final List<String> prosecutors) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.legalAdviserUserId = legalAdviserUserId;
        this.courtHouseCode = courtHouseCode;
        this.courtHouseName = courtHouseName;
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
        this.magistrate = magistrate;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.type = Objects.isNull(magistrate) ? DELEGATED_POWERS : MAGISTRATE;
        this.prosecutors = prosecutors;
    }

    public Session(final UUID sessionId, final UUID userId, final String courtHouseCode, final String courtHouseName, final String localJusticeAreaNationalCourtCode, final ZonedDateTime startedAt, List<String> prosecutors) {
        this(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, null, startedAt, prosecutors);
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCourtHouseCode() {
        return courtHouseCode;
    }

    public void setCourtHouseCode(final String courtHouseCode) {
        this.courtHouseCode = courtHouseCode;
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public void setCourtHouseName(final String courtHouseName) {
        this.courtHouseName = courtHouseName;
    }

    public String getLocalJusticeAreaNationalCourtCode() {
        return localJusticeAreaNationalCourtCode;
    }

    public void setLocalJusticeAreaNationalCourtCode(final String localJusticeAreaNationalCourtCode) {
        this.localJusticeAreaNationalCourtCode = localJusticeAreaNationalCourtCode;
    }

    public SessionType getType() {
        return type;
    }

    public Optional<String> getMagistrate() {
        return Optional.ofNullable(magistrate);
    }

    public void setMagistrate(final String magistrate) {
        this.magistrate = magistrate;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public Optional<ZonedDateTime> getEndedAt() {
        return Optional.ofNullable(endedAt);
    }

    public void setEndedAt(ZonedDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public UUID getLegalAdviserUserId() {
        return legalAdviserUserId;
    }

    public void setLegalAdviserUserId(final UUID legalAdviserUserId) {
        this.legalAdviserUserId = legalAdviserUserId;
    }

    public void setType(final SessionType type) {
        this.type = type;
    }

    public List<String> getProsecutors() {
        return prosecutors;
    }

    public void setProsecutors(final List<String> prosecutors) {
        this.prosecutors = prosecutors;
    }
}
