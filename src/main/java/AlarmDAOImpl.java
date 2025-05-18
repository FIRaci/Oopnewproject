import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList; // Giữ lại nếu có các phương thức trả về List<Alarm>
import java.util.List;    // Giữ lại nếu có các phương thức trả về List<Alarm>

public class AlarmDAOImpl implements AlarmDAO {

    @Override
    public long addAlarm(Alarm alarm) throws SQLException {
        if (alarm == null) {
            throw new IllegalArgumentException("Alarm object cannot be null");
        }
        if (alarm.getAlarmTime() == null) {
            throw new IllegalArgumentException("Alarm time cannot be null");
        }

        String sql = "INSERT INTO public.Alarms (alarm_time, is_recurring, recurrence_pattern) " +
                "VALUES (?, ?, ?) RETURNING id_alarm";
        long generatedId = -1;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(alarm.getAlarmTime()));
            pstmt.setBoolean(2, alarm.isRecurring());
            if (alarm.isRecurring() && alarm.getRecurrencePattern() != null && !alarm.getRecurrencePattern().isEmpty()) {
                pstmt.setString(3, alarm.getRecurrencePattern());
            } else {
                pstmt.setNull(3, Types.VARCHAR); // Nếu không lặp lại hoặc pattern rỗng, lưu là NULL
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    generatedId = rs.getLong("id_alarm");
                    alarm.setId(generatedId); // Quan trọng: Cập nhật ID cho đối tượng Alarm được truyền vào
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
            // Hoặc throw IllegalArgumentException, hoặc trả về null tùy theo logic chung
            return null;
        }
        String sql = "SELECT id_alarm, alarm_time, is_recurring, recurrence_pattern " +
                "FROM public.Alarms WHERE id_alarm = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, alarmId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime alarmTime = rs.getTimestamp("alarm_time").toLocalDateTime();
                    boolean isRecurring = rs.getBoolean("is_recurring");
                    String recurrencePattern = rs.getString("recurrence_pattern");

                    // Sử dụng constructor Alarm(long id, LocalDateTime alarmTime, boolean recurring, String recurrencePattern)
                    return new Alarm(rs.getLong("id_alarm"), alarmTime, isRecurring, recurrencePattern);
                }
            }
        }
        return null; // Không tìm thấy Alarm
    }

    @Override
    public void updateAlarm(long alarmId, Alarm alarm) throws SQLException {
        if (alarmId <= 0) {
            throw new IllegalArgumentException("Invalid alarm ID for update: " + alarmId);
        }
        if (alarm == null) {
            throw new IllegalArgumentException("Alarm object for update cannot be null");
        }
        if (alarm.getAlarmTime() == null) {
            throw new IllegalArgumentException("Alarm time cannot be null for update");
        }

        String sql = "UPDATE public.Alarms SET alarm_time = ?, is_recurring = ?, recurrence_pattern = ? " +
                "WHERE id_alarm = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(alarm.getAlarmTime()));
            pstmt.setBoolean(2, alarm.isRecurring());
            if (alarm.isRecurring() && alarm.getRecurrencePattern() != null && !alarm.getRecurrencePattern().isEmpty()) {
                pstmt.setString(3, alarm.getRecurrencePattern());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }
            pstmt.setLong(4, alarmId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                // Có thể throw exception nếu không tìm thấy Alarm để update,
                // hoặc coi như "không có gì để làm" nếu không tìm thấy.
                // throw new SQLException("Updating alarm failed, no rows affected. Alarm with id " + alarmId + " not found.");
            }
        }
    }

    @Override
    public void deleteAlarm(long alarmId) throws SQLException {
        if (alarmId <= 0) {
            throw new IllegalArgumentException("Invalid alarm ID for delete: " + alarmId);
        }
        // Giả sử CSDL có Foreign Key Constraint: Notes.alarm_id -> Alarms.id_alarm với ON DELETE SET NULL.
        // Nếu không, tầng Service cần đảm bảo cập nhật Notes.alarm_id = NULL trước khi xóa Alarm.
        String sql = "DELETE FROM public.Alarms WHERE id_alarm = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, alarmId);
            int affectedRows = pstmt.executeUpdate();
            // if (affectedRows == 0) {
            // Có thể throw exception nếu không tìm thấy Alarm để xóa.
            // System.out.println("Warning: No alarm found with ID " + alarmId + " to delete.");
            // }
        }
    }
}