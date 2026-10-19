package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** The backend architecture declared in docs/02_specification.md (AR-02, AR-03). */
class ArchitectureTest {

  private static final String ROOT = "si.konferenca.registration";
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  @Test
  void AR_02_domainDependsOnNothingOutsideDomainAndTheJdk() {
    classes()
        .that()
        .resideInAPackage(ROOT + ".domain..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(ROOT + ".domain..", "java..")
        .check(CLASSES);
  }

  @Test
  void AR_02_applicationDependsOnlyOnDomainJdkAndSpringTransactions() {
    classes()
        .that()
        .resideInAPackage(ROOT + ".application..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            ROOT + ".application..",
            ROOT + ".domain..",
            "java..",
            "org.springframework.transaction..",
            "org.slf4j..")
        .check(CLASSES);
  }

  @Test
  void AR_02_webDoesNotDependOnInfrastructure() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".web..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ROOT + ".infrastructure..")
        .check(CLASSES);
  }

  @Test
  void AR_02_infrastructureDoesNotDependOnWeb() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ROOT + ".web..")
        .check(CLASSES);
  }

  @Test
  void AR_02_onlyTheEntryPointLivesInTheRootPackage() {
    classes()
        .that()
        .resideInAPackage(ROOT)
        .should()
        .haveSimpleNameStartingWith("RegistrationApplication")
        .check(CLASSES);
  }

  @Test
  void AR_03_noCyclesBetweenSlices() {
    slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(CLASSES);
  }
}
