import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class TessDataUtil {

    public static File extractTessDataFolder() throws IOException {
        // Thư mục này sẽ là giá trị cho TESSDATA_PREFIX (đường dẫn cha của thư mục "tessdata")
        File tessDataParentDir = Files.createTempDirectory("app_tessdata_prefix_").toFile();
        // Không cần deleteOnExit cho thư mục cha ở đây, sẽ xóa thủ công sau khi ứng dụng đóng

        // Tạo thư mục con "tessdata" bên trong thư mục cha ở trên
        File actualTessdataSubDir = new File(tessDataParentDir, "tessdata");
        if (!actualTessdataSubDir.mkdir()) {
            // Nếu không tạo được, dọn dẹp thư mục cha đã tạo và báo lỗi
            deleteDirectory(tessDataParentDir);
            throw new IOException("Không thể tạo thư mục con tessdata: " + actualTessdataSubDir.getAbsolutePath());
        }
        // Không cần deleteOnExit cho thư mục con ở đây, sẽ xóa thủ công sau

        // "tessdata" là tên thư mục trong resources (src/main/resources/tessdata)
        // actualTessdataSubDir là thư mục tạm thời (TEMP_DIR_PARENT/tessdata/) mà file sẽ được copy vào
        copyTrainedDataFromResources("tessdata", actualTessdataSubDir);

        return tessDataParentDir; // Trả về thư mục cha để làm TESSDATA_PREFIX
    }

    private static void copyTrainedDataFromResources(String resourceTessdataFolder, File targetTempTessdataFolder) throws IOException {
        // Các file ngôn ngữ mà ScanWindow cần (và có trong src/main/resources/tessdata/)
        // Đã bỏ "jpn.traineddata" theo yêu cầu
        String[] trainedDataFiles = {
                "eng.traineddata",
                "vie.traineddata"
        };

        for (String fileName : trainedDataFiles) {
            String resourcePath = resourceTessdataFolder + "/" + fileName;
            try (InputStream is = TessDataUtil.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new FileNotFoundException("Resource không tìm thấy: " + resourcePath +
                            ". Đảm bảo file '" + fileName + "' có trong 'src/main/resources/" + resourceTessdataFolder + "/'");
                }

                File outFile = new File(targetTempTessdataFolder, fileName); // Copy vào thư mục đích (TEMP_DIR_PARENT/tessdata/fileName)
                try (OutputStream os = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
                // Không cần outFile.deleteOnExit(); sẽ xóa cả thư mục sau
            }
        }
    }

    /**
     * Xóa thư mục và tất cả nội dung bên trong nó.
     * @param directoryToBeDeleted Thư mục cần xóa.
     * @return true nếu xóa thành công, false nếu không.
     */
    public static boolean deleteDirectory(File directoryToBeDeleted) {
        if (!directoryToBeDeleted.exists()) {
            return true; // Thư mục không tồn tại, coi như đã xóa
        }
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                // Đệ quy xóa từng file và thư mục con
                deleteDirectory(file);
            }
        }
        // Cuối cùng, xóa thư mục rỗng
        return directoryToBeDeleted.delete();
    }
}