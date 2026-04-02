import java.sql.*;
public class tmp_check_analytics_map {
  public static void main(String[] args) throws Exception {
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:C:/Users/Omar/OneDrive/Desktop/android/backend/athar.db")) {
      System.out.println("VOLUNTEERS:");
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT id, full_name, email, volunteer_live, role_verified_at FROM accounts WHERE role='Volunteer' ORDER BY full_name")) {
        while (rs.next()) {
          System.out.println(rs.getString(1)+" | "+rs.getString(2)+" | "+rs.getString(3)+" | live="+rs.getInt(4)+" | verified="+rs.getString(5));
        }
      }
      System.out.println("\nCOMPLETED REQUESTS BY VOLUNTEER:");
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT volunteer_id, COUNT(*), COALESCE(SUM(service_fee),0) FROM help_requests WHERE volunteer_id IS NOT NULL AND status IN ('completed','rated','archived') GROUP BY volunteer_id ORDER BY COUNT(*) DESC")) {
        while (rs.next()) {
          System.out.println(rs.getString(1)+" | count="+rs.getInt(2)+" | fee_sum="+rs.getDouble(3));
        }
      }
      System.out.println("\nREVIEWS BY VOLUNTEER:");
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT volunteer_id, COUNT(*) FROM volunteer_reviews GROUP BY volunteer_id ORDER BY COUNT(*) DESC")) {
        while (rs.next()) {
          System.out.println(rs.getString(1)+" | reviews="+rs.getInt(2));
        }
      }
    }
  }
}
