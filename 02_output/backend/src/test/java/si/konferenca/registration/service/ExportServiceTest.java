package si.konferenca.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationRepository;
import si.konferenca.registration.infra.ExcelWriter;

class ExportServiceTest {

  @Test
  void writesAllRegistrationsInRegistrationOrder() {
    RegistrationRepository repository = mock(RegistrationRepository.class);
    ExcelWriter excel = mock(ExcelWriter.class);
    List<Registration> registrations = List.of(mock(Registration.class));
    when(repository.findAllByOrderByRegisteredAtAsc()).thenReturn(registrations);
    when(excel.write(registrations)).thenReturn(new byte[] {1, 2});

    assertThat(new ExportService(repository, excel).exportWorkbook()).containsExactly(1, 2);
  }
}
