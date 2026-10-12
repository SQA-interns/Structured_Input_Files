package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The declared architecture of specification S2 (AR-02) and no package cycles (AR-03). */
class ArchitectureTest {

  private static final String ROOT = "si.konferenca.registration";
  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
  }

  @Test
  @DisplayName("AR-02 layers depend only as declared in S2")
  void layersDependAsDeclared() {
    layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("domain")
        .definedBy(ROOT + ".domain..")
        .layer("application")
        .definedBy(ROOT + ".application..")
        .layer("web")
        .definedBy(ROOT + ".web..")
        .layer("infrastructure")
        .definedBy(ROOT + ".infrastructure..")
        .layer("config")
        .definedBy(ROOT + ".config..")
        .whereLayer("config")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("web")
        .mayOnlyBeAccessedByLayers("config")
        .whereLayer("infrastructure")
        .mayOnlyBeAccessedByLayers("config")
        .whereLayer("application")
        .mayOnlyBeAccessedByLayers("web", "infrastructure", "config")
        .check(classes);
  }

  @Test
  @DisplayName("AR-02 the domain uses no framework")
  void domainIsFrameworkFree() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.servlet..",
            "jakarta.mail..",
            "org.apache.poi..",
            "tools.jackson..",
            "com.fasterxml..")
        .check(classes);
  }

  @Test
  @DisplayName("AR-02 the application layer uses only Spring transactions from the framework")
  void applicationUsesOnlyTransactions() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.web..",
            "org.springframework.stereotype..",
            "jakarta.persistence..",
            "jakarta.servlet..",
            "jakarta.mail..",
            "org.apache.poi..")
        .check(classes);
  }

  @Test
  @DisplayName("AR-02 infrastructure adapters do not depend on each other")
  void adaptersAreIndependent() {
    slices()
        .matching(ROOT + ".infrastructure.(*)..")
        .should()
        .notDependOnEachOther()
        .check(classes);
  }

  @Test
  @DisplayName("AR-03 no package cycles")
  void noPackageCycles() {
    slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(classes);
    slices().matching(ROOT + ".infrastructure.(*)..").should().beFreeOfCycles().check(classes);
  }
}
