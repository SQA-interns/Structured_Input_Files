package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    return o instanceof ConsentRow other
        && Objects.equals(consentId, other.consentId)
        && Objects.equals(consentText, other.consentText)
        && Objects.equals(givenAt, other.givenAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consentId, consentText, givenAt);
  }
}
