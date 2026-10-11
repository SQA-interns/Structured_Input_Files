package si.konferenca.registration.adapter.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Conference configuration read once from a JSON file (contract conference-config.schema.json). Any
 * violation throws, so the application refuses to start (AC-003-03).
 */
public final class JsonFileConferenceCatalog implements ConferenceCatalog {

  static final String DEFAULT_RESOURCE = "/conference-options.json";
  private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
  private static final Set<String> TOP_KEYS = Set.of("options", "consents");
  private static final Set<String> OPTION_KEYS =
      Set.of("id", "name", "category", "active", "availableTo");
  private static final Set<String> CONSENT_KEYS = Set.of("id", "text", "mandatory");

  private final List<ConferenceOption> options;
  private final List<ConsentDefinition> consents;

  private JsonFileConferenceCatalog(
      List<ConferenceOption> options, List<ConsentDefinition> consents) {
    this.options = List.copyOf(options);
    this.consents = List.copyOf(consents);
  }

  /** Loads the file, or the bundled default when {@code file} is null. */
  public static JsonFileConferenceCatalog load(Path file) {
    try (InputStream in =
        file == null
            ? JsonFileConferenceCatalog.class.getResourceAsStream(DEFAULT_RESOURCE)
            : Files.newInputStream(file)) {
      if (in == null) {
        throw invalid("bundled configuration missing");
      }
      return parse(in.readAllBytes());
    } catch (IOException e) {
      throw new IllegalStateException("Conference configuration cannot be read", e);
    }
  }

  static JsonFileConferenceCatalog parse(byte[] json) {
    Object root;
    try {
      root = JsonMapper.builder().build().readValue(json, Object.class);
    } catch (JacksonException e) {
      throw invalid("not valid JSON");
    }
    Map<?, ?> top = object(root, "configuration", TOP_KEYS);
    List<ConferenceOption> options = new ArrayList<>();
    Set<String> optionIds = new HashSet<>();
    for (Object item : list(top.get("options"), "options")) {
      ConferenceOption option = option(object(item, "option", OPTION_KEYS));
      if (!optionIds.add(option.id())) {
        throw invalid("duplicate option id " + option.id());
      }
      options.add(option);
    }
    List<ConsentDefinition> consents = new ArrayList<>();
    Set<String> consentIds = new HashSet<>();
    for (Object item : list(top.get("consents"), "consents")) {
      Map<?, ?> c = object(item, "consent", CONSENT_KEYS);
      String id = id(c.get("id"), "consent");
      if (!consentIds.add(id)) {
        throw invalid("duplicate consent id " + id);
      }
      consents.add(
          new ConsentDefinition(
              id, text(c.get("text"), "consent " + id + " text", 1000), bool(c.get("mandatory"))));
    }
    return new JsonFileConferenceCatalog(options, consents);
  }

  private static ConferenceOption option(Map<?, ?> o) {
    String id = id(o.get("id"), "option");
    String name = text(o.get("name"), "option " + id + " name", 200);
    OptionCategory category = category(o.get("category"), id);
    Set<RegistrationType> availableTo = EnumSet.allOf(RegistrationType.class);
    if (o.containsKey("availableTo")) {
      List<?> types = list(o.get("availableTo"), "option " + id + " availableTo");
      if (types.isEmpty() || new HashSet<>(types).size() != types.size()) {
        throw invalid("option " + id + " availableTo must be non-empty and unique");
      }
      availableTo = EnumSet.noneOf(RegistrationType.class);
      for (Object t : types) {
        availableTo.add(registrationType(t, id));
      }
    }
    return new ConferenceOption(id, name, category, bool(o.get("active")), availableTo);
  }

  private static Map<?, ?> object(Object value, String what, Set<String> allowed) {
    if (!(value instanceof Map<?, ?> map)) {
      throw invalid(what + " must be an object");
    }
    for (Object key : map.keySet()) {
      if (!allowed.contains(key)) {
        throw invalid(what + " has unknown property " + key);
      }
    }
    return map;
  }

  private static List<?> list(Object value, String what) {
    if (!(value instanceof List<?> list)) {
      throw invalid(what + " must be an array");
    }
    return list;
  }

  private static String id(Object value, String what) {
    if (!(value instanceof String s) || !ID.matcher(s).matches()) {
      throw invalid(what + " id missing or not matching " + ID.pattern());
    }
    return s;
  }

  private static String text(Object value, String what, int max) {
    if (!(value instanceof String s) || s.isBlank() || s.length() > max) {
      throw invalid(what + " missing, empty or longer than " + max);
    }
    return s;
  }

  private static boolean bool(Object value) {
    if (!(value instanceof Boolean b)) {
      throw invalid("active and mandatory must be true or false");
    }
    return b;
  }

  private static OptionCategory category(Object value, String id) {
    for (OptionCategory c : OptionCategory.values()) {
      if (c.name().equals(value)) {
        return c;
      }
    }
    throw invalid("option " + id + " has unknown category");
  }

  private static RegistrationType registrationType(Object value, String id) {
    for (RegistrationType t : RegistrationType.values()) {
      if (t.name().equals(value)) {
        return t;
      }
    }
    throw invalid("option " + id + " has unknown registration type");
  }

  private static IllegalStateException invalid(String reason) {
    return new IllegalStateException("Invalid conference configuration: " + reason);
  }

  @Override
  public List<ConferenceOption> options() {
    return options;
  }

  @Override
  public List<ConsentDefinition> consents() {
    return consents;
  }
}
