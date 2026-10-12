package si.konferenca.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** A consent the participant gave, with its wording and the time it was given (SB-14). */
@Embeddable
public record GivenConsent(
    @Column(name = "consent_id", nullable = false, length = 64) String consentId,
    @Column(name = "consent_text", nullable = false, columnDefinition = "text") String consentText,
    @Column(name = "given_at", nullable = false) Instant givenAt) {}
