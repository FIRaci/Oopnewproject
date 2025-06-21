import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScanWindowWithSelection extends JFrame {
    private BufferedImage loadedImage;
    private ImageSelectionPanel imagePanel;
    private Tesseract tesseract;
    private File tessDataParentDirForOCR;

    public ScanWindowWithSelection() {
        super("Scan Selected Screen Area");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setAlwaysOnTop(true);

        try {
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
                    "Lỗi khi sao chép dữ liệu ngôn ngữ (tessdata): " + e.getMessage(),
                    "Lỗi Khởi Tạo OCR", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

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
                                BufferedImage enhanced = enhanceImageSimple(cropped);
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

    private BufferedImage enhanceImageSimple(BufferedImage input) {
        BufferedImage gray = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        int scale = 2;
        int newWidth = gray.getWidth() * scale;
        int newHeight = gray.getHeight() * scale;
        BufferedImage resized = new BufferedImage(newWidth, newHeight, gray.getType());
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(gray, 0, 0, newWidth, newHeight, null);
        g2.dispose();

        return resized;
    }

    private void performOCRForSelectionAsync(BufferedImage imageToScan, Window owner) {
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
    public static void scanBufferedImage(BufferedImage image) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("tessdata");
        tesseract.setLanguage("eng+vie");

        try {
            String result = tesseract.doOCR(image);
            JOptionPane.showMessageDialog(null, result, "Kết quả OCR", JOptionPane.INFORMATION_MESSAGE);
        } catch (TesseractException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Lỗi OCR: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
    }
}
