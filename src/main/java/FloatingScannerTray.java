import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class FloatingScannerTray extends JFrame {
    private static FloatingScannerTray instance;

    public static FloatingScannerTray getInstance() {
        if (instance == null) {
            instance = new FloatingScannerTray();
        }
        return instance;
    }

    private final JPopupMenu popupMenu;
    private final Image iconImage;

    private FloatingScannerTray() {
        setUndecorated(true);
        setAlwaysOnTop(true);
        setSize(50, 50);  // kích thước icon window

        // Load ảnh và resize
        iconImage = loadAndResizeImage("/images/scanner.jpg", 48, 48);

        // Tạo label chứa icon, để click bắt sự kiện
        JLabel iconLabel = new JLabel(new ImageIcon(iconImage));
        iconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(iconLabel);

        // Đặt vị trí cửa sổ: trên cùng bên phải
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - getWidth() - 10;
        int y = 10;
        setLocation(x, y);

        // Tạo popup menu
        popupMenu = new JPopupMenu();

        JMenuItem scanItem = new JMenuItem("Scan");
        scanItem.addActionListener(e -> openScanWindow());

        JMenuItem quitItem = new JMenuItem("Quit");
        quitItem.addActionListener(e -> {
            // Thoát ứng dụng một cách an toàn hơn
            // Có thể cần dispose các cửa sổ khác nếu có
            // System.exit(0); // Cân nhắc nếu đây là cửa sổ chính
            setVisible(false); // Chỉ ẩn cửa sổ này
            dispose(); // Giải phóng tài nguyên của cửa sổ này
        });

        popupMenu.add(scanItem);
        popupMenu.addSeparator();
        popupMenu.add(quitItem);

        // Khi click icon thì hiện popup menu
        iconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isLeftMouseButton(e)) {
                    popupMenu.show(iconLabel, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(iconLabel, e.getX(), e.getY());
                }
            }
        });

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // Hoặc EXIT_ON_CLOSE nếu đây là cửa sổ chính duy nhất
    }

    private Image loadAndResizeImage(String path, int width, int height) {
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) {
                System.err.println("Không tìm thấy resource ảnh: " + path);
                // Có thể trả về một ảnh mặc định hoặc ném lỗi
                return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); // Ảnh trống
            }
            Image img = Toolkit.getDefaultToolkit().getImage(imgUrl);
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            // Đảm bảo ảnh được load hoàn toàn
            ImageIcon tempIcon = new ImageIcon(scaled);
            tempIcon.getImage().flush();
            return tempIcon.getImage();
        } catch (Exception e) {
            e.printStackTrace();
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); // Ảnh trống nếu lỗi
        }
    }

    private void openScanWindow() {
        SwingUtilities.invokeLater(() -> {
            ScreenCaptureOCR captureOCR = new ScreenCaptureOCR();
            captureOCR.setVisible(true);
        });
    }

    // Thêm main method để có thể chạy thử nghiệm độc lập nếu cần
    // public static void main(String[] args) {
    //     SwingUtilities.invokeLater(() -> {
    //         FloatingScannerTray.getInstance().setVisible(true);
    //     });
    // }
}