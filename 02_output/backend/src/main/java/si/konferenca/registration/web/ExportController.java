package si.konferenca.registration.web;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.ExportService;

/** {@code GET /api/organizer/export}; organizer access is enforced by Spring Security (BR-08). */
@RestController
class ExportController {

  static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final ExportService exports;

  ExportController(ExportService exports) {
    this.exports = exports;
  }

  @GetMapping("/api/organizer/export")
  ResponseEntity<byte[]> export() {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename("registrations.xlsx").build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(exports.export());
  }
}
