import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Random;

public class MainFrame extends JFrame {
    private final NoteController controller;
    private AlarmController alarmController;
    private MainMenuScreen mainMenuScreen;
    private MissionScreen missionScreen;
    private NoteEditorScreen noteEditorScreen;
    private DrawScreen drawScreen;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private ImageSpinner imageSpinner;
    private MouseEventDispatcher mouseEventDispatcher;

    public MainFrame(NoteController controllerParam) {
        System.out.println("[MainFrame Constructor] NoteController được truyền vào: " + (controllerParam == null ? "NULL" : "KHÔNG NULL"));
        if (controllerParam == null) {
            System.err.println("LỖI NGHIÊM TRỌNG: MainFrame constructor nhận một NoteController null!");
            JOptionPane.showMessageDialog(null, "Lỗi nghiêm trọng: Controller không được khởi tạo cho MainFrame.", "Lỗi Khởi Động", JOptionPane.ERROR_MESSAGE);
            this.controller = null;
        } else {
            this.controller = controllerParam;
        }

        if (this.controller != null) {
            System.out.println("[MainFrame Constructor] Đang tạo AlarmController với controller: " + this.controller);
            this.alarmController = new AlarmController(this.controller, this);
        } else {
            System.err.println("[MainFrame Constructor] Cảnh báo: controller là null, AlarmController sẽ không được khởi tạo đúng cách.");
        }

        initializeUI();
        setupShortcuts();
        System.out.println("[MainFrame Constructor] Hoàn tất constructor.");
    }

    private void initializeUI() {
        System.out.println("[MainFrame initializeUI] Bắt đầu. Controller hiện tại: " + (this.controller == null ? "NULL" : "KHÔNG NULL"));
        setTitle("XiNoClo");
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

        if (this.controller != null) {
            System.out.println("[MainFrame initializeUI] Đang tạo MainMenuScreen với controller: " + this.controller);
            mainMenuScreen = new MainMenuScreen(this.controller, this);
            System.out.println("[MainFrame initializeUI] Đang tạo MissionScreen với controller: " + this.controller);
            missionScreen = new MissionScreen(this.controller, this);

            System.out.println("[MainFrame initializeUI] Đang tạo DrawScreen với controller: " + this.controller);
            drawScreen = new DrawScreen(this, this.controller);

            contentPanel.add(mainMenuScreen, "Notes");
            contentPanel.add(missionScreen, "Missions");
            contentPanel.add(drawScreen, "DrawScreen");

            mouseEventDispatcher.addMouseMotionListener(mainMenuScreen);
            mouseEventDispatcher.addMouseMotionListener(missionScreen);
            mouseEventDispatcher.addMouseMotionListener(drawScreen);

        } else {
            System.err.println("[MainFrame initializeUI] LỖI: controller là null, không thể tạo các màn hình chính.");
            JPanel errorPanel = new JPanel(new BorderLayout());
            errorPanel.add(new JLabel("Lỗi nghiêm trọng: Không thể tải giao diện chính do controller bị lỗi.", SwingConstants.CENTER), BorderLayout.CENTER);
            contentPanel.add(errorPanel, "ErrorScreen");
            cardLayout.show(contentPanel, "ErrorScreen");
        }

        add(topBarPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        applyTheme(false);

        try {
            String[] iconNames = {
                    "spinner.jpg"
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


        if (this.controller != null) {
            showScreen("Notes");
        }
        System.out.println("[MainFrame initializeUI] Hoàn tất.");
    }

    private void applyTheme(boolean isDarkTheme) {
        try {
            if (isDarkTheme) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("[applyTheme] Error applying theme: " + e.getMessage());
        }

    }

    private void showScreen(String screenName) {
        if ("Notes".equals(screenName) && mainMenuScreen == null) {
            System.err.println("Lỗi: MainMenuScreen là null, không thể hiển thị 'Notes'.");
            if (controller == null) cardLayout.show(contentPanel, "ErrorScreen");
            return;
        }
        if ("Missions".equals(screenName) && missionScreen == null) {
            System.err.println("Lỗi: MissionScreen là null, không thể hiển thị 'Missions'.");
            if (controller == null) cardLayout.show(contentPanel, "ErrorScreen");
            return;
        }
        if ("DrawScreen".equals(screenName) && drawScreen == null) {
            System.err.println("Lỗi: DrawScreen là null, không thể hiển thị 'DrawScreen'.");
            if (controller == null) cardLayout.show(contentPanel, "ErrorScreen");
            return;
        }
        if ("NoteEditor".equals(screenName) && getNoteEditorScreenInstance() == null && controller == null) {
            System.err.println("Lỗi: NoteEditorScreen không thể tạo do controller là null.");
            cardLayout.show(contentPanel, "ErrorScreen");
            return;
        }


        System.out.println("[MainFrame showScreen] Hiển thị màn hình: " + screenName);
        cardLayout.show(contentPanel, screenName);
        for (Component comp : contentPanel.getComponents()) {
            if (comp.isVisible() && comp instanceof JComponent) {
                ((JComponent)comp).requestFocusInWindow();
                break;
            }
        }
    }

    private NoteEditorScreen getNoteEditorScreenInstance() {
        if (this.controller == null) {
            System.err.println("LỖI: Không thể tạo NoteEditorScreen vì controller là null.");
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
        }
        return noteEditorScreen;
    }

    public void showAddNoteScreen() {
        NoteEditorScreen editor = getNoteEditorScreenInstance();
        if (editor != null) {
            editor.setNote(null);
            showScreen("NoteEditor");
        }
    }

    public void showNoteDetailScreen(Note note) {
        if (note == null || note.getNoteType() != Note.NoteType.TEXT) {
            System.err.println("Lỗi: showNoteDetailScreen chỉ dành cho TEXT notes.");
            if (note != null && note.getNoteType() == Note.NoteType.DRAWING) {
                showEditDrawScreen(note);
            }
            return;
        }
        NoteEditorScreen editor = getNoteEditorScreenInstance();
        if (editor != null) {
            editor.setNote(note);
            showScreen("NoteEditor");
        }
    }

    // Phương thức mới để hiển thị màn hình vẽ cho bản vẽ mới
    public void showNewDrawScreen() {
        if (drawScreen == null) {
            if (this.controller != null) {
                drawScreen = new DrawScreen(this, this.controller);
                contentPanel.add(drawScreen, "DrawScreen");
                mouseEventDispatcher.addMouseMotionListener(drawScreen);
            } else {
                System.err.println("Lỗi: Không thể tạo DrawScreen vì controller là null.");
                cardLayout.show(contentPanel, "ErrorScreen");
                return;
            }
        }
        drawScreen.setDrawingNote(null);
        showScreen("DrawScreen");
    }

    // Phương thức mới để hiển thị màn hình vẽ để chỉnh sửa bản vẽ đã có
    public void showEditDrawScreen(Note drawingNote) {
        if (drawingNote == null || drawingNote.getNoteType() != Note.NoteType.DRAWING) {
            System.err.println("Lỗi: showEditDrawScreen chỉ dành cho DRAWING notes.");
            return;
        }
        if (drawScreen == null) {
            if (this.controller != null) {
                drawScreen = new DrawScreen(this, this.controller);
                contentPanel.add(drawScreen, "DrawScreen");
                mouseEventDispatcher.addMouseMotionListener(drawScreen);
            } else {
                System.err.println("Lỗi: Không thể tạo DrawScreen vì controller là null.");
                cardLayout.show(contentPanel, "ErrorScreen");
                return;
            }
        }
        drawScreen.setDrawingNote(drawingNote);
        showScreen("DrawScreen");
    }


    public void showMainMenuScreen() {
        showScreen("Notes");
        if (mainMenuScreen != null) {
            mainMenuScreen.refresh();
        } else if (this.controller == null) {
            cardLayout.show(contentPanel, "ErrorScreen");
        }
    }

    public void showMissionsScreen() {
        showScreen("Missions");
        if (missionScreen != null) {
            missionScreen.refreshMissions();
        }
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
            if (this.controller != null) {
                NoteService service = controller.getNoteService();
                if (service != null) {
                    NoteManager manager = service.getNoteManager();
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
        if (this.controller == null) {
            System.err.println("LỖI: Controller là null trong setupShortcuts. Phím tắt có thể không hoạt động.");
            return;
        }
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
                if (controller == null) return;
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

    public NoteController getAppController() {
        return this.controller;
    }
}
