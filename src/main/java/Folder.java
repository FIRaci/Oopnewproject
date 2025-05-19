import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Folder {
    private long id; // ID của folder trong cơ sở dữ liệu
    private String name;
    private transient List<Note> notes; // 'transient' vì sẽ không lưu trực tiếp vào JSON theo cách này nếu dùng GSON default
    private transient List<Folder> subFolders; // Tương tự, 'transient'
    List<String> subFolderNames; // Dùng để lưu/tải tên subfolder (phục vụ DataStorage hiện tại)
    private boolean favorite;

    /**
     * Constructor để tạo folder mới (chưa có ID từ CSDL).
     * @param name Tên của folder.
     * @throws IllegalArgumentException Nếu tên folder là null hoặc rỗng.
     */
    public Folder(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be null or empty");
        }
        this.id = 0; // Giá trị mặc định cho folder chưa được lưu
        this.name = name;
        this.notes = new ArrayList<>();
        this.subFolders = new ArrayList<>();
        this.subFolderNames = new ArrayList<>();
        this.favorite = false;
    }

    /**
     * Constructor để tạo folder từ dữ liệu đã có trong CSDL (có ID).
     * @param id ID của folder.
     * @param name Tên của folder.
     * @throws IllegalArgumentException Nếu tên folder là null hoặc rỗng.
     */
    public Folder(long id, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be null or empty");
        }
        this.id = id;
        this.name = name;
        this.notes = new ArrayList<>(); // Khởi tạo rỗng, sẽ được populate sau nếu cần
        this.subFolders = new ArrayList<>(); // Khởi tạo rỗng
        this.subFolderNames = new ArrayList<>(); // Khởi tạo rỗng
        this.favorite = false; // Có thể cần thêm trường 'favorite' vào CSDL và constructor này
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be null or empty");
        }
        this.name = name;
    }

    public List<Note> getNotes() {
        // Giữ nguyên logic khởi tạo lười nếu notes là null
        return notes != null ? notes : (notes = new ArrayList<>());
    }

    public void addNote(Note note) {
        if (note != null && !getNotes().contains(note)) {
            getNotes().add(note);
            note.setFolder(this); // Giả sử Note có setFolder
        }
    }

    public void removeNote(Note note) {
        if (note != null) {
            getNotes().remove(note);
            if (note.getFolder() == this) {
                note.setFolder(null); // Giả sử Note có setFolder
            }
        }
    }

    public List<Folder> getSubFolders() {
        // Giữ nguyên logic khởi tạo lười
        return subFolders != null ? subFolders : (subFolders = new ArrayList<>());
    }

    public void addSubFolder(Folder subFolder) {
        if (subFolder != null && !getSubFolders().contains(subFolder)) {
            getSubFolders().add(subFolder);
            // Nếu bạn chuyển sang quản lý subFolderNames qua DB, logic ở đây có thể thay đổi
            if (this.subFolderNames == null) {
                this.subFolderNames = new ArrayList<>();
            }
            if (!this.subFolderNames.contains(subFolder.getName())) {
                this.subFolderNames.add(subFolder.getName());
            }
        }
    }

    public void removeSubFolder(Folder subFolder) {
        getSubFolders().remove(subFolder);
        if (this.subFolderNames != null && subFolder != null) {
            this.subFolderNames.remove(subFolder.getName());
        }
    }

    // Giữ lại subFolderNames và các phương thức liên quan nếu DataStorage.java vẫn dùng
    public List<String> getSubFolderNames() {
        if (this.subFolderNames == null) {
            this.subFolderNames = new ArrayList<>();
        }
        return this.subFolderNames;
    }

    public void setSubFolderNames(List<String> subFolderNames) {
        this.subFolderNames = subFolderNames;
    }


    public void deleteFolder(boolean deleteNotes) {
        if (deleteNotes) {
            getNotes().clear();
        } else {
            getNotes().forEach(note -> note.setFolder(null)); // Giả sử Note có setFolder(null)
        }
        getSubFolders().clear();
        if (this.subFolderNames != null) {
            this.subFolderNames.clear();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Folder folder = (Folder) o;
        if (id != 0 && folder.id != 0) { // Nếu cả hai có ID hợp lệ, so sánh bằng ID
            return id == folder.id;
        }
        // Nếu không, so sánh bằng tên (trường hợp folder mới tạo, hoặc một trong hai chưa có ID)
        // Điều này quan trọng cho logic kiểm tra folder đã tồn tại bằng tên trước khi lưu vào DB
        return Objects.equals(name, folder.name);
    }

    @Override
    public int hashCode() {
        if (id != 0) { // Nếu có ID hợp lệ, sử dụng ID để hash
            return Objects.hash(id);
        }
        // Nếu không, sử dụng tên
        return Objects.hash(name);
    }

    public void setMission(boolean isMission) {
        // Logic này áp dụng cho các note bên trong, có thể cần xem xét lại
        // nếu 'mission' là thuộc tính của Folder trong CSDL
        getNotes().forEach(note -> note.setMission(isMission));
        getSubFolders().forEach(folder -> folder.setMission(isMission));
    }

    public boolean isMission() {
        // Logic này dựa trên notes, có thể cần thay đổi
        return getNotes().stream().anyMatch(Note::isMission);
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
        // Nếu 'favorite' được lưu trong CSDL, cần gọi service/DAO để cập nhật
    }
}