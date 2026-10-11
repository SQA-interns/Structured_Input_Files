package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Payloads.external;
import static si.konferenca.registration.acceptance.support.Payloads.with;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Mailpit;
import si.konferenca.registration.acceptance.support.TestEnvironment;

/** US-006 Participant email confirmation. */
class ParticipantEmailAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-006-01: the participant receives one confirmation email")
  void ac_006_01_participantReceivesConfirmation() throws InterruptedException {
    Map<String, Object> submission = external();
    String email = (String) submission.get("email");

    UUID id = registerAccepted(submission);

    DocumentContext message = mailpit.awaitMessageTo(email);
    assertThat(Mailpit.recipients(message)).containsExactly(email);
    assertThat(message.<String>read("$.Subject")).contains(TestEnvironment.CONFERENCE_NAME);
    String text = message.read("$.Text");
    assertThat(text)
        .contains(TestEnvironment.CONFERENCE_NAME)
        .contains((String) submission.get("firstName"))
        .contains((String) submission.get("lastName"))
        .contains("Workshop: AI v praksi")
        .contains("Kosilo")
        .contains(id.toString());
    Thread.sleep(1500);
    assertThat(mailpit.messagesTo(email)).hasSize(1);
  }

  @Test
  @DisplayName("AC-006-02: user input cannot add headers, recipients or markup to the email")
  void ac_006_02_userInputCannotInjectHeadersOrMarkup() {
    Map<String, Object> submission = external();
    String email = (String) submission.get("email");
    String uniqueLastName = (String) submission.get("lastName");
    submission = with(submission, "firstName", "Ana\r\nBcc: attacker@evil.test");
    submission =
        with(submission, "lastName", "<b>" + uniqueLastName + "</b><script>alert(1)</script>");
    submission = with(submission, "organization", "ACME\r\nX-Injected: yes");

    registerAccepted(submission);

    DocumentContext message = mailpit.awaitMessageTo(email);
    assertThat(Mailpit.recipients(message)).containsExactly(email);
    String messageId = message.read("$.ID");
    Map<String, List<String>> headers = mailpit.headers(messageId);
    assertThat(headers.keySet())
        .noneMatch(h -> h.equalsIgnoreCase("X-Injected"))
        .noneMatch(h -> h.equalsIgnoreCase("Bcc"));
    assertThat(message.<String>read("$.Subject")).doesNotContain("Bcc", "attacker", "<b>");
    String html = message.read("$.HTML");
    assertThat(html == null ? "" : html).isEmpty();
    assertThat(message.<String>read("$.Text"))
        .contains("<b>" + uniqueLastName + "</b><script>alert(1)</script>");
    assertThat(mailpit.messagesTo("attacker@evil.test")).isEmpty();
  }
}
