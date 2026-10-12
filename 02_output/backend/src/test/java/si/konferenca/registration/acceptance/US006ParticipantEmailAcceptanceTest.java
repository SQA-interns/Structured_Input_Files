package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-006 participant confirmation email (contract {@code emails.md}). */
class US006ParticipantEmailAcceptanceTest extends AcceptanceTestBase {

  private Mailbox.Message onlyMessageTo(String address) throws Exception {
    List<Mailbox.Message> messages = mailbox.awaitMessages(3);
    List<Mailbox.Message> toParticipant = mailbox.messagesTo(messages, address);
    assertThat(toParticipant).as("messages to %s", address).hasSize(1);
    return toParticipant.get(0);
  }

  @Test
  @DisplayName("AC-006-01 the participant receives one email with name, type and options")
  void ac006_01_participantReceivesConfirmation() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    String email = (String) request.get("email");

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    Mailbox.Message message = onlyMessageTo(email);
    assertThat(message.to()).containsExactly(email);
    assertThat(message.subject()).isEqualTo("Registration confirmation – " + CONFERENCE_NAME);
    assertThat(message.text())
        .contains("Ana")
        .contains("Novak")
        .contains("External participant")
        .contains("Workshop A: Testing")
        .contains("Lunch");
  }

  @Test
  @DisplayName("AC-006-02 Slovenian characters in the name are unchanged in the email")
  void ac006_02_slovenianCharactersAreUnchanged() throws Exception {
    Map<String, Object> request = Registrations.validStudent();
    request.put("firstName", "Špela");
    request.put("lastName", "Žagar Čeh");
    String email = (String) request.get("email");

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    assertThat(onlyMessageTo(email).text()).contains("Špela").contains("Žagar Čeh");
  }

  @Test
  @DisplayName("AC-006-03 markup and line breaks in input stay plain text and add no header")
  void ac006_03_inputCannotInjectMarkupOrHeaders() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("firstName", "<b>Ana</b><script>alert(1)</script>");
    request.put("lastName", "Novak\r\nBcc: intruder@example.si\r\nX-Injected: yes");
    request.put("organization", "<a href=\"https://evil.example\">Acme</a>");
    String email = (String) request.get("email");

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    Mailbox.Message message = onlyMessageTo(email);
    assertThat(message.html()).isEmpty();
    assertThat(message.text()).contains("<b>Ana</b><script>alert(1)</script>");
    assertThat(message.to()).containsExactly(email);
    assertThat(message.cc()).isEmpty();
    assertThat(message.bcc()).isEmpty();
    assertThat(message.headers().keySet())
        .noneMatch(h -> h.equalsIgnoreCase("X-Injected") || h.equalsIgnoreCase("Bcc"));
    assertThat(mailbox.messagesTo(mailbox.messages(), "intruder@example.si")).isEmpty();
  }

  @Test
  @DisplayName("AC-006-05 a rejected registration sends no email")
  void ac006_05_rejectedRegistrationSendsNoEmail() throws Exception {
    Map<String, Object> request = Registrations.validExternal();
    request.put("lastName", " ");

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(400);
    assertThat(mailbox.messagesAfterSettling()).isEmpty();
  }
}
