import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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

            // Xóa file tạm sau khi OCR xong trong done() của SwingWorker
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
                        imageFile.delete();
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
        tesseract.setDatapath("C:/testdata");
        tesseract.setLanguage("eng");

        try {
            return tesseract.doOCR(imageFile);
        } catch (TesseractException e) {
            e.printStackTrace();
            return "OCR failed: " + e.getMessage();
        }
    }
}
