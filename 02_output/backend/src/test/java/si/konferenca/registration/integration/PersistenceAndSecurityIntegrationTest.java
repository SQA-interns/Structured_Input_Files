package si.konferenca.registration.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Api;
import si.konferenca.registration.acceptance.support.TestEnvironment;
import si.konferenca.registration.adapter.Fixtures;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationStore;

/** Integration tests against PostgreSQL and the security chain (phase 5, not frozen). */
class PersistenceAndSecurityIntegrationTest extends AcceptanceTestBase {

  @Autowired RegistrationStore store;

  @Test
  void storedRegistrationReadsBackUnchanged() {
    Registration r = Fixtures.external();

    store.saveAtomically(r, () -> {});

    Registration read =
        store.findAll().stream().filter(x -> x.id().equals(r.id())).findFirst().orElseThrow();
    assertThat(read).usingRecursiveComparison().ignoringFields("options").isEqualTo(r);
    assertThat(read.options()).containsExactlyInAnyOrderElementsOf(r.options());
    assertThat(read.consents()).containsExactlyElementsOf(r.consents());
  }

  @Test
  void authenticatedOrganizerIsDeniedEverythingButTheExport() {
    Api.Response denied =
        api.getAsOrganizer(
            "/api/registrations",
            TestEnvironment.ORGANIZER_USERNAME,
            TestEnvironment.ORGANIZER_PASSWORD);

    assertThat(denied.status()).isEqualTo(403);
    assertThat(denied.text()).isEqualTo("{\"message\":\"Access denied.\",\"fieldErrors\":[]}");
  }

  @Test
  void wrongContentTypeIsUnsupported() {
    Api.Response r = api.postRaw("/api/registrations", "x", "Content-Type", "text/plain");

    assertThat(r.status()).isEqualTo(415);
    assertThat(r.text()).contains("\"fieldErrors\":[]");
  }
}
