import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager; // Vẫn cần cho Class.forName, hoặc có thể bỏ nếu driver tự đăng ký
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database connections using HikariCP connection pool,
 * with support for SSH tunneling.
 */
public class DBConnectionManager {

    // --- THÔNG TIN CHO SSH TUNNEL (Giữ nguyên từ code của bạn) ---
    private static final String SSH_USER = "superhome";
    private static final String SSH_PASSWORD = "BachDuong223@";
    private static final String SSH_HOST = "103.186.101.178";
    private static final int SSH_PORT = 22;

    // --- THÔNG TIN CHO POSTGRESQL SERVER (TRÊN MÁY REMOTE, SAU KHI ĐÃ TUNNEL) ---
    private static final String REMOTE_DB_HOST = "localhost";
    private static final int REMOTE_DB_PORT = 5432;

    // --- CẤU HÌNH KẾT NỐI DATABASE QUA TUNNEL ---
    private static final int LOCAL_FORWARD_PORT = 8682;
    // URL JDBC sẽ kết nối đến port local này
    private static final String JDBC_URL_VIA_TUNNEL = "jdbc:postgresql://localhost:" + LOCAL_FORWARD_PORT + "/oop_project";

    // --- THÔNG TIN ĐĂNG NHẬP VÀO POSTGRESQL DATABASE ---
    private static final String DB_USERNAME = "postgres"; // User PostgreSQL
    private static final String DB_PASSWORD = "admin";    // Password PostgreSQL

    // Cờ để quyết định có sử dụng SSH Tunnel hay không
    // Đặt thành true nếu bạn cần SSH tunnel để kết nối DB từ môi trường phát triển.
    // Đặt thành false nếu DB có thể truy cập trực tiếp (ví dụ: DB chạy trên localhost không qua tunnel).
    private static final boolean USE_SSH_TUNNEL = true; // <<--- ĐIỀU CHỈNH NẾU CẦN

    private static HikariDataSource dataSource;

    static {
        try {
            // 1. Load the PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL JDBC Driver loaded successfully.");

            // 2. Thiết lập SSH Tunnel NẾU được cấu hình để sử dụng
            if (USE_SSH_TUNNEL) {
                System.out.println("Attempting to establish SSH tunnel for DB connection pool...");
                // SshTunnelManager.connect sẽ tự kiểm tra nếu đã kết nối
                SshTunnelManager.connect(SSH_USER, SSH_PASSWORD, SSH_HOST, SSH_PORT,
                        REMOTE_DB_HOST, LOCAL_FORWARD_PORT, REMOTE_DB_PORT);
                // Nếu SshTunnelManager.connect ném exception, khối static này sẽ dừng và báo lỗi.
                System.out.println("SSH tunnel connection seems successful for DB pool.");
            } else {
                System.out.println("SSH Tunnel is disabled. Attempting direct DB connection.");
            }

            // 3. Cấu hình và Khởi tạo HikariCP DataSource
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(JDBC_URL_VIA_TUNNEL); // Luôn dùng URL qua tunnel (nếu tunnel bật) hoặc URL trực tiếp
            config.setUsername(DB_USERNAME);
            config.setPassword(DB_PASSWORD);

            // Các cài đặt HikariCP phổ biến (có thể điều chỉnh)
            config.setMaximumPoolSize(10); // Số lượng kết nối tối đa trong pool
            config.setMinimumIdle(2);    // Số lượng kết nối nhàn rỗi tối thiểu
            config.setConnectionTimeout(30000); // Thời gian chờ tối đa để lấy kết nối (ms)
            config.setIdleTimeout(600000);      // Thời gian tối đa một kết nối có thể nhàn rỗi (ms)
            config.setMaxLifetime(1800000);     // Thời gian sống tối đa của một kết nối (ms)
            config.setPoolName("NoteAppHikariPool");
            // config.setConnectionTestQuery("SELECT 1"); // Câu lệnh kiểm tra kết nối (tùy chọn)

            System.out.println("Initializing HikariCP connection pool to: " + config.getJdbcUrl());
            dataSource = new HikariDataSource(config);
            System.out.println("HikariCP connection pool initialized successfully.");

        } catch (ClassNotFoundException e) {
            System.err.println("FATAL: PostgreSQL JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to load PostgreSQL JDBC driver, cannot initialize DBConnectionManager.", e);
        } catch (Exception e) { // Bắt các lỗi khác từ SSH Tunnel hoặc HikariCP
            System.err.println("FATAL: Failed to initialize DBConnectionManager (SSH or HikariCP setup): " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize DB connection resources.", e);
        }
    }

    /**
     * Retrieves a connection from the HikariCP pool.
     * @return A database connection.
     * @throws SQLException If a database access error occurs or the pool is not initialized.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            // Điều này không nên xảy ra nếu khối static chạy thành công
            System.err.println("FATAL: HikariDataSource is not initialized! Check static initializer block.");
            throw new SQLException("Database connection pool (HikariDataSource) is not initialized.");
        }
        // System.out.println("Fetching connection from HikariCP pool..."); // Bỏ comment để debug
        return dataSource.getConnection();
    }

    /**
     * Closes the HikariCP DataSource and disconnects the SSH tunnel if used.
     * Should be called when the application is shutting down.
     */
    public static void shutdown() {
        System.out.println("Shutting down database resources...");
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("Closing HikariCP DataSource (" + dataSource.getPoolName() + ")...");
            dataSource.close();
            System.out.println("HikariCP DataSource closed.");
        }
        if (USE_SSH_TUNNEL) {
            SshTunnelManager.disconnect(); // SshTunnelManager tự kiểm tra isConnected
        }
        System.out.println("Database resources shut down complete.");
    }

    // Các phương thức closeStatement, closePreparedStatement, closeResultSet giữ nguyên
    // vì chúng vẫn hữu ích cho việc quản lý tài nguyên JDBC trong các lớp DAO.
    // Phương thức closeConnection(Connection) không còn quá cần thiết khi dùng pool,
    // vì connection.close() sẽ trả connection về pool, không phải đóng vật lý.
    // Tuy nhiên, giữ lại cũng không sao, nó sẽ hoạt động đúng với connection từ pool.

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close(); // Với pool, thao tác này trả connection về pool
                }
            } catch (SQLException e) {
                System.err.println("Error returning connection to pool: " + e.getMessage());
            }
        }
    }

    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing statement: " + e.getMessage());
            }
        }
    }

    public static void closePreparedStatement(PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error closing prepared statement: " + e.getMessage());
            }
        }
    }

    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                System.err.println("Error closing result set: " + e.getMessage());
            }
        }
    }
}
