import javax.swing.*;
import java.awt.*;

public class TranslateDialog extends JDialog {
    private static final String SAVE_LABEL = "Save";
    private static final String CANCEL_LABEL = "Cancel";

    private JTextArea translateArea;
    private String result;

    public TranslateDialog(Frame owner) {
        super(owner, "Translate", true);
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(400, 300);
        setLocationRelativeTo(getOwner());

        // Text area for translation
        translateArea = new JTextArea();
        translateArea.setLineWrap(true);
        translateArea.setWrapStyleWord(true);
        translateArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        add(new JScrollPane(translateArea), BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = new JButton(SAVE_LABEL);
        saveButton.addActionListener(e -> {
            result = translateArea.getText().trim();
            dispose();
        });
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton(CANCEL_LABEL);
        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setTranslation(String translation) {
        translateArea.setText(translation != null ? translation : "");
    }

    public String getResult() {
        return result;
    }
}
