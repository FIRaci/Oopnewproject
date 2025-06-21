import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StyledResultDialog extends JDialog {

    public StyledResultDialog(Frame owner, String title, String textContent) {
        super(owner, title, true);
        initializeUI(textContent);
    }

    private void initializeUI(String textContent) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- Content Area ---
        JTextArea resultArea = new JTextArea(textContent);
        resultArea.setWrapStyleWord(true);
        resultArea.setLineWrap(true);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resultArea.setMargin(new Insets(5, 5, 5, 5));
        // Use UIManager colors for consistency with the current theme
        resultArea.setBackground(UIManager.getColor("TextArea.background"));
        resultArea.setForeground(UIManager.getColor("TextArea.foreground"));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
                new EmptyBorder(5,5,5,5)
        ));
        scrollPane.setPreferredSize(new Dimension(550, 350));

        add(scrollPane, BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        JButton okButton = new JButton("Đóng");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        okButton.setPreferredSize(new Dimension(100, 30));
        okButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Dialog Properties ---
        pack();
        setMinimumSize(new Dimension(400, 300));
        setSize(Math.max(getMinimumSize().width, getPreferredSize().width + 20),
                Math.max(getMinimumSize().height, getPreferredSize().height + 20));

        if (getOwner() != null && getOwner().getIconImages() != null && !getOwner().getIconImages().isEmpty()) {
            setIconImages(getOwner().getIconImages());
        }

        setLocationRelativeTo(getOwner());
    }

    public static void showDialog(Frame owner, String title, String textContent) {
        StyledResultDialog dialog = new StyledResultDialog(owner, title, textContent);
        dialog.setVisible(true);
    }
}
