package si.konferenca.registration.adapter.mail;

import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SelectedOption;

/** Plain-text bodies of the two emails (contract email.md). */
final class MailTexts {

  private MailTexts() {}

  static String participant(Registration r, String conferenceName) {
    Participant p = r.participant();
    StringBuilder b = new StringBuilder();
    b.append("Dear ").append(p.firstName()).append(' ').append(p.lastName()).append(",\n\n");
    b.append("your registration for ").append(conferenceName).append(" has been received.\n\n");
    b.append("Registration ID: ").append(r.id()).append('\n');
    b.append("Registration type: ").append(r.type().label()).append('\n');
    appendOptions(b, r);
    b.append("\nKind regards,\n").append(conferenceName).append(" organizers\n");
    return b.toString();
  }

  static String organizer(Registration r) {
    Participant p = r.participant();
    StringBuilder b = new StringBuilder("A new registration was received.\n\n");
    b.append("Registration ID: ").append(r.id()).append('\n');
    b.append("Registration type: ").append(r.type().label()).append('\n');
    b.append("Registered at: ").append(r.registeredAt()).append('\n');
    line(b, "First name", p.firstName());
    line(b, "Last name", p.lastName());
    line(b, "Email", p.email());
    line(b, "Organization / institution", p.organization());
    line(b, "Study institution", p.studyInstitution());
    line(b, "Study programme", p.studyProgramme());
    line(b, "Student ID", p.studentId());
    appendOptions(b, r);
    b.append("Consents:\n");
    for (GivenConsent c : r.consents()) {
      b.append("- ").append(c.id()).append(" at ").append(c.givenAt()).append('\n');
    }
    b.append("\nThe raw registration JSON is attached.\n");
    return b.toString();
  }

  private static void line(StringBuilder b, String label, String value) {
    if (value != null) {
      b.append(label).append(": ").append(value).append('\n');
    }
  }

  private static void appendOptions(StringBuilder b, Registration r) {
    b.append("Selected options:\n");
    if (r.options().isEmpty()) {
      b.append("- none\n");
    }
    for (SelectedOption o : r.options()) {
      b.append("- ").append(o.category().label()).append(": ").append(o.name()).append('\n');
    }
  }
}
