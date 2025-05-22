// File: StyledResultDialog.java
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StyledResultDialog extends JDialog {

    public StyledResultDialog(Frame owner, String title, String textContent) {
        super(owner, title, true); // Modal
        initializeUI(textContent);
    }

    private void initializeUI(String textContent) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // Gap between components
        getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding for the dialog window

        // --- Content Area ---
        JTextArea resultArea = new JTextArea(textContent);
        resultArea.setWrapStyleWord(true);
        resultArea.setLineWrap(true);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resultArea.setMargin(new Insets(5, 5, 5, 5)); // Padding inside text area
        // Use UIManager colors for consistency with the current theme
        resultArea.setBackground(UIManager.getColor("TextArea.background"));
        resultArea.setForeground(UIManager.getColor("TextArea.foreground"));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")), // Theme-aware border
                new EmptyBorder(5,5,5,5) // Padding inside scrollpane border
        ));
        // Set preferred size for the scroll pane, which dictates the text area's visible size
        scrollPane.setPreferredSize(new Dimension(550, 350));

        add(scrollPane, BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10)); // Centered button with top margin
        JButton okButton = new JButton("Đóng");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        okButton.setPreferredSize(new Dimension(100, 30)); // Set a preferred size for the button
        okButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Dialog Properties ---
        pack(); // Pack after adding all components to get preferred sizes
        setMinimumSize(new Dimension(400, 300)); // Ensure a minimum reasonable size
        // Set initial size slightly larger than minimum or based on packed size
        setSize(Math.max(getMinimumSize().width, getPreferredSize().width + 20), // Add some padding
                Math.max(getMinimumSize().height, getPreferredSize().height + 20));


        // Set icon if owner has one
        if (getOwner() != null && getOwner().getIconImages() != null && !getOwner().getIconImages().isEmpty()) {
            setIconImages(getOwner().getIconImages());
        }

        setLocationRelativeTo(getOwner()); // Center relative to the owner frame
    }

    /**
     * Utility method to quickly show this custom message dialog.
     * @param owner The parent frame.
     * @param title The title of the dialog.
     * @param textContent The text content to display.
     */
    public static void showDialog(Frame owner, String title, String textContent) {
        StyledResultDialog dialog = new StyledResultDialog(owner, title, textContent);
        dialog.setVisible(true);
    }

    // Main method for testing this dialog independently
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        SwingUtilities.invokeLater(() -> {
            JFrame testFrame = new JFrame("Test Owner");
            testFrame.setSize(800, 600);
            testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            testFrame.setLocationRelativeTo(null);
            testFrame.setVisible(true);

            String sampleContent = "Đây là nội dung kết quả mẫu để kiểm tra hiển thị của dialog.\n" +
                    "Nội dung này có thể rất dài để kiểm tra thanh cuộn.\n\n" +
                    String.join("\n", java.util.Collections.nCopies(20, "Dòng văn bản lặp lại để tạo nội dung dài."));

            StyledResultDialog.showDialog(testFrame, "Kết quả Mẫu", sampleContent);
        });
    }
}
