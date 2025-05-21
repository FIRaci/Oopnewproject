// Các import khác giữ nguyên...
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Objects;
import java.util.Random;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class MainFrame extends JFrame {
    private final NoteController controller; // Trường này sẽ được gán trong constructor
    private AlarmController alarmController;
    private MainMenuScreen mainMenuScreen;
    private MissionScreen missionScreen;
    private NoteEditorScreen noteEditorScreen;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private ImageSpinner imageSpinner;
    private MouseEventDispatcher mouseEventDispatcher;

    public MainFrame(NoteController controllerParam) { // Đổi tên tham số để rõ ràng
        // In ra để kiểm tra xem controllerParam có null không khi được truyền vào
        System.out.println("[MainFrame Constructor] NoteController được truyền vào: " + (controllerParam == null ? "NULL" : "KHÔNG NULL"));

        if (controllerParam == null) {
            System.err.println("LỖI NGHIÊM TRỌNG: MainFrame constructor nhận một NoteController null!");
            // Hiển thị lỗi và có thể thoát ứng dụng nếu controller là bắt buộc
            JOptionPane.showMessageDialog(null, "Lỗi nghiêm trọng: Controller không được khởi tạo cho MainFrame.", "Lỗi Khởi Động", JOptionPane.ERROR_MESSAGE);
            // throw new IllegalStateException("NoteController không thể null trong MainFrame constructor");
            // Nếu không throw exception, gán một controller mặc định hoặc xử lý phù hợp
            // Tạm thời, để tránh lỗi ngay lập tức ở các dòng sau, ta không làm gì thêm ở đây,
            // nhưng ứng dụng sẽ không hoạt động đúng.
            this.controller = null; // Gán null để rõ ràng
        } else {
            this.controller = controllerParam; // Gán tham số cho trường của lớp
        }

        // Chỉ khởi tạo AlarmController nếu this.controller không null
        if (this.controller != null) {
            System.out.println("[MainFrame Constructor] Đang tạo AlarmController với controller: " + this.controller);
            this.alarmController = new AlarmController(this.controller, this);
        } else {
            System.err.println("[MainFrame Constructor] Cảnh báo: controller là null, AlarmController sẽ không được khởi tạo đúng cách.");
            // this.alarmController có thể vẫn là null, cần xử lý ở những nơi dùng đến nó
        }

        initializeUI();
        setupShortcuts();
        System.out.println("[MainFrame Constructor] Hoàn tất constructor.");
    }

    private void initializeUI() {
        System.out.println("[MainFrame initializeUI] Bắt đầu. Controller hiện tại: " + (this.controller == null ? "NULL" : "KHÔNG NULL"));
        setTitle("XiNoClo - Note App");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndExit();
            }
        });
        setMinimumSize(new Dimension(800, 550));
        setSize(900, 650);
        setLocationRelativeTo(null);

        JPanel topBarPanel = new JPanel(new BorderLayout(0, 0));
        JButton notesButton = new JButton("📝 Notes");
        notesButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        notesButton.setFocusPainted(false);
        notesButton.addActionListener(e -> showScreen("Notes"));

        JButton missionsButton = new JButton("🎯 Missions");
        missionsButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        missionsButton.setFocusPainted(false);
        missionsButton.addActionListener(e -> showScreen("Missions"));

        JPanel tabBarContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        tabBarContainer.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        imageSpinner = new ImageSpinner(50, "/images/spinner.jpg");
        imageSpinner.setToolTipText("Rotating Indicator");
        imageSpinner.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    HelpScreen.showDialog(MainFrame.this);
                }
            }
        });

        mouseEventDispatcher = new MouseEventDispatcher(imageSpinner, this);
        mouseEventDispatcher.addMouseMotionListener(this);

        tabBarContainer.add(notesButton);
        tabBarContainer.add(imageSpinner);
        tabBarContainer.add(missionsButton);
        topBarPanel.add(tabBarContainer, BorderLayout.CENTER);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Sử dụng trường this.controller một cách tường minh
        if (this.controller != null) {
            System.out.println("[MainFrame initializeUI] Đang tạo MainMenuScreen với controller: " + this.controller);
            mainMenuScreen = new MainMenuScreen(this.controller, this);
            System.out.println("[MainFrame initializeUI] Đang tạo MissionScreen với controller: " + this.controller);
            missionScreen = new MissionScreen(this.controller, this);

            contentPanel.add(mainMenuScreen, "Notes");
            contentPanel.add(missionScreen, "Missions");

            mouseEventDispatcher.addMouseMotionListener(mainMenuScreen);
            mouseEventDispatcher.addMouseMotionListener(missionScreen);
        } else {
            System.err.println("[MainFrame initializeUI] LỖI: controller là null, không thể tạo MainMenuScreen hoặc MissionScreen.");
            // Hiển thị một panel lỗi hoặc thông báo cho người dùng
            JPanel errorPanel = new JPanel(new BorderLayout());
            errorPanel.add(new JLabel("Lỗi nghiêm trọng: Không thể tải giao diện chính do controller bị lỗi.", SwingConstants.CENTER), BorderLayout.CENTER);
            contentPanel.add(errorPanel, "ErrorScreen");
            cardLayout.show(contentPanel, "ErrorScreen");
        }

        add(topBarPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        // Tạm thời comment out việc áp dụng theme cho đến khi controller ổn định
        // if (this.controller != null) {
        //     applyTheme(this.controller.getCurrentTheme().equals("dark"));
        // } else {
        //     applyTheme(false); // Mặc định light theme nếu controller lỗi
        // }
        applyTheme(false); // Mặc định light theme

        try {
            String[] iconNames = {
                    "Acheron.jpg", "Aglaea.jpg", "BlackSwan.jpg", "Bronya.jpg", "Castroice.jpg", "Clara.jpg",
                    "Feixiao.jpg", "Firefly.jpg", "Fugue.jpg", "FuXuan.jpg", "Hanabi.jpg", "Herta.jpg",
                    "Himeko.jpg", "HuoHuo.jpg", "Jade.jpg", "Jingliu.jpg", "Kafka.jpg", "Lingsha.jpg",
                    "Robin.jpg", "RuanMei.jpg", "Seele.jpg", "SilverWolf.jpg", "Tribbie.jpg", "Yunli.jpg"
            };
            Random random = new Random();
            String randomIconName = iconNames[random.nextInt(iconNames.length)];
            URL appIconUrl = getClass().getResource("/images/" + randomIconName);
            if (appIconUrl != null) {
                setIconImage(new ImageIcon(appIconUrl).getImage());
            } else {
                System.err.println("Không thể tải icon ứng dụng: /images/" + randomIconName);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tải icon ứng dụng: " + e.getMessage());
        }

        if (this.controller != null) { // Chỉ show "Notes" nếu controller và mainMenuScreen đã được tạo
            showScreen("Notes");
        }
        System.out.println("[MainFrame initializeUI] Hoàn tất.");
    }

    private void applyTheme(boolean switchToDark) {
        // ... (giữ nguyên)
        try {
            if (switchToDark) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
            if (imageSpinner != null) {
                imageSpinner.repaint();
            }
        } catch (UnsupportedLookAndFeelException e) {
            JOptionPane.showMessageDialog(this, "Failed to set theme: " + e.getMessage(), "Theme Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showScreen(String screenName) {
        // ... (giữ nguyên)
        if ("NoteEditor".equals(screenName) && getNoteEditorScreenInstance() == null) {
            // NoteEditorScreen được tạo on-demand
        }
        cardLayout.show(contentPanel, screenName);
        contentPanel.requestFocusInWindow();
    }

    private NoteEditorScreen getNoteEditorScreenInstance() {
        // ... (giữ nguyên, nhưng kiểm tra this.controller trước khi tạo NoteEditorScreen)
        if (this.controller == null) {
            System.err.println("LỖI: Không thể tạo NoteEditorScreen vì controller là null.");
            // Có thể hiển thị lỗi hoặc không làm gì cả
            return null;
        }
        boolean found = false;
        for (Component comp : contentPanel.getComponents()) {
            if (comp == noteEditorScreen) {
                found = true;
                break;
            }
        }
        if (noteEditorScreen == null || !found) {
            noteEditorScreen = new NoteEditorScreen(this, this.controller, null);
            contentPanel.add(noteEditorScreen, "NoteEditor");
            contentPanel.revalidate();
            contentPanel.repaint();
            mouseEventDispatcher.addMouseMotionListener(noteEditorScreen);
        }
        return noteEditorScreen;
    }

    public void showAddNoteScreen() {
        // ... (giữ nguyên)
        NoteEditorScreen editor = getNoteEditorScreenInstance();
        if (editor != null) { // Kiểm tra null
            editor.setNote(null);
            showScreen("NoteEditor");
        }
    }

    public void showNoteDetailScreen(Note note) {
        // ... (giữ nguyên)
        NoteEditorScreen editor = getNoteEditorScreenInstance();
        if (editor != null) { // Kiểm tra null
            editor.setNote(note);
            showScreen("NoteEditor");
        }
    }

    public void showMainMenuScreen() {
        // ... (giữ nguyên)
        showScreen("Notes");
        if (mainMenuScreen != null) { // Kiểm tra null
            mainMenuScreen.refresh();
        } else if (this.controller == null) {
            cardLayout.show(contentPanel, "ErrorScreen"); // Hiển thị màn hình lỗi nếu mainMenuScreen không thể tạo
        }
    }

    public void showMissionsScreen() {
        // ... (giữ nguyên)
        showScreen("Missions");
        if (missionScreen != null) { // Kiểm tra null
            missionScreen.refreshMissions();
        }
    }

    public void openCanvasPanel() {
        // ... (giữ nguyên, nhưng kiểm tra this.controller trước khi gọi controller.getFolders())
        if (this.controller == null) {
            JOptionPane.showMessageDialog(this, "Lỗi: Controller chưa sẵn sàng để hiển thị thống kê.", "Lỗi Controller", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // ... (phần còn lại của phương thức giữ nguyên)
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        java.util.List<Folder> folders = controller.getFolders();
        if (folders == null || folders.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No folders found to display statistics.", "Statistics", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        java.util.List<Note> allNotes = controller.getNotes();
        if (allNotes == null) {
            JOptionPane.showMessageDialog(this, "Could not retrieve notes for statistics.", "Statistics Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (Folder folder : folders) {
            long noteCount = allNotes.stream()
                    .filter(note -> note.getFolder() != null && note.getFolder().getId() == folder.getId())
                    .count();
            dataset.addValue(noteCount, "Notes", folder.getName());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Note Count by Folder", "Folder", "Number of Notes", dataset);

        chart.setBackgroundPaint(UIManager.getColor("Panel.background"));
        if (chart.getTitle() != null) chart.getTitle().setPaint(UIManager.getColor("Label.foreground"));
        if (chart.getCategoryPlot() != null) {
            chart.getCategoryPlot().getDomainAxis().setLabelPaint(UIManager.getColor("Label.foreground"));
            chart.getCategoryPlot().getDomainAxis().setTickLabelPaint(UIManager.getColor("Label.foreground"));
            chart.getCategoryPlot().getRangeAxis().setLabelPaint(UIManager.getColor("Label.foreground"));
            chart.getCategoryPlot().getRangeAxis().setTickLabelPaint(UIManager.getColor("Label.foreground"));
            chart.getCategoryPlot().setBackgroundPaint(UIManager.getColor("TextField.background"));
        }
        if (chart.getLegend() != null) chart.getLegend().setItemPaint(UIManager.getColor("Label.foreground"));

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(600, 400));
        JDialog dialog = new JDialog(this, "Statistics", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(chartPanel, BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        mouseEventDispatcher.addMouseMotionListenerToWindow(dialog);
        dialog.setVisible(true);
    }

    private void confirmAndExit() {
        System.out.println("[MainFrame confirmAndExit] Bắt đầu quá trình thoát...");
        int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                "Bạn có chắc muốn thoát XiNoClo?", "Xác Nhận Thoát",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (this.alarmController != null) {
                System.out.println("[MainFrame confirmAndExit] Đang dừng AlarmController...");
                this.alarmController.stopSoundAndScheduler();
            }
            // Thêm lệnh gọi saveData() ở đây
            if (this.controller != null) {
                NoteService service = controller.getNoteService(); // Cần getter cho NoteService trong NoteController
                if (service != null) {
                    NoteManager manager = service.getNoteManager(); // Cần getter cho NoteManager trong NoteService
                    if (manager != null) {
                        System.out.println("[MainFrame confirmAndExit] Đang lưu dữ liệu cuối cùng...");
                        manager.saveData();
                    } else {
                        System.err.println("[MainFrame confirmAndExit] Lỗi: NoteManager là null, không thể lưu dữ liệu khi thoát.");
                    }
                } else {
                    System.err.println("[MainFrame confirmAndExit] Lỗi: NoteService là null, không thể lưu dữ liệu khi thoát.");
                }
            } else {
                System.err.println("[MainFrame confirmAndExit] Lỗi: Controller là null, không thể truy cập NoteManager để lưu.");
            }
            System.out.println("[MainFrame confirmAndExit] Đang thoát ứng dụng...");
            System.exit(0);
        } else {
            System.out.println("[MainFrame confirmAndExit] Người dùng đã hủy thoát.");
        }
    }

    private void setupShortcuts() {
        // ... (giữ nguyên, nhưng đảm bảo this.controller không null khi các action được thực thi)
        if (this.controller == null) {
            System.err.println("LỖI: Controller là null trong setupShortcuts. Phím tắt có thể không hoạt động.");
            return; // Không thiết lập phím tắt nếu controller không có
        }
        // ... (phần còn lại của setupShortcuts giữ nguyên)
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK), "exitApp");
        actionMap.put("exitApp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { confirmAndExit(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK), "toggleTheme");
        actionMap.put("toggleTheme", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (controller != null) controller.changeTheme();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK), "showNotesScreen");
        actionMap.put("showNotesScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { showMainMenuScreen(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_DOWN_MASK), "showMissionsScreen");
        actionMap.put("showMissionsScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { showMissionsScreen(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), "addNoteGlobal");
        actionMap.put("addNoteGlobal", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { showAddNoteScreen(); }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "addFolderGlobal");
        actionMap.put("addFolderGlobal", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (controller == null) return; // Kiểm tra null
                String name = JOptionPane.showInputDialog(MainFrame.this, "Enter folder name:");
                if (name != null && !name.trim().isEmpty()) {
                    controller.addNewFolder(name.trim());
                    if (mainMenuScreen != null && mainMenuScreen.isShowing()) {
                        mainMenuScreen.refreshFolderPanel();
                    }
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "showShortcutsDialog");
        actionMap.put("showShortcutsDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HelpScreen dialog = new HelpScreen(MainFrame.this);
                mouseEventDispatcher.addMouseMotionListenerToWindow(dialog);
                dialog.setVisible(true);
            }
        });
    }

    public void triggerThemeUpdate(boolean isNowDark) {
        applyTheme(isNowDark);
    }

    public MouseEventDispatcher getMouseEventDispatcher() {
        return mouseEventDispatcher;
    }

    // Cần thêm các getter này để confirmAndExit() có thể truy cập NoteManager
    public NoteController getAppController() { // Đổi tên để tránh nhầm lẫn với trường 'controller'
        return this.controller;
    }
}
