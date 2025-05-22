import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ScanWindowWithSelection extends JFrame {
    private BufferedImage image;
    private ImageSelectionPanel imagePanel;
    private JTextArea resultArea;

    public ScanWindowWithSelection() {
        super("Scan Text with Region Selection");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JButton btnOpen = new JButton("Open Image");
        btnOpen.addActionListener(e -> openImage());

        resultArea = new JTextArea(5, 30);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnOpen);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.SOUTH);
    }

    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                image = ImageIO.read(chooser.getSelectedFile());
                if (imagePanel != null) {
                    remove(imagePanel);
                }
                imagePanel = new ImageSelectionPanel(image);
                add(imagePanel, BorderLayout.CENTER);
                revalidate();
                repaint();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to open image: " + ex.getMessage());
            }
        }
    }

    // Panel cho phép kéo chọn vùng
    private class ImageSelectionPanel extends JPanel {
        private BufferedImage image;
        private Rectangle selection = new Rectangle();
        private Point startPoint;

        public ImageSelectionPanel(BufferedImage img) {
            this.image = img;

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
                        try {
                            BufferedImage cropped = image.getSubimage(selection.x, selection.y, selection.width, selection.height);
                            // OCR ảnh crop
                            String ocrText = doOCR(cropped);
                            resultArea.setText(ocrText);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(ScanWindowWithSelection.this, "OCR error: " + ex.getMessage());
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
            g.drawImage(image, 0, 0, this);
            if (selection.width > 0 && selection.height > 0) {
                g.setColor(new Color(0, 0, 255, 50));
                g.fillRect(selection.x, selection.y, selection.width, selection.height);
                g.setColor(Color.BLUE);
                g.drawRect(selection.x, selection.y, selection.width, selection.height);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
    }

    private String doOCR(BufferedImage img) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:/testdata"); // Thay thành thư mục tessdata trên máy bạn
        tesseract.setLanguage("eng"); // Hoặc "eng+vie"
        return tesseract.doOCR(img);
    }
}
