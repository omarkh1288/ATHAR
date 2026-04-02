import java.sql.*;

public class tmp_accounts_read {
  public static void main(String[] args) throws Exception {
    String db = "jdbc:sqlite:C:/Users/Omar/OneDrive/Desktop/android/backend/athar.db";
    try (Connection c = DriverManager.getConnection(db)) {
      System.out.println("ACCOUNTS");
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT account_id, role, full_name, email, volunteer_live, role_verified_at FROM accounts ORDER BY role, full_name")) {
        while (rs.next()) {
          System.out.println(rs.getString(1)+" | "+rs.getString(2)+" | "+rs.getString(3)+" | "+rs.getString(4)+" | live="+rs.getInt(5)+" | verified="+rs.getString(6));
        }
      }
      System.out.println("\nREQUESTS BY VOLUNTEER");
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT volunteer_id, COUNT(*), COALESCE(SUM(service_fee),0) FROM help_requests WHERE volunteer_id IS NOT NULL GROUP BY volunteer_id ORDER BY COUNT(*) DESC")) {
        while (rs.next()) {
          System.out.println(rs.getString(1)+" | requests="+rs.getInt(2)+" | service_fee_sum="+rs.getDouble(3));
        }
      }
      System.out.println("\nPAYMENTS BY REQUEST VOLUNTEER");
      String sql = "SELECT h.volunteer_id, COUNT(p.payment_id), COALESCE(SUM(p.amount),0) FROM payments p LEFT JOIN help_requests h ON h.request_id = p.request_id WHERE h.volunteer_id IS NOT NULL GROUP BY h.volunteer_id ORDER BY COUNT(p.payment_id) DESC";
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
        while (rs.next()) {
          System.out.println(rs.getString(1)+" | payments="+rs.getInt(2)+" | amount_sum="+rs.getDouble(3));
        }
      }
    }
  }
}
