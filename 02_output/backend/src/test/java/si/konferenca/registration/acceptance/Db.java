package si.konferenca.registration.acceptance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the registration tables of contract {@code database.md}. */
final class Db {

  private final String url;
  private final String user;
  private final String password;

  Db(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  private Connection connect() throws Exception {
    return DriverManager.getConnection(url, user, password);
  }

  private boolean tableExists(Connection c, String table) throws Exception {
    try (PreparedStatement s = c.prepareStatement("select to_regclass(?) is not null")) {
      s.setString(1, table);
      try (ResultSet r = s.executeQuery()) {
        r.next();
        return r.getBoolean(1);
      }
    }
  }

  void deleteAllRegistrations() throws Exception {
    try (Connection c = connect()) {
      if (tableExists(c, "registration")) {
        try (PreparedStatement s = c.prepareStatement("truncate table registration cascade")) {
          s.execute();
        }
      }
    }
  }

  long countRegistrations() throws Exception {
    try (Connection c = connect()) {
      if (!tableExists(c, "registration")) {
        return 0;
      }
      try (PreparedStatement s = c.prepareStatement("select count(*) from registration");
          ResultSet r = s.executeQuery()) {
        r.next();
        return r.getLong(1);
      }
    }
  }

  /** The {@code registration} row with this id, column name to value; null if absent. */
  Map<String, Object> registration(String id) throws Exception {
    try (Connection c = connect()) {
      if (!tableExists(c, "registration")) {
        return null;
      }
      try (PreparedStatement s =
          c.prepareStatement("select * from registration where id = cast(? as uuid)")) {
        s.setString(1, id);
        try (ResultSet r = s.executeQuery()) {
          if (!r.next()) {
            return null;
          }
          ResultSetMetaData m = r.getMetaData();
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= m.getColumnCount(); i++) {
            row.put(m.getColumnName(i), r.getObject(i));
          }
          return row;
        }
      }
    }
  }

  /** Option ids stored for a registration, in selection order. */
  List<String> optionIds(String registrationId) throws Exception {
    List<String> ids = new ArrayList<>();
    try (Connection c = connect()) {
      if (!tableExists(c, "registration_option")) {
        return ids;
      }
      try (PreparedStatement s =
          c.prepareStatement(
              "select option_id from registration_option where registration_id = cast(? as uuid)"
                  + " order by position")) {
        s.setString(1, registrationId);
        try (ResultSet r = s.executeQuery()) {
          while (r.next()) {
            ids.add(r.getString(1));
          }
        }
      }
    }
    return ids;
  }
}
