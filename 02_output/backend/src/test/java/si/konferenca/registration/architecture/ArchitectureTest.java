package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** The AR-02 declaration of docs/02_specification.md section 2, rules 1 to 5, and AR-03. */
@AnalyzeClasses(
    packages = "si.konferenca.registration",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String ROOT = "si.konferenca.registration";

  @ArchTest
  static final ArchRule domainDependsOnNoOtherApplicationPackageOrFramework =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              ROOT + ".api..",
              ROOT + ".adapter..",
              ROOT + ".config..",
              "org.springframework.web..",
              "org.springframework.data..",
              "org.springframework.mail..",
              "jakarta.persistence..",
              "jakarta.mail..",
              "org.apache.poi..");

  @ArchTest
  static final ArchRule apiDependsOnlyOnDomain =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".api..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(ROOT + ".adapter..", ROOT + ".config..");

  @ArchTest
  static final ArchRule adaptersDependOnlyOnDomain =
      slices()
          .matching(ROOT + ".adapter.(*)..")
          .should()
          .notDependOnEachOther()
          .because("each adapter depends only on the domain");

  @ArchTest
  static final ArchRule adaptersDoNotUseApiOrConfig =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".adapter..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(ROOT + ".api..", ROOT + ".config..");

  @ArchTest
  static final ArchRule onlyConfigUsesAdapters =
      classes()
          .that()
          .resideInAPackage(ROOT + ".adapter..")
          .should()
          .onlyHaveDependentClassesThat()
          .resideInAnyPackage(ROOT + ".adapter..", ROOT + ".config..");

  @ArchTest
  static final ArchRule noPackageCycles =
      slices().matching(ROOT + ".(*)..").should().beFreeOfCycles();
}
