import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
// import java.awt.geom.Path2D; // Không thấy sử dụng, có thể bỏ
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
// import java.util.ArrayList; // Không thấy sử dụng trực tiếp, có thể bỏ
import java.util.Base64;
// import java.util.List; // Không thấy sử dụng trực tiếp, có thể bỏ
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

public class DrawScreen extends JPanel {
    private static final String SAVE_LABEL = "Lưu Bản Vẽ";
    private static final String BACK_LABEL = "Quay Lại";
    private static final String CLEAR_LABEL = "Xóa Hết";
    private static final String ERASER_LABEL = "Tẩy";
    private static final String PENCIL_LABEL = "Bút";
    private static final String IMPORT_IMAGE_LABEL = "Chèn Ảnh";
    private static final String HAND_TOOL_LABEL = "Bàn Tay (Pan)";
    // Các hằng số cho nút zoom đã được loại bỏ khỏi giao diện


    private final MainFrame mainFrame;
    private final NoteController controller;
    private Note currentDrawingNote;

    private DrawingPanel drawingPanel;
    private JTextField titleField;
    private JSlider strokeSlider;
    private JButton colorButton;
    private JButton eraserButton;
    private JButton pencilButton;
    private JLabel currentStrokeLabel;
    private JButton importImageButton;
    private JButton handToolButton;
    // Các biến cho nút zoom đã được loại bỏ
    // private JButton zoomInButton;
    // private JButton zoomOutButton;
    // private JButton zoomResetButton;

    private Color currentColor = Color.BLACK;
    private int currentStrokeSize = 3;
    private boolean eraserMode = false;
    private boolean handMode = false;

    public DrawScreen(MainFrame mainFrame, NoteController controller) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        initializeUI();
        setDrawingNote(null); // Khởi tạo với một bản vẽ mới hoặc trống

        // Key listener for ALT key to temporarily enable pan mode and zoom shortcuts
        this.setFocusable(true); // Panel cần focusable để nhận key events
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    if (!handMode && !drawingPanel.isPanModeActive()) { // Chỉ kích hoạt nếu chưa ở hand mode
                        drawingPanel.setTemporaryPanMode(true);
                        // Cursor sẽ được quản lý bởi DrawingPanel khi temporaryPanMode là true
                    }
                } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_EQUALS) { // Ctrl + '+' or Ctrl + '='
                    drawingPanel.zoomIn();
                } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_MINUS) { // Ctrl + '-'
                    drawingPanel.zoomOut();
                } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_0) { // Ctrl + '0'
                    drawingPanel.resetZoomAndPan();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    if (drawingPanel.isTemporaryPanMode()) {
                        drawingPanel.setTemporaryPanMode(false);
                        // Cursor sẽ được DrawingPanel tự đặt lại
                    }
                }
            }
        });
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top Control Panel (Title and Main Buttons) ---
        JPanel topControlPanel = new JPanel(new BorderLayout(10, 5));
        titleField = new JTextField("Bản vẽ không tên");
        titleField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        JPanel titlePanel = new JPanel(new BorderLayout(5,0));
        titlePanel.add(new JLabel("Tiêu đề:"), BorderLayout.WEST);
        titlePanel.add(titleField, BorderLayout.CENTER);
        topControlPanel.add(titlePanel, BorderLayout.CENTER);


        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton saveButton = createToolbarButton(SAVE_LABEL, null);
        saveButton.addActionListener(e -> saveDrawing());
        JButton backButton = createToolbarButton(BACK_LABEL, null);
        backButton.addActionListener(e -> {
            setHandMode(false); // Tắt hand mode khi quay lại
            drawingPanel.setTemporaryPanMode(false); // Tắt temporary pan mode
            // Cursor sẽ được set lại bởi updateToolStates hoặc setPencilMode
            mainFrame.showMainMenuScreen();
        });
        mainButtonsPanel.add(saveButton);
        mainButtonsPanel.add(backButton);
        topControlPanel.add(mainButtonsPanel, BorderLayout.EAST);
        add(topControlPanel, BorderLayout.NORTH);

        // --- Toolbar Panel (West) ---
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new GridBagLayout()); // Sử dụng GridBagLayout
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Công cụ"),
                BorderFactory.createEmptyBorder(5,5,5,5)
        ));
        toolbarPanel.setPreferredSize(new Dimension(200, 0)); // Tăng chiều rộng một chút
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Mỗi component chiếm một hàng
        gbc.fill = GridBagConstraints.HORIZONTAL;    // Cho component giãn theo chiều ngang
        gbc.weightx = 1.0;                           // Cho phép giãn chiều ngang
        gbc.insets = new Insets(3, 2, 3, 2);    // Padding giữa các component

        colorButton = createToolbarButton("Chọn Màu", null);
        colorButton.setOpaque(true);
        updateColorButtonAppearance();
        colorButton.addActionListener(e -> chooseColor());
        toolbarPanel.add(colorButton, gbc);

        JLabel strokeLabel = new JLabel("Kích thước nét:");
        strokeLabel.setHorizontalAlignment(SwingConstants.LEFT);
        toolbarPanel.add(strokeLabel, gbc);

        strokeSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, currentStrokeSize);
        strokeSlider.setMajorTickSpacing(20);
        strokeSlider.setMinorTickSpacing(5);
        strokeSlider.setPaintTicks(false); // Tắt paintTicks để gọn hơn, label đã đủ
        strokeSlider.setPaintLabels(false);
        currentStrokeLabel = new JLabel(String.valueOf(currentStrokeSize), SwingConstants.RIGHT);
        currentStrokeLabel.setPreferredSize(new Dimension(25, currentStrokeLabel.getPreferredSize().height)); // Cố định chiều rộng label
        strokeSlider.addChangeListener(e -> {
            currentStrokeSize = strokeSlider.getValue();
            currentStrokeLabel.setText(String.valueOf(currentStrokeSize));
            drawingPanel.setCurrentStrokeSize(currentStrokeSize);
        });
        JPanel strokePanel = new JPanel(new BorderLayout(5,0));
        strokePanel.add(strokeSlider, BorderLayout.CENTER);
        strokePanel.add(currentStrokeLabel, BorderLayout.EAST);
        toolbarPanel.add(strokePanel, gbc);

        pencilButton = createToolbarButton(PENCIL_LABEL, "icons/pencil.png"); // Ví dụ icon
        pencilButton.addActionListener(e -> setPencilMode());
        toolbarPanel.add(pencilButton, gbc);

        eraserButton = createToolbarButton(ERASER_LABEL, "icons/eraser.png"); // Ví dụ icon
        eraserButton.addActionListener(e -> setEraserMode());
        toolbarPanel.add(eraserButton, gbc);

        handToolButton = createToolbarButton(HAND_TOOL_LABEL, "icons/hand.png"); // Ví dụ icon
        handToolButton.addActionListener(e -> toggleHandMode());
        toolbarPanel.add(handToolButton, gbc);

        importImageButton = createToolbarButton(IMPORT_IMAGE_LABEL, "icons/import_image.png"); // Ví dụ icon
        importImageButton.addActionListener(e -> importImage());
        toolbarPanel.add(importImageButton, gbc);

        // Không còn zoomPanel và các nút zoom ở đây

        JButton clearButton = createToolbarButton(CLEAR_LABEL, "icons/clear.png");
        clearButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn xóa toàn bộ bản vẽ không?",
                    "Xác nhận Xóa",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                drawingPanel.clearDrawing();
            }
        });
        gbc.weighty = 1.0; // Cho nút Clear đẩy xuống dưới cùng
        gbc.anchor = GridBagConstraints.SOUTH;
        toolbarPanel.add(clearButton, gbc);

        add(new JScrollPane(toolbarPanel), BorderLayout.WEST); // Cho toolbar vào scrollpane nếu quá dài

        // --- Drawing Panel (Center) ---
        drawingPanel = new DrawingPanel();
        // drawingPanel.setBackground(Color.WHITE); // Đã set trong DrawingPanel
        JScrollPane scrollPane = new JScrollPane(drawingPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        // scrollPane.setWheelScrollingEnabled(true); // DrawingPanel đã xử lý mouse wheel
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Thêm border cho scrollpane
        add(scrollPane, BorderLayout.CENTER);

        setPencilMode(); // Đặt chế độ mặc định là bút vẽ
    }

    private JButton createToolbarButton(String text, String iconPath) {
        JButton button = new JButton(text);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10); // Khoảng cách giữa icon và text
        button.setFocusPainted(false); // Tắt viền focus để gọn hơn
        // button.setPreferredSize(new Dimension(150, 30)); // Cân nhắc kích thước cố định
        if (iconPath != null) {
            try {
                ImageIcon icon = new ImageIcon(new ImageIcon(getClass().getResource(iconPath))
                        .getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
                button.setIcon(icon);
            } catch (Exception e) {
                System.err.println("Không thể tải icon: " + iconPath + " - " + e.getMessage());
            }
        }
        return button;
    }


    private void updateColorButtonAppearance() {
        colorButton.setBackground(currentColor);
        colorButton.setForeground(getContrastColor(currentColor));
    }

    private Color getContrastColor(Color color) {
        double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000.0;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    private void updateToolStates() {
        // Cập nhật trạng thái enable/disable của các nút công cụ
        pencilButton.setSelected(!eraserMode && !handMode);
        eraserButton.setSelected(eraserMode && !handMode);
        handToolButton.setSelected(handMode);

        // Cập nhật cursor dựa trên trạng thái hiện tại của DrawScreen (không phải DrawingPanel)
        // DrawingPanel sẽ tự quản lý cursor của nó khi isTemporaryPanMode là true.
        if (handMode) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else if (eraserMode) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)); // Hoặc một cursor tẩy tùy chỉnh
        } else { // Pencil mode
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)); // Hoặc một cursor bút vẽ tùy chỉnh
        }
        drawingPanel.setCursorBasedOnTool(); // Yêu cầu drawing panel cũng cập nhật cursor của nó
    }


    private void setPencilMode() {
        handMode = false;
        eraserMode = false;
        drawingPanel.setPanMode(false);
        drawingPanel.setEraserMode(false);
        drawingPanel.setCurrentColor(currentColor);
        System.out.println("Chế độ: Bút vẽ, Màu: " + currentColor);
        updateToolStates();
    }

    private void setEraserMode() {
        handMode = false;
        eraserMode = true;
        drawingPanel.setPanMode(false);
        drawingPanel.setEraserMode(true);
        System.out.println("Chế độ: Tẩy");
        updateToolStates();
    }

    private void toggleHandMode() {
        setHandMode(!this.handMode);
    }

    private void setHandMode(boolean enable) {
        this.handMode = enable;
        drawingPanel.setPanMode(this.handMode); // Kích hoạt/tắt pan mode trong DrawingPanel
        if (this.handMode) {
            this.eraserMode = false; // Tắt chế độ tẩy nếu đang ở hand mode
            drawingPanel.setEraserMode(false);
            System.out.println("Chế độ: Bàn Tay (Pan)");
        } else {
            // Khi tắt hand mode, quay lại chế độ bút vẽ mặc định
            // Hoặc có thể lưu trạng thái trước đó (bút/tẩy) và quay lại trạng thái đó
            setPencilMode();
        }
        updateToolStates();
    }


    private void chooseColor() {
        if (handMode) setHandMode(false); // Tắt hand mode nếu đang chọn màu
        Color newColor = JColorChooser.showDialog(this, "Chọn màu vẽ", currentColor);
        if (newColor != null) {
            currentColor = newColor;
            updateColorButtonAppearance();
            if (!eraserMode) { // Chỉ đặt màu cho drawingPanel nếu không ở chế độ tẩy
                drawingPanel.setCurrentColor(currentColor);
            }
        }
    }

    private void importImage() {
        if (handMode) setHandMode(false); // Tắt hand mode
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file ảnh để chèn");
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Ảnh JPG & PNG", "jpg", "jpeg", "png");
        fileChooser.addChoosableFileFilter(filter);

        int returnValue = fileChooser.showOpenDialog(mainFrame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage importedImage = ImageIO.read(selectedFile);
                if (importedImage != null) {
                    // Thay vì resize canvas theo ảnh, ta sẽ chèn ảnh vào canvas hiện tại
                    // và cho phép người dùng di chuyển/resize ảnh đó sau (nếu có tính năng đó)
                    // Hiện tại, chỉ đơn giản là vẽ ảnh lên canvas tại vị trí zoom hiện tại
                    drawingPanel.importImage(importedImage); // Sử dụng phương thức mới trong DrawingPanel
                } else {
                    JOptionPane.showMessageDialog(this, "Không thể đọc file ảnh đã chọn.", "Lỗi Chèn Ảnh", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi khi đọc file ảnh: " + ex.getMessage(), "Lỗi Chèn Ảnh", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void setDrawingNote(Note note) {
        setHandMode(false); // Luôn tắt hand mode khi set note mới
        drawingPanel.resetZoomAndPan(); // Reset zoom và pan khi mở note mới

        if (note != null && note.getNoteType() == Note.NoteType.DRAWING) {
            this.currentDrawingNote = note;
            titleField.setText(note.getTitle());
            if (note.getDrawingData() != null && !note.getDrawingData().isEmpty()) {
                try {
                    // Load ảnh và resize canvas THEO KÍCH THƯỚC CỦA ẢNH ĐÃ LƯU
                    drawingPanel.loadImageFromBase64AndResizeCanvas(note.getDrawingData());
                } catch (IOException e) {
                    System.err.println("Lỗi khi tải dữ liệu bản vẽ: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu bản vẽ. Tạo bản vẽ mới.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    drawingPanel.clearDrawingAndResizeCanvas(800, 600); // Kích thước mặc định
                }
            } else {
                // Note drawing nhưng không có data, tạo canvas mới
                drawingPanel.clearDrawingAndResizeCanvas(800, 600);
            }
        } else {
            // Tạo một note drawing mới nếu note là null hoặc không phải DRAWING
            this.currentDrawingNote = new Note("Bản vẽ " + System.currentTimeMillis() % 10000, Note.NoteType.DRAWING, controller.getCurrentFolder());
            titleField.setText(this.currentDrawingNote.getTitle());
            drawingPanel.clearDrawingAndResizeCanvas(800, 600); // Kích thước mặc định cho bản vẽ mới
        }

        // Reset công cụ về trạng thái mặc định
        currentColor = Color.BLACK;
        updateColorButtonAppearance();
        currentStrokeSize = 3;
        strokeSlider.setValue(currentStrokeSize);
        // currentStrokeLabel đã được cập nhật bởi listener của slider
        setPencilMode(); // Đặt lại chế độ bút vẽ
        drawingPanel.setCurrentColor(currentColor); // Đảm bảo drawingPanel nhận màu mới
        drawingPanel.setCurrentStrokeSize(currentStrokeSize); // và kích thước nét mới
    }

    private void saveDrawing() {
        if (handMode) setHandMode(false); // Tắt hand mode trước khi lưu
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tiêu đề không được để trống.", "Lỗi Lưu", JOptionPane.WARNING_MESSAGE);
            titleField.requestFocus();
            return;
        }

        try {
            String base64Image = drawingPanel.getImageAsBase64("png");
            if (base64Image == null || base64Image.isEmpty()) {
                // Hỏi người dùng nếu họ muốn lưu bản vẽ trống
                int choice = JOptionPane.showConfirmDialog(this,
                        "Bản vẽ hiện tại trống. Bạn vẫn muốn lưu nó với tiêu đề này không?",
                        "Lưu Bản Vẽ Trống?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (choice == JOptionPane.NO_OPTION) {
                    return; // Không lưu nếu người dùng chọn No
                }
                // Nếu chọn Yes, base64Image sẽ là null, NoteController cần xử lý việc này
                // Hoặc tạo một ảnh trắng nhỏ để lưu
                // For now, we allow saving an "empty" drawing note if user confirms
            }


            if (currentDrawingNote == null || currentDrawingNote.getId() == 0) { // Note mới
                currentDrawingNote.setTitle(title);
                currentDrawingNote.setDrawingData(base64Image); // Có thể là null nếu bản vẽ trống và người dùng đồng ý
                currentDrawingNote.setNoteType(Note.NoteType.DRAWING);
                currentDrawingNote.setContent(null); // Drawing notes không có content text

                // Đảm bảo note được gán vào một thư mục
                if (currentDrawingNote.getFolder() == null || currentDrawingNote.getFolder().getId() == 0) {
                    Folder targetFolder = controller.getCurrentFolder();
                    if (targetFolder == null) { // Nếu không có current folder, thử lấy root
                        targetFolder = controller.getFolderByName("Root").orElse(null);
                    }
                    if (targetFolder == null) { // Nếu vẫn không có, tạo root (hoặc báo lỗi)
                        // Đây là một tình huống cần xử lý cẩn thận hơn trong NoteController
                        // Hoặc yêu cầu người dùng chọn thư mục
                        JOptionPane.showMessageDialog(this, "Không tìm thấy thư mục để lưu. Vui lòng tạo thư mục trước.", "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    currentDrawingNote.setFolder(targetFolder);
                }
                controller.addNote(currentDrawingNote);
                JOptionPane.showMessageDialog(this, "Bản vẽ '" + title + "' đã được lưu!", "Lưu Thành Công", JOptionPane.INFORMATION_MESSAGE);

            } else { // Cập nhật note hiện có
                currentDrawingNote.setTitle(title);
                currentDrawingNote.setDrawingData(base64Image);
                // currentDrawingNote.setNoteType(Note.NoteType.DRAWING); // Loại note không đổi
                currentDrawingNote.setContent(null);
                controller.updateNote(currentDrawingNote);
                JOptionPane.showMessageDialog(this, "Bản vẽ '" + title + "' đã được cập nhật!", "Cập Nhật Thành Công", JOptionPane.INFORMATION_MESSAGE);
            }
            mainFrame.showMainMenuScreen();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu bản vẽ: " + e.getMessage(), "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- DrawingPanel Inner Class ---
    // (Giữ nguyên phần lớn logic của DrawingPanel, chỉ điều chỉnh nhỏ nếu cần)
    private static class DrawingPanel extends JPanel {
        private BufferedImage canvasImage;
        private Graphics2D g2dCanvas;

        private Color currentColor = Color.BLACK;
        private int currentStrokeSize = 3;
        private boolean isEraserMode = false;
        private boolean isPanMode = false; // Chế độ Pan chính thức (khi nhấn nút Hand)
        private boolean isTemporaryPanMode = false; // Chế độ Pan tạm thời (khi giữ ALT)

        private Point lastMousePoint; // Tọa độ canvas của điểm chuột cuối cùng
        private Point panStartPointScreen; // Tọa độ màn hình khi bắt đầu pan

        private double scaleFactor = 1.0;
        private double offsetX = 0; // Offset của canvasImage so với góc trên trái của JPanel
        private double offsetY = 0;
        private static final double ZOOM_INCREMENT = 0.1; // Tăng/giảm 10% mỗi lần zoom
        private static final double MIN_ZOOM = 0.1;     // Giới hạn zoom nhỏ nhất
        private static final double MAX_ZOOM = 10.0;    // Giới hạn zoom lớn nhất

        public DrawingPanel() {
            setBackground(Color.LIGHT_GRAY); // Màu nền cho vùng JPanel bên ngoài canvasImage
            clearDrawingAndResizeCanvas(800, 600); // Khởi tạo canvas trắng

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow(); // Đảm bảo panel có focus để nhận key events
                    if (isPanModeActive()) {
                        panStartPointScreen = e.getPoint(); // Lưu điểm bắt đầu pan trên màn hình
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    } else {
                        lastMousePoint = transformScreenPointToCanvas(e.getPoint());
                        if (g2dCanvas != null) {
                            g2dCanvas.setColor(isEraserMode ? Color.WHITE : currentColor);
                            g2dCanvas.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            // Vẽ một điểm tròn nhỏ để bắt đầu nét vẽ mượt hơn
                            g2dCanvas.fillOval(lastMousePoint.x - currentStrokeSize / 2, lastMousePoint.y - currentStrokeSize / 2, currentStrokeSize, currentStrokeSize);
                            repaint();
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isPanModeActive()) {
                        panStartPointScreen = null;
                        if (!isPanMode) { // Nếu là temporary pan mode, quay lại cursor của tool trước đó
                            setCursorBasedOnTool();
                        }
                    } else {
                        lastMousePoint = null;
                    }
                }
            };
            addMouseListener(mouseAdapter);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isPanModeActive() && panStartPointScreen != null) {
                        int dxScreen = e.getX() - panStartPointScreen.x;
                        int dyScreen = e.getY() - panStartPointScreen.y;
                        offsetX += dxScreen; // Offset thay đổi trực tiếp theo di chuyển màn hình
                        offsetY += dyScreen;
                        panStartPointScreen = e.getPoint();
                        repaint();
                    } else if (!isPanModeActive() && lastMousePoint != null && g2dCanvas != null) {
                        Point currentCanvasPoint = transformScreenPointToCanvas(e.getPoint());
                        g2dCanvas.setColor(isEraserMode ? Color.WHITE : currentColor);
                        g2dCanvas.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2dCanvas.drawLine(lastMousePoint.x, lastMousePoint.y, currentCanvasPoint.x, currentCanvasPoint.y);
                        lastMousePoint = currentCanvasPoint;
                        repaint();
                    }
                }
            });

            addMouseWheelListener(e -> {
                if (e.isControlDown()) { // Chỉ zoom khi giữ Ctrl
                    Point mousePosOnPanel = e.getPoint(); // Vị trí chuột trên panel
                    int rotation = e.getWheelRotation();
                    double zoomFactor = (rotation < 0) ? (1 + ZOOM_INCREMENT) : (1 / (1 + ZOOM_INCREMENT));
                    applyZoom(zoomFactor, mousePosOnPanel);
                    e.consume(); // Ngăn JScrollPane cuộn khi đang zoom
                } else {
                    // Nếu không giữ Ctrl, cho phép JScrollPane xử lý cuộn
                    // Điều này quan trọng nếu canvas lớn hơn vùng nhìn thấy
                    getParent().dispatchEvent(e);
                }
            });
        }

        public void setCursorBasedOnTool() {
            DrawScreen parentDrawScreen = (DrawScreen) SwingUtilities.getAncestorOfClass(DrawScreen.class, this);
            if (parentDrawScreen != null) {
                if (isPanModeActive()) { // Ưu tiên cursor pan nếu đang pan
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (parentDrawScreen.eraserMode) {
                    // Cân nhắc tạo custom cursor cho tẩy
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                } else { // Pencil mode
                    // Cân nhắc tạo custom cursor cho bút
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            } else {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }


        public boolean isPanModeActive() { return isPanMode || isTemporaryPanMode; }
        public boolean isTemporaryPanMode() { return isTemporaryPanMode; }


        public void setPanMode(boolean panMode) {
            this.isPanMode = panMode;
            if (panMode) {
                this.isTemporaryPanMode = false; // Tắt temporary pan nếu pan chính thức được bật
            }
            setCursorBasedOnTool();
        }

        public void setTemporaryPanMode(boolean temporaryPanMode) {
            // Chỉ cho phép bật temporary pan nếu pan chính thức đang tắt
            if (!isPanMode) {
                this.isTemporaryPanMode = temporaryPanMode;
            } else if (isPanMode && !temporaryPanMode) {
                // Nếu pan chính thức đang bật, không thể tắt temporary pan (vì nó đã bị vô hiệu hóa)
                // Tuy nhiên, nếu logic bên ngoài gọi setTemporaryPanMode(false) khi isPanMode là true,
                // thì isTemporaryPanMode vẫn nên là false.
                this.isTemporaryPanMode = false;
            }
            setCursorBasedOnTool();
        }


        private Point transformScreenPointToCanvas(Point screenPoint) {
            // Chuyển đổi tọa độ từ màn hình (view) sang tọa độ của canvasImage
            int canvasX = (int) ((screenPoint.x - offsetX) / scaleFactor);
            int canvasY = (int) ((screenPoint.y - offsetY) / scaleFactor);
            return new Point(canvasX, canvasY);
        }

        private Point2D.Double transformScreenPointToCanvasDouble(Point screenPoint, double currentScale, double currentOffsetX, double currentOffsetY) {
            // Phiên bản double cho độ chính xác cao hơn khi tính toán zoom
            double canvasX = (screenPoint.x - currentOffsetX) / currentScale;
            double canvasY = (screenPoint.y - currentOffsetY) / currentScale;
            return new Point2D.Double(canvasX, canvasY);
        }

        public void setCurrentColor(Color color) { this.currentColor = color; }
        public void setCurrentStrokeSize(int size) { this.currentStrokeSize = Math.max(1, size); }
        public void setEraserMode(boolean isEraser) { this.isEraserMode = isEraser; }

        private void resizeCanvas(int newWidth, int newHeight, boolean copyOldContent) {
            if (newWidth <= 0 || newHeight <= 0) {
                System.err.println("Cảnh báo: Kích thước canvas không hợp lệ: " + newWidth + "x" + newHeight + ". Sử dụng kích thước mặc định.");
                newWidth = Math.max(100, newWidth); // Kích thước tối thiểu
                newHeight = Math.max(100, newHeight);
            }

            BufferedImage newCanvas = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D newG2d = newCanvas.createGraphics();
            newG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            newG2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE); // Nét vẽ mượt hơn
            newG2d.setColor(Color.WHITE); // Nền của canvasImage luôn là trắng
            newG2d.fillRect(0, 0, newWidth, newHeight);

            if (copyOldContent && canvasImage != null) {
                // Vẽ lại nội dung cũ lên canvas mới, có thể cần transform nếu kích thước thay đổi nhiều
                newG2d.drawImage(canvasImage, 0, 0, null);
            }

            if (g2dCanvas != null) {
                g2dCanvas.dispose(); // Giải phóng Graphics2D cũ
            }
            canvasImage = newCanvas;
            g2dCanvas = newG2d;

            // Cập nhật preferredSize để JScrollPane biết kích thước mới của canvas
            setPreferredSize(new Dimension(newWidth, newHeight));
            revalidate(); // Yêu cầu layout lại
            repaint();
            System.out.println("Canvas resized to: " + newWidth + "x" + newHeight);
        }


        public void clearDrawingAndResizeCanvas(int width, int height) {
            resizeCanvas(width, height, false); // false để không copy nội dung cũ
        }

        // Được gọi khi load note có drawing data
        public void loadImageFromBase64AndResizeCanvas(String base64Image) throws IOException {
            if (base64Image == null || base64Image.isEmpty()) {
                System.out.println("Dữ liệu bản vẽ trống, tạo canvas mặc định.");
                clearDrawingAndResizeCanvas(800, 600); // Kích thước mặc định
                return;
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                BufferedImage loadedImage = ImageIO.read(bais);
                if (loadedImage != null) {
                    // Resize canvas theo kích thước của ảnh đã load
                    resizeCanvas(loadedImage.getWidth(), loadedImage.getHeight(), false);
                    if (g2dCanvas != null) {
                        g2dCanvas.drawImage(loadedImage, 0, 0, null);
                        repaint();
                    }
                } else {
                    throw new IOException("Không thể giải mã dữ liệu ảnh từ Base64 (ImageIO.read trả về null).");
                }
            }
        }


        public void clearDrawing() {
            if (g2dCanvas != null && canvasImage != null) {
                g2dCanvas.setColor(Color.WHITE); // Nền của canvasImage luôn là trắng
                g2dCanvas.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
                repaint();
            }
        }

        // Được gọi khi người dùng chọn "Chèn Ảnh"
        public void importImage(BufferedImage importedImage) {
            if (importedImage == null) return;
            if (g2dCanvas == null || canvasImage == null) {
                // Nếu canvas chưa được khởi tạo đúng cách, thử resize theo ảnh chèn vào
                System.err.println("Canvas chưa sẵn sàng, thử tạo canvas theo kích thước ảnh chèn.");
                resizeCanvas(importedImage.getWidth(), importedImage.getHeight(), false);
                if (g2dCanvas == null) { // Nếu vẫn null sau khi resize thì có lỗi nghiêm trọng
                    JOptionPane.showMessageDialog(this, "Không thể chèn ảnh, canvas không sẵn sàng.", "Lỗi Canvas", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Tính toán vị trí để chèn ảnh vào giữa vùng nhìn thấy hiện tại của canvas
            // (đã được transform bởi offsetX, offsetY, scaleFactor)
            // Tọa độ (0,0) của vùng nhìn thấy trên panel tương ứng với điểm nào trên canvas?
            Point viewTopLeftOnCanvas = transformScreenPointToCanvas(new Point(0,0));
            // Tọa độ (getWidth(), getHeight()) của vùng nhìn thấy trên panel
            Point viewBottomRightOnCanvas = transformScreenPointToCanvas(new Point(getWidth(), getHeight()));

            // Kích thước vùng nhìn thấy trên canvas
            int viewWidthOnCanvas = viewBottomRightOnCanvas.x - viewTopLeftOnCanvas.x;
            int viewHeightOnCanvas = viewBottomRightOnCanvas.y - viewTopLeftOnCanvas.y;

            // Chèn ảnh vào giữa vùng nhìn thấy này
            int drawX = viewTopLeftOnCanvas.x + (viewWidthOnCanvas - importedImage.getWidth()) / 2;
            int drawY = viewTopLeftOnCanvas.y + (viewHeightOnCanvas - importedImage.getHeight()) / 2;

            g2dCanvas.drawImage(importedImage, drawX, drawY, null);
            repaint();
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create(); // Tạo bản sao để không ảnh hưởng context gốc

            // Áp dụng transform cho việc pan và zoom
            AffineTransform at = new AffineTransform();
            at.translate(offsetX, offsetY);
            at.scale(scaleFactor, scaleFactor);
            g2.transform(at);

            // Vẽ canvasImage
            if (canvasImage != null) {
                g2.drawImage(canvasImage, 0, 0, this);
            } else if (getWidth() > 0 && getHeight() > 0) {
                // Trường hợp canvasImage bị null nhưng panel có kích thước
                // -> Khởi tạo lại canvas với kích thước mặc định
                System.err.println("canvasImage is null in paintComponent, re-initializing.");
                clearDrawingAndResizeCanvas(800, 600); // Hoặc kích thước panel hiện tại
                if (canvasImage != null) { // Vẽ lại sau khi khởi tạo
                    g2.drawImage(canvasImage, 0, 0, this);
                }
            }
            g2.dispose(); // Giải phóng context đã tạo
        }


        public String getImageAsBase64(String formatName) throws IOException {
            if (canvasImage == null) {
                System.out.println("canvasImage is null, không thể lấy Base64.");
                return null;
            }

            // Kiểm tra xem canvas có thực sự trống không (toàn màu trắng)
            boolean isEmpty = true;
            if (canvasImage.getWidth() > 0 && canvasImage.getHeight() > 0) {
                int whiteRgb = Color.WHITE.getRGB();
                // Kiểm tra một số điểm ảnh đại diện thay vì toàn bộ để tăng tốc
                outerLoop:
                for (int x = 0; x < canvasImage.getWidth(); x += Math.max(1, canvasImage.getWidth() / 20)) { // Kiểm tra 20 điểm theo chiều X
                    for (int y = 0; y < canvasImage.getHeight(); y += Math.max(1, canvasImage.getHeight() / 20)) { // Kiểm tra 20 điểm theo chiều Y
                        if (canvasImage.getRGB(x, y) != whiteRgb) {
                            isEmpty = false;
                            break outerLoop; // Thoát cả hai vòng lặp
                        }
                    }
                }
            }

            if (isEmpty) {
                System.out.println("Bản vẽ được coi là trống (toàn màu trắng), không tạo Base64.");
                return null; // Trả về null nếu bản vẽ trống
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (!ImageIO.write(canvasImage, formatName, baos)) {
                    throw new IOException("Không tìm thấy ImageWriter phù hợp cho định dạng: " + formatName);
                }
                byte[] imageBytes = baos.toByteArray();
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        }

        public void zoomIn() {
            Point center = new Point(getWidth() / 2, getHeight() / 2); // Zoom vào giữa panel
            applyZoom(1 + ZOOM_INCREMENT * 2, center); // Zoom mạnh hơn một chút
        }

        public void zoomOut() {
            Point center = new Point(getWidth() / 2, getHeight() / 2);
            applyZoom(1 / (1 + ZOOM_INCREMENT * 2), center);
        }

        private void applyZoom(double factorChange, Point anchorScreen) {
            // anchorScreen là điểm trên panel mà người dùng muốn giữ cố định khi zoom
            double oldScale = scaleFactor;
            double newScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, scaleFactor * factorChange));

            if (Math.abs(newScale - oldScale) < 0.001) return; // Không thay đổi đáng kể

            // Tọa độ của điểm neo trên canvas TRƯỚC KHI zoom
            Point2D.Double anchorCanvasOld = transformScreenPointToCanvasDouble(anchorScreen, oldScale, offsetX, offsetY);

            // Cập nhật scaleFactor
            scaleFactor = newScale;

            // Tính toán offsetX, offsetY mới để điểm neo trên canvas vẫn nằm ở vị trí anchorScreen trên panel
            offsetX = anchorScreen.x - anchorCanvasOld.x * scaleFactor;
            offsetY = anchorScreen.y - anchorCanvasOld.y * scaleFactor;

            System.out.println("Applied Zoom: " + scaleFactor + ", OffsetX: " + offsetX + ", OffsetY: " + offsetY);
            repaint();
        }


        public void resetZoomAndPan() {
            scaleFactor = 1.0;
            if (canvasImage != null) {
                // Căn giữa canvasImage trong vùng nhìn của panel
                offsetX = (getWidth() - canvasImage.getWidth() * scaleFactor) / 2.0;
                offsetY = (getHeight() - canvasImage.getHeight() * scaleFactor) / 2.0;
            } else {
                offsetX = 0;
                offsetY = 0;
            }
            System.out.println("Zoom/Pan Reset. Scale: " + scaleFactor + ", OffsetX: " + offsetX + ", OffsetY: " + offsetY);
            repaint();
        }
    }
}