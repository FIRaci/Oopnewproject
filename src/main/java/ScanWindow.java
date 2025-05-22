import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;

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
            performOCRAsync(file);
        }
    }

    private void captureScreenAndScan() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenImage = robot.createScreenCapture(screenRect);

            // Lưu tạm file ảnh
            File tempFile = File.createTempFile("screenshot", ".png");
            ImageIO.write(screenImage, "png", tempFile);

            resultArea.setText("Scanning captured screen, please wait...");
            performOCRAsync(tempFile);

        } catch (AWTException | IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error capturing screen: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performOCRAsync(File imageFile) {
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
                    // Nếu là file tạm, xóa ở đây
                    if (imageFile.getName().startsWith("screenshot")) {
                        if (!imageFile.delete()) {
                            System.err.println("Failed to delete temp screenshot file: " + imageFile.getAbsolutePath());
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ScanWindow.this,
                            "Error during OCR: " + ex.getMessage(),
                            "OCR Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private String doOCR(File imageFile) {
        Tesseract tesseract = new Tesseract();

        try {
            File tessDataFolder = TessDataUtil.extractTessDataFolder();
            tesseract.setDatapath(tessDataFolder.getAbsolutePath());
            tesseract.setLanguage("eng+vie+jpn");

            return tesseract.doOCR(imageFile);
        } catch (IOException | TesseractException e) {
            e.printStackTrace();
            return "OCR failed: " + e.getMessage();
        }
    }

    // Tiện thể thêm main để chạy thử
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ScanWindow window = new ScanWindow();
            window.setVisible(true);
        });
    }
}


class TessDataUtil {

    /**
     * Copy thư mục tessdata từ resource ra thư mục temp ngoài file system.
     * @return File thư mục tessdata temp
     * @throws IOException nếu lỗi IO
     */
    public static File extractTessDataFolder() throws IOException {
        File tempDir = Files.createTempDirectory("tessdata").toFile();
        tempDir.deleteOnExit();

        copyFolderFromResources("tessdata", tempDir);

        return tempDir;
    }

    /**
     * Copy các file trong resource folder tessdata ra thư mục targetFolder
     */
    private static void copyFolderFromResources(String resourceFolder, File targetFolder) throws IOException {
        // Nếu bạn có nhiều file .traineddata thì thêm vào đây
        String[] trainedDataFiles = {
                "eng.traineddata"
                //, "vie.traineddata" nếu bạn có thêm ngôn ngữ
        };

        for (String fileName : trainedDataFiles) {
            try (InputStream is = TessDataUtil.class.getClassLoader().getResourceAsStream(resourceFolder + "/" + fileName)) {
                if (is == null) {
                    throw new FileNotFoundException("Resource not found: " + resourceFolder + "/" + fileName);
                }

                File outFile = new File(targetFolder, fileName);
                try (OutputStream os = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
                outFile.deleteOnExit();
            }
        }
    }
}
