package si.konferenca.registration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** An option selected in a registration, with its name and category at the time of selection. */
@Embeddable
public record SelectedOption(
    @Column(name = "option_id", nullable = false, length = 64) String optionId,
    @Column(name = "option_name", nullable = false, length = 200) String optionName,
    @Column(name = "category", nullable = false, length = 16) String category) {}
