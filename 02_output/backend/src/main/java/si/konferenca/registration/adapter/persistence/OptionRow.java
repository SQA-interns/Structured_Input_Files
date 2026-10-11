package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/** Row of registration_option. */
@Embeddable
public class OptionRow {

  @Column(name = "option_id", nullable = false, length = 64)
  private String optionId;

  @Column(name = "option_name", nullable = false, length = 200)
  private String optionName;

  @Column(name = "category", nullable = false, length = 16)
  private String category;

  protected OptionRow() {}

  OptionRow(String optionId, String optionName, String category) {
    this.optionId = optionId;
    this.optionName = optionName;
    this.category = category;
  }

  String optionId() {
    return optionId;
  }

  String optionName() {
    return optionName;
  }

  String category() {
    return category;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof OptionRow other
        && Objects.equals(optionId, other.optionId)
        && Objects.equals(optionName, other.optionName)
        && Objects.equals(category, other.category);
  }

  @Override
  public int hashCode() {
    return Objects.hash(optionId, optionName, category);
  }
}
