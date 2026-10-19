package si.konferenca.registration.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.ExportRegistrations;

/** GET /api/registrations/export (openapi.json exportRegistrations). */
@RestController
public class ExportController {

  static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final ExportRegistrations exportRegistrations;

  public ExportController(ExportRegistrations exportRegistrations) {
    this.exportRegistrations = exportRegistrations;
  }

  @GetMapping("/api/registrations/export")
  public ResponseEntity<byte[]> export() {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("registrations.xlsx").build().toString())
        .cacheControl(CacheControl.noStore())
        .body(exportRegistrations.export());
  }
}
