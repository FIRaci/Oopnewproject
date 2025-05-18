import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of FolderDAO for interacting with the Folder table in the database.
 */
public class FolderDAOImpl implements FolderDAO {

    @Override
    public long addFolder(Folder folder) throws SQLException {
        if (folder == null) {
            throw new IllegalArgumentException("Folder cannot be null");
        }
        // Tương tự TagDAOImpl, SQL này xử lý "upsert" dựa trên tên folder.
        String sql = "INSERT INTO public.Folder (name) VALUES (?) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id_folder";
        long generatedId = -1;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, folder.getName());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    generatedId = rs.getLong("id_folder");
                }
            }
        }
        // Tầng Service sẽ chịu trách nhiệm cập nhật ID cho đối tượng folder nếu cần.
        return generatedId;
    }

    @Override
    public Folder getFolderById(long id_folder) throws SQLException {
        // Bỏ qua kiểm tra id_folder <= 0 ở đây nếu CSDL đảm bảo ID luôn > 0
        // hoặc nếu bạn muốn cho phép các ID đặc biệt.
        // Hiện tại, nếu không tìm thấy sẽ trả về null.
        String sql = "SELECT id_folder, name FROM public.Folder WHERE id_folder = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id_folder);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Sử dụng constructor mới của Folder.java: Folder(long id, String name)
                    return new Folder(rs.getLong("id_folder"), rs.getString("name"));
                }
            }
        }
        return null;
    }

    @Override
    public Folder getFolderByName(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be null or empty");
        }
        String sql = "SELECT id_folder, name FROM public.Folder WHERE name = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Sử dụng constructor mới của Folder.java: Folder(long id, String name)
                    return new Folder(rs.getLong("id_folder"), rs.getString("name"));
                }
            }
        }
        return null;
    }

    @Override
    public long getFolderIdByName(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be null or empty");
        }
        String sql = "SELECT id_folder FROM public.Folder WHERE name = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id_folder");
                }
            }
        }
        return -1; // Trả về -1 nếu không tìm thấy
    }

    @Override
    public List<Folder> getAllFolders() throws SQLException {
        List<Folder> folders = new ArrayList<>();
        String sql = "SELECT id_folder, name FROM public.Folder ORDER BY name"; // Giả sử có cột is_favorite để sort ưu tiên
        // Ví dụ: ORDER BY is_favorite DESC, name ASC
        try (Connection conn = DBConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Sử dụng constructor mới của Folder.java: Folder(long id, String name)
                // Nếu Folder có thêm trường is_favorite từ CSDL, cần đọc ở đây
                // Folder folder = new Folder(rs.getLong("id_folder"), rs.getString("name"));
                // folder.setFavorite(rs.getBoolean("is_favorite")); // Ví dụ
                // folders.add(folder);
                folders.add(new Folder(rs.getLong("id_folder"), rs.getString("name")));
            }
        }
        return folders;
    }

    @Override
    public void updateFolder(long id_folder, Folder folder) throws SQLException {
        if (id_folder <= 0) {
            throw new IllegalArgumentException("Invalid folder ID for update: " + id_folder);
        }
        if (folder == null || folder.getName() == null || folder.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Folder or folder name for update cannot be null or empty");
        }
        // Cần xem xét việc cập nhật các trường khác như 'is_favorite' nếu có
        String sql = "UPDATE public.Folder SET name = ? WHERE id_folder = ?";
        // Nếu có is_favorite: "UPDATE public.Folder SET name = ?, is_favorite = ? WHERE id_folder = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, folder.getName());
            // pstmt.setBoolean(2, folder.isFavorite()); // Ví dụ nếu có is_favorite
            // pstmt.setLong(X, id_folder); // X là vị trí tham số của id_folder
            pstmt.setLong(2, id_folder);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteFolder(long id_folder) throws SQLException {
        if (id_folder <= 0) {
            throw new IllegalArgumentException("Invalid folder ID for delete: " + id_folder);
        }
        // Việc xóa folder ở DAO chỉ nên xóa bản ghi trong bảng Folder.
        // Logic xử lý các Notes thuộc folder này (xóa theo, chuyển sang folder khác,...)
        // nên được thực hiện ở tầng Service (NoteService) để đảm bảo tính toàn vẹn nghiệp vụ.
        // Tầng service sẽ gọi NoteDAO để cập nhật folderId của các notes liên quan
        // trước khi gọi FolderDAO.deleteFolder(id_folder).
        // Hoặc, CSDL có thể có Foreign Key Constraint với ON DELETE SET NULL hoặc ON DELETE CASCADE
        // cho cột id_folder trong bảng Note.

        String sql = "DELETE FROM public.Folder WHERE id_folder = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id_folder);
            pstmt.executeUpdate();
        }
    }

    @Override
    public boolean folderExists(long id_folder) throws SQLException {
        if (id_folder <= 0) {
            return false; // ID không hợp lệ không thể tồn tại
        }
        String sql = "SELECT 1 FROM public.Folder WHERE id_folder = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id_folder);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // true nếu có bản ghi, false nếu không
            }
        }
    }
}