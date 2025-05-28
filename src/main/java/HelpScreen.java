// File: HelpScreen.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelpScreen extends JDialog {

    public HelpScreen(Frame owner) {
        super(owner, "Trợ giúp", true); // Modal dialog
        initializeUI();
    }

    private void initializeUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10,10)); // Gaps for main layout
        getRootPane().setBorder(new EmptyBorder(10,10,10,10)); // Padding for the dialog window

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        // Add padding to the content area of each tab
        // UIManager.put("TabbedPane.contentBorderInsets", new Insets(10, 10, 10, 10)); // May not work with all L&Fs
        // Alternatively, add padding to each panel added to tabs.

        JScrollPane shortcutsPanel = createShortcutsPanel();
        tabbedPane.addTab("Phím tắt", shortcutsPanel);

        JScrollPane contributorsPanel = createContributorsPanel();
        tabbedPane.addTab("Người đóng góp", contributorsPanel);

        JScrollPane guidePanel = createGuidePanel(); // Changed to JScrollPane
        tabbedPane.addTab("Hướng dẫn", guidePanel);

        add(tabbedPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Đóng");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // Removed border from buttonPanel as dialog root pane has padding
        // buttonPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack(); // Pack first
        setMinimumSize(new Dimension(500, 400)); // Then set minimum
        // Ensure preferred size is reasonable if content is large
        setPreferredSize(new Dimension(Math.min(700, getPreferredSize().width), Math.min(600, getPreferredSize().height)));
        setLocationRelativeTo(getOwner());
        setResizable(true);
    }

    private JScrollPane createShortcutsPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        // Padding applied to JScrollPane's viewport for better control with various L&Fs
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove scrollpane border
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding for the content itself

        Map<String, String> shortcuts = new LinkedHashMap<>();
        shortcuts.put("Ctrl + S", "Lưu ghi chú hiện tại");
        shortcuts.put("Ctrl + N", "Thêm ghi chú mới (văn bản)");
        shortcuts.put("Ctrl + Shift + N", "Thêm bản vẽ mới");
        shortcuts.put("Ctrl + T", "Thêm tag vào ghi chú hiện tại");
        shortcuts.put("Ctrl + G", "Đặt/Sửa báo thức cho ghi chú hiện tại"); // Note: Ctrl+G was Summary before, now Alarm
        shortcuts.put("Ctrl + M", "Đặt/Sửa nhiệm vụ cho ghi chú hiện tại");
        shortcuts.put("Ctrl + D", "Dịch nội dung ghi chú");
        shortcuts.put("Ctrl + U", "Tóm tắt nội dung ghi chú");
        shortcuts.put("Ctrl + I", "AI tự động thêm tag cho ghi chú hiện tại");
        shortcuts.put("Ctrl + F", "Thêm thư mục mới (ở Màn hình chính)");
        shortcuts.put("Ctrl + Z", "Hoàn tác hành động cuối trong trình soạn thảo");
        shortcuts.put("Ctrl + Y", "Làm lại hành động cuối trong trình soạn thảo");
        shortcuts.put("Esc", "Quay lại / Đóng cửa sổ");
        shortcuts.put("---", "---"); // Separator
        shortcuts.put("Ctrl + 1", "Chuyển sang màn hình Ghi chú");
        shortcuts.put("Ctrl + 2", "Chuyển sang màn hình Nhiệm vụ");
        shortcuts.put("Ctrl + W", "Chuyển đổi giao diện Sáng/Tối");
        shortcuts.put("F1", "Mở cửa sổ Trợ giúp này");
        shortcuts.put("Ctrl + Q", "Thoát ứng dụng");


        Font keyFont = new Font("Segoe UI", Font.BOLD, 13);
        Font descriptionFont = new Font("Segoe UI", Font.PLAIN, 13);
        Color keyColor = UIManager.getColor("Label.foreground"); // Use theme colors
        Color descriptionColor = UIManager.getColor("Label.foreground");

        for (Map.Entry<String, String> entry : shortcuts.entrySet()) {
            if (entry.getKey().equals("---")) {
                contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                JSeparator separator = new JSeparator();
                separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, separator.getPreferredSize().height));
                contentPanel.add(separator);
                contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                continue;
            }

            JPanel shortcutEntryPanel = new JPanel(new BorderLayout(15, 0)); // Increased gap
            shortcutEntryPanel.setOpaque(false); // Make transparent if contentPanel has bg
            JLabel keyLabel = new JLabel(entry.getKey());
            keyLabel.setFont(keyFont);
            keyLabel.setForeground(keyColor);
            // Set a preferred width for keyLabel to align descriptions
            keyLabel.setPreferredSize(new Dimension(120, keyLabel.getPreferredSize().height));


            JLabel descriptionLabel = new JLabel(entry.getValue());
            descriptionLabel.setFont(descriptionFont);
            descriptionLabel.setForeground(descriptionColor);

            shortcutEntryPanel.add(keyLabel, BorderLayout.WEST);
            shortcutEntryPanel.add(descriptionLabel, BorderLayout.CENTER);
            contentPanel.add(shortcutEntryPanel);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Increased spacing
        }
        return scrollPane;
    }

    private JScrollPane createContributorsPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));


        String[] contributors = {
                "Liem-san - Leader + Primary Tester",
                "Shirin - AI Developer + UI Developer + Principal Funder",
                "FIRaci - Frontend Developer + Backend Developer",
                "Yui - Secondary Tester + Secondary Bug Fixer",
                "Drako - Main Bug Fixer + OCR Developer",
                "Missing someone here bruh! Do you know where they are?" // Corrected typo
        };

        Font contributorFont = new Font("Segoe UI", Font.PLAIN, 14); // Slightly larger
        Color contributorColor = UIManager.getColor("Label.foreground");

        JLabel titleLabel = new JLabel("Những người đóng góp:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setBorder(new EmptyBorder(0,0,10,0)); // Margin below title
        contentPanel.add(titleLabel);


        for (String contributor : contributors) {
            JLabel contributorLabel = new JLabel("• " + contributor); // Added bullet point
            contributorLabel.setFont(contributorFont);
            contributorLabel.setForeground(contributorColor);
            contentPanel.add(contributorLabel);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        return scrollPane;
    }

    private JScrollPane createGuidePanel() { // Changed to JScrollPane
        JPanel contentPanel = new JPanel(new BorderLayout());
        // Padding applied to JScrollPane's viewport
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextArea guideText = new JTextArea();
        guideText.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Consistent font size
        guideText.setLineWrap(true);
        guideText.setWrapStyleWord(true);
        guideText.setEditable(false);
        guideText.setOpaque(false); // Make transparent to use panel's background
        guideText.setBackground(UIManager.getColor("TextArea.background")); // Use theme color
        guideText.setForeground(UIManager.getColor("TextArea.foreground"));
        guideText.setMargin(new Insets(5,5,5,5)); // Internal padding for text area

        guideText.setText(
                "Chào mừng đến với XiNoClo!\n\n" +
                        "Bắt đầu:\n" +
                        "- Tạo ghi chú mới: Sử dụng Ctrl+N (văn bản) hoặc Ctrl+Shift+N (bản vẽ).\n" +
                        "- Sắp xếp ghi chú: Thêm ghi chú vào thư mục. Sử dụng Ctrl+F để tạo thư mục mới.\n" +
                        "- Thêm tag: Sử dụng Ctrl+T để thêm tag, giúp bạn tổ chức và tìm kiếm tốt hơn.\n" +
                        "- Đặt báo thức: Sử dụng Ctrl+G để đặt hoặc chỉnh sửa báo thức cho ghi chú.\n" +
                        "- Đặt nhiệm vụ: Sử dụng Ctrl+M để tạo hoặc chỉnh sửa nhiệm vụ liên quan đến ghi chú.\n" +
                        "- Tính năng AI: Dịch (Ctrl+D), Tóm tắt (Ctrl+U), Tự động thêm Tag (Ctrl+I).\n" +
                        "- Lưu công việc: Luôn nhớ lưu ghi chú của bạn bằng Ctrl+S.\n\n" +
                        "Mẹo hữu ích:\n" +
                        "- Hoàn tác/Làm lại: Ctrl+Z để hoàn tác, Ctrl+Y để làm lại trong trình soạn thảo.\n" +
                        "- Chuyển màn hình: Ctrl+1 cho Ghi chú, Ctrl+2 cho Nhiệm vụ.\n" +
                        "- Đổi giao diện: Ctrl+W để chuyển đổi giữa giao diện Sáng và Tối.\n" +
                        "- Trợ giúp: Nhấn F1 để mở lại hướng dẫn này.\n" +
                        "- Thoát ứng dụng: Ctrl+Q để đóng ứng dụng."
        );

        contentPanel.add(guideText, BorderLayout.CENTER);
        return scrollPane;
    }

    public static void showDialog(Frame owner) {
        HelpScreen dialog = new HelpScreen(owner);
        dialog.setVisible(true);
    }
}
