package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.domain.RegistrationRepository;

/** The backend architecture declared in docs/02_specification.md section 2 (AR-02, AR-03). */
class ArchitectureTest {

  private static final String ROOT = "si.konferenca.registration";
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  @Test
  void layersDependOnlyDownwards() {
    layeredArchitecture()
        .consideringOnlyDependenciesInLayers()
        .layer("web")
        .definedBy(ROOT + ".web..")
        .layer("security")
        .definedBy(ROOT + ".security..")
        .layer("service")
        .definedBy(ROOT + ".service..")
        .layer("infra")
        .definedBy(ROOT + ".infra..")
        .layer("conference")
        .definedBy(ROOT + ".conference..")
        .layer("domain")
        .definedBy(ROOT + ".domain..")
        .layer("config")
        .definedBy(ROOT + ".config..")
        .whereLayer("web")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("security")
        .mayNotBeAccessedByAnyLayer()
        .whereLayer("service")
        .mayOnlyBeAccessedByLayers("web")
        .whereLayer("infra")
        .mayOnlyBeAccessedByLayers("service")
        .whereLayer("conference")
        .mayOnlyBeAccessedByLayers("web", "service", "infra")
        .whereLayer("domain")
        .mayOnlyBeAccessedByLayers("web", "service", "infra", "conference")
        .whereLayer("config")
        .mayOnlyBeAccessedByLayers("web", "security", "service", "infra", "conference")
        .check(CLASSES);
  }

  @Test
  void domainAndConfigDependOnNoOtherApplicationPackage() {
    noClasses()
        .that()
        .resideInAnyPackage(ROOT + ".domain..", ROOT + ".config..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            ROOT + ".web..",
            ROOT + ".security..",
            ROOT + ".service..",
            ROOT + ".infra..",
            ROOT + ".conference..")
        .check(CLASSES);
  }

  @Test
  void packagesAreFreeOfCycles() {
    slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(CLASSES);
  }

  @Test
  void controllersLiveOnlyInWeb() {
    classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .resideInAPackage(ROOT + ".web..")
        .check(CLASSES);
  }

  @Test
  void entitiesLiveOnlyInDomain() {
    classes()
        .that()
        .areAnnotatedWith(Entity.class)
        .should()
        .resideInAPackage(ROOT + ".domain..")
        .check(CLASSES);
  }

  @Test
  void webDoesNotUseRepositories() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".web..")
        .should()
        .dependOnClassesThat()
        .areAssignableTo(RegistrationRepository.class)
        .check(CLASSES);
  }
}
