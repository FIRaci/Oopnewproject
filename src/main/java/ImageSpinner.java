import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

public class ImageSpinner extends JPanel {
    private BufferedImage image;
    private Point currentMousePosition;
    private int imageSize;

    public ImageSpinner(int size, String imagePath) {
        this.imageSize = size;
        this.currentMousePosition = new Point(size / 2, 0);

        try {
            image = ImageIO.read(getClass().getResource(imagePath));
            if (image.getWidth() != size || image.getHeight() != size) {
                image = resizeImage(image, size, size);
            }
        } catch (IOException e) {
            e.printStackTrace();
            image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.RED);
            g2d.fillOval(0, 0, size, size);
            g2d.dispose();
        }

        setPreferredSize(new Dimension(size, size));
        setOpaque(false);
    }

    // Phương thức resize hình ảnh
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    public void updateMousePosition(Point mousePosRelativeToThisPanel) {
        this.currentMousePosition = mousePosRelativeToThisPanel;
        if (this.currentMousePosition == null || getWidth() <= 0 || getHeight() <= 0) {
            this.currentMousePosition = new Point(getWidth() / 2, 0);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        if (panelWidth <= 0 || panelHeight <= 0) {
            g2d.dispose();
            return;
        }

        int centerX = panelWidth / 2;
        int centerY = panelHeight / 2;

        // Tính góc xoay dựa trên vị trí chuột
        double angle = 0;
        if (currentMousePosition != null) {
            angle = Math.atan2(currentMousePosition.y - centerY, currentMousePosition.x - centerX);
        }

        // Tạo transform để xoay hình ảnh
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX - imageSize / 2.0, centerY - imageSize / 2.0);
        transform.rotate(angle, imageSize / 2.0, imageSize / 2.0); // Xoay quanh tâm hình ảnh

        // Vẽ hình ảnh đã xoay
        g2d.drawImage(image, transform, null);

        g2d.dispose();
    }

    // Phương thức để cập nhật kích thước nếu cần
    public void setImageSize(int newSize) {
        this.imageSize = newSize;
        setPreferredSize(new Dimension(newSize, newSize));
        if (image != null) {
            image = resizeImage(image, newSize, newSize);
        }
        revalidate();
        repaint();
    }
}