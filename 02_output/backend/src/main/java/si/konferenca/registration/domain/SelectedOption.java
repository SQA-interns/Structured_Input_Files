package si.konferenca.registration.domain;

/** An option as selected, with its name and category at acceptance time. */
public record SelectedOption(String id, String name, Category category) {}
