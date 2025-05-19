import java.sql.SQLException;
import java.util.List;
// import java.time.LocalDateTime; // Bỏ đi nếu không dùng đến getUpcomingAlarms

public interface AlarmDAO {

    long addAlarm(Alarm alarm) throws SQLException;

    Alarm getAlarmById(long alarmId) throws SQLException;

    void updateAlarm(long alarmId, Alarm alarm) throws SQLException;

    void deleteAlarm(long alarmId) throws SQLException;

    // List<Alarm> getAllAlarms() throws SQLException;
    // List<Alarm> getUpcomingAlarms(LocalDateTime untilTime) throws SQLException;
}