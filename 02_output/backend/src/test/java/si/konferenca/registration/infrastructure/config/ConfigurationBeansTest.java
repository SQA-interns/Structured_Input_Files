package si.konferenca.registration.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.application.CaptchaSettings;
import si.konferenca.registration.application.FormConfigurationService;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.infrastructure.captcha.TestModeCaptchaVerifier;
import si.konferenca.registration.infrastructure.export.ExcelWorkbookWriter;
import si.konferenca.registration.infrastructure.file.FileJsonCopyStore;

/**
 * Covers AC-003-01, AC-003-02, AC-005-02, AC-008-01: the bean factories, called directly (phase 5
 * survivors; a cached Spring test context does not rebuild beans for each mutant).
 */
class ConfigurationBeansTest {

  @TempDir Path dir;

  static final ConferenceConfiguration CONFIG =
      ConferenceConfiguration.of(
          "Conf",
          List.of(new ConsentDefinition("privacy", "I agree.", true)),
          List.of(new ConferenceOption("ws-a", "A", OptionCategory.WORKSHOP, true, null)));

  @Test
  void AC_003_02_conferenceConfigurationIsLoadedFromTheConfiguredFile() {
    ConferenceConfiguration loaded =
        new ConferenceConfigurationBeans()
            .conferenceConfiguration(
                Path.of("src", "test", "resources", "acceptance", "conference-test.json")
                    .toString());

    assertThat(loaded.conferenceName()).isEqualTo("Konferenca Test 2026");
  }

  @Test
  void AC_003_01_captchaSettingsHideTheSiteKeyOnlyInTestMode() {
    ConferenceConfigurationBeans beans = new ConferenceConfigurationBeans();

    assertThat(beans.captchaSettings("public-key", true)).isEqualTo(new CaptchaSettings("", true));
    assertThat(beans.captchaSettings("public-key", false))
        .isEqualTo(new CaptchaSettings("public-key", false));
  }

  @Test
  void AC_003_01_formConfigurationServiceUsesTheConfiguration() {
    FormConfigurationService service =
        new ConferenceConfigurationBeans()
            .formConfigurationService(CONFIG, new CaptchaSettings("", true));

    assertThat(service.formConfiguration().conferenceName()).isEqualTo("Conf");
  }

  @Test
  void AC_005_02_registrationBeansAreCreated() {
    RegistrationBeans beans = new RegistrationBeans();

    assertThat(beans.clock()).isEqualTo(Clock.systemUTC());
    assertThat(beans.captchaVerifier("test", true, "", "", "https://example.com/verify"))
        .isInstanceOf(TestModeCaptchaVerifier.class);
    assertThat(beans.jsonCopyStore(dir.toString())).isInstanceOf(FileJsonCopyStore.class);
    assertThat(
            beans.registerParticipant(
                CONFIG,
                token -> true,
                registration -> {},
                registration -> {},
                registration -> {},
                Clock.systemUTC()))
        .isNotNull();
  }

  @Test
  void AC_008_01_exportBeansAreCreated() {
    ExportBeans beans = new ExportBeans();

    assertThat(beans.workbookWriter()).isInstanceOf(ExcelWorkbookWriter.class);
    assertThat(beans.exportRegistrations(List::of, registrations -> new byte[] {1}).export())
        .containsExactly(1);
  }

  @Test
  void SR_02_unsafeCaptchaSettingsStopTheBean() {
    assertThatThrownBy(
            () ->
                new RegistrationBeans()
                    .captchaVerifier("production", true, "s", "k", "https://example.com/v"))
        .isInstanceOf(IllegalStateException.class);
  }
}
