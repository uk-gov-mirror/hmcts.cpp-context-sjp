package uk.gov.moj.cpp.sjp.command.controller;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.lang.annotation.Repeatable;

import javax.inject.Inject;

/**
 * Controller what directly forwards the payload from the API to the Handler TODO: refactor once
 * {@link Handles} becomes {@link Repeatable}
 */
@ServiceComponent(COMMAND_CONTROLLER)
public class NoActionController {

    @Inject
    private Sender sender;

    @Handles("sjp.command.add-case-document")
    public void addCaseDocument(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.mark-case-reopened-in-libra")
    public void markCaseReopenedInLibra(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.mark-as-legal-soc-checked")
    public void markAsLegalSocChecked(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-case-reopened-in-libra")
    public void updateCaseReopenedInLibra(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.undo-case-reopened-in-libra")
    public void undoCaseReopenedInLibra(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.add-dates-to-avoid")
    public void addDatesToAvoid(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-employer")
    public void updateEmployer(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.delete-employer")
    public void deleteEmployer(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.start-session")
    public void startSession(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.end-session")
    public void endSession(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-session-bdf")
    public void updateSessionBdf(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.reset-aocp-session")
    public void resetAocpSession(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.assign-next-case")
    public void assignNextCase(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.unassign-case")
    public void unassignCase(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-defendant-national-insurance-number")
    public void updateDefendantNationalInsuranceNumber(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-financial-means")
    public void updateFinancialMeans(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.delete-defendant-financial-means-information")
    public void deleteDefendantFinancialMeansInformation(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-all-financial-means")
    public void updateAllFinancialMeans(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-hearing-requirements")
    public void updateHearingRequirements(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.upload-case-document")
    public void uploadCaseDocument(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.acknowledge-defendant-details-updates")
    public void acknowledgeDefendantDetailsUpdates(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.request-transparency-report")
    public void requestTransparencyReport(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.resubmit-results")
    public void resubmitResults(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.request-press-transparency-report")
    public void requestPressTransparencyReport(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.set-dates-to-avoid-required")
    public void setDatesToAvoidRequired(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.set-offences-withdrawal-requests-status")
    public void setOffencesWithdrawalRequestsStatus(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.set-pleas")
    public void setPleas(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.resolve-case-status")
    public void resolveCaseStatus(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.reserve-case")
    public void reserveCase(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.undo-reserve-case")
    public void undoReserveCase(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.update-cases-management-status")
    public void updateCasesManagementStatus(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.request-delete-docs")
    public void requestDeleteDocs(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.add-financial-imposition-correlation-id")
    public void addFinancialImpositionCorrelationId(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.add-financial-imposition-account-number-bdf")
    public void addFinancialImpositionAccountNumberBdf(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.resolve-conviction-court-bdf")
    public void resolveConvictionCourtBdf(final JsonEnvelope envelope) {
        send(envelope);
    }

    @Handles("sjp.command.plead-online-pcq-visited")
    public void pleadOnlinePcqVisited(final JsonEnvelope envelope) { send(envelope); }

    @Handles("sjp.command.accept-pending-defendant-changes")
    public void acceptPendingDefendantChanges(final JsonEnvelope envelope) { send(envelope); }

    @Handles("sjp.command.reject-pending-defendant-changes")
    public void rejectPendingDefendantChanges(final JsonEnvelope envelope) { send(envelope); }

    @Handles("sjp.command.delete-case-document")
    public void deleteCaseDocument(final JsonEnvelope envelope) {
        send(envelope);
    }

    private void send(final Envelope<?> envelope) {
        sender.send(envelope);
    }


}
