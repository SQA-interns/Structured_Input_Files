package si.konferenca.registration.mail;

import java.util.List;
import si.konferenca.registration.domain.Registration;

/** Plain-text bodies of the two emails (contract emails.md); user input is literal text. */
final class EmailTexts {

  private EmailTexts() {}

  static String participantSubject(String conferenceName) {
    return "Registration confirmed: " + conferenceName;
  }

  static String organizerSubject(String conferenceName) {
    return "New registration: " + conferenceName;
  }

  static String participantBody(Registration r, String conferenceName) {
    StringBuilder text = new StringBuilder();
    text.append("Dear ").append(r.firstName()).append(' ').append(r.lastName()).append(",\n\n");
    text.append("thank you for registering for ")
        .append(conferenceName)
        .append(". Your registration has been received.\n\n");
    text.append("Registration type: ").append(r.type().label()).append('\n');
    text.append("Selected options:\n");
    appendOptions(text, r.options());
    text.append("\nRegistration ID: ").append(r.id()).append('\n');
    return text.toString();
  }

  static String organizerBody(Registration r, String conferenceName) {
    StringBuilder text = new StringBuilder();
    text.append("A new registration was received for ").append(conferenceName).append(".\n\n");
    line(text, "Registration ID", r.id().toString());
    line(text, "Registered at", r.registeredAt().toString());
    line(text, "Type", r.type().name());
    line(text, "First name", r.firstName());
    line(text, "Last name", r.lastName());
    line(text, "Email", r.email());
    line(text, "Organization / institution", r.organization());
    line(text, "Study institution", r.studyInstitution());
    line(text, "Study programme", r.studyProgramme());
    line(text, "Student ID", r.studentId());
    text.append("Options:\n");
    appendOptions(text, r.options());
    text.append("Consents:\n");
    for (Registration.GivenConsent consent : r.consents()) {
      text.append("- ")
          .append(consent.id())
          .append(": given ")
          .append(consent.givenAt())
          .append('\n');
    }
    text.append("\nThe raw registration is attached as JSON.\n");
    return text.toString();
  }

  private static void line(StringBuilder text, String label, String value) {
    if (value != null) {
      text.append(label).append(": ").append(value).append('\n');
    }
  }

  private static void appendOptions(StringBuilder text, List<Registration.SelectedOption> options) {
    if (options.isEmpty()) {
      text.append("- none\n");
    }
    for (Registration.SelectedOption option : options) {
      text.append("- ")
          .append(option.name())
          .append(" (")
          .append(option.category().label())
          .append(")\n");
    }
  }
}
