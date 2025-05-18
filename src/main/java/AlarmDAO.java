import java.sql.SQLException;
import java.util.List; // Có thể cần nếu muốn lấy nhiều alarms, ví dụ: tất cả alarms sắp tới

public interface AlarmDAO {

    /**
     * Thêm một đối tượng Alarm mới vào cơ sở dữ liệu.
     * @param alarm Đối tượng Alarm cần thêm.
     * @return ID được tạo ra cho Alarm mới.
     * @throws SQLException Nếu có lỗi khi thao tác với CSDL.
     */
    long addAlarm(Alarm alarm) throws SQLException;

    /**
     * Lấy một đối tượng Alarm từ CSDL bằng ID của nó.
     * @param alarmId ID của Alarm cần lấy.
     * @return Đối tượng Alarm nếu tìm thấy, ngược lại trả về null.
     * @throws SQLException Nếu có lỗi khi thao tác với CSDL.
     */
    Alarm getAlarmById(long alarmId) throws SQLException;

    /**
     * Cập nhật thông tin một Alarm đã có trong CSDL.
     * @param alarmId ID của Alarm cần cập nhật.
     * @param alarm Đối tượng Alarm chứa thông tin mới.
     * @throws SQLException Nếu có lỗi khi thao tác với CSDL.
     */
    void updateAlarm(long alarmId, Alarm alarm) throws SQLException;

    /**
     * Xóa một Alarm khỏi CSDL bằng ID của nó.
     * Quan trọng: Việc xóa Alarm ở đây chỉ xóa bản ghi Alarm.
     * Nếu Note đang tham chiếu đến Alarm này, Note.alarmId cần được cập nhật thành null
     * ở tầng Service trước hoặc sau khi gọi phương thức này.
     * @param alarmId ID của Alarm cần xóa.
     * @throws SQLException Nếu có lỗi khi thao tác với CSDL.
     */
    void deleteAlarm(long alarmId) throws SQLException;

    // Bạn có thể thêm các phương thức khác nếu cần, ví dụ:
    // List<Alarm> getAllAlarms() throws SQLException;
    // List<Alarm> getUpcomingAlarms(LocalDateTime untilTime) throws SQLException;
    // Alarm getAlarmByNoteId(long noteId) throws SQLException; // Nếu mối quan hệ là 1-1 chặt chẽ từ Note
}