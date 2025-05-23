import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScanWindow extends JFrame {
    private JTextArea resultArea;

    public ScanWindow() {
        super("Scan Text from Image");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JButton selectImageButton = new JButton("Select Image to Scan");
        selectImageButton.addActionListener(e -> selectAndScanImage());

        JButton captureScreenButton = new JButton("Capture Screen and Scan");
        captureScreenButton.addActionListener(e -> captureScreenAndScan());

        JPanel topPanel = new JPanel();
        topPanel.add(selectImageButton);
        topPanel.add(captureScreenButton);

        resultArea = new JTextArea();
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(resultArea);

        setLayout(new BorderLayout(10, 10));
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void selectAndScanImage() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            resultArea.setText("Scanning image, please wait...");
            performOCRAsync(file, false); // false vì đây không phải file tạm
        }
    }

    private void captureScreenAndScan() {
        try {
            // Ẩn cửa sổ ScanWindow trước khi chụp màn hình
            this.setState(Frame.ICONIFIED); // Thu nhỏ cửa sổ
            // Đợi một chút để việc thu nhỏ hoàn tất
            Thread.sleep(250);


            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenImage = robot.createScreenCapture(screenRect);

            // Hiện lại cửa sổ
            this.setState(Frame.NORMAL);


            File tempFile = File.createTempFile("screenshot_", ".png");
            ImageIO.write(screenImage, "png", tempFile);

            resultArea.setText("Scanning captured screen, please wait...");
            performOCRAsync(tempFile, true); // true vì đây là file tạm cần xóa

        } catch (AWTException | IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error capturing screen: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            this.setState(Frame.NORMAL); // Đảm bảo cửa sổ hiện lại nếu có lỗi
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Screen capture interrupted: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            this.setState(Frame.NORMAL);
        }
    }

    private void performOCRAsync(File imageFile, boolean deleteAfter) {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return doOCR(imageFile);
            }

            @Override
            protected void done() {
                try {
                    String text = get();
                    resultArea.setText(text);
                } catch (Exception ex) {
                    resultArea.setText("OCR Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ScanWindow.this,
                            "Error during OCR: " + ex.getCause().getMessage(), // Hiển thị lỗi gốc từ Tesseract nếu có
                            "OCR Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                } finally {
                    if (deleteAfter && imageFile != null && imageFile.getName().startsWith("screenshot_")) {
                        if (!imageFile.delete()) {
                            System.err.println("Failed to delete temp screenshot file: " + imageFile.getAbsolutePath());
                        }
                    }
                }
            }
        };
        worker.execute();
    }

    private String doOCR(File imageFile) {
        Tesseract tesseract = new Tesseract();
        File tessDataParentDir = null;
        try {
            tessDataParentDir = TessDataUtil.extractTessDataFolder();
            tesseract.setDatapath(tessDataParentDir.getAbsolutePath());
            // Ngôn ngữ cần cho ScanWindow
            tesseract.setLanguage("eng+vie+jpn");

            return tesseract.doOCR(imageFile);
        } catch (IOException | TesseractException e) {
            e.printStackTrace();
            return "OCR failed: " + e.getMessage();
        } finally {
            // Dọn dẹp thư mục tạm chứa tessdata
            if (tessDataParentDir != null) {
                TessDataUtil.deleteDirectory(new File(tessDataParentDir, "tessdata"));
                TessDataUtil.deleteDirectory(tessDataParentDir);
            }
        }
    }
}


class TessDataUtil {

    public static File extractTessDataFolder() throws IOException {
        // Thư mục này sẽ là giá trị cho TESSDATA_PREFIX (đường dẫn cha của thư mục "tessdata")
        File tessDataParentDir = Files.createTempDirectory("app_tessdata_prefix_").toFile();
        // Không cần deleteOnExit cho thư mục cha ở đây, sẽ xóa thủ công sau

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
        String[] trainedDataFiles = {
                "eng.traineddata",
                "vie.traineddata", // Đảm bảo file này có trong resources/tessdata
                "jpn.traineddata"  // Đảm bảo file này có trong resources/tessdata
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
            return true;
        }
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}