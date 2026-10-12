package si.konferenca.registration.domain;

import java.time.Instant;

/** Proof of the given consent: which one, its wording and when (SB-14). */
public record ConsentRecord(String id, String text, Instant givenAt) {}
