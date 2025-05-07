import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.Date;

public class AlarmDialog extends JDialog {
    private Alarm result;
    private JSpinner timeSpinner;
    private JComboBox<String> recurrenceCombo;

    public AlarmDialog(Frame owner) {
        super(owner, "Set Alarm", true);
        setLayout(new GridBagLayout());
        setSize(350, 200);
        setLocationRelativeTo(owner);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Time picker
        JLabel timeLabel = new JLabel("Alarm Time:");
        timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor editor = new JSpinner.DateEditor(timeSpinner, "yyyy-MM-dd HH:mm");
        timeSpinner.setEditor(editor);
        timeSpinner.setValue(new Date());
        gbc.gridx = 0; gbc.gridy = 0; add(timeLabel, gbc);
        gbc.gridx = 1; add(timeSpinner, gbc);

        // Recurrence
        JLabel recurrenceLabel = new JLabel("Recurrence:");
        recurrenceCombo = new JComboBox<>(new String[]{"Once", "Daily", "Weekly", "Monthly"});
        gbc.gridx = 0; gbc.gridy = 1; add(recurrenceLabel, gbc);
        gbc.gridx = 1; add(recurrenceCombo, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.setIcon(new ImageIcon("src/main/resources/icons/check.png"));
        okButton.addActionListener(e -> {
            Date date = (Date) timeSpinner.getValue();
            String pattern = (String) recurrenceCombo.getSelectedItem();
            LocalDateTime alarmTime = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            result = new Alarm(alarmTime, !pattern.equals("Once"), pattern.toUpperCase());
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setIcon(new ImageIcon("src/main/resources/icons/cancel.png"));
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        add(buttonPanel, gbc);
    }

    public Alarm getResult() {
        return result;
    }
}