package si.konferenca.registration.acceptance.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Read-only view of the registration tables (contract database.md), used to observe results. */
public final class Db {

  public Optional<Map<String, Object>> registration(UUID id) {
    List<Map<String, Object>> rows = query("SELECT * FROM registration WHERE id = ?", id);
    return rows.stream().findFirst();
  }

  public long countByEmail(String email) {
    List<Map<String, Object>> rows =
        query("SELECT count(*) AS n FROM registration WHERE email = ?", email);
    return ((Number) rows.get(0).get("n")).longValue();
  }

  public long countByLastName(String lastName) {
    List<Map<String, Object>> rows =
        query("SELECT count(*) AS n FROM registration WHERE last_name = ?", lastName);
    return ((Number) rows.get(0).get("n")).longValue();
  }

  public List<Map<String, Object>> options(UUID id) {
    return query(
        "SELECT option_id, option_name, category FROM registration_option"
            + " WHERE registration_id = ? ORDER BY option_id",
        id);
  }

  public List<Map<String, Object>> consents(UUID id) {
    return query(
        "SELECT consent_id, consent_text, given_at FROM registration_consent"
            + " WHERE registration_id = ? ORDER BY consent_id",
        id);
  }

  private List<Map<String, Object>> query(String sql, Object param) {
    try (Connection c =
            DriverManager.getConnection(
                TestEnvironment.POSTGRES.getJdbcUrl(),
                TestEnvironment.POSTGRES.getUsername(),
                TestEnvironment.POSTGRES.getPassword());
        PreparedStatement s = c.prepareStatement(sql)) {
      s.setObject(1, param);
      try (ResultSet rs = s.executeQuery()) {
        ResultSetMetaData md = rs.getMetaData();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= md.getColumnCount(); i++) {
            row.put(md.getColumnLabel(i), rs.getObject(i));
          }
          rows.add(row);
        }
        return rows;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("query failed: " + sql, e);
    }
  }
}
