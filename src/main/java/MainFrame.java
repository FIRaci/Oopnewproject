import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class MainFrame extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private MainMenuScreen mainMenuScreen;
    private NoteEditorScreen noteEditorScreen;
    private CanvasPanel canvasPanel;
    private final NoteController noteController;
    private final AlarmController alarmController;

    public MainFrame(NoteController noteController) {
        this.noteController = noteController;
        this.alarmController = new AlarmController(noteController.getNoteManager(), this);

        applyTheme(noteController.getCurrentTheme());

        setTitle("XiNoClo");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Đặt icon cho cửa sổ
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/5048557ff0fc302e6cc0d419e2fc0219.jpg")));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load icon: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                alarmController.stop();
                System.exit(0);
            }
        });

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainMenuScreen = new MainMenuScreen(noteController, this);
        noteEditorScreen = new NoteEditorScreen(this, noteController, new Note("New Note", "", false));
        canvasPanel = new CanvasPanel(noteController, this);

        mainPanel.add(mainMenuScreen, "MainMenu");
        mainPanel.add(noteEditorScreen, "NoteEditor");
        mainPanel.add(canvasPanel, "CanvasPanel");

        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem switchThemeItem = new JMenuItem("Switch Theme");
        switchThemeItem.addActionListener(e -> {
            String newTheme = noteController.getCurrentTheme().equalsIgnoreCase("Dark") ? "Light" : "Dark";
            noteController.changeTheme(newTheme);
            applyTheme(newTheme);
        });
        settingsMenu.add(switchThemeItem);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);

        add(mainPanel);
        cardLayout.show(mainPanel, "MainMenu");
    }

    private void applyTheme(String newTheme) {
        try {
            if ("Dark".equalsIgnoreCase(newTheme)) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to apply theme: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showNoteDetailScreen(Note selectedNote) {
        if (selectedNote == null) {
            JOptionPane.showMessageDialog(this, "No note selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        noteEditorScreen.setNote(selectedNote);
        cardLayout.show(mainPanel, "NoteEditor");
    }

    public void showAddNoteScreen() {
        noteEditorScreen.setNote(new Note("New Note", "", false));
        cardLayout.show(mainPanel, "NoteEditor");
    }

    public void openCanvasPanel() {
        cardLayout.show(mainPanel, "CanvasPanel");
    }

    public void showMainMenuScreen() {
        cardLayout.show(mainPanel, "MainMenu");
    }
}
