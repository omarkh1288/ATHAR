import java.sql.*;
public class DbScan {
  public static void main(String[] args) throws Exception {
    String db = "jdbc:sqlite:C:/Users/Omar/OneDrive/Desktop/android/backend/athar.db";
    Class.forName("org.sqlite.JDBC");
    try (Connection c = DriverManager.getConnection(db)) {
      System.out.println("-- help_requests for vol-seed-1 --");
      try (PreparedStatement ps = c.prepareStatement(
        "SELECT id, user_id, user_name, help_type, payment_method, status, hours, price_per_hour, service_fee, created_at, completed_at FROM help_requests WHERE volunteer_id = ? ORDER BY created_at DESC")) {
        ps.setString(1, "vol-seed-1");
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            System.out.println(String.join(" | ",
              rs.getString("id"),
              rs.getString("user_id"),
              rs.getString("user_name"),
              rs.getString("help_type"),
              rs.getString("payment_method"),
              rs.getString("status"),
              String.valueOf(rs.getInt("hours")),
              String.valueOf(rs.getInt("price_per_hour")),
              String.valueOf(rs.getDouble("service_fee")),
              String.valueOf(rs.getLong("created_at")),
              String.valueOf(rs.getLong("completed_at"))
            ));
          }
        }
      }
      System.out.println("-- successful payments joined to vol-seed-1 requests --");
      try (PreparedStatement ps = c.prepareStatement(
        "SELECT p.id, p.request_id, p.payment_method, p.status, p.success, p.amount, p.created_at FROM payments p JOIN help_requests h ON h.id = p.request_id WHERE h.volunteer_id = ? AND p.success = 1 ORDER BY p.created_at DESC")) {
        ps.setString(1, "vol-seed-1");
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            System.out.println(String.join(" | ",
              rs.getString("id"),
              rs.getString("request_id"),
              rs.getString("payment_method"),
              rs.getString("status"),
              String.valueOf(rs.getBoolean("success")),
              String.valueOf(rs.getDouble("amount")),
              String.valueOf(rs.getLong("created_at"))
            ));
          }
        }
      }
      System.out.println("-- completed/rated/archived requests for vol-seed-1 with successful payment match --");
      try (PreparedStatement ps = c.prepareStatement(
        "SELECT h.id, h.status, h.payment_method, h.completed_at, p.id AS payment_id, p.status AS payment_status FROM help_requests h LEFT JOIN payments p ON p.request_id = h.id AND p.success = 1 WHERE h.volunteer_id = ? AND h.status IN ('completed','rated','archived') ORDER BY h.created_at DESC")) {
        ps.setString(1, "vol-seed-1");
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            System.out.println(String.join(" | ",
              rs.getString("id"),
              rs.getString("status"),
              rs.getString("payment_method"),
              String.valueOf(rs.getLong("completed_at")),
              String.valueOf(rs.getString("payment_id")),
              String.valueOf(rs.getString("payment_status"))
            ));
          }
        }
      }
    }
  }
}
