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

public class ScreenCaptureOCR extends JWindow {
    private Point startPoint;
    private Point endPoint;
    private Rectangle captureRect = new Rectangle();

    public ScreenCaptureOCR() {
        setBackground(new Color(0, 0, 0, 30)); // semi-transparent
        setAlwaysOnTop(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(0, 0, screenSize.width, screenSize.height);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                captureRect.setBounds(startPoint.x, startPoint.y, 0, 0);
                repaint();
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
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setVisible(false);
                if (captureRect.width > 5 && captureRect.height > 5) {
                    try {
                        Robot robot = new Robot();
                        BufferedImage capture = robot.createScreenCapture(captureRect);
                        String text = doOCR(capture);

                        // Copy vào clipboard
                        StringSelection selection = new StringSelection(text);
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, null);

                        // Hiện hộp thoại
                        JOptionPane.showMessageDialog(null, "Text đã được copy vào Clipboard:\n\n" + text, "OCR Result", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                dispose();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (captureRect != null && captureRect.width > 0 && captureRect.height > 0) {
            g.setColor(new Color(0, 0, 255, 50));
            g.fillRect(captureRect.x, captureRect.y, captureRect.width, captureRect.height);
            g.setColor(Color.BLUE);
            g.drawRect(captureRect.x, captureRect.y, captureRect.width, captureRect.height);
        }
    }

    private String doOCR(BufferedImage img) throws TesseractException, IOException {
        Tesseract tesseract = new Tesseract();

        File tessDataFolder = extractTessDataFolder();
        tesseract.setDatapath(tessDataFolder.getAbsolutePath());

        tesseract.setLanguage("vie");
        return tesseract.doOCR(img);
    }

    private File extractTessDataFolder() throws IOException {
        File tempDir = Files.createTempDirectory("tessdata").toFile();
        tempDir.deleteOnExit();

        String[] trainedDataFiles = { "vie.traineddata"}; // Thêm nếu cần

        for (String fileName : trainedDataFiles) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("tessdata/" + fileName)) {
                if (is == null) {
                    throw new FileNotFoundException("Resource not found: tessdata/" + fileName);
                }
                File outFile = new File(tempDir, fileName);
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

        return tempDir;
    }
}
