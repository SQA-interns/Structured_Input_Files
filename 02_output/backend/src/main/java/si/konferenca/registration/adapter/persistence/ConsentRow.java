package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

/** Row of registration_consent. */
@Embeddable
public class ConsentRow {

  @Column(name = "consent_id", nullable = false, length = 64)
  private String consentId;

  @Column(name = "consent_text", nullable = false, length = 1000)
  private String consentText;

  @Column(name = "given_at", nullable = false)
  private Instant givenAt;

  protected ConsentRow() {}

  ConsentRow(String consentId, String consentText, Instant givenAt) {
    this.consentId = consentId;
    this.consentText = consentText;
    this.givenAt = givenAt;
  }

  String consentId() {
    return consentId;
  }

  String consentText() {
    return consentText;
  }

  Instant givenAt() {
    return givenAt;
  }
}
