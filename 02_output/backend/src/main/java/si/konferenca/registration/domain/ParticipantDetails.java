package si.konferenca.registration.domain;

/** The participant fields of BR-01; fields the registration type does not use are null. */
public record ParticipantDetails(
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId) {}
