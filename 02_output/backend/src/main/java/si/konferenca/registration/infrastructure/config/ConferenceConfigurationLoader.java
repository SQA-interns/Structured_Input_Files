package si.konferenca.registration.infrastructure.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.InvalidConfigurationException;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads conference-config.schema.json files; every violation is an InvalidConfigurationException.
 */
public class ConferenceConfigurationLoader {

  private static final Set<String> ROOT_PROPERTIES =
      Set.of("conferenceName", "consents", "options");
  private static final Set<String> CONSENT_PROPERTIES = Set.of("id", "text", "mandatory");
  private static final Set<String> OPTION_PROPERTIES =
      Set.of("id", "name", "category", "active", "availableTo");

  private final JsonMapper json = JsonMapper.builder().build();

  public ConferenceConfiguration load(Path file) {
    String content;
    try {
      content = Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidConfigurationException("cannot read file '" + file + "'", e);
    }
    JsonNode root;
    try {
      root = json.readTree(content);
    } catch (JacksonException e) {
      throw new InvalidConfigurationException("file '" + file + "' is not valid JSON", e);
    }
    if (root == null || !root.isObject()) {
      throw new InvalidConfigurationException("the root must be an object");
    }
    onlyKnownProperties(root, ROOT_PROPERTIES, "the root");
    List<ConsentDefinition> consents = new ArrayList<>();
    for (JsonNode consent : array(root, "consents")) {
      onlyKnownProperties(consent, CONSENT_PROPERTIES, "a consent");
      consents.add(
          new ConsentDefinition(
              text(consent, "id"), text(consent, "text"), bool(consent, "mandatory")));
    }
    List<ConferenceOption> options = new ArrayList<>();
    for (JsonNode option : array(root, "options")) {
      onlyKnownProperties(option, OPTION_PROPERTIES, "an option");
      String id = text(option, "id");
      options.add(
          new ConferenceOption(
              id,
              text(option, "name"),
              category(option, id),
              bool(option, "active"),
              availableTo(option, id)));
    }
    return ConferenceConfiguration.of(text(root, "conferenceName"), consents, options);
  }

  private static void onlyKnownProperties(JsonNode node, Set<String> known, String where) {
    if (!node.isObject()) {
      throw new InvalidConfigurationException(where + " must be an object");
    }
    for (String name : node.propertyNames()) {
      if (!known.contains(name)) {
        throw new InvalidConfigurationException(where + " has the unknown property '" + name + "'");
      }
    }
  }

  private static JsonNode array(JsonNode node, String property) {
    JsonNode value = node.get(property);
    if (value == null || !value.isArray()) {
      throw new InvalidConfigurationException("'" + property + "' must be a list");
    }
    return value;
  }

  private static String text(JsonNode node, String property) {
    JsonNode value = node.get(property);
    if (value == null || value.isNull()) {
      return null;
    }
    if (!value.isString()) {
      throw new InvalidConfigurationException("'" + property + "' must be text");
    }
    return value.asString();
  }

  private static boolean bool(JsonNode node, String property) {
    JsonNode value = node.get(property);
    if (value == null || !value.isBoolean()) {
      throw new InvalidConfigurationException("'" + property + "' must be true or false");
    }
    return value.asBoolean();
  }

  private static OptionCategory category(JsonNode option, String id) {
    String value = text(option, "category");
    try {
      return value == null ? null : OptionCategory.valueOf(value);
    } catch (IllegalArgumentException e) {
      throw new InvalidConfigurationException(
          "option '" + id + "' has the unknown category '" + value + "'", e);
    }
  }

  private static Set<RegistrationType> availableTo(JsonNode option, String id) {
    JsonNode value = option.get("availableTo");
    if (value == null || value.isNull()) {
      return null;
    }
    if (!value.isArray() || value.isEmpty()) {
      throw new InvalidConfigurationException(
          "option '" + id + "' availableTo must be a non-empty list");
    }
    Set<RegistrationType> types = EnumSet.noneOf(RegistrationType.class);
    for (JsonNode type : value) {
      try {
        types.add(RegistrationType.valueOf(type.asString()));
      } catch (IllegalArgumentException e) {
        throw new InvalidConfigurationException(
            "option '" + id + "' has the unknown registration type '" + type.asString() + "'", e);
      }
    }
    return types;
  }
}
