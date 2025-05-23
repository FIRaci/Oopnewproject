import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScreenCaptureOCR extends JWindow {
    private Point startPoint;
    private Point endPoint;
    private Rectangle captureRect = new Rectangle();
    private File tessDataParentDirForOCR; // Để lưu và dọn dẹp

    public ScreenCaptureOCR() {
        super((Frame) null); // JWindow nên có owner, hoặc null nếu không có frame cụ thể

        // DEBUG: Làm cho cửa sổ dễ nhận biết hơn
        // setOpacity(0.3f); // Tạm thời bỏ qua để không làm mờ toàn bộ cửa sổ
        setBackground(new Color(0, 0, 0, 70)); // Lớp phủ hơi tối để biết cửa sổ đã xuất hiện

        setAlwaysOnTop(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screenSize.width, screenSize.height);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        System.out.println("ScreenCaptureOCR window created and configured. Ready to capture mouse events.");

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                captureRect.setBounds(startPoint.x, startPoint.y, 0, 0);
                System.out.println("Mouse pressed at: " + startPoint + ", captureRect initialized: " + captureRect);
                repaint(); // Yêu cầu vẽ lại để hiển thị vùng chọn
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                endPoint = e.getPoint();
                captureRect.setBounds(
                        Math.min(startPoint.x, endPoint.x),
                        Math.min(startPoint.y, endPoint.y),
                        Math.abs(startPoint.x - endPoint.x),
                        Math.abs(startPoint.y - endPoint.y)
                );
                // System.out.println("Mouse dragged to: " + endPoint + ", captureRect updated: " + captureRect); // Có thể gây nhiều output
                repaint(); // Cập nhật vùng chọn khi kéo chuột
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                System.out.println("Mouse released. Final captureRect: " + captureRect);
                setVisible(false); // Ẩn cửa sổ ngay lập tức

                if (captureRect.width > 5 && captureRect.height > 5) {
                    try {
                        System.out.println("Processing capture...");
                        Robot robot = new Robot();
                        // Chụp đúng vùng đã chọn
                        BufferedImage capture = robot.createScreenCapture(captureRect);
                        System.out.println("Screen captured successfully.");

                        // Thực hiện OCR và hiển thị kết quả
                        String text = doOCR(capture);
                        System.out.println("OCR result: " + (text == null ? "null" : "\"" + text.substring(0, Math.min(text.length(), 50)) + "...\""));


                        if (text != null && !text.trim().isEmpty()) {
                            StringSelection selection = new StringSelection(text);
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(selection, null);
                            JOptionPane.showMessageDialog(null, "Văn bản đã được copy vào Clipboard:\n\n" + text, "Kết quả OCR", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null, "Không nhận dạng được văn bản từ vùng đã chọn.", "Kết quả OCR", JOptionPane.WARNING_MESSAGE);
                        }

                    } catch (AWTException awtEx) {
                        System.err.println("AWTException during capture/robot: " + awtEx.getMessage());
                        awtEx.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Lỗi khi chụp màn hình: " + awtEx.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    } catch (TesseractException tessEx) {
                        System.err.println("TesseractException: " + tessEx.getMessage());
                        tessEx.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Lỗi Tesseract OCR: " + tessEx.getMessage(), "Lỗi OCR", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException ioEx) {
                        System.err.println("IOException (likely tessdata): " + ioEx.getMessage());
                        ioEx.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Lỗi IO khi xử lý tessdata: " + ioEx.getMessage(), "Lỗi IO", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) { // Bắt các lỗi không mong muốn khác
                        System.err.println("Generic Exception: " + ex.getMessage());
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Đã xảy ra lỗi không xác định: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    System.out.println("Capture rectangle too small or invalid. Width: " + captureRect.width + ", Height: " + captureRect.height);
                }
                // Dọn dẹp và đóng cửa sổ
                cleanupTessData();
                System.out.println("Disposing ScreenCaptureOCR window.");
                dispose();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g); // Quan trọng để vẽ nền trong suốt (hoặc lớp phủ mờ)
        Graphics2D g2d = (Graphics2D) g.create();

        // Không cần vẽ lại nền nếu super.paint(g) đã xử lý với setBackground ở trên.

        if (captureRect != null && captureRect.width > 0 && captureRect.height > 0) {
            // DEBUG: Làm cho vùng chọn rất rõ ràng
            g2d.setColor(new Color(0, 120, 255, 100)); // Màu xanh lam, độ mờ vừa phải (alpha 100)
            g2d.fillRect(captureRect.x, captureRect.y, captureRect.width, captureRect.height);

            g2d.setColor(Color.YELLOW); // Viền màu vàng
            g2d.setStroke(new BasicStroke(2)); // Viền dày hơn
            g2d.drawRect(captureRect.x, captureRect.y, captureRect.width, captureRect.height);
            // System.out.println("Paint called, drawing rect: " + captureRect); // Có thể gây rất nhiều output
        }
        g2d.dispose();
    }

    private String doOCR(BufferedImage img) throws TesseractException, IOException {
        Tesseract tesseract = new Tesseract();
        // Dọn dẹp thư mục tessdata cũ nếu có
        cleanupTessData();

        System.out.println("Extracting tessdata for OCR...");
        tessDataParentDirForOCR = extractTessDataParentFolder(); // Tạo và lưu lại để dọn dẹp
        tesseract.setDatapath(tessDataParentDirForOCR.getAbsolutePath());
        tesseract.setLanguage("vie"); // Chỉ cần tiếng Việt cho chức năng này
        System.out.println("Tessdata path set to: " + tessDataParentDirForOCR.getAbsolutePath() + ", language: vie");


        String result = tesseract.doOCR(img);
        System.out.println("doOCR call completed.");
        return result;
    }

    private File extractTessDataParentFolder() throws IOException {
        File tempParentDir = Files.createTempDirectory("cap_tess_parent_").toFile();
        // Không dùng deleteOnExit ở đây, sẽ xóa thủ công

        File tessdataSubDir = new File(tempParentDir, "tessdata");
        if (!tessdataSubDir.mkdir()) {
            deleteDirectoryTree(tempParentDir.toPath()); // Dọn dẹp nếu không tạo được subdir
            throw new IOException("Không thể tạo thư mục con tessdata: " + tessdataSubDir.getAbsolutePath());
        }

        // Chỉ cần file vie.traineddata (phải có trong src/main/resources/tessdata/)
        String[] trainedDataFiles = { "vie.traineddata" };
        String resourcePrefix = "tessdata/";

        for (String fileName : trainedDataFiles) {
            String resourcePath = resourcePrefix + fileName;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    deleteDirectoryTree(tempParentDir.toPath()); // Dọn dẹp
                    throw new FileNotFoundException("Resource không tìm thấy: " + resourcePath +
                            ". Đảm bảo file có trong 'src/main/resources/" + resourcePrefix + "'");
                }
                File outFile = new File(tessdataSubDir, fileName);
                try (OutputStream os = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
            }
        }
        System.out.println("Tessdata files extracted to: " + tessdataSubDir.getAbsolutePath());
        return tempParentDir;
    }

    private void cleanupTessData() {
        if (tessDataParentDirForOCR != null && tessDataParentDirForOCR.exists()) {
            System.out.println("Cleaning up temporary tessdata directory: " + tessDataParentDirForOCR.getAbsolutePath());
            try {
                deleteDirectoryTree(tessDataParentDirForOCR.toPath());
                System.out.println("Temporary tessdata directory cleaned up successfully.");
            } catch (IOException e) {
                System.err.println("Lỗi khi xóa thư mục tessdata tạm: " + e.getMessage());
            }
            tessDataParentDirForOCR = null;
        } else {
            System.out.println("No temporary tessdata directory to clean up or directory does not exist.");
        }
    }

    // Phương thức tiện ích để xóa cây thư mục (cần thiết vì thư mục phải rỗng trước khi xóa)
    private static void deleteDirectoryTree(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            // Sử dụng try-with-resources cho Files.list
            try (var entries = Files.list(path)) {
                // Phải chuyển Stream thành List hoặc xử lý cẩn thận để tránh ConcurrentModificationException
                // khi xóa đệ quy trong một stream đang duyệt. entries.toList() (Java 16+) là một cách.
                // Hoặc dùng forEach với UncheckedIOException như trước, nhưng cần cẩn thận.
                // Cách an toàn hơn cho các phiên bản Java cũ hơn là dùng File#listFiles()
                File[] filesInDir = path.toFile().listFiles();
                if (filesInDir != null) {
                    for (File entryFile : filesInDir) {
                        deleteDirectoryTree(entryFile.toPath());
                    }
                }
            }
        }
        Files.deleteIfExists(path); // Xóa file hoặc thư mục rỗng
    }
}