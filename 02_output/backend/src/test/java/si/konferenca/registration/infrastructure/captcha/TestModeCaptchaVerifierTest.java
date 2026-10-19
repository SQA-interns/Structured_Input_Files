package si.konferenca.registration.infrastructure.captcha;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Covers AC-001-07, AC-002-06. */
class TestModeCaptchaVerifierTest {

  private final TestModeCaptchaVerifier verifier = new TestModeCaptchaVerifier();

  @Test
  void AC_001_07_testModeAcceptsOnlyTheTestToken() {
    assertThat(verifier.verify("test-pass")).isTrue();
  }

  @ParameterizedTest(name = "AC-001-07 rejects [{0}]")
  @NullSource
  @ValueSource(strings = {"", "test-fail", "TEST-PASS", " test-pass", "anything"})
  void AC_001_07_testModeRejectsEveryOtherToken(String token) {
    assertThat(verifier.verify(token)).isFalse();
  }
}
