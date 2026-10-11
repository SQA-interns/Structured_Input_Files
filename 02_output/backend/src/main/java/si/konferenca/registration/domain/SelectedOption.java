package si.konferenca.registration.domain;

/** An option as selected at registration time. */
public record SelectedOption(String id, String name, OptionCategory category) {}
