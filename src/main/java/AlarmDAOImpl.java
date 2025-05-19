import java.sql.*;
import java.time.LocalDateTime;
// import java.util.ArrayList; // Bỏ nếu không dùng đến List<Alarm>
// import java.util.List;    // Bỏ nếu không dùng đến List<Alarm>

public class AlarmDAOImpl implements AlarmDAO {

    @Override
    public long addAlarm(Alarm alarm) throws SQLException {
        if (alarm == null) {
            throw new IllegalArgumentException("Alarm object cannot be null");
        }
        // Kiểm tra này có thể không cần thiết nếu constructor của Alarm đã đảm bảo
        // if (alarm.getAlarmTime() == null) {
        //     throw new IllegalArgumentException("Alarm time cannot be null");
        // }

        // Tên cột id trong DB của bạn là 'id' hay 'id_alarm'?
        // Trong getAlarmById bạn dùng "id_alarm", ở đây "id_alarm" cũng hợp lý.
        String sql = "INSERT INTO public.Alarms (alarm_time, is_recurring, recurrence_pattern) " +
                "VALUES (?, ?, ?) RETURNING id_alarm"; // Giả sử tên cột ID là id_alarm
        long generatedId = 0L; // Khởi tạo là 0L thay vì -1

        try (Connection conn = DBConnectionManager.getConnection(); // Giả sử DBConnectionManager tồn tại
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(alarm.getAlarmTime()));
            pstmt.setBoolean(2, alarm.isRecurring());

            // Lớp Alarm đã được sửa để recurrencePattern sẽ là null nếu không recurring
            if (alarm.getRecurrencePattern() != null) {
                pstmt.setString(3, alarm.getRecurrencePattern());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    generatedId = rs.getLong("id_alarm"); // Lấy ID từ cột bạn đặt tên là "id_alarm"
                    alarm.setId(generatedId); // Quan trọng: Cập nhật ID cho đối tượng Alarm
                } else {
                    throw new SQLException("Creating alarm failed, no ID obtained.");
                }
            }
        }
        return generatedId;
    }

    @Override
    public Alarm getAlarmById(long alarmId) throws SQLException {
        if (alarmId <= 0) {
            return null;
        }
        // Đảm bảo tên cột khớp với CSDL của bạn
        String sql = "SELECT id_alarm, alarm_time, is_recurring, recurrence_pattern " +
                "FROM public.Alarms WHERE id_alarm = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, alarmId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long currentId = rs.getLong("id_alarm");
                    LocalDateTime alarmTime = null;
                    Timestamp timestamp = rs.getTimestamp("alarm_time");
                    if (timestamp != null) {
                        alarmTime = timestamp.toLocalDateTime();
                    }
                    boolean isRecurring = rs.getBoolean("is_recurring");
                    String recurrencePattern = rs.getString("recurrence_pattern"); // Sẽ là null nếu cột DB là NULL

                    // Sử dụng constructor của Alarm đã được cải tiến.
                    // Nó sẽ tự xử lý để recurrencePattern là null nếu isRecurring là false.
                    return new Alarm(currentId, alarmTime, isRecurring, recurrencePattern);
                }
            }
        }
        return null;
    }

    @Override
    public void updateAlarm(long alarmId, Alarm alarm) throws SQLException {
        if (alarmId <= 0) { // Hoặc alarm.getId() nếu bạn muốn cập nhật dựa trên ID của object
            throw new IllegalArgumentException("Invalid alarm ID for update: " + alarmId);
        }
        if (alarm == null) {
            throw new IllegalArgumentException("Alarm object for update cannot be null");
        }
        // if (alarm.getAlarmTime() == null) { // Không cần nếu constructor Alarm đảm bảo
        //     throw new IllegalArgumentException("Alarm time cannot be null for update");
        // }

        String sql = "UPDATE public.Alarms SET alarm_time = ?, is_recurring = ?, recurrence_pattern = ? " +
                "WHERE id_alarm = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(alarm.getAlarmTime()));
            pstmt.setBoolean(2, alarm.isRecurring());

            // Lớp Alarm đã được sửa để recurrencePattern sẽ là null nếu không recurring
            if (alarm.getRecurrencePattern() != null) {
                pstmt.setString(3, alarm.getRecurrencePattern());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            // Đảm bảo bạn đang cập nhật đúng alarmId
            // Nếu alarm object đã có ID đúng, có thể dùng alarm.getId() thay cho alarmId tham số
            pstmt.setLong(4, alarm.getId()); // Sử dụng alarm.getId() nếu nó được đảm bảo là đúng

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Warning: Updating alarm ID " + alarm.getId() + " affected 0 rows. Alarm might not exist.");
                // throw new SQLException("Updating alarm failed, no rows affected. Alarm with id " + alarm.getId() + " not found.");
            }
        }
    }

    @Override
    public void deleteAlarm(long alarmId) throws SQLException {
        if (alarmId <= 0) {
            throw new IllegalArgumentException("Invalid alarm ID for delete: " + alarmId);
        }
        String sql = "DELETE FROM public.Alarms WHERE id_alarm = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, alarmId);
            pstmt.executeUpdate();
            // int affectedRows = pstmt.executeUpdate();
            // if (affectedRows == 0) {
            //     System.out.println("Warning: No alarm found with ID " + alarmId + " to delete.");
            // }
        }
    }
}