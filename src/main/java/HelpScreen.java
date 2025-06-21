import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelpScreen extends JDialog {

    public HelpScreen(Frame owner) {
        super(owner, "Trợ giúp", true);
        initializeUI();
    }

    private void initializeUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10,10));
        getRootPane().setBorder(new EmptyBorder(10,10,10,10));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JScrollPane shortcutsPanel = createShortcutsPanel();
        tabbedPane.addTab("Phím tắt", shortcutsPanel);

        JScrollPane contributorsPanel = createContributorsPanel();
        tabbedPane.addTab("Người đóng góp", contributorsPanel);

        JScrollPane guidePanel = createGuidePanel();
        tabbedPane.addTab("Hướng dẫn", guidePanel);

        add(tabbedPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Đóng");
        closeButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(500, 400));
        setPreferredSize(new Dimension(Math.min(700, getPreferredSize().width), Math.min(600, getPreferredSize().height)));
        setLocationRelativeTo(getOwner());
        setResizable(true);
    }

    private JScrollPane createShortcutsPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        Map<String, String> shortcuts = new LinkedHashMap<>();
        shortcuts.put("Ctrl + S", "Lưu ghi chú hiện tại");
        shortcuts.put("Ctrl + N", "Thêm ghi chú mới (văn bản)");
        shortcuts.put("Ctrl + Shift + N", "Thêm bản vẽ mới");
        shortcuts.put("Ctrl + T", "Thêm tag vào ghi chú hiện tại");
        shortcuts.put("Ctrl + G", "Đặt/Sửa báo thức cho ghi chú hiện tại");
        shortcuts.put("Ctrl + M", "Đặt/Sửa nhiệm vụ cho ghi chú hiện tại");
        shortcuts.put("Ctrl + D", "Dịch nội dung ghi chú");
        shortcuts.put("Ctrl + U", "Tóm tắt nội dung ghi chú");
        shortcuts.put("Ctrl + I", "AI tự động thêm tag cho ghi chú hiện tại");
        shortcuts.put("Ctrl + F", "Thêm thư mục mới (ở Màn hình chính)");
        shortcuts.put("Ctrl + Z", "Hoàn tác hành động cuối trong trình soạn thảo");
        shortcuts.put("Ctrl + Y", "Làm lại hành động cuối trong trình soạn thảo");
        shortcuts.put("Esc", "Quay lại / Đóng cửa sổ");
        shortcuts.put("---", "---");
        shortcuts.put("Ctrl + 1", "Chuyển sang màn hình Ghi chú");
        shortcuts.put("Ctrl + 2", "Chuyển sang màn hình Nhiệm vụ");
        shortcuts.put("Ctrl + W", "Chuyển đổi giao diện Sáng/Tối");
        shortcuts.put("F1", "Mở cửa sổ Trợ giúp này");
        shortcuts.put("Ctrl + Q", "Thoát ứng dụng");


        Font keyFont = new Font("Segoe UI", Font.BOLD, 13);
        Font descriptionFont = new Font("Segoe UI", Font.PLAIN, 13);
        Color keyColor = UIManager.getColor("Label.foreground");
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

            JPanel shortcutEntryPanel = new JPanel(new BorderLayout(15, 0));
            shortcutEntryPanel.setOpaque(false);
            JLabel keyLabel = new JLabel(entry.getKey());
            keyLabel.setFont(keyFont);
            keyLabel.setForeground(keyColor);

            keyLabel.setPreferredSize(new Dimension(120, keyLabel.getPreferredSize().height));


            JLabel descriptionLabel = new JLabel(entry.getValue());
            descriptionLabel.setFont(descriptionFont);
            descriptionLabel.setForeground(descriptionColor);

            shortcutEntryPanel.add(keyLabel, BorderLayout.WEST);
            shortcutEntryPanel.add(descriptionLabel, BorderLayout.CENTER);
            contentPanel.add(shortcutEntryPanel);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
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
                "Liêm - Leader + Tester",
                "Bách - AI Developer + UI Developer",
                "Thành - Fullstack Developer",
                "Trung - Tester + Bug Fixer",
                "Thanh - Bug Fixer + OCR Developer",
                "Hoang - Report + Slide Maker"
        };

        Font contributorFont = new Font("Segoe UI", Font.PLAIN, 14);
        Color contributorColor = UIManager.getColor("Label.foreground");

        JLabel titleLabel = new JLabel("Những người đóng góp:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setBorder(new EmptyBorder(0,0,10,0));
        contentPanel.add(titleLabel);


        for (String contributor : contributors) {
            JLabel contributorLabel = new JLabel("• " + contributor);
            contributorLabel.setFont(contributorFont);
            contributorLabel.setForeground(contributorColor);
            contentPanel.add(contributorLabel);
            contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        }
        return scrollPane;
    }

    private JScrollPane createGuidePanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextArea guideText = new JTextArea();
        guideText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        guideText.setLineWrap(true);
        guideText.setWrapStyleWord(true);
        guideText.setEditable(false);
        guideText.setOpaque(false);
        guideText.setBackground(UIManager.getColor("TextArea.background"));
        guideText.setForeground(UIManager.getColor("TextArea.foreground"));
        guideText.setMargin(new Insets(5,5,5,5));

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
