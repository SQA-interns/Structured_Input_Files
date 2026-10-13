package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** The declared architecture of spec §2 (AR-02, AR-03, DoD-04). */
@AnalyzeClasses(
    packages = "si.konferenca.registration",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String ROOT = "si.konferenca.registration";

  /** A1: the domain is pure Java. */
  @ArchTest
  static final ArchRule domainDependsOnNoOtherLayerOrFramework =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              ROOT + ".application..",
              ROOT + ".adapter..",
              ROOT + ".config..",
              "org.springframework..",
              "jakarta.persistence..",
              "org.apache.poi..",
              "jakarta.mail..",
              "tools.jackson..");

  /** A2: use cases depend only on the domain among project packages. */
  @ArchTest
  static final ArchRule applicationDependsOnlyOnDomain =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(ROOT + ".adapter..", ROOT + ".config..");

  /** A3: adapters do not depend on each other. */
  @ArchTest
  static final ArchRule adaptersAreIndependent =
      slices().matching(ROOT + ".adapter.(*)..").should().notDependOnEachOther();

  /** Config builds beans from application and domain only. */
  @ArchTest
  static final ArchRule configDoesNotDependOnAdapters =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".config..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(ROOT + ".adapter..");

  /** A4: JPA and Spring Data only in the persistence adapter. */
  @ArchTest
  static final ArchRule persistenceTechnologyOnlyInPersistenceAdapter =
      noClasses()
          .that()
          .resideOutsideOfPackage(ROOT + ".adapter.persistence..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("jakarta.persistence..", "org.springframework.data..");

  /** A4: Apache POI only in the export adapter. */
  @ArchTest
  static final ArchRule poiOnlyInExportAdapter =
      noClasses()
          .that()
          .resideOutsideOfPackage(ROOT + ".adapter.export..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("org.apache.poi..");

  /** A4: mail APIs only in the mail adapter. */
  @ArchTest
  static final ArchRule mailOnlyInMailAdapter =
      noClasses()
          .that()
          .resideOutsideOfPackage(ROOT + ".adapter.mail..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework.mail..", "jakarta.mail..");

  /** A5 (AR-03): no package cycles. */
  @ArchTest
  static final ArchRule noPackageCycles =
      slices().matching(ROOT + ".(**)").should().beFreeOfCycles();
}
