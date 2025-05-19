import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of TagDAO for interacting with the Tag table in the database.
 */
public class TagDAOImpl implements TagDAO {

    @Override
    public long addTag(Tag tag) throws SQLException {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        // SQL này xử lý việc "upsert" dựa trên tên tag.
        // Nếu tag.name đã tồn tại, nó sẽ không tạo mới mà trả về id hiện có (sau khi đảm bảo tên được cập nhật nếu khác).
        String sql = "INSERT INTO public.Tag (name) VALUES (?) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id_tag";
        long generatedId = -1;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tag.getName());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    generatedId = rs.getLong("id_tag");
                }
            }
        }
        // Đối tượng 'tag' được truyền vào không được cập nhật ID ở đây.
        // Tầng Service (NoteService) sẽ chịu trách nhiệm cập nhật ID cho đối tượng tag nếu cần.
        return generatedId;
    }

    @Override
    public Tag getTagById(long id_tag) throws SQLException {
        if (id_tag <= 0) {
            // Cân nhắc việc có nên throw IllegalArgumentException ở đây hay trả về null,
            // tùy theo logic của ứng dụng. Hiện tại trả về null nếu không tìm thấy.
            // throw new IllegalArgumentException("Invalid tag ID: " + id_tag);
        }
        String sql = "SELECT id_tag, name FROM public.Tag WHERE id_tag = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id_tag);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Sử dụng constructor mới của Tag.java
                    return new Tag(rs.getLong("id_tag"), rs.getString("name"));
                }
            }
        }
        return null;
    }

    @Override
    public Tag getTagByName(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        String sql = "SELECT id_tag, name FROM public.Tag WHERE name = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Sử dụng constructor mới của Tag.java
                    return new Tag(rs.getLong("id_tag"), rs.getString("name"));
                }
            }
        }
        return null;
    }

    @Override
    public long getTagIdByName(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        String sql = "SELECT id_tag FROM public.Tag WHERE name = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id_tag");
                }
            }
        }
        return -1; // Trả về -1 nếu không tìm thấy
    }

    @Override
    public Tag findByName(String trim) {
        return null;
    }

    @Override
    public List<Tag> getAllTags() throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT id_tag, name FROM public.Tag ORDER BY name";
        try (Connection conn = DBConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Sử dụng constructor mới của Tag.java
                tags.add(new Tag(rs.getLong("id_tag"), rs.getString("name")));
            }
        }
        return tags;
    }

    @Override
    public void updateTag(long id_tag, Tag tag) throws SQLException {
        if (id_tag <= 0) {
            throw new IllegalArgumentException("Invalid tag ID for update: " + id_tag);
        }
        if (tag == null || tag.getName() == null || tag.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tag or tag name for update cannot be null or empty");
        }
        // Cập nhật tên của tag dựa trên id_tag.
        // Cần đảm bảo rằng tên mới không trùng với một tag khác (trừ khi CSDL có constraint unique cho name).
        // Câu lệnh INSERT ... ON CONFLICT ... DO UPDATE ở addTag đã xử lý việc này cho tên.
        // Khi update, nếu muốn đổi tên, cần kiểm tra xem tên mới có bị trùng không.
        // Tuy nhiên, logic hiện tại của bạn chỉ đơn giản là update.
        String sql = "UPDATE public.Tag SET name = ? WHERE id_tag = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tag.getName());
            pstmt.setLong(2, id_tag);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                // Có thể throw exception nếu không tìm thấy tag để update
                // throw new SQLException("Updating tag failed, no rows affected. Tag with id " + id_tag + " not found.");
            }
        }
    }

    @Override
    public void deleteTag(long id_tag) throws SQLException {
        if (id_tag <= 0) {
            throw new IllegalArgumentException("Invalid tag ID for delete: " + id_tag);
        }
        // Trước khi xóa tag, cần đảm bảo không có note nào đang tham chiếu đến tag này,
        // hoặc CSDL có cơ chế ON DELETE CASCADE/SET NULL cho bảng Note_Tag.
        // Logic hiện tại chỉ xóa tag.
        String sqlDeleteNoteTags = "DELETE FROM public.Note_Tag WHERE id_tag = ?";
        String sqlDeleteTag = "DELETE FROM public.Tag WHERE id_tag = ?";

        Connection conn = null;
        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // Xóa các tham chiếu trong bảng Note_Tag trước
            try (PreparedStatement pstmtNoteTags = conn.prepareStatement(sqlDeleteNoteTags)) {
                pstmtNoteTags.setLong(1, id_tag);
                pstmtNoteTags.executeUpdate();
            }

            // Sau đó xóa tag trong bảng Tag
            try (PreparedStatement pstmtTag = conn.prepareStatement(sqlDeleteTag)) {
                pstmtTag.setLong(1, id_tag);
                int affectedRows = pstmtTag.executeUpdate();
                if (affectedRows == 0) {
                    // Không tìm thấy tag để xóa, có thể là lỗi hoặc tag đã được xóa
                    // throw new SQLException("Deleting tag failed, no rows affected. Tag with id " + id_tag + " not found.");
                }
            }
            conn.commit(); // Hoàn tất transaction
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback nếu có lỗi
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
            }
            throw e; // Ném lại exception gốc
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Khôi phục auto-commit
                } catch (SQLException ex) {
                    // Log or handle
                }
                // DBConnectionManager.closeConnection(conn); // Try-with-resources đã đóng rồi
            }
        }
    }
}