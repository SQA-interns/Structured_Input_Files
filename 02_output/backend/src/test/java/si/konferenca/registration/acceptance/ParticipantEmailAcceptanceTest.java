package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Mailpit.Message;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-006 Participant email confirmation (Mailpit as SMTP substitute). */
class ParticipantEmailAcceptanceTest extends AcceptanceTestBase {

  private static final String PARTICIPANT = "ana.novak@example.org";

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  @DisplayName("AC-006-01 one email to the participant names the conference, participant, options")
  void ac_006_01_participantReceivesOneConfirmation() throws Exception {
    register(validExternal());

    mailpit.awaitAtLeast(3);
    List<Message> messages = mailpit.to(PARTICIPANT);
    assertThat(messages).hasSize(1);
    Message message = messages.getFirst();
    assertThat(message.to()).containsExactly(PARTICIPANT);
    assertThat(message.text())
        .contains(TestStack.CONFERENCE_NAME)
        .contains("Ana")
        .contains("Novak")
        .contains("Workshop: Software testing")
        .contains("Lunch");
  }

  @Test
  @DisplayName("AC-006-01 student participant receives one confirmation")
  void ac_006_01_studentReceivesOneConfirmation() throws Exception {
    register(validStudent());

    mailpit.awaitAtLeast(3);
    List<Message> messages = mailpit.to("luka.kranjc@student.example.org");
    assertThat(messages).hasSize(1);
    assertThat(messages.getFirst().text())
        .contains(TestStack.CONFERENCE_NAME)
        .contains("Luka")
        .contains("Kranjc")
        .contains("Workshop: Software testing")
        .contains("Opening reception");
  }

  @Test
  @DisplayName("AC-006-02 line breaks, header-like text and markup cannot inject headers or HTML")
  void ac_006_02_userInputCannotInjectHeadersOrMarkup() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("firstName", "Eve\r\nBcc: attacker@evil.example");
    body.put("lastName", "<b>Bold</b> <script>alert(1)</script>");
    body.put("organization", "Org\nTo: other@evil.example\nSubject: injected\n\n<i>x</i>");

    register(body);

    List<Message> all = mailpit.awaitAtLeast(3);
    Thread.sleep(500);
    all = mailpit.all();
    assertThat(all).as("participant plus two organizer emails").hasSize(3);
    Set<String> recipients =
        all.stream()
            .flatMap(m -> Stream.of(m.to(), m.cc(), m.bcc()).flatMap(List::stream))
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    assertThat(recipients)
        .containsExactlyInAnyOrder(
            PARTICIPANT, TestStack.ORGANIZER_EMAIL_1, TestStack.ORGANIZER_EMAIL_2);
    assertThat(mailpit.to("attacker@evil.example")).isEmpty();
    assertThat(mailpit.to("other@evil.example")).isEmpty();

    Message confirmation = mailpit.to(PARTICIPANT).getFirst();
    assertThat(confirmation.to()).containsExactly(PARTICIPANT);
    assertThat(confirmation.cc()).isEmpty();
    assertThat(confirmation.bcc()).isEmpty();
    assertThat(confirmation.subject()).doesNotContain("injected");
    assertThat(confirmation.html()).as("no HTML part").isEmpty();
    assertThat(confirmation.text()).contains("<b>Bold</b>").contains("<script>alert(1)</script>");

    Map<String, List<String>> headers = mailpit.headers(confirmation.id());
    assertThat(headers.keySet().stream().map(String::toLowerCase)).doesNotContain("bcc", "cc");
    assertThat(header(headers, "To")).hasSize(1);
    assertThat(header(headers, "To").getFirst()).doesNotContain("evil.example");
    assertThat(header(headers, "Subject").getFirst()).doesNotContain("injected");
    assertThat(header(headers, "Content-Type").getFirst()).startsWith("text/plain");
  }

  private static List<String> header(Map<String, List<String>> headers, String name) {
    return headers.entrySet().stream()
        .filter(e -> e.getKey().equalsIgnoreCase(name))
        .flatMap(e -> e.getValue().stream())
        .toList();
  }
}
