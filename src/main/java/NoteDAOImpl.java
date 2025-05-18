import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of NoteDAO for interacting with the Note table in the database.
 */
public class NoteDAOImpl implements NoteDAO {

    @Override
    public long addNote(Note note, FolderDAO folderDAO, TagDAO tagDAO) throws SQLException {
        if (note == null) {
            throw new IllegalArgumentException("Note cannot be null");
        }
        // Folder ID nên được kiểm tra ở tầng Service hoặc Controller trước khi gọi DAO
        // if (note.getFolderId() <= 0) {
        //     throw new IllegalArgumentException("Invalid folder ID for note: " + note.getFolderId());
        // }

        String sql = "INSERT INTO public.Note (title, content, created_at, updated_at, id_folder, " +
                "is_favorite, is_mission, mission_content, is_mission_completed, alarm_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_note";
        long noteId = -1;
        Connection conn = null;

        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, note.getTitle());
                pstmt.setString(2, note.getContent());
                pstmt.setTimestamp(3, Timestamp.valueOf(note.getCreatedAt() != null ? note.getCreatedAt() : LocalDateTime.now()));
                pstmt.setTimestamp(4, Timestamp.valueOf(note.getUpdatedAt() != null ? note.getUpdatedAt() : LocalDateTime.now()));
                pstmt.setLong(5, note.getFolderId());
                pstmt.setBoolean(6, note.isFavorite());
                pstmt.setBoolean(7, note.isMission());
                pstmt.setString(8, note.getMissionContent());
                pstmt.setBoolean(9, note.isMissionCompleted());
                if (note.getAlarmId() != null && note.getAlarmId() > 0) {
                    pstmt.setLong(10, note.getAlarmId());
                } else {
                    pstmt.setNull(10, Types.BIGINT);
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        noteId = rs.getLong("id_note");
                        note.setId(noteId); // Cập nhật ID cho đối tượng note được truyền vào
                    } else {
                        throw new SQLException("Creating note failed, no ID obtained.");
                    }
                }
            }

            // Thêm tags vào bảng Note_Tag sau khi có noteId
            if (noteId > 0 && note.getTags() != null && !note.getTags().isEmpty()) {
                addNoteTags(conn, noteId, note.getTags(), tagDAO);
            }

            conn.commit(); // Hoàn tất transaction
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) { /* log or handle */ }
                // Connection sẽ được đóng bởi try-with-resources của DBConnectionManager nếu get
                // hoặc phải đóng thủ công nếu không.
                // Trong NoteService, connection được quản lý ở đó.
                // Nếu DAO tự quản lý connection thì cần đóng ở đây.
                // Hiện tại, giả sử connection được quản lý từ bên ngoài (ví dụ: NoteService)
                // Hoặc nếu DAO tự lấy connection, nó phải tự đóng.
                // Với cấu trúc hiện tại của NoteService, nó truyền connection,
                // nhưng các phương thức DAO này lại tự gọi DBConnectionManager.getConnection().
                // => Cần thống nhất: DAO tự lấy và đóng connection, hay connection được truyền từ Service.
                // Giả định DAO tự lấy và đóng cho các phương thức non-transactional đơn giản.
                // Transactional methods (như addNote này) thì nên có conn truyền vào hoặc quản lý cẩn thận.
                // Tôi đã sửa lại để addNote quản lý transaction nội bộ.
                DBConnectionManager.closeConnection(conn);
            }
        }
        return noteId;
    }

    @Override
    public Note getNoteById(long noteId, FolderDAO folderDAO, TagDAO tagDAO) throws SQLException {
        if (noteId <= 0) {
            // throw new IllegalArgumentException("Invalid note ID: " + noteId);
            return null; // Hoặc throw exception tùy theo yêu cầu
        }
        String sql = "SELECT id_note, title, content, created_at, updated_at, id_folder, " +
                "is_favorite, is_mission, mission_content, is_mission_completed, alarm_id " +
                "FROM public.Note WHERE id_note = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, noteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    List<Tag> tags = getNoteTags(conn, noteId, tagDAO); // conn được tái sử dụng
                    Long alarmId = rs.getLong("alarm_id");
                    if (rs.wasNull()) {
                        alarmId = null;
                    }
                    // Sử dụng constructor đầy đủ của Note.java
                    return new Note(
                            rs.getLong("id_note"),
                            rs.getString("title"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime(),
                            rs.getLong("id_folder"),
                            rs.getBoolean("is_favorite"),
                            rs.getBoolean("is_mission"),
                            rs.getBoolean("is_mission_completed"),
                            rs.getString("mission_content"),
                            alarmId,
                            tags
                    );
                }
            }
        }
        return null;
    }

    @Override
    public List<Note> getAllNotes(FolderDAO folderDAO, TagDAO tagDAO) throws SQLException {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT id_note, title, content, created_at, updated_at, id_folder, " +
                "is_favorite, is_mission, mission_content, is_mission_completed, alarm_id " +
                "FROM public.Note ORDER BY updated_at DESC"; // Hoặc created_at DESC
        try (Connection conn = DBConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                List<Tag> tags = getNoteTags(conn, rs.getLong("id_note"), tagDAO); // conn được tái sử dụng
                Long alarmId = rs.getLong("alarm_id");
                if (rs.wasNull()) {
                    alarmId = null;
                }
                notes.add(new Note(
                        rs.getLong("id_note"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime(),
                        rs.getLong("id_folder"),
                        rs.getBoolean("is_favorite"),
                        rs.getBoolean("is_mission"),
                        rs.getBoolean("is_mission_completed"),
                        rs.getString("mission_content"),
                        alarmId,
                        tags
                ));
            }
        }
        return notes;
    }

    @Override
    public void updateNote(long noteId, Note note, FolderDAO folderDAO, TagDAO tagDAO) throws SQLException {
        if (noteId <= 0) {
            throw new IllegalArgumentException("Invalid note ID for update: " + noteId);
        }
        if (note == null) {
            throw new IllegalArgumentException("Note for update cannot be null");
        }
        // Folder ID nên được kiểm tra ở tầng Service hoặc Controller
        // if (note.getFolderId() <= 0) {
        //    throw new IllegalArgumentException("Invalid folder ID for note update: " + note.getFolderId());
        // }

        String sql = "UPDATE public.Note SET title = ?, content = ?, updated_at = ?, id_folder = ?, " +
                "is_favorite = ?, is_mission = ?, mission_content = ?, is_mission_completed = ?, alarm_id = ? " +
                "WHERE id_note = ?";
        Connection conn = null;
        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, note.getTitle());
                pstmt.setString(2, note.getContent());
                pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now())); // Luôn cập nhật updated_at
                pstmt.setLong(4, note.getFolderId());
                pstmt.setBoolean(5, note.isFavorite());
                pstmt.setBoolean(6, note.isMission());
                pstmt.setString(7, note.getMissionContent());
                pstmt.setBoolean(8, note.isMissionCompleted());
                if (note.getAlarmId() != null && note.getAlarmId() > 0) {
                    pstmt.setLong(9, note.getAlarmId());
                } else {
                    pstmt.setNull(9, Types.BIGINT);
                }
                pstmt.setLong(10, noteId); // WHERE clause
                pstmt.executeUpdate();
            }

            // Cập nhật tags trong bảng Note_Tag
            if (note.getTags() != null) { // Chỉ cập nhật tags nếu danh sách tags được cung cấp (có thể là rỗng)
                updateNoteTags(conn, noteId, note.getTags(), tagDAO);
            }

            conn.commit(); // Hoàn tất transaction
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) { /* log or handle */ }
                DBConnectionManager.closeConnection(conn);
            }
        }
    }

    @Override
    public void deleteNote(long noteId) throws SQLException {
        if (noteId <= 0) {
            throw new IllegalArgumentException("Invalid note ID for delete: " + noteId);
        }

        String sqlDeleteNoteTags = "DELETE FROM public.Note_Tag WHERE id_note = ?";
        // String sqlDeleteAlarm = "DELETE FROM public.Alarms WHERE id_note = ?"; // Nếu có bảng Alarms liên kết
        String sqlDeleteNote = "DELETE FROM public.Note WHERE id_note = ?";
        Connection conn = null;

        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Xóa các tham chiếu trong bảng Note_Tag
            try (PreparedStatement pstmtNoteTags = conn.prepareStatement(sqlDeleteNoteTags)) {
                pstmtNoteTags.setLong(1, noteId);
                pstmtNoteTags.executeUpdate();
            }

            // 2. (Tùy chọn) Xóa Alarm liên quan nếu có và không được xử lý bằng ON DELETE CASCADE
            // Ví dụ: nếu Alarms.id_note là FK đến Note.id_note
            // try (PreparedStatement pstmtAlarm = conn.prepareStatement(sqlDeleteAlarm)) {
            //     pstmtAlarm.setLong(1, noteId);
            //     pstmtAlarm.executeUpdate();
            // }

            // 3. Xóa Note chính
            try (PreparedStatement pstmtNote = conn.prepareStatement(sqlDeleteNote)) {
                pstmtNote.setLong(1, noteId);
                int affectedRows = pstmtNote.executeUpdate();
                if (affectedRows == 0) {
                    // Có thể note đã bị xóa hoặc không tồn tại
                    // throw new SQLException("Deleting note failed, no rows affected. Note with id " + noteId + " not found.");
                }
            }

            conn.commit(); // Hoàn tất transaction
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) { /* log or handle */ }
                DBConnectionManager.closeConnection(conn);
            }
        }
    }

    // --- Helper methods for Tags ---
    // Những phương thức này nên sử dụng connection được truyền vào từ phương thức gọi chính
    // để đảm bảo chúng chạy trong cùng một transaction.

    private void addNoteTags(Connection conn, long noteId, List<Tag> tags, TagDAO tagDAO) throws SQLException {
        // Giả sử tagDAO.addTag và tagDAO.getTagIdByName cũng có thể cần sử dụng cùng 'conn'
        // nếu chúng không tự quản lý transaction một cách an toàn.
        // Tuy nhiên, các phương thức của TagDAOImpl hiện tại tự lấy connection.
        // Để an toàn nhất trong transaction, TagDAO nên có các phương thức nhận Connection.
        // Hoặc, đảm bảo TagDAOImpl.addTag là idempotent/an toàn khi gọi nhiều lần.
        // Logic hiện tại của TagDAOImpl.addTag (ON CONFLICT) là khá an toàn.

        String sql = "INSERT INTO public.Note_Tag (id_note, id_tag) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Tag tag : tags) {
                long tagId = tag.getId();
                if (tagId <= 0) { // Nếu tag chưa có ID (ví dụ, tag mới nhập từ UI)
                    // Cố gắng lấy ID bằng tên trước (nếu tag đã tồn tại trong DB)
                    tagId = tagDAO.getTagIdByName(tag.getName()); // TagDAO sẽ tự lấy connection của nó
                    if (tagId == -1) { // Nếu vẫn không có, thêm tag mới vào DB
                        tagId = tagDAO.addTag(tag); // TagDAO sẽ tự lấy connection của nó
                    }
                    tag.setId(tagId); // Cập nhật ID cho đối tượng tag
                }
                pstmt.setLong(1, noteId);
                pstmt.setLong(2, tagId);
                pstmt.addBatch(); // Sử dụng batch insert để tối ưu
            }
            pstmt.executeBatch(); // Thực thi batch
        }
    }

    private void updateNoteTags(Connection conn, long noteId, List<Tag> tags, TagDAO tagDAO) throws SQLException {
        // 1. Xóa tất cả các tag hiện có của note này trong bảng Note_Tag
        String deleteSql = "DELETE FROM public.Note_Tag WHERE id_note = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setLong(1, noteId);
            pstmt.executeUpdate();
        }
        // 2. Thêm lại các tag mới (nếu có)
        if (tags != null && !tags.isEmpty()) {
            addNoteTags(conn, noteId, tags, tagDAO);
        }
    }

    private List<Tag> getNoteTags(Connection conn, long noteId, TagDAO tagDAO) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT id_tag FROM public.Note_Tag WHERE id_note = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, noteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long tagId = rs.getLong("id_tag");
                    // TagDAO sẽ tự lấy connection của nó
                    Tag tag = tagDAO.getTagById(tagId);
                    if (tag != null) {
                        tags.add(tag);
                    }
                }
            }
        }
        return tags;
    }
}