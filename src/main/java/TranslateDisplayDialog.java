// File: TranslateDisplayDialog.java
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TranslateDisplayDialog extends JDialog {

    public TranslateDisplayDialog(Frame owner, String title, Component messageComponent) {
        super(owner, title, true);
        initializeUI(messageComponent);

        // --- Icon Setting Logic (Keep this for debugging) ---
        System.out.println("TranslateDisplayDialog: Initializing. Attempting to set window icon for title: \"" + title + "\"");
        if (owner != null) {
            List<Image> icons = owner.getIconImages();
            if (icons != null && !icons.isEmpty()) {
                setIconImage(icons.get(0));
                System.out.println("TranslateDisplayDialog: Window icon set from owner. Icons found: " + icons.size());
            } else {
                System.out.println("TranslateDisplayDialog: Owner found, but owner has no icons or icon list is empty/null. Window icon not set from owner.");
            }
        } else {
            System.out.println("TranslateDisplayDialog: Owner is null. Cannot set window icon from owner.");
        }
        // --- End Icon Setting Logic ---

        // --- Setting Dialog Size and Position ---
        int dialogWidth;
        int dialogHeight;

        if (owner != null) {
            // Set size relative to the owner (e.g., MainFrame)
            dialogWidth = (int) (owner.getWidth() * 0.75); // 75% of owner's width
            dialogHeight = (int) (owner.getHeight() * 0.70); // 70% of owner's height
        } else {
            // Fallback size if no owner is provided (e.g., 700x500)
            // Or a percentage of screen size
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            dialogWidth = (int) (screenSize.width * 0.5); // 50% of screen width
            dialogHeight = (int) (screenSize.height * 0.5); // 50% of screen height
        }

        // Ensure a minimum size
        dialogWidth = Math.max(dialogWidth, 600); // Minimum width 600px
        dialogHeight = Math.max(dialogHeight, 400); // Minimum height 400px

        setSize(dialogWidth, dialogHeight);
        setMinimumSize(new Dimension(500, 300)); // Set a reasonable minimum resizable size
        setResizable(true); // Allow resizing

        // Center the dialog on the screen
        setLocationRelativeTo(null); // Passing null centers on the screen
        // If you prefer to center relative to the owner (MainFrame):
        // setLocationRelativeTo(owner);
        // --- End Setting Dialog Size and Position ---
    }

    private void initializeUI(Component messageComponent) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // messageComponent is the JScrollPane containing the JTextArea
        mainPanel.add(messageComponent, BorderLayout.CENTER);

        // Button panel for the "OK" button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Added bottom padding too

        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        okButton.setPreferredSize(new Dimension(120, 35)); // Slightly larger button
        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
    }

    /**
     * Utility method to quickly show this custom message dialog.
     */
    public static void showMessage(Frame owner, String title, Component messageComponent) {
        TranslateDisplayDialog dialog = new TranslateDisplayDialog(owner, title, messageComponent);
        dialog.setVisible(true);
    }
}
