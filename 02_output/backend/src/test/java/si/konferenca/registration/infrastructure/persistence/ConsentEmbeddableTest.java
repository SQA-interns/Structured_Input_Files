package si.konferenca.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Covers AC-005-01: value equality of a stored consent (phase 5 survivors). */
class ConsentEmbeddableTest {

  static final Instant T1 = Instant.parse("2026-10-19T12:00:00Z");
  static final Instant T2 = Instant.parse("2026-10-19T12:00:01Z");

  @Test
  void AC_005_01_consentsAreEqualOnlyWithTheSameIdAndTime() {
    ConsentEmbeddable consent = new ConsentEmbeddable("privacy", T1);

    assertThat(consent).isEqualTo(new ConsentEmbeddable("privacy", T1));
    assertThat(consent).hasSameHashCodeAs(new ConsentEmbeddable("privacy", T1));
    assertThat(consent).isNotEqualTo(new ConsentEmbeddable("photos", T1));
    assertThat(consent).isNotEqualTo(new ConsentEmbeddable("privacy", T2));
    assertThat(consent).isNotEqualTo("privacy");
    assertThat(consent.hashCode()).isNotEqualTo(new ConsentEmbeddable("photos", T2).hashCode());
  }
}
