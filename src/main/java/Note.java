import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Note {
    private String title; // Tiêu đề của ghi chú
    private String content; // Nội dung của ghi chú
    private boolean isFavorite; // Có phải ghi chú yêu thích hay không
    private LocalDate creationDate; // Ngày tạo ghi chú

    // Constructor
    public Note(String title, String content, boolean isFavorite) {
        this.title = title;
        this.content = content;
        this.isFavorite = isFavorite;
        this.creationDate = LocalDate.now(); // Ghi nhận ngày ghi chú được tạo
    }

    // Getter và Setter cho tiêu đề
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // Getter và Setter cho nội dung
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // Getter và Setter cho trạng thái yêu thích
    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    // Getter cho ngày tạo
    public LocalDate getCreationDate() {
        return creationDate;
    }

    // Phương thức trả về ngày tạo được định dạng (chỉ ngày/tháng/năm)
    public String getFormattedCreationDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        return creationDate.format(formatter);
    }
}