package si.konferenca.registration.catalog;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Parses and validates the conference configuration (contract conference-config.schema.json). */
public final class ConferenceCatalogParser {

  private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
  private static final Set<String> ROOT_FIELDS = Set.of("options", "consents");
  private static final Set<String> OPTION_FIELDS =
      Set.of("id", "name", "category", "active", "availableTo");
  private static final Set<String> CONSENT_FIELDS = Set.of("id", "text", "mandatory");
  private static final int MAX_NAME = 200;
  private static final int MAX_TEXT = 1000;

  private final JsonMapper json = JsonMapper.builder().build();

  public ConferenceCatalog parse(byte[] content) {
    JsonNode root;
    try {
      root = json.readTree(content);
    } catch (JacksonException e) {
      throw new InvalidConferenceConfigurationException("Configuration is not valid JSON", e);
    }
    if (root == null || !root.isObject()) {
      throw invalid("the configuration must be a JSON object");
    }
    onlyFields(root, ROOT_FIELDS, "configuration");
    List<ConferenceOption> options = new ArrayList<>();
    Set<String> optionIds = new HashSet<>();
    for (JsonNode node : array(root, "options")) {
      ConferenceOption option = option(node);
      if (!optionIds.add(option.id())) {
        throw invalid("duplicate option id " + option.id());
      }
      options.add(option);
    }
    List<ConsentDefinition> consents = new ArrayList<>();
    Set<String> consentIds = new HashSet<>();
    for (JsonNode node : array(root, "consents")) {
      ConsentDefinition consent = consent(node);
      if (!consentIds.add(consent.id())) {
        throw invalid("duplicate consent id " + consent.id());
      }
      consents.add(consent);
    }
    return new ConferenceCatalog(options, consents);
  }

  private static ConferenceOption option(JsonNode node) {
    requireObject(node, "option");
    onlyFields(node, OPTION_FIELDS, "option");
    String id = id(node, "option");
    String name = text(node, "name", MAX_NAME, "option " + id);
    OptionCategory category = category(node, id);
    boolean active = active(node, id);
    Set<RegistrationType> availableTo = availableTo(node, id);
    return new ConferenceOption(id, name, category, active, availableTo);
  }

  private static ConsentDefinition consent(JsonNode node) {
    requireObject(node, "consent");
    onlyFields(node, CONSENT_FIELDS, "consent");
    String id = id(node, "consent");
    String text = text(node, "text", MAX_TEXT, "consent " + id);
    JsonNode mandatory = node.get("mandatory");
    if (mandatory == null || !mandatory.isBoolean()) {
      throw invalid("consent " + id + " needs a boolean mandatory flag");
    }
    return new ConsentDefinition(id, text, mandatory.booleanValue());
  }

  private static List<JsonNode> array(JsonNode root, String field) {
    JsonNode node = root.get(field);
    if (node == null || !node.isArray()) {
      throw invalid(field + " must be an array");
    }
    List<JsonNode> items = new ArrayList<>();
    node.forEach(items::add);
    return items;
  }

  private static void requireObject(JsonNode node, String what) {
    if (!node.isObject()) {
      throw invalid("every " + what + " must be an object");
    }
  }

  private static void onlyFields(JsonNode node, Set<String> allowed, String what) {
    for (String name : node.propertyNames()) {
      if (!allowed.contains(name)) {
        throw invalid("unknown field " + name + " in " + what);
      }
    }
  }

  private static String id(JsonNode node, String what) {
    JsonNode id = node.get("id");
    if (id == null || !id.isString() || !ID.matcher(id.stringValue()).matches()) {
      throw invalid(what + " id missing or not matching " + ID.pattern());
    }
    return id.stringValue();
  }

  private static String text(JsonNode node, String field, int max, String what) {
    JsonNode value = node.get(field);
    if (value == null || !value.isString() || value.stringValue().isBlank()) {
      throw invalid(what + " needs a non-blank " + field);
    }
    if (value.stringValue().length() > max) {
      throw invalid(what + " " + field + " is longer than " + max);
    }
    return value.stringValue();
  }

  private static OptionCategory category(JsonNode node, String id) {
    JsonNode value = node.get("category");
    if (value == null || !value.isString()) {
      throw invalid("option " + id + " needs a category");
    }
    for (OptionCategory category : OptionCategory.values()) {
      if (category.name().equals(value.stringValue())) {
        return category;
      }
    }
    throw invalid("option " + id + " has unknown category " + value.stringValue());
  }

  private static boolean active(JsonNode node, String id) {
    JsonNode value = node.get("active");
    if (value == null) {
      return true;
    }
    if (!value.isBoolean()) {
      throw invalid("option " + id + " active must be true or false");
    }
    return value.booleanValue();
  }

  private static Set<RegistrationType> availableTo(JsonNode node, String id) {
    JsonNode value = node.get("availableTo");
    if (value == null) {
      return EnumSet.allOf(RegistrationType.class);
    }
    if (!value.isArray() || value.isEmpty()) {
      throw invalid("option " + id + " availableTo must be a non-empty array");
    }
    Set<RegistrationType> types = EnumSet.noneOf(RegistrationType.class);
    for (JsonNode type : value) {
      RegistrationType parsed = registrationType(type, id);
      if (!types.add(parsed)) {
        throw invalid("option " + id + " lists " + parsed + " twice");
      }
    }
    return types;
  }

  private static RegistrationType registrationType(JsonNode type, String id) {
    if (type.isString()) {
      for (RegistrationType candidate : RegistrationType.values()) {
        if (candidate.name().equals(type.stringValue())) {
          return candidate;
        }
      }
    }
    throw invalid("option " + id + " has unknown registration type " + type);
  }

  private static InvalidConferenceConfigurationException invalid(String reason) {
    return new InvalidConferenceConfigurationException(
        "Invalid conference configuration: " + reason);
  }
}
