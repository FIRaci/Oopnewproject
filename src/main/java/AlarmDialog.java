
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import javax.swing.border.EmptyBorder;

public class AlarmDialog extends JDialog {
    private Alarm resultAlarm = null;
    private boolean okPressed = false;

    private JSpinner dateTimeSpinner;
    private JSpinner timeOnlySpinner;
    private JComboBox<String> recurrenceTypeComboBox;
    private JRadioButton specificDateTimeRadio;
    private JRadioButton recurringTimeRadio;

    private JPanel specificDateTimePanel;
    private JPanel recurringPanel;

    private Alarm alarmToEdit = null;

    public AlarmDialog(Frame owner) {
        this(owner, null);
    }
    public AlarmDialog(Frame owner, Alarm alarmToEdit) {
        super(owner, (alarmToEdit == null || alarmToEdit.getId() == 0) ? "Đặt Báo thức Mới" : "Sửa Báo thức", true);
        this.alarmToEdit = alarmToEdit;
        initializeUI();
        if (this.alarmToEdit != null) {
            populateFieldsFromAlarm(this.alarmToEdit);
        } else {
            setInitialDefaults();
        }
        updatePanelsVisibility();
        pack();
        setMinimumSize(new Dimension(420, getHeight()));
        setLocationRelativeTo(owner);
    }


    private void initializeUI() {
        setLayout(new BorderLayout(10,10));
        getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding for the whole dialog
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Panel for radio buttons and their associated controls
        JPanel mainControlsPanel = new JPanel();
        mainControlsPanel.setLayout(new BoxLayout(mainControlsPanel, BoxLayout.Y_AXIS));


        // 1. Radio buttons for mode selection
        specificDateTimeRadio = new JRadioButton("Ngày & Giờ cụ thể", true);
        recurringTimeRadio = new JRadioButton("Lặp lại theo thời gian");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(specificDateTimeRadio);
        modeGroup.add(recurringTimeRadio);

        // Panel for radio buttons themselves
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        radioPanel.add(specificDateTimeRadio);
        radioPanel.add(Box.createHorizontalStrut(20));
        radioPanel.add(recurringTimeRadio);
        radioPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainControlsPanel.add(radioPanel);
        mainControlsPanel.add(Box.createRigidArea(new Dimension(0,10)));


        specificDateTimeRadio.addActionListener(e -> updatePanelsVisibility());
        recurringTimeRadio.addActionListener(e -> updatePanelsVisibility());


        // 2. Panel for "Specific Date & Time" (ONCE)
        specificDateTimePanel = new JPanel(new GridBagLayout());
        specificDateTimePanel.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        GridBagConstraints dtpGbc = new GridBagConstraints();
        dtpGbc.insets = new Insets(5,5,5,5);
        dtpGbc.anchor = GridBagConstraints.WEST;

        dtpGbc.gridx = 0; dtpGbc.gridy = 0;
        specificDateTimePanel.add(new JLabel("Ngày & Giờ:"), dtpGbc);
        dateTimeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateTimeSpinner, "yyyy-MM-dd HH:mm");
        dateTimeSpinner.setEditor(dateEditor);
        dtpGbc.gridx = 1; dtpGbc.weightx = 1.0; dtpGbc.fill = GridBagConstraints.HORIZONTAL;
        specificDateTimePanel.add(dateTimeSpinner, dtpGbc);
        specificDateTimePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainControlsPanel.add(specificDateTimePanel);


        // 3. Panel for "Recurring Time"
        recurringPanel = new JPanel(new GridBagLayout());
        recurringPanel.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        GridBagConstraints rpGbc = new GridBagConstraints();
        rpGbc.insets = new Insets(5,5,5,5);
        rpGbc.anchor = GridBagConstraints.WEST;

        rpGbc.gridx = 0; rpGbc.gridy = 0;
        recurringPanel.add(new JLabel("Thời gian:"), rpGbc);
        timeOnlySpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeOnlySpinner, "HH:mm");
        timeOnlySpinner.setEditor(timeEditor);
        rpGbc.gridx = 1; rpGbc.weightx = 1.0; rpGbc.fill = GridBagConstraints.HORIZONTAL;
        recurringPanel.add(timeOnlySpinner, rpGbc);

        rpGbc.gridx = 0; rpGbc.gridy = 1; rpGbc.weightx = 0.0; rpGbc.fill = GridBagConstraints.NONE;
        recurringPanel.add(new JLabel("Lặp lại:"), rpGbc);
        String[] recurrenceOptions = {"DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        recurrenceTypeComboBox = new JComboBox<>(recurrenceOptions);
        rpGbc.gridx = 1; rpGbc.weightx = 1.0; rpGbc.fill = GridBagConstraints.HORIZONTAL;
        recurringPanel.add(recurrenceTypeComboBox, rpGbc);
        recurringPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainControlsPanel.add(recurringPanel);

        add(mainControlsPanel, BorderLayout.CENTER);

        // 4. Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        buttonPanel.setBorder(new EmptyBorder(10,0,0,0));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> handleOkAction());
        JButton cancelButton = new JButton("Hủy");
        cancelButton.addActionListener(e -> {
            okPressed = false;
            resultAlarm = null;
            dispose();
        });
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(okButton);
    }

    private void setInitialDefaults() {
        specificDateTimeRadio.setSelected(true);
        LocalDateTime defaultDateTime = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
        dateTimeSpinner.setValue(Date.from(defaultDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        LocalDateTime defaultTimeOnly = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        timeOnlySpinner.setValue(Date.from(defaultTimeOnly.atZone(ZoneId.systemDefault()).toInstant()));
        recurrenceTypeComboBox.setSelectedItem("DAILY");
    }

    private void populateFieldsFromAlarm(Alarm alarm) {
        if (alarm.isRecurring()) {
            recurringTimeRadio.setSelected(true);
            if (alarm.getAlarmTime() != null) {
                timeOnlySpinner.setValue(Date.from(alarm.getAlarmTime().atZone(ZoneId.systemDefault()).toInstant()));
            } else { // Fallback if alarmTime is somehow null for a recurring alarm
                LocalDateTime defaultTimeOnly = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
                timeOnlySpinner.setValue(Date.from(defaultTimeOnly.atZone(ZoneId.systemDefault()).toInstant()));
            }
            if (alarm.getRecurrencePattern() != null) {
                recurrenceTypeComboBox.setSelectedItem(alarm.getRecurrencePattern().toUpperCase());
            } else {
                recurrenceTypeComboBox.setSelectedItem("DAILY");
            }
        } else {
            specificDateTimeRadio.setSelected(true);
            if (alarm.getAlarmTime() != null) {
                dateTimeSpinner.setValue(Date.from(alarm.getAlarmTime().atZone(ZoneId.systemDefault()).toInstant()));
            } else { // Fallback if alarmTime is null for a non-recurring alarm
                LocalDateTime defaultDateTime = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
                dateTimeSpinner.setValue(Date.from(defaultDateTime.atZone(ZoneId.systemDefault()).toInstant()));
            }
        }
    }

    private void updatePanelsVisibility() {
        specificDateTimePanel.setVisible(specificDateTimeRadio.isSelected());
        recurringPanel.setVisible(recurringTimeRadio.isSelected());
        pack();

    }

    private void handleOkAction() {
        LocalDateTime selectedAlarmTime;
        boolean isRecurring;
        String recurrencePattern = null;
        long currentAlarmId = (alarmToEdit != null) ? alarmToEdit.getId() : 0L;

        if (specificDateTimeRadio.isSelected()) {
            isRecurring = false;
            Date selectedDate = (Date) dateTimeSpinner.getValue();
            selectedAlarmTime = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().withSecond(0).withNano(0);
            if (selectedAlarmTime.isBefore(LocalDateTime.now().withSecond(0).withNano(0))) {
                JOptionPane.showMessageDialog(this, "Thời gian báo thức phải ở trong tương lai.", "Thời gian không hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            isRecurring = true;
            Date spinnerTime = (Date) timeOnlySpinner.getValue();
            LocalTime timePart = spinnerTime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);

            LocalDate datePart;
            if (alarmToEdit != null && alarmToEdit.isRecurring() && alarmToEdit.getAlarmTime() != null) {
                datePart = alarmToEdit.getAlarmTime().toLocalDate();
            } else {
                datePart = LocalDate.now();
            }
            selectedAlarmTime = LocalDateTime.of(datePart, timePart);
            recurrencePattern = (String) recurrenceTypeComboBox.getSelectedItem();
            if (recurrencePattern == null) recurrencePattern = "DAILY";
        }

        if (currentAlarmId > 0 && alarmToEdit != null) {
            this.resultAlarm = alarmToEdit;
            this.resultAlarm.setAlarmTime(selectedAlarmTime);
            this.resultAlarm.setRecurring(isRecurring);
            this.resultAlarm.setRecurrencePattern(isRecurring ? recurrencePattern : null);
        } else {
            this.resultAlarm = new Alarm(selectedAlarmTime, isRecurring, recurrencePattern);
        }
        this.okPressed = true;
        dispose();
    }

    public Alarm getResult() {
        return resultAlarm;
    }

    public boolean isOkPressed() {
        return okPressed;
    }


    public void setAlarmToEdit(Alarm alarm) {
        this.alarmToEdit = alarm;
        setTitle((alarm == null || alarm.getId() == 0) ? "Đặt Báo thức Mới" : "Sửa Báo thức");
        if (alarm != null) {
            populateFieldsFromAlarm(alarm);
        } else {
            setInitialDefaults();
        }
        updatePanelsVisibility();
    }
}
