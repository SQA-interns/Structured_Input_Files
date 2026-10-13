package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The architecture declared in docs/02_specification.md section 2 (AR-02, AR-03, DoD-04). */
class ArchitectureTest {

  private static final String ROOT = "si.konferenca.registration";
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  private static final List<String> PACKAGES =
      List.of(
          "domain",
          "catalog",
          "service",
          "persistence",
          "storage",
          "mail",
          "captcha",
          "export",
          "security",
          "web");

  /** Spec 2: the application packages each package may depend on (besides itself and root). */
  private static final Map<String, List<String>> ALLOWED =
      Map.of(
          "domain", List.of(),
          "catalog", List.of("domain"),
          "service", List.of("domain", "persistence"),
          "persistence", List.of("domain"),
          "storage", List.of("domain", "service"),
          "mail", List.of("domain", "service"),
          "captcha", List.of("domain", "service"),
          "export", List.of("domain", "persistence"),
          "security", List.of("domain"),
          "web", List.of("domain", "catalog", "service", "export"));

  @Test
  void domainIsPlainJava() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "jakarta.persistence..", "tools.jackson..", ROOT)
        .check(CLASSES);
  }

  @Test
  void eachPackageDependsOnlyOnItsDeclaredPackages() {
    for (String pkg : PACKAGES) {
      String[] forbidden =
          PACKAGES.stream()
              .filter(other -> !other.equals(pkg) && !ALLOWED.get(pkg).contains(other))
              .map(other -> ROOT + "." + other + "..")
              .toArray(String[]::new);
      noClasses()
          .that()
          .resideInAPackage(ROOT + "." + pkg + "..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(forbidden)
          .because("spec section 2 allows " + pkg + " to use only " + ALLOWED.get(pkg))
          .check(CLASSES);
    }
  }

  @Test
  void nothingDependsOnTheWebLayer() {
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".web..")
        .and()
        .resideOutsideOfPackage(ROOT)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ROOT + ".web..")
        .check(CLASSES);
  }

  @Test
  void serviceUsesSideEffectsOnlyThroughPorts() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".service..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ROOT + ".storage..", ROOT + ".mail..", ROOT + ".captcha..")
        .check(CLASSES);
  }

  @Test
  void noPackageCycles() {
    slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(CLASSES);
  }
}
