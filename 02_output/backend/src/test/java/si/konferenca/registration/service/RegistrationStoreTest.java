package si.konferenca.registration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.domain.ParticipantDetails;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationRepository;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.infra.JsonCopyStore;

class RegistrationStoreTest {

  private final RegistrationRepository repository = mock(RegistrationRepository.class);
  private final JsonCopyStore copies = mock(JsonCopyStore.class);
  private final RegistrationStore store = new RegistrationStore(repository, copies);
  private final Registration registration =
      new Registration(
          UUID.randomUUID(),
          RegistrationType.EXTERNAL,
          new ParticipantDetails("A", "B", "a@b.org", "O", null, null, null),
          Instant.now());

  @BeforeEach
  void startSynchronization() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void clearSynchronization() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  void flushesTheRowThenWritesTheCopy() {
    when(repository.saveAndFlush(registration)).thenReturn(registration);
    when(copies.write(registration)).thenReturn(new byte[] {7});

    RegistrationStore.StoredRegistration stored = store.save(registration);

    assertThat(stored.registration()).isSameAs(registration);
    assertThat(stored.jsonCopy()).containsExactly(7);
    List<TransactionSynchronization> synchronizations =
        TransactionSynchronizationManager.getSynchronizations();
    assertThat(synchronizations).hasSize(1);

    synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    verify(copies, never()).delete(any());
  }

  @Test
  void removesTheCopyWhenTheTransactionDoesNotCommit() {
    when(repository.saveAndFlush(registration)).thenReturn(registration);
    store.save(registration);
    TransactionSynchronization synchronization =
        TransactionSynchronizationManager.getSynchronizations().getFirst();

    synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    synchronization.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

    verify(copies, org.mockito.Mockito.times(2)).delete(registration.getId());
  }

  @Test
  void aFailedCopyPropagatesSoTheTransactionRollsBack() {
    when(repository.saveAndFlush(registration)).thenReturn(registration);
    when(copies.write(registration)).thenThrow(new UncheckedIOException(new IOException("x")));

    assertThatThrownBy(() -> store.save(registration)).isInstanceOf(UncheckedIOException.class);
    assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
  }
}
