package si.konferenca.registration.acceptance.support;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGenerator;

/** Shows {@code ac_001_02_text} as {@code AC-001-02 text} so every test name carries its AC id. */
public class AcIdDisplayNames extends DisplayNameGenerator.Standard {

  @Override
  public String generateDisplayNameForMethod(
      List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
    String name = testMethod.getName();
    if (name.matches("ac_\\d{3}_\\d{2}_.*")) {
      return "AC-"
          + name.substring(3, 6)
          + "-"
          + name.substring(7, 9)
          + " "
          + name.substring(10).replace('_', ' ');
    }
    return super.generateDisplayNameForMethod(enclosingInstanceTypes, testClass, testMethod);
  }
}
