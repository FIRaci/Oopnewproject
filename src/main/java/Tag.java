import java.util.Objects;

public class Tag {
    private long id; // ID của tag trong cơ sở dữ liệu
    private String name;

    /**
     * Constructor để tạo tag mới (chưa có ID từ CSDL).
     * @param name Tên của tag.
     * @throws IllegalArgumentException Nếu tên tag là null hoặc rỗng.
     */
    public Tag(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        this.id = 0; // Giá trị mặc định cho tag chưa được lưu vào CSDL
        this.name = name.trim();
    }

    /**
     * Constructor để tạo tag từ dữ liệu đã có trong CSDL (có ID).
     * @param id ID của tag.
     * @param name Tên của tag.
     * @throws IllegalArgumentException Nếu tên tag là null hoặc rỗng.
     */
    public Tag(long id, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        if (id <= 0 && !name.equals("TempDefault")) { // Cho phép ID <=0 nếu là TempDefault, hoặc cần logic khác
            // Xem xét lại logic kiểm tra ID này. Thông thường ID từ DB sẽ > 0.
            // Có thể bỏ qua kiểm tra id <= 0 ở đây nếu việc gán id luôn đúng.
        }
        this.id = id;
        this.name = name.trim();
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
            throw new IllegalArgumentException("Tag name cannot be null or empty");
        }
        this.name = name.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        // Nếu cả hai đối tượng đều có ID hợp lệ (từ CSDL), so sánh bằng ID
        if (id != 0 && tag.id != 0) {
            return id == tag.id;
        }
        // Nếu không, so sánh bằng tên (trường hợp tag mới tạo, chưa có ID)
        return Objects.equals(name, tag.name);
    }

    @Override
    public int hashCode() {
        // Nếu có ID hợp lệ, sử dụng ID để hash
        if (id != 0) {
            return Objects.hash(id);
        }
        // Nếu không, sử dụng tên
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    // Phương thức getTags() đã được loại bỏ vì nó trùng với getName() và gây nhầm lẫn.
    // public String getTags() {
    //     return name;
    // }
}