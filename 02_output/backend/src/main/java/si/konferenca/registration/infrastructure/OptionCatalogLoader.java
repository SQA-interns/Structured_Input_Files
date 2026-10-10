package si.konferenca.registration.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Consent;
import si.konferenca.registration.domain.OptionCatalog;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the options and consents file (contract conference-options.schema.json) and rejects any
 * file that does not satisfy it, naming the offending entry (AC-003-03).
 */
public final class OptionCatalogLoader {

  private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
  private static final int MAX_NAME = 200;
  private static final int MAX_CONSENT_TEXT = 2000;
  private static final Set<String> OPTION_FIELDS =
      Set.of("id", "name", "category", "active", "audience");
  private static final Set<String> CONSENT_FIELDS = Set.of("id", "text", "mandatory");

  private final JsonMapper json = JsonMapper.builder().build();

  /** Parses and validates the file; {@code source} names it in error messages. */
  public OptionCatalog load(InputStream content, String source) {
    JsonNode root;
    try (content) {
      root = json.readTree(content);
    } catch (IOException | JacksonException e) {
      throw new InvalidOptionsException(source + ": not valid JSON");
    }
    if (root == null || !root.isObject()) {
      throw new InvalidOptionsException(source + ": the top level must be an object");
    }
    checkFields(root, Set.of("options", "consents"), source + ": top level");
    List<ConferenceOption> options = readOptions(array(root, "options", source), source);
    List<Consent> consents = readConsents(array(root, "consents", source), source);
    return new OptionCatalog(options, consents);
  }

  private List<ConferenceOption> readOptions(JsonNode array, String source) {
    List<ConferenceOption> options = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    int index = 0;
    for (JsonNode node : array) {
      String id = id(node, source, "option #" + index);
      String where = source + ": option '" + id + "'";
      if (!ids.add(id)) {
        throw new InvalidOptionsException(where + " is defined more than once");
      }
      checkFields(node, OPTION_FIELDS, where);
      String name = text(node, "name", where, MAX_NAME);
      OptionCategory category = category(node, where);
      JsonNode active = node.get("active");
      if (active == null || !active.isBoolean()) {
        throw new InvalidOptionsException(where + ": 'active' must be true or false");
      }
      options.add(
          new ConferenceOption(id, name, category, active.booleanValue(), audience(node, where)));
      index++;
    }
    return options;
  }

  private List<Consent> readConsents(JsonNode array, String source) {
    List<Consent> consents = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    int index = 0;
    for (JsonNode node : array) {
      String id = id(node, source, "consent #" + index);
      String where = source + ": consent '" + id + "'";
      if (!ids.add(id)) {
        throw new InvalidOptionsException(where + " is defined more than once");
      }
      checkFields(node, CONSENT_FIELDS, where);
      String text = text(node, "text", where, MAX_CONSENT_TEXT);
      JsonNode mandatory = node.get("mandatory");
      if (mandatory == null || !mandatory.isBoolean()) {
        throw new InvalidOptionsException(where + ": 'mandatory' must be true or false");
      }
      consents.add(new Consent(id, text, mandatory.booleanValue()));
      index++;
    }
    return consents;
  }

  private static JsonNode array(JsonNode root, String field, String source) {
    JsonNode node = root.get(field);
    if (node == null || !node.isArray()) {
      throw new InvalidOptionsException(source + ": '" + field + "' must be a list");
    }
    return node;
  }

  private static String id(JsonNode node, String source, String position) {
    if (!node.isObject()) {
      throw new InvalidOptionsException(source + ": " + position + " must be an object");
    }
    JsonNode id = node.get("id");
    if (id == null || !id.isString() || !ID.matcher(id.stringValue()).matches()) {
      throw new InvalidOptionsException(
          source
              + ": "
              + position
              + " has a missing or invalid 'id' (lower-case letters, digits, '-')");
    }
    return id.stringValue();
  }

  private static String text(JsonNode node, String field, String where, int maxLength) {
    JsonNode value = node.get(field);
    if (value == null || !value.isString() || value.stringValue().isBlank()) {
      throw new InvalidOptionsException(where + ": '" + field + "' is missing or empty");
    }
    if (value.stringValue().length() > maxLength) {
      throw new InvalidOptionsException(where + ": '" + field + "' is longer than " + maxLength);
    }
    return value.stringValue().strip();
  }

  private static OptionCategory category(JsonNode node, String where) {
    JsonNode value = node.get("category");
    String raw = value != null && value.isString() ? value.stringValue() : String.valueOf(value);
    try {
      return OptionCategory.valueOf(raw);
    } catch (IllegalArgumentException e) {
      throw new InvalidOptionsException(
          where + ": unknown category '" + raw + "' (WORKSHOP, EVENT, MEAL, OTHER)");
    }
  }

  private static Set<RegistrationType> audience(JsonNode node, String where) {
    JsonNode value = node.get("audience");
    if (value == null) {
      return EnumSet.allOf(RegistrationType.class);
    }
    if (!value.isArray() || value.isEmpty()) {
      throw new InvalidOptionsException(where + ": 'audience' must be a non-empty list");
    }
    Set<RegistrationType> audience = EnumSet.noneOf(RegistrationType.class);
    for (JsonNode entry : value) {
      String raw = entry.isString() ? entry.stringValue() : entry.toString();
      try {
        audience.add(RegistrationType.valueOf(raw));
      } catch (IllegalArgumentException e) {
        throw new InvalidOptionsException(
            where + ": unknown audience '" + raw + "' (EXTERNAL, STUDENT)");
      }
    }
    return audience;
  }

  private static void checkFields(JsonNode node, Set<String> allowed, String where) {
    for (String field : node.propertyNames()) {
      if (!allowed.contains(field)) {
        throw new InvalidOptionsException(where + ": unknown field '" + field + "'");
      }
    }
  }
}
