package si.konferenca.registration.infrastructure;

import java.util.List;
import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.GivenConsent;
import si.konferenca.registration.domain.AcceptedRegistration.Participant;
import si.konferenca.registration.domain.AcceptedRegistration.SelectedOption;
import si.konferenca.registration.domain.OptionCategory;

/** Plain-text bodies of the emails (contract emails.yaml). */
final class MailTexts {

  private MailTexts() {}

  static String participantConfirmation(AcceptedRegistration r, String conferenceName) {
    Participant p = r.participant();
    StringBuilder text = new StringBuilder();
    text.append("Dear ").append(p.firstName()).append(' ').append(p.lastName()).append(",\n\n");
    text.append("your registration for ").append(conferenceName).append(" has been received.\n\n");
    text.append("Reference: ").append(r.reference()).append('\n');
    text.append("Registration type: ").append(r.type().label()).append("\n\n");
    text.append("Selected activities:\n");
    if (r.options().isEmpty()) {
      text.append("- none\n");
    }
    for (SelectedOption option : r.options()) {
      text.append(MailNotifier.optionLine(option)).append('\n');
    }
    return text.toString();
  }

  /** Registration data only (SR-07): form fields, options, consents, reference, time. */
  static String organizerNotification(AcceptedRegistration r) {
    Participant p = r.participant();
    StringBuilder text = new StringBuilder();
    text.append("New registration ")
        .append(r.reference())
        .append(" submitted at ")
        .append(r.submittedAt())
        .append(".\n\n");
    line(text, "Type", r.type().label());
    line(text, "First name", p.firstName());
    line(text, "Last name", p.lastName());
    line(text, "Email", p.email());
    if (p.organization() != null) {
      line(text, "Organization / institution", p.organization());
    }
    if (p.studyInstitution() != null) {
      line(text, "Study institution", p.studyInstitution());
      line(text, "Study programme", p.studyProgramme());
      line(text, "Student ID", p.studentId());
    }
    for (OptionCategory category : OptionCategory.values()) {
      List<String> names =
          r.options().stream()
              .filter(o -> o.category() == category)
              .map(SelectedOption::name)
              .toList();
      line(text, category.pluralLabel(), names.isEmpty() ? "-" : String.join(", ", names));
    }
    text.append("Consents:\n");
    for (GivenConsent consent : r.consents()) {
      text.append("- ")
          .append(consent.id())
          .append(" given at ")
          .append(consent.givenAt())
          .append('\n');
    }
    text.append("\nThe registration as accepted is attached as JSON.\n");
    return text.toString();
  }

  private static void line(StringBuilder text, String label, String value) {
    text.append(label).append(": ").append(value).append('\n');
  }
}
