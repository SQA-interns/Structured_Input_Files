package si.konferenca.registration.domain;

import java.time.Instant;

/** A consent given with a registration, with the time it was given (SB-14). */
public record GivenConsent(String id, Instant givenAt) {}
