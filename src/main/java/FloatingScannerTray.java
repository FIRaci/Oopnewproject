import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class FloatingScannerTray extends JWindow {

    private static FloatingScannerTray instance;
    private Point initialClick;
    private JLabel statusLabel;

    private FloatingScannerTray(Frame owner) {
        super(owner); // JWindow can take an owner Frame
        initComponents();
    }

    public static FloatingScannerTray getInstance() {
        // Assuming MainFrame is the main application window and can be accessed
        // Statically or passed around. For simplicity, let's assume it can be null
        // or we find the active frame. This might need adjustment based on your app structure.
        Frame mainAppFrame = null;
        for (Frame frame : Frame.getFrames()) {
            if (frame.isActive() && frame.isVisible() && "XiNoClo - Note App".equals(frame.getTitle())) { // Check title to identify MainFrame
                mainAppFrame = frame;
                break;
            }
        }

        if (instance == null) {
            instance = new FloatingScannerTray(mainAppFrame);
        } else if (instance.getOwner() == null && mainAppFrame != null) {
            // If instance was created with null owner but now we have a frame
            // This part is tricky as JWindow owner cannot be easily changed after creation.
            // For simplicity, we might recreate or just update position.
            // For now, let's assume the first owner (or null) is sufficient.
        }
        return instance;
    }


    private void initComponents() {
        // --- Window Properties ---
        setSize(300, 150); // Default size, can be adjusted
        setLayout(new BorderLayout());
        // Make it always on top
        setAlwaysOnTop(true);

        // --- Panel for Content ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true), // Rounded border look
                new EmptyBorder(10, 10, 10, 10) // Inner padding
        ));
        mainPanel.setBackground(UIManager.getColor("Panel.background"));


        // --- Title Bar (Custom) ---
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UIManager.getColor("ToolBar.background")); // Or another suitable color
        titleBar.setBorder(new EmptyBorder(5,5,5,5));

        JLabel titleLabel = new JLabel("Scanner Tray");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(UIManager.getColor("Label.foreground"));
        titleBar.add(titleLabel, BorderLayout.CENTER);

        JButton closeButton = new JButton("X");
        closeButton.setMargin(new Insets(1, 4, 1, 4));
        closeButton.setFont(new Font("Arial", Font.BOLD, 12));
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> setVisible(false)); // Hide the window
        titleBar.add(closeButton, BorderLayout.EAST);

        mainPanel.add(titleBar, BorderLayout.NORTH);

        // --- Status/Content Area ---
        statusLabel = new JLabel("Khay Scanner sẵn sàng.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        mainPanel.add(statusLabel, BorderLayout.CENTER);

        // --- Placeholder for Scanner Functionality ---
        JButton scanButton = new JButton("Bắt đầu Scan (Placeholder)");
        scanButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        scanButton.addActionListener(e -> {
            // TODO: Implement actual scanner logic or integration here
            statusLabel.setText("Đang scan... (chức năng chưa có)");
            JOptionPane.showMessageDialog(this, "Chức năng Scan chưa được triển khai.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("Khay Scanner sẵn sàng.");
        });
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        buttonPanel.add(scanButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);


        add(mainPanel, BorderLayout.CENTER);

        // --- Make the window draggable ---
        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                getComponentAt(initialClick); // To handle clicks on components within titleBar
            }
        });

        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (initialClick == null) return;
                // get location of Window
                int thisX = getLocation().x;
                int thisY = getLocation().y;

                // Determine how much the mouse moved since the initial click
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                // Move window to this position
                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                setLocation(X, Y);
            }
        });

        // Center on screen initially, or relative to owner if owner exists
        if (getOwner() != null) {
            setLocationRelativeTo(getOwner());
        } else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(screenSize.width / 2 - getWidth() / 2, screenSize.height / 2 - getHeight() / 2);
        }
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            // Recenter or position appropriately when shown
            if (getOwner() != null && getOwner().isVisible()) {
                Point ownerLoc = getOwner().getLocationOnScreen();
                // Position it near the owner, e.g., top-right or as a small floating panel
                int xPos = ownerLoc.x + getOwner().getWidth() - getWidth() - 20; // Example: top right
                int yPos = ownerLoc.y + 20;
                setLocation(xPos, yPos);
            } else {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                setLocation(screenSize.width - getWidth() - 50, 50); // Default to top-right of screen
            }
        }
        super.setVisible(b);
    }

    public void setStatus(String text) {
        statusLabel.setText(text);
    }
}