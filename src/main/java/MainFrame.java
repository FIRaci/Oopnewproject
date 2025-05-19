// Hãy đảm bảo bạn có import này nếu AlarmController không cùng package
// import com.yourpackage.AlarmController; // << THAY com.yourpackage bằng package đúng

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
    private final NoteController controller;
    private AlarmController alarmController; // <<<< SỬA 1: Thêm trường alarmController
    private MainMenuScreen mainMenuScreen;
    private MissionScreen missionScreen;
    private NoteEditorScreen noteEditorScreen;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private ImageSpinner imageSpinner;
    private MouseEventDispatcher mouseEventDispatcher;

    public MainFrame(NoteController controller) {
        this.controller = controller;
        // <<<< SỬA 2: Khởi tạo AlarmController sau khi NoteController đã có
        // Giả định AlarmController, NoteController, MainFrame ở cùng package hoặc đã import đúng
        this.alarmController = new AlarmController(this.controller, this);
        // --------------------------------------------------------------------
        initializeUI();
        setupShortcuts();
    }

    private void initializeUI() {
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

        // --- Tab Panel Setup ---
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
                    HelpScreen.showDialog(MainFrame.this); // Giả sử HelpScreen.showDialog tồn tại
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

        mainMenuScreen = new MainMenuScreen(controller, this);
        missionScreen = new MissionScreen(controller, this);

        contentPanel.add(mainMenuScreen, "Notes");
        contentPanel.add(missionScreen, "Missions");

        mouseEventDispatcher.addMouseMotionListener(mainMenuScreen);
        mouseEventDispatcher.addMouseMotionListener(missionScreen);

        add(topBarPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        applyTheme(controller.getCurrentTheme().equals("dark"));

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

        showScreen("Notes");
    }

    private void applyTheme(boolean switchToDark) {
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
        if ("NoteEditor".equals(screenName) && getNoteEditorScreenInstance() == null) {
            // NoteEditorScreen được tạo on-demand
        }
        cardLayout.show(contentPanel, screenName);
        contentPanel.requestFocusInWindow();
    }

    private NoteEditorScreen getNoteEditorScreenInstance() {
        boolean found = false;
        for (Component comp : contentPanel.getComponents()) {
            if (comp == noteEditorScreen) {
                found = true;
                break;
            }
        }
        if (noteEditorScreen == null || !found) {
            noteEditorScreen = new NoteEditorScreen(this, controller, null); // Giả sử NoteEditorScreen tồn tại
            contentPanel.add(noteEditorScreen, "NoteEditor");
            contentPanel.revalidate();
            contentPanel.repaint();
            mouseEventDispatcher.addMouseMotionListener(noteEditorScreen);
        }
        return noteEditorScreen;
    }

    public void showAddNoteScreen() {
        NoteEditorScreen editor = getNoteEditorScreenInstance();
        editor.setNote(null);
        showScreen("NoteEditor");
    }

    public void showNoteDetailScreen(Note note) {
        NoteEditorScreen editor = getNoteEditorScreenInstance();
        editor.setNote(note);
        showScreen("NoteEditor");
    }

    public void showMainMenuScreen() {
        showScreen("Notes");
        if (mainMenuScreen != null) {
            mainMenuScreen.refresh();
        }
    }

    public void showMissionsScreen() {
        showScreen("Missions");
        if (missionScreen != null) {
            missionScreen.refreshMissions();
        }
    }

    public void openCanvasPanel() {
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
                    .filter(note -> note.getFolderId() == folder.getId())
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
        int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                "Are you sure you want to exit XiNoClo?", "Confirm Exit",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            // <<<< SỬA 3: Dừng AlarmController scheduler trước khi thoát
            if (this.alarmController != null) {
                this.alarmController.stopSoundAndScheduler();
            }
            // ---------------------------------------------------------
            DBConnectionManager.shutdown(); // Giả sử DBConnectionManager tồn tại và có phương thức này
            System.exit(0);
        }
    }

    private void setupShortcuts() {
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
                controller.changeTheme();
                // Note: controller.changeTheme() đã gọi SwingUtilities.updateComponentTreeUI(mainFrameInstance);
                // Nếu mainFrameInstance trong NoteController chính là MainFrame này thì không cần gọi lại ở đây.
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
                HelpScreen dialog = new HelpScreen(MainFrame.this); // Giả sử HelpScreen tồn tại
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
}