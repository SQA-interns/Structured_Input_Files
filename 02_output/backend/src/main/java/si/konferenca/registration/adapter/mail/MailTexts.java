package si.konferenca.registration.adapter.mail;

import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.Registration.GivenConsent;
import si.konferenca.registration.domain.Registration.SelectedOption;
import si.konferenca.registration.domain.RegistrationType;

/** Plain-text bodies of the two emails (`emails.md`). */
final class MailTexts {

  private static final String NL = "\n";

  private MailTexts() {}

  static String confirmation(Registration r, String conferenceName) {
    StringBuilder text = new StringBuilder();
    text.append("Dear ").append(r.firstName()).append(' ').append(r.lastName()).append(',');
    text.append(NL).append(NL);
    text.append("your registration for ").append(conferenceName).append(" has been received.");
    text.append(NL).append(NL);
    text.append("Registration ID: ").append(r.id()).append(NL);
    text.append("Registration type: ").append(r.type().label()).append(NL);
    typeFields(text, r);
    text.append(NL);
    options(text, r);
    text.append(NL).append("Kind regards,").append(NL);
    text.append(conferenceName).append(" organizers").append(NL);
    return text.toString();
  }

  static String organizerNotification(Registration r) {
    StringBuilder text = new StringBuilder();
    text.append("A new registration was received.").append(NL).append(NL);
    line(text, "Registration ID", r.id().toString());
    line(text, "Registration type", r.type().label());
    line(text, "Registered at", Registration.format(r.registeredAt()));
    line(text, "First name", r.firstName());
    line(text, "Last name", r.lastName());
    line(text, "Email", r.email());
    typeFields(text, r);
    text.append(NL);
    options(text, r);
    text.append(NL).append("Consents:").append(NL);
    for (GivenConsent consent : r.consents()) {
      text.append("- ")
          .append(consent.id())
          .append(": given at ")
          .append(Registration.format(consent.givenAt()))
          .append(NL);
    }
    return text.toString();
  }

  private static void options(StringBuilder text, Registration r) {
    text.append("Selected options:").append(NL);
    if (r.options().isEmpty()) {
      text.append("- none").append(NL);
    }
    for (SelectedOption option : r.options()) {
      text.append("- ")
          .append(option.name())
          .append(" (")
          .append(option.category().singular())
          .append(')')
          .append(NL);
    }
  }

  private static void typeFields(StringBuilder text, Registration r) {
    if (r.type() == RegistrationType.EXTERNAL) {
      line(text, "Organization / institution", r.organization());
    } else {
      line(text, "Study institution", r.studyInstitution());
      line(text, "Study programme", r.studyProgramme());
      line(text, "Student ID", r.studentId());
    }
  }

  private static void line(StringBuilder text, String label, String value) {
    text.append(label).append(": ").append(value).append(NL);
  }
}
