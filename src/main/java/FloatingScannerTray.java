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
        setSize(50, 50);

        // Load ảnh và resize
        iconImage = loadAndResizeImage("/images/scanner.jpg", 48, 48);

        // Tạo label chứa icon
        JLabel iconLabel = new JLabel(new ImageIcon(iconImage));
        iconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        add(iconLabel);

        // Đặt vị trí cửa sổ
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
            setVisible(false);
            dispose();
        });

        popupMenu.add(scanItem);
        popupMenu.addSeparator();
        popupMenu.add(quitItem);

        // Hiện popup menu
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

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    private Image loadAndResizeImage(String path, int width, int height) {
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) {
                System.err.println("Không tìm thấy resource ảnh: " + path);
                return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }
            Image img = Toolkit.getDefaultToolkit().getImage(imgUrl);
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            ImageIcon tempIcon = new ImageIcon(scaled);
            tempIcon.getImage().flush();
            return tempIcon.getImage();
        } catch (Exception e) {
            e.printStackTrace();
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private void openScanWindow() {
        SwingUtilities.invokeLater(() -> {
            new ScanWindowWithSelection().setVisible(true);
        });
    }
}