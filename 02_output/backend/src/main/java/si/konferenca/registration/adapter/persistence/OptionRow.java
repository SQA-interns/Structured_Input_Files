package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

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
}
