package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The layered architecture declared in spec section 2 (AR-02) and no package cycles (AR-03). */
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
  void domainUsesOnlyJavaSe() {
    classes()
        .that()
        .resideInAPackage(ROOT + ".domain..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(ROOT + ".domain..", "java..")
        .check(classes);
  }

  @Test
  void applicationUsesOnlyDomainAndSpringTransactions() {
    classes()
        .that()
        .resideInAPackage(ROOT + ".application..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            ROOT + ".application..",
            ROOT + ".domain..",
            "java..",
            "org.slf4j..",
            "org.springframework.transaction..")
        .check(classes);
  }

  @Test
  void apiDoesNotUseAdaptersOrConfiguration() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".api..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ROOT + ".persistence..", ROOT + ".infrastructure..", ROOT + ".config..")
        .check(classes);
  }

  @Test
  void adaptersDoNotUseApiOrConfiguration() {
    noClasses()
        .that()
        .resideInAnyPackage(ROOT + ".persistence..", ROOT + ".infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ROOT + ".api..", ROOT + ".config..")
        .check(classes);
  }

  @Test
  void adaptersDoNotUseEachOther() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".persistence..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ROOT + ".infrastructure..")
        .check(classes);
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ROOT + ".persistence..")
        .check(classes);
  }

  @Test
  void jpaOnlyInPersistence() {
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".persistence..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
        .check(classes);
  }

  @Test
  void noPackageCycles() {
    slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(classes);
  }
}
