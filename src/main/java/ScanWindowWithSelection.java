import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp; // Thêm import cho ConvolveOp
import java.awt.image.Kernel;   // Thêm import cho Kernel
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScanWindowWithSelection extends JFrame {
    private BufferedImage loadedImage;
    private ImageSelectionPanel imagePanel;

    // Tesseract instance và đường dẫn tessdata được khai báo ở cấp độ class
    // Để khởi tạo một lần và tái sử dụng
    private Tesseract tesseract;
    private File tessDataParentDirForOCR; // Thư mục cha của "tessdata"

    public ScanWindowWithSelection() {
        super("Scan Selected Screen Area");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Toàn màn hình
        setUndecorated(true); // Bỏ khung cửa sổ
        setAlwaysOnTop(true); // Luôn ở trên cùng

        // --- Khởi tạo Tesseract và tải Tessdata (chỉ 1 lần) ---
        try {
            // Không còn System.loadLibrary(opencv_core.NATIVE_LIBRARY_NAME);

            tessDataParentDirForOCR = extractTessDataParentFolder();
            System.out.println("Tesseract Data Path (TESSDATA_PREFIX): " + tessDataParentDirForOCR.getAbsolutePath());

            tesseract = new Tesseract();
            tesseract.setDatapath(tessDataParentDirForOCR.getAbsolutePath());
            tesseract.setLanguage("eng+vie");
            tesseract.setTessVariable("load_system_dawg", "false");
            tesseract.setTessVariable("load_freq_dawg", "false");
            System.out.println("Tesseract initialized successfully.");

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi sao chép dữ liệu ngôn ngữ (tessdata): " + e.getMessage() +
                            "\nKiểm tra console để biết chi tiết. Ứng dụng sẽ thoát.",
                    "Lỗi Khởi Tạo OCR", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }


        // --- Chụp màn hình và hiển thị lựa chọn ---
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            loadedImage = robot.createScreenCapture(screenRect);

            imagePanel = new ImageSelectionPanel(loadedImage);
            add(imagePanel, BorderLayout.CENTER);
        } catch (AWTException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể chụp màn hình: " + ex.getMessage(), "Lỗi Chụp Màn Hình", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        // --- Xử lý đóng cửa sổ để dọn dẹp tessdata ---
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cleanupTessData();
            }
        });
    }

    private class ImageSelectionPanel extends JPanel {
        private final BufferedImage displayImage;
        private Rectangle selection = new Rectangle();
        private Point startPoint;

        public ImageSelectionPanel(BufferedImage img) {
            this.displayImage = img;
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startPoint = e.getPoint();
                    selection.setBounds(startPoint.x, startPoint.y, 0, 0);
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    int x = Math.min(startPoint.x, e.getX());
                    int y = Math.min(startPoint.y, e.getY());
                    int w = Math.abs(startPoint.x - e.getX());
                    int h = Math.abs(startPoint.y - e.getY());
                    selection.setBounds(x, y, w, h);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (selection.width > 5 && selection.height > 5) {
                        Rectangle actual = selection.intersection(new Rectangle(displayImage.getWidth(), displayImage.getHeight()));
                        if (actual.width > 5 && actual.height > 5) {
                            try {
                                BufferedImage cropped = displayImage.getSubimage(actual.x, actual.y, actual.width, actual.height);
                                // --- THAY THẾ OpenCV bằng xử lý Java thuần túy ---
                                BufferedImage enhanced = enhanceImageManually(cropped);
                                // --------------------------------------------------
                                performOCRForSelectionAsync(enhanced, ScanWindowWithSelection.this);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(ScanWindowWithSelection.this, "Lỗi xử lý ảnh hoặc OCR: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                                dispose();
                            }
                        }
                    }
                    if (SwingUtilities.isLeftMouseButton(e) && selection.width <= 5 && selection.height <= 5) {
                        dispose();
                    }
                }
            };

            addMouseListener(adapter);
            addMouseMotionListener(adapter);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        dispose();
                    }
                }
            });
            setFocusable(true);
            requestFocusInWindow();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (displayImage != null) {
                g.drawImage(displayImage, 0, 0, this);
                if (selection.width > 0 && selection.height > 0) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setColor(new Color(0, 0, 255, 50));
                    g2d.fillRect(selection.x, selection.y, selection.width, selection.height);
                    g2d.setColor(Color.BLUE);
                    g2d.drawRect(selection.x, selection.y, selection.width, selection.height);
                    g2d.dispose();
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return displayImage == null ? new Dimension(200, 200) : new Dimension(displayImage.getWidth(), displayImage.getHeight());
        }
    }

    // --- CÁC PHƯƠNG THỨC XỬ LÝ ẢNH BẰNG JAVA THUẦN TÚY ---
    private BufferedImage enhanceImageManually(BufferedImage input) {
        if (input == null) {
            return null;
        }

        // Bước 1: Chuyển sang ảnh xám
        BufferedImage grayImage = toGrayscale(input);

        // Bước 2: Tăng độ phân giải (ví dụ: gấp đôi)
        // OCR thường hoạt động tốt hơn trên ảnh có DPI cao (ví dụ 300 DPI)
        // Ảnh chụp màn hình thường có DPI thấp (72-96 DPI), nên tăng gấp đôi/ba là hợp lý
        BufferedImage upscaledImage = upscaleImage(grayImage, 2.0); // Tăng gấp đôi

        // Bước 3: Làm nét
        BufferedImage sharpenedImage = sharpenImage(upscaledImage);

        // Bước 4: Nhị phân hóa (chuyển sang đen trắng hoàn toàn)
        // Ngưỡng 128 là một điểm khởi đầu tốt, bạn có thể thử nghiệm để tìm ngưỡng tối ưu
        BufferedImage finalBinaryImage = binarizeImage(sharpenedImage, 128);

        return finalBinaryImage;
    }

    // Phương thức chuyển ảnh sang Grayscale
    private BufferedImage toGrayscale(BufferedImage originalImage) {
        // Đảm bảo ảnh là loại phù hợp để chuyển sang Grayscale nếu không phải
        BufferedImage convertedImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB); // Chuyển đổi về ARGB để vẽ, sau đó chuyển về GRAY
        Graphics2D g = convertedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        BufferedImage grayImage = new BufferedImage(
                convertedImage.getWidth(), convertedImage.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics gGray = grayImage.getGraphics();
        gGray.drawImage(convertedImage, 0, 0, null);
        gGray.dispose();
        return grayImage;
    }


    // Phương thức tăng độ phân giải ảnh
    private BufferedImage upscaleImage(BufferedImage originalImage, double scaleFactor) {
        int newWidth = (int) (originalImage.getWidth() * scaleFactor);
        int newHeight = (int) (originalImage.getHeight() * scaleFactor);

        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g = scaledImage.createGraphics();

        // Sử dụng RenderingHints để có chất lượng tốt nhất khi upscale
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return scaledImage;
    }

    // Phương thức làm nét ảnh bằng Convolution
    private BufferedImage sharpenImage(BufferedImage originalImage) {
        if (originalImage == null) {
            return null;
        }

        // Kernel làm nét nhẹ nhàng hơn (Laplacian)
        float[] sharpenMatrix = {
                0.0f, -1.0f, 0.0f,
                -1.0f, 5.0f, -1.0f,
                0.0f, -1.0f, 0.0f
        };

        Kernel kernel = new Kernel(3, 3, sharpenMatrix);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        // Đảm bảo loại ảnh đầu ra phù hợp với loại ảnh đầu vào
        BufferedImage sharpenedImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
        op.filter(originalImage, sharpenedImage);
        return sharpenedImage;
    }

    // Phương thức nhị phân hóa ảnh (chuyển sang đen trắng)
    private BufferedImage binarizeImage(BufferedImage grayImage, int threshold) {
        // Đảm bảo grayImage là TYPE_BYTE_GRAY
        if (grayImage.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            grayImage = toGrayscale(grayImage); // Chuyển đổi nếu cần
        }

        BufferedImage binaryImage = new BufferedImage(
                grayImage.getWidth(), grayImage.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY); // Kiểu ảnh nhị phân

        for (int y = 0; y < grayImage.getHeight(); y++) {
            for (int x = 0; x < grayImage.getWidth(); x++) {
                int pixel = grayImage.getRGB(x, y);
                // Với TYPE_BYTE_GRAY, tất cả các kênh RGB đều có cùng giá trị,
                // chỉ cần lấy một kênh (ví dụ: RED) để có giá trị xám.
                int grayValue = (pixel >> 16) & 0xFF; // Lấy giá trị kênh đỏ (hoặc xanh lá/xanh dương)

                if (grayValue > threshold) {
                    binaryImage.setRGB(x, y, 0xFFFFFF); // Trắng
                } else {
                    binaryImage.setRGB(x, y, 0x000000); // Đen
                }
            }
        }
        return binaryImage;
    }
    // ----------------------------------------------------------


    private void performOCRForSelectionAsync(BufferedImage imageToScan, Window owner) {
        JDialog progressDialog = new JDialog((Frame) owner, "Đang quét...", true);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressDialog.add(BorderLayout.CENTER, progressBar);
        progressDialog.add(BorderLayout.NORTH, new JLabel("Đang xử lý OCR, vui lòng đợi...", SwingConstants.CENTER));
        progressDialog.setSize(300, 75);
        progressDialog.setLocationRelativeTo(owner);
        progressDialog.setUndecorated(true);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    return tesseract.doOCR(imageToScan);
                } catch (TesseractException e) {
                    throw new RuntimeException("Lỗi trong quá trình OCR: " + e.getMessage(), e);
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    String result = get().trim();
                    if (result.isEmpty()) {
                        JOptionPane.showMessageDialog(owner, "Không nhận được kết quả từ ảnh đã chọn.", "OCR Trống", JOptionPane.WARNING_MESSAGE);
                    } else {
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);
                        JOptionPane.showMessageDialog(owner, "Kết quả đã được copy:\n" + result, "Kết quả OCR", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(owner, "Lỗi OCR: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), "Lỗi OCR", JOptionPane.ERROR_MESSAGE);
                } finally {
                    owner.dispose();
                }
            }
        };
        worker.execute();
        progressDialog.setVisible(true);
    }

    private File extractTessDataParentFolder() throws IOException {
        File tempParentDir = Files.createTempDirectory("sel_tess_parent_").toFile();
        File tessdataSubDir = new File(tempParentDir, "tessdata");

        if (!tessdataSubDir.mkdir()) {
            deleteDirectoryTree(tempParentDir.toPath());
            throw new IOException("Không thể tạo thư mục con tessdata: " + tessdataSubDir.getAbsolutePath());
        }

        String[] trainedDataFiles = {"eng.traineddata", "vie.traineddata"};
        for (String fileName : trainedDataFiles) {
            String resourcePath = "tessdata/" + fileName;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    deleteDirectoryTree(tempParentDir.toPath());
                    throw new FileNotFoundException("Không tìm thấy resource: " + resourcePath +
                            "\nĐảm bảo file '" + fileName + "' có trong 'src/main/resources/tessdata/'");
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
        return tempParentDir;
    }

    private void cleanupTessData() {
        if (tessDataParentDirForOCR != null && tessDataParentDirForOCR.exists()) {
            try {
                deleteDirectoryTree(tessDataParentDirForOCR.toPath());
                System.out.println("Đã dọn dẹp thư mục tessdata tạm thời: " + tessDataParentDirForOCR.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Lỗi khi xóa thư mục tessdata tạm: " + tessDataParentDirForOCR.getAbsolutePath() + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                tessDataParentDirForOCR = null;
            }
        }
    }

    private static void deleteDirectoryTree(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            File[] entries = path.toFile().listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectoryTree(entry.toPath());
                }
            }
        }
        Files.deleteIfExists(path);
    }
}