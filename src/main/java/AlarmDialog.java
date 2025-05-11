import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class AlarmDialog extends JDialog {
    private Alarm result;
    private JSpinner dateTimeSpinner;
    private JSpinner timeSpinner;
    private JComboBox<String> recurrenceCombo;
    private JRadioButton specificDateRadio;
    private JRadioButton recurringTimeRadio;
    private JPanel dateTimePanel;
    private JPanel timePanel;

    public AlarmDialog(Frame owner) {
        super(owner, "Set Alarm", true);
        setLayout(new GridBagLayout());
        setSize(400, 250);
        setLocationRelativeTo(owner);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Radio buttons for mode selection
        specificDateRadio = new JRadioButton("Specific Date", true);
        recurringTimeRadio = new JRadioButton("Recurring Time");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(specificDateRadio);
        modeGroup.add(recurringTimeRadio);

        specificDateRadio.addActionListener(e -> updatePanels());
        recurringTimeRadio.addActionListener(e -> updatePanels());

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(specificDateRadio, gbc);

        gbc.gridy = 1;
        add(recurringTimeRadio, gbc);

        // Specific Date panel
        dateTimePanel = new JPanel(new GridBagLayout());
        JLabel dateTimeLabel = new JLabel("Date and Time:");
        dateTimeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateTimeSpinner, "yyyy-MM-dd HH:mm");
        dateTimeSpinner.setEditor(dateEditor);
        dateTimeSpinner.setValue(new Date());

        GridBagConstraints panelGbc = new GridBagConstraints();
        panelGbc.insets = new Insets(5, 5, 5, 5);
        panelGbc.gridx = 0;
        panelGbc.gridy = 0;
        dateTimePanel.add(dateTimeLabel, panelGbc);
        panelGbc.gridx = 1;
        dateTimePanel.add(dateTimeSpinner, panelGbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(dateTimePanel, gbc);

        // Recurring Time panel
        timePanel = new JPanel(new GridBagLayout());
        JLabel timeLabel = new JLabel("Time:");
        timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setValue(new Date());

        panelGbc.gridx = 0;
        panelGbc.gridy = 0;
        timePanel.add(timeLabel, panelGbc);
        panelGbc.gridx = 1;
        timePanel.add(timeSpinner, panelGbc);

        JLabel recurrenceLabel = new JLabel("Recurrence:");
        recurrenceCombo = new JComboBox<>(new String[]{"Daily", "Weekly", "Monthly", "Yearly"});
        panelGbc.gridx = 0;
        panelGbc.gridy = 1;
        timePanel.add(recurrenceLabel, panelGbc);
        panelGbc.gridx = 1;
        timePanel.add(recurrenceCombo, panelGbc);

        gbc.gridy = 3;
        add(timePanel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            if (specificDateRadio.isSelected()) {
                Date date = (Date) dateTimeSpinner.getValue();
                LocalDateTime alarmTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                result = new Alarm(alarmTime, false, null);
            } else {
                Date time = (Date) timeSpinner.getValue();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime alarmTime = LocalDateTime.of(
                        now.getYear(), now.getMonth(), now.getDayOfMonth(),
                        time.toInstant().atZone(ZoneId.systemDefault()).getHour(),
                        time.toInstant().atZone(ZoneId.systemDefault()).getMinute()
                );
                String pattern = (String) recurrenceCombo.getSelectedItem();
                result = new Alarm(alarmTime, true, pattern.toUpperCase());
            }
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        updatePanels();
    }

    private void updatePanels() {
        dateTimePanel.setVisible(specificDateRadio.isSelected());
        timePanel.setVisible(recurringTimeRadio.isSelected());
        revalidate();
        repaint();
    }

    public Alarm getResult() {
        return result;
    }
}