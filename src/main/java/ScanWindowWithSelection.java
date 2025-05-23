import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScanWindowWithSelection extends JFrame {
    private BufferedImage loadedImage; // Đổi tên để rõ ràng hơn
    private ImageSelectionPanel imagePanel;
    private JTextArea resultArea;
    private File tessDataParentDirForOCR; // Lưu trữ thư mục tessdata để dọn dẹp

    public ScanWindowWithSelection() {
        super("Scan Text with Region Selection");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Thay vì EXIT_ON_CLOSE nếu đây không phải cửa sổ chính
        setSize(800, 600);
        setLocationRelativeTo(null);

        JButton btnOpen = new JButton("Open Image");
        btnOpen.addActionListener(e -> openImage());

        resultArea = new JTextArea(5, 30);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnOpen);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.SOUTH);

        // Đảm bảo dọn dẹp thư mục tessdata khi cửa sổ đóng
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cleanupTessData();
            }
        });
    }

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                loadedImage = ImageIO.read(chooser.getSelectedFile());
                if (loadedImage == null) {
                    JOptionPane.showMessageDialog(this, "Không thể đọc file ảnh đã chọn.", "Lỗi mở ảnh", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (imagePanel != null) {
                    remove(imagePanel);
                }
                imagePanel = new ImageSelectionPanel(loadedImage);
                add(imagePanel, BorderLayout.CENTER);
                revalidate();
                repaint();
                resultArea.setText(""); // Xóa kết quả cũ
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi khi mở ảnh: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class ImageSelectionPanel extends JPanel {
        private BufferedImage displayImage; // Ảnh để hiển thị và cắt
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
                        // Đảm bảo vùng chọn nằm trong giới hạn ảnh
                        Rectangle imageBounds = new Rectangle(0,0, displayImage.getWidth(), displayImage.getHeight());
                        Rectangle actualSelection = selection.intersection(imageBounds);

                        if (actualSelection.width > 5 && actualSelection.height > 5) {
                            try {
                                BufferedImage cropped = displayImage.getSubimage(actualSelection.x, actualSelection.y, actualSelection.width, actualSelection.height);
                                resultArea.setText("Scanning selected region...");
                                // Thực hiện OCR trên một luồng khác để không block UI
                                performOCRForSelectionAsync(cropped);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                resultArea.setText("OCR error: " + ex.getMessage());
                                JOptionPane.showMessageDialog(ScanWindowWithSelection.this, "Lỗi OCR: " + ex.getMessage(), "Lỗi OCR", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            resultArea.setText("Vùng chọn quá nhỏ hoặc nằm ngoài ảnh.");
                        }
                    }
                }
            };

            addMouseListener(adapter);
            addMouseMotionListener(adapter);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (displayImage != null) {
                // Căn chỉnh kích thước panel cho vừa ảnh, hoặc scale ảnh cho vừa panel
                // Ở đây, panel sẽ tự điều chỉnh theo kích thước ảnh thông qua getPreferredSize
                g.drawImage(displayImage, 0, 0, this);
                if (selection.width > 0 && selection.height > 0) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setColor(new Color(0, 0, 255, 50)); // Màu xanh lam trong suốt
                    g2d.fillRect(selection.x, selection.y, selection.width, selection.height);
                    g2d.setColor(Color.BLUE);
                    g2d.drawRect(selection.x, selection.y, selection.width, selection.height);
                    g2d.dispose();
                }
            }
        }

        @Override
        public Dimension getPreferredSize() {
            // Panel có kích thước bằng kích thước ảnh
            return displayImage == null ? new Dimension(200, 200) : new Dimension(displayImage.getWidth(), displayImage.getHeight());
        }
    }

    private void performOCRForSelectionAsync(BufferedImage imageToScan) {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return doOCR(imageToScan);
            }

            @Override
            protected void done() {
                try {
                    resultArea.setText(get());
                } catch (Exception e) {
                    e.printStackTrace();
                    resultArea.setText("OCR Error: " + e.getCause().getMessage());
                    JOptionPane.showMessageDialog(ScanWindowWithSelection.this,
                            "Lỗi khi thực hiện OCR: " + e.getCause().getMessage(),
                            "Lỗi OCR", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }


    private String doOCR(BufferedImage img) throws TesseractException, IOException {
        Tesseract tesseract = new Tesseract();
        // Dọn dẹp thư mục tessdata cũ nếu có trước khi tạo mới
        cleanupTessData();

        tessDataParentDirForOCR = extractTessDataParentFolder(); // Tạo và lưu lại để dọn dẹp
        tesseract.setDatapath(tessDataParentDirForOCR.getAbsolutePath());
        tesseract.setLanguage("eng+vie+jpn"); // Ngôn ngữ cần thiết

        return tesseract.doOCR(img);
    }

    private File extractTessDataParentFolder() throws IOException {
        File tempParentDir = Files.createTempDirectory("sel_tess_parent_").toFile();
        // Không dùng deleteOnExit ở đây, sẽ xóa thủ công

        File tessdataSubDir = new File(tempParentDir, "tessdata");
        if (!tessdataSubDir.mkdir()) {
            deleteDirectoryTree(tempParentDir.toPath()); // Dọn dẹp nếu không tạo được subdir
            throw new IOException("Không thể tạo thư mục con tessdata: " + tessdataSubDir.getAbsolutePath());
        }

        // Các file ngôn ngữ cần thiết (phải có trong src/main/resources/tessdata/)
        String[] trainedDataFiles = {"eng.traineddata", "vie.traineddata", "jpn.traineddata"};
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
        return tempParentDir;
    }

    private void cleanupTessData() {
        if (tessDataParentDirForOCR != null && tessDataParentDirForOCR.exists()) {
            try {
                deleteDirectoryTree(tessDataParentDirForOCR.toPath());
            } catch (IOException e) {
                System.err.println("Lỗi khi xóa thư mục tessdata tạm: " + e.getMessage());
            }
            tessDataParentDirForOCR = null;
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