import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class AlarmDialog extends JDialog {
    private Alarm resultAlarm = null; // Sẽ chứa Alarm object kết quả
    private boolean okPressed = false; // Cờ để biết nút OK có được nhấn không

    private JSpinner dateTimeSpinner; // Cho ngày và giờ (khi type là ONCE)
    private JSpinner timeOnlySpinner;   // Chỉ cho giờ:phút (khi type là RECURRING)
    private JComboBox<String> recurrenceTypeComboBox;
    private JRadioButton specificDateTimeRadio; // Đổi tên cho rõ
    private JRadioButton recurringTimeRadio;

    private JPanel specificDateTimePanel; // Panel cho chọn ngày giờ cụ thể
    private JPanel recurringPanel;      // Panel cho chọn lặp lại

    private Alarm alarmToEdit = null; // Alarm hiện tại để chỉnh sửa (nếu có)

    public AlarmDialog(Frame owner) {
        super(owner, "Set or Edit Alarm", true); // Tiêu đề chung
        initializeUI();
        // Nếu không có alarmToEdit, hiển thị giá trị mặc định
        if (this.alarmToEdit == null) {
            setInitialDefaults();
        }
        updatePanelsVisibility(); // Cập nhật hiển thị panel dựa trên radio button
    }

    // Constructor mới để nhận Alarm cần chỉnh sửa
    public AlarmDialog(Frame owner, Alarm alarmToEdit) {
        super(owner, "Set or Edit Alarm", true);
        this.alarmToEdit = alarmToEdit;
        initializeUI();
        if (this.alarmToEdit != null) {
            populateFieldsFromAlarm(this.alarmToEdit);
        } else {
            setInitialDefaults();
        }
        updatePanelsVisibility();
    }


    private void initializeUI() {
        setLayout(new GridBagLayout());
        // setSize(420, 300); // Kích thước có thể điều chỉnh bằng pack()
        setLocationRelativeTo(getOwner());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 1. Radio buttons for mode selection
        specificDateTimeRadio = new JRadioButton("Specific Date & Time", true);
        recurringTimeRadio = new JRadioButton("Recurring Time");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(specificDateTimeRadio);
        modeGroup.add(recurringTimeRadio);

        specificDateTimeRadio.addActionListener(e -> updatePanelsVisibility());
        recurringTimeRadio.addActionListener(e -> updatePanelsVisibility());

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(specificDateTimeRadio, gbc);
        gbc.gridy++;
        add(recurringTimeRadio, gbc);

        // 2. Panel for "Specific Date & Time" (ONCE)
        specificDateTimePanel = new JPanel(new GridBagLayout());
        GridBagConstraints dtpGbc = new GridBagConstraints();
        dtpGbc.insets = new Insets(3,3,3,3); dtpGbc.fill = GridBagConstraints.HORIZONTAL;

        dtpGbc.gridx = 0; dtpGbc.gridy = 0;
        specificDateTimePanel.add(new JLabel("Date & Time:"), dtpGbc);
        dateTimeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateTimeSpinner, "yyyy-MM-dd HH:mm");
        dateTimeSpinner.setEditor(dateEditor);
        dtpGbc.gridx = 1;
        specificDateTimePanel.add(dateTimeSpinner, dtpGbc);

        gbc.gridy++; gbc.gridwidth = 2;
        add(specificDateTimePanel, gbc);

        // 3. Panel for "Recurring Time"
        recurringPanel = new JPanel(new GridBagLayout());
        GridBagConstraints rpGbc = new GridBagConstraints();
        rpGbc.insets = new Insets(3,3,3,3); rpGbc.fill = GridBagConstraints.HORIZONTAL;

        rpGbc.gridx = 0; rpGbc.gridy = 0;
        recurringPanel.add(new JLabel("Time:"), rpGbc);
        timeOnlySpinner = new JSpinner(new SpinnerDateModel()); // Dùng model khác cho chỉ giờ:phút
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeOnlySpinner, "HH:mm");
        timeOnlySpinner.setEditor(timeEditor);
        rpGbc.gridx = 1;
        recurringPanel.add(timeOnlySpinner, rpGbc);

        rpGbc.gridx = 0; rpGbc.gridy = 1;
        recurringPanel.add(new JLabel("Recurrence:"), rpGbc);
        String[] recurrenceOptions = {"DAILY", "WEEKLY", "MONTHLY", "YEARLY"};
        recurrenceTypeComboBox = new JComboBox<>(recurrenceOptions);
        rpGbc.gridx = 1;
        recurringPanel.add(recurrenceTypeComboBox, rpGbc);

        gbc.gridy++;
        add(recurringPanel, gbc);

        // 4. Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> handleOkAction());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            okPressed = false;
            resultAlarm = null;
            dispose();
        });
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        gbc.gridy++; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        add(buttonPanel, gbc);

        pack(); // Điều chỉnh kích thước tự động
        setSize(Math.max(getWidth(), 400), Math.max(getHeight(), 280)); // Đảm bảo kích thước tối thiểu
    }

    private void setInitialDefaults() {
        specificDateTimeRadio.setSelected(true);
        LocalDateTime defaultDateTime = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0);
        dateTimeSpinner.setValue(Date.from(defaultDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        timeOnlySpinner.setValue(Date.from(defaultDateTime.atZone(ZoneId.systemDefault()).toInstant())); // Mặc định giờ
        recurrenceTypeComboBox.setSelectedItem("DAILY");
    }

    private void populateFieldsFromAlarm(Alarm alarm) {
        if (alarm.isRecurring()) {
            recurringTimeRadio.setSelected(true);
            timeOnlySpinner.setValue(Date.from(alarm.getAlarmTime().atZone(ZoneId.systemDefault()).toInstant()));
            if (alarm.getRecurrencePattern() != null) {
                recurrenceTypeComboBox.setSelectedItem(alarm.getRecurrencePattern().toUpperCase());
            } else {
                recurrenceTypeComboBox.setSelectedItem("DAILY"); // Mặc định nếu pattern null
            }
        } else { // ONCE
            specificDateTimeRadio.setSelected(true);
            dateTimeSpinner.setValue(Date.from(alarm.getAlarmTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
    }

    private void updatePanelsVisibility() {
        specificDateTimePanel.setVisible(specificDateTimeRadio.isSelected());
        recurringPanel.setVisible(recurringTimeRadio.isSelected());
        // pack(); // Có thể gọi pack() ở đây để dialog tự điều chỉnh kích thước
        // setSize(Math.max(getWidth(), 400), Math.max(getHeight(), 280));
    }

    private void handleOkAction() {
        LocalDateTime selectedAlarmTime;
        boolean isRecurring;
        String recurrencePattern = null;
        long currentAlarmId = (alarmToEdit != null) ? alarmToEdit.getId() : 0L;

        if (specificDateTimeRadio.isSelected()) {
            isRecurring = false;
            Date selectedDate = (Date) dateTimeSpinner.getValue();
            selectedAlarmTime = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            if (selectedAlarmTime.isBefore(LocalDateTime.now())) {
                JOptionPane.showMessageDialog(this, "Alarm time must be in the future.", "Invalid Time", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else { // recurringTimeRadio is selected
            isRecurring = true;
            Date spinnerTime = (Date) timeOnlySpinner.getValue();
            LocalTime timePart = spinnerTime.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
            // Đối với recurring, ngày không quá quan trọng, nhưng ta cần một ngày tham chiếu.
            // Nếu đang sửa alarm cũ và nó là recurring, giữ lại ngày cũ.
            // Nếu tạo mới recurring, hoặc sửa từ ONCE sang recurring, dùng ngày hiện tại.
            LocalDate datePart;
            if (alarmToEdit != null && alarmToEdit.isRecurring()) {
                datePart = alarmToEdit.getAlarmTime().toLocalDate();
            } else {
                datePart = LocalDate.now();
            }
            selectedAlarmTime = LocalDateTime.of(datePart, timePart);
            recurrencePattern = (String) recurrenceTypeComboBox.getSelectedItem();
        }

        if (currentAlarmId > 0) { // Đang chỉnh sửa alarm đã có
            this.resultAlarm = alarmToEdit; // Sử dụng lại object cũ để giữ ID
            this.resultAlarm.setAlarmTime(selectedAlarmTime);
            this.resultAlarm.setRecurring(isRecurring);
            this.resultAlarm.setRecurrencePattern(isRecurring ? recurrencePattern : null);
        } else { // Tạo alarm mới
            this.resultAlarm = new Alarm(selectedAlarmTime, isRecurring, recurrencePattern);
            // ID của resultAlarm sẽ là 0L theo constructor của Alarm
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
        if (alarm != null) {
            populateFieldsFromAlarm(alarm);
        } else {
            setInitialDefaults();
        }
        updatePanelsVisibility();

    }

    // Phương thức này không còn cần thiết vì getResult() trả về null nếu cancel
    // public boolean wasCancelOrNoChange() { ... }
}
