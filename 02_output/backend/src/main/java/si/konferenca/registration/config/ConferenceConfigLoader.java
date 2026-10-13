package si.konferenca.registration.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.InvalidConfigurationException;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeType;

/**
 * Reads the conference configuration (`conference-config.schema.json`) and rejects every violation
 * with a message naming the offending entry (AC-003-03).
 */
public final class ConferenceConfigLoader {

  private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
  private static final Set<String> ROOT_KEYS = Set.of("options", "consents");
  private static final Set<String> OPTION_KEYS =
      Set.of("id", "name", "category", "active", "availableTo");
  private static final Set<String> CONSENT_KEYS = Set.of("id", "text", "mandatory");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ConferenceConfigLoader() {}

  /** Parses and validates the configuration; the source description is used in messages. */
  public static ConferenceCatalogue load(InputStream in, String source) {
    JsonNode root;
    try (in) {
      root = JSON.readTree(in);
    } catch (IOException | JacksonException e) {
      throw new InvalidConfigurationException(
          "Conference configuration " + source + " is not valid JSON");
    }
    if (root == null || !root.isObject()) {
      throw new InvalidConfigurationException(
          "Conference configuration " + source + " must be a JSON object");
    }
    onlyKeys(root, ROOT_KEYS, "Conference configuration");
    List<ConferenceOption> options = new ArrayList<>();
    for (JsonNode node : array(root, "options")) {
      options.add(option(node));
    }
    List<ConsentDefinition> consents = new ArrayList<>();
    for (JsonNode node : array(root, "consents")) {
      consents.add(consent(node));
    }
    return new ConferenceCatalogue(options, consents);
  }

  private static ConferenceOption option(JsonNode node) {
    if (!node.isObject()) {
      throw new InvalidConfigurationException("Every option must be a JSON object");
    }
    String id = id(node, "Option");
    String what = "Option '" + id + "'";
    onlyKeys(node, OPTION_KEYS, what);
    String name = text(node, "name", 200, what + " has no name");
    String categoryValue = string(node.get("category"), what + " has no category");
    OptionCategory category =
        OptionCategory.parse(categoryValue)
            .orElseThrow(
                () ->
                    new InvalidConfigurationException(
                        what + " has unknown category '" + categoryValue + "'"));
    boolean active = bool(node.get("active"), true, what + " has a non-boolean 'active'");
    return new ConferenceOption(id, name, category, active, availableTo(node, what));
  }

  private static Set<RegistrationType> availableTo(JsonNode node, String what) {
    JsonNode types = node.get("availableTo");
    if (types == null) {
      return EnumSet.allOf(RegistrationType.class);
    }
    if (!types.isArray() || types.isEmpty()) {
      throw new InvalidConfigurationException(
          what + " must list at least one registration type in 'availableTo'");
    }
    Set<RegistrationType> result = EnumSet.noneOf(RegistrationType.class);
    for (JsonNode type : types) {
      String value = string(type, what + " has a non-text registration type");
      RegistrationType parsed =
          RegistrationType.parse(value)
              .orElseThrow(
                  () ->
                      new InvalidConfigurationException(
                          what + " has unknown registration type '" + value + "'"));
      if (!result.add(parsed)) {
        throw new InvalidConfigurationException(
            what + " lists registration type '" + value + "' twice");
      }
    }
    return result;
  }

  private static ConsentDefinition consent(JsonNode node) {
    if (!node.isObject()) {
      throw new InvalidConfigurationException("Every consent must be a JSON object");
    }
    String id = id(node, "Consent");
    String what = "Consent '" + id + "'";
    onlyKeys(node, CONSENT_KEYS, what);
    String text = text(node, "text", 1000, what + " has no text");
    JsonNode mandatory = node.get("mandatory");
    if (mandatory == null || !mandatory.isBoolean()) {
      throw new InvalidConfigurationException(what + " needs a boolean 'mandatory'");
    }
    return new ConsentDefinition(id, text, mandatory.booleanValue());
  }

  private static String id(JsonNode node, String kind) {
    String id = string(node.get("id"), kind + " without id");
    if (!ID.matcher(id).matches()) {
      throw new InvalidConfigurationException(kind + " id '" + id + "' is not a valid id");
    }
    return id;
  }

  private static String text(JsonNode node, String field, int max, String missing) {
    String value = string(node.get(field), missing);
    if (value.isBlank()) {
      throw new InvalidConfigurationException(missing);
    }
    if (value.length() > max) {
      throw new InvalidConfigurationException(missing + " shorter than " + (max + 1));
    }
    return value;
  }

  private static String string(JsonNode node, String message) {
    if (node == null || node.getNodeType() != JsonNodeType.STRING) {
      throw new InvalidConfigurationException(message);
    }
    return node.asString();
  }

  private static boolean bool(JsonNode node, boolean fallback, String message) {
    if (node == null) {
      return fallback;
    }
    if (!node.isBoolean()) {
      throw new InvalidConfigurationException(message);
    }
    return node.booleanValue();
  }

  private static JsonNode array(JsonNode root, String field) {
    JsonNode node = root.get(field);
    if (node == null || !node.isArray()) {
      throw new InvalidConfigurationException(
          "Conference configuration needs an array '" + field + "'");
    }
    return node;
  }

  private static void onlyKeys(JsonNode node, Set<String> allowed, String what) {
    for (String key : node.propertyNames()) {
      if (!allowed.contains(key)) {
        throw new InvalidConfigurationException(what + " has unknown property '" + key + "'");
      }
    }
  }
}
