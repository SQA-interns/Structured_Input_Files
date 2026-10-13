package si.konferenca.registration.web;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.export.RegistrationExport;

/** {@code GET /api/organizer/registrations.xlsx}, organizers only (US-008, BR-08). */
@RestController
public class ExportController {

  static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final RegistrationExport export;

  public ExportController(RegistrationExport export) {
    this.export = export;
  }

  @GetMapping("/api/organizer/registrations.xlsx")
  public ResponseEntity<byte[]> registrations() {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("registrations.xlsx").build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(export.workbook());
  }
}
