package si.konferenca.registration.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import si.konferenca.registration.domain.Category;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Consent;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the option file (contract {@code conference-options.schema.json}) and refuses invalid
 * content with a message naming the faulty entry (AR-04, AC-003-03).
 */
final class OptionCatalogLoader {

  private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
  private static final JsonMapper JSON =
      JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  /** File shape; strings are checked here so that errors can name the entry. */
  record FileContent(ConsentEntry consent, List<OptionEntry> options) {}

  record ConsentEntry(String id, String text) {}

  record OptionEntry(
      String id, String name, String category, Boolean active, List<String> registrationTypes) {}

  private OptionCatalogLoader() {}

  static OptionCatalog load(Resource resource) {
    FileContent content;
    try (InputStream in = resource.getInputStream()) {
      content = JSON.readValue(in, FileContent.class);
    } catch (IOException | JacksonException e) {
      throw new InvalidOptionsException(
          "Cannot read conference options from "
              + resource.getDescription()
              + ": "
              + e.getMessage(),
          e);
    }
    return toCatalog(content);
  }

  static OptionCatalog toCatalog(FileContent content) {
    if (content == null || content.consent() == null || content.options() == null) {
      throw new InvalidOptionsException("Conference options need 'consent' and 'options'");
    }
    ConsentEntry consent = content.consent();
    if (consent.id() == null || !ID.matcher(consent.id()).matches()) {
      throw new InvalidOptionsException("Consent id is missing or invalid");
    }
    if (blank(consent.text()) || consent.text().length() > 1000) {
      throw new InvalidOptionsException("Consent text must have 1 to 1000 characters");
    }
    List<ConferenceOption> options = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < content.options().size(); i++) {
      OptionEntry entry = content.options().get(i);
      String where = "option[" + i + "] '" + (entry == null ? null : entry.id()) + "'";
      if (entry == null || entry.id() == null || !ID.matcher(entry.id()).matches()) {
        throw new InvalidOptionsException(where + ": id is missing or invalid");
      }
      if (!seen.add(entry.id())) {
        throw new InvalidOptionsException(where + ": duplicate id");
      }
      if (blank(entry.name()) || entry.name().length() > 200) {
        throw new InvalidOptionsException(where + ": name must have 1 to 200 characters");
      }
      if (entry.active() == null) {
        throw new InvalidOptionsException(where + ": 'active' is required");
      }
      options.add(
          new ConferenceOption(
              entry.id(),
              entry.name(),
              category(where, entry.category()),
              entry.active(),
              types(where, entry.registrationTypes())));
    }
    return new OptionCatalog(new Consent(consent.id(), consent.text()), options);
  }

  private static Category category(String where, String value) {
    for (Category category : Category.values()) {
      if (category.name().equals(value)) {
        return category;
      }
    }
    throw new InvalidOptionsException(where + ": unknown category '" + value + "'");
  }

  private static Set<RegistrationType> types(String where, List<String> values) {
    if (values == null) {
      return EnumSet.allOf(RegistrationType.class);
    }
    if (values.isEmpty()) {
      throw new InvalidOptionsException(where + ": registrationTypes must not be empty");
    }
    Set<RegistrationType> types = EnumSet.noneOf(RegistrationType.class);
    for (String value : values) {
      RegistrationType type = null;
      for (RegistrationType candidate : RegistrationType.values()) {
        if (candidate.name().equals(value)) {
          type = candidate;
        }
      }
      if (type == null || !types.add(type)) {
        throw new InvalidOptionsException(where + ": invalid registration type '" + value + "'");
      }
    }
    return types;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /** The option file is invalid; the application must not start. */
  static final class InvalidOptionsException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    InvalidOptionsException(String message) {
      super(message);
    }

    InvalidOptionsException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
