package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-007 organizer notification email with the JSON attachment (contract {@code emails.md}). */
class US007OrganizerNotificationAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-007-01 each organizer address receives one email with the registration data")
  void ac007_01_eachOrganizerReceivesTheRegistration() throws Exception {
    Map<String, Object> request = Registrations.validStudent();

    Response response = api.postRegistration(request);

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    List<Mailbox.Message> messages = mailbox.awaitMessages(3);
    for (String organizer : List.of(ORGANIZER_EMAIL_1, ORGANIZER_EMAIL_2)) {
      List<Mailbox.Message> received = mailbox.messagesTo(messages, organizer);
      assertThat(received).as("messages to %s", organizer).hasSize(1);
      Mailbox.Message message = received.get(0);
      assertThat(message.to()).containsExactly(organizer);
      assertThat(message.subject()).isEqualTo("New registration – Student – " + CONFERENCE_NAME);
      assertThat(message.text())
          .contains("Luka")
          .contains("Kranjc")
          .contains((String) request.get("email"))
          .contains("University of Ljubljana")
          .contains("Computer Science")
          .contains("63210001")
          .contains("Workshop B: Security")
          .contains("Career fair");
    }
  }

  @Test
  @DisplayName("AC-007-02 the organizer email attaches the registration's JSON copy")
  void ac007_02_attachmentEqualsTheJsonCopy() throws Exception {
    Response response = api.postRegistration(Registrations.validExternal());

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    String id = response.json("$.id");
    List<Mailbox.Message> toOrganizer =
        mailbox.messagesTo(mailbox.awaitMessages(3), ORGANIZER_EMAIL_1);
    assertThat(toOrganizer).hasSize(1);
    List<Mailbox.Attachment> attachments = toOrganizer.get(0).attachments();
    assertThat(attachments).hasSize(1);
    Mailbox.Attachment attachment = attachments.get(0);
    assertThat(attachment.fileName()).isEqualTo("registration-" + id + ".json");
    assertThat(attachment.contentType()).startsWith("application/json");
    assertThat(attachment.content()).isEqualTo(Files.readAllBytes(copies.copyOf(id)));
  }

  @Test
  @DisplayName("AC-007-03 the organizer email contains only this registration's data")
  void ac007_03_organizerEmailHasOnlyRegistrationData() throws Exception {
    Map<String, Object> earlier = Registrations.validExternal();
    earlier.put("lastName", "Earlierperson");
    assertThat(api.postRegistration(earlier).status()).isEqualTo(201);
    mailbox.awaitMessages(3);
    mailbox.deleteAll();

    Response response = api.postRegistration(Registrations.validExternal());

    assertThat(response.status()).as(response.toString()).isEqualTo(201);
    List<Mailbox.Message> toOrganizer =
        mailbox.messagesTo(mailbox.awaitMessages(3), ORGANIZER_EMAIL_1);
    assertThat(toOrganizer).hasSize(1);
    String text = toOrganizer.get(0).text();
    assertThat(text)
        .doesNotContain("Earlierperson")
        .doesNotContain((String) earlier.get("email"))
        .doesNotContain(Registrations.TEST_MODE_TOKEN)
        .doesNotContainIgnoringCase("recaptcha")
        .doesNotContainIgnoringCase("email_normalized")
        .doesNotContainIgnoringCase("exception")
        .doesNotContain(ORGANIZER_PASSWORD)
        .doesNotContain(POSTGRES.getPassword())
        .doesNotContain("jdbc:");
  }
}
