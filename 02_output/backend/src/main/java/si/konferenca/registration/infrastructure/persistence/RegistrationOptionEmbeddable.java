package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Row of table {@code registration_option}. */
@Embeddable
class RegistrationOptionEmbeddable {

  @Column(name = "option_id", nullable = false)
  private String optionId;

  @Column(name = "option_name", nullable = false)
  private String optionName;

  @Column(name = "category", nullable = false)
  private String category;

  protected RegistrationOptionEmbeddable() {}

  RegistrationOptionEmbeddable(String optionId, String optionName, String category) {
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
