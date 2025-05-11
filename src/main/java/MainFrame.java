import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class MainFrame extends JFrame {
    private final NoteController controller;
    private MainMenuScreen mainMenuScreen;
    private MissionScreen missionScreen;
    private NoteEditorScreen noteEditorScreen;
    private JTabbedPane tabbedPane;
    private boolean isDarkTheme = false;

    public MainFrame(NoteController controller) {
        this.controller = controller;
        initializeUI();
        setupShortcuts();
    }

    private void initializeUI() {
        setTitle("XiNoClo - Note App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600); // Tăng kích thước cửa sổ để chứa khung lớn hơn
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        mainMenuScreen = new MainMenuScreen(controller, this);
        missionScreen = new MissionScreen(controller, this);
        tabbedPane.addTab("Notes", mainMenuScreen);
        tabbedPane.addTab("Missions", missionScreen);
        add(tabbedPane, BorderLayout.CENTER);

        // Đảm bảo kích thước tab phù hợp với MissionScreen
        tabbedPane.setPreferredSize(new Dimension(820, 550)); // Thêm padding nhẹ quanh 800x500
        tabbedPane.setMinimumSize(new Dimension(820, 550));
        tabbedPane.setMaximumSize(new Dimension(820, 550));

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException e) {
            JOptionPane.showMessageDialog(this, "Failed to set default theme!", "Error", JOptionPane.ERROR_MESSAGE);
        }

        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/5048557ff0fc302e6cc0d419e2fc0219.jpg")));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load icon: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showMainMenuScreen() {
        tabbedPane.setSelectedIndex(0);
    }

    public void showAddNoteScreen() {
        getContentPane().removeAll();
        noteEditorScreen = new NoteEditorScreen(this, controller, null);
        add(noteEditorScreen, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void showNoteDetailScreen(Note note) {
        getContentPane().removeAll();
        noteEditorScreen = new NoteEditorScreen(this, controller, note);
        noteEditorScreen.setNote(note);
        add(noteEditorScreen, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void openCanvasPanel() {
        JOptionPane.showMessageDialog(this, "Canvas Panel Placeholder", "Stats", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setupShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // Ctrl + Q: Thoát ứng dụng
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK), "exit");
        actionMap.put("exit", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                        "Are you sure you want to exit?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        // Ctrl + W: Đổi theme
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK), "toggleTheme");
        actionMap.put("toggleTheme", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    if (isDarkTheme) {
                        UIManager.setLookAndFeel(new FlatLightLaf());
                    } else {
                        UIManager.setLookAndFeel(new FlatDarkLaf());
                    }
                    isDarkTheme = !isDarkTheme;
                    SwingUtilities.updateComponentTreeUI(MainFrame.this);
                } catch (UnsupportedLookAndFeelException ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "Failed to change theme!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}