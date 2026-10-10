package si.konferenca.registration.infrastructure;

import si.konferenca.registration.domain.AcceptedRegistration;
import si.konferenca.registration.domain.AcceptedRegistration.Participant;
import si.konferenca.registration.domain.AcceptedRegistration.SelectedOption;

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
}
