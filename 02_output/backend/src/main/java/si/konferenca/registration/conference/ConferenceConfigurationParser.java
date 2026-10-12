package si.konferenca.registration.conference;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses and validates the options configuration (docs/02_contracts/conference-config.schema.json,
 * specification 3.3). Every violation throws {@link InvalidConferenceConfigurationException}.
 */
public final class ConferenceConfigurationParser {

  private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ConferenceConfigurationParser() {}

  public static ConferenceConfiguration parse(String json) {
    JsonNode root;
    try {
      root = JSON.readTree(json);
    } catch (JacksonException e) {
      throw new InvalidConferenceConfigurationException("the file is not valid JSON", e);
    }
    if (root == null || !root.isObject()) {
      throw new InvalidConferenceConfigurationException("the root must be an object");
    }
    return new ConferenceConfiguration(
        parseOptions(array(root, "options")), parseConsents(array(root, "consents")));
  }

  private static List<ConferenceOption> parseOptions(JsonNode array) {
    List<ConferenceOption> options = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    for (JsonNode node : array) {
      String id = requiredId(node, "option");
      if (!ids.add(id)) {
        throw new InvalidConferenceConfigurationException("duplicate option id " + id);
      }
      String name = requiredText(node, "name", "option " + id);
      Category category = category(node, id);
      boolean active = !node.has("active") || booleanValue(node, "active", "option " + id);
      options.add(new ConferenceOption(id, name, category, active, availableTo(node, id)));
    }
    return options;
  }

  private static List<ConsentDefinition> parseConsents(JsonNode array) {
    List<ConsentDefinition> consents = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    for (JsonNode node : array) {
      String id = requiredId(node, "consent");
      if (!ids.add(id)) {
        throw new InvalidConferenceConfigurationException("duplicate consent id " + id);
      }
      String text = requiredText(node, "text", "consent " + id);
      boolean mandatory = booleanValue(node, "mandatory", "consent " + id);
      consents.add(new ConsentDefinition(id, text, mandatory));
    }
    return consents;
  }

  private static JsonNode array(JsonNode root, String field) {
    JsonNode node = root.get(field);
    if (node == null || !node.isArray()) {
      throw new InvalidConferenceConfigurationException(field + " must be a list");
    }
    return node;
  }

  private static String requiredId(JsonNode node, String kind) {
    if (!node.isObject()) {
      throw new InvalidConferenceConfigurationException("every " + kind + " must be an object");
    }
    JsonNode id = node.get("id");
    if (id == null || !id.isString() || !ID.matcher(id.asString()).matches()) {
      throw new InvalidConferenceConfigurationException(kind + " with a missing or invalid id");
    }
    return id.asString();
  }

  private static String requiredText(JsonNode node, String field, String owner) {
    JsonNode value = node.get(field);
    if (value == null || !value.isString() || value.asString().isBlank()) {
      throw new InvalidConferenceConfigurationException(owner + " has no " + field);
    }
    return value.asString().trim();
  }

  private static boolean booleanValue(JsonNode node, String field, String owner) {
    JsonNode value = node.get(field);
    if (value == null || !value.isBoolean()) {
      throw new InvalidConferenceConfigurationException(
          owner + ": " + field + " must be true or false");
    }
    return value.asBoolean();
  }

  private static Category category(JsonNode node, String id) {
    JsonNode value = node.get("category");
    if (value == null || !value.isString()) {
      throw new InvalidConferenceConfigurationException("option " + id + " has no category");
    }
    try {
      return Category.valueOf(value.asString());
    } catch (IllegalArgumentException e) {
      throw new InvalidConferenceConfigurationException(
          "option " + id + " has an unknown category", e);
    }
  }

  private static Set<RegistrationType> availableTo(JsonNode node, String id) {
    JsonNode value = node.get("availableTo");
    if (value == null) {
      return EnumSet.allOf(RegistrationType.class);
    }
    if (!value.isArray() || value.isEmpty()) {
      throw new InvalidConferenceConfigurationException(
          "option " + id + ": availableTo must list at least one registration type");
    }
    Set<RegistrationType> types = EnumSet.noneOf(RegistrationType.class);
    for (JsonNode type : value) {
      try {
        types.add(RegistrationType.valueOf(type.asString()));
      } catch (IllegalArgumentException e) {
        throw new InvalidConferenceConfigurationException(
            "option " + id + " has an unknown registration type", e);
      }
    }
    return types;
  }
}
