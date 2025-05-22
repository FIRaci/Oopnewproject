import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
            setVisible(false);
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

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    private Image loadAndResizeImage(String path, int width, int height) {
        Image img = Toolkit.getDefaultToolkit().getImage(getClass().getResource(path));
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return scaled;
    }

    private void openScanWindow() {
        SwingUtilities.invokeLater(() -> {
            ScreenCaptureOCR captureOCR = new ScreenCaptureOCR();
            captureOCR.setVisible(true);
        });
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FloatingScannerTray.getInstance().setVisible(true);
        });
    }

}