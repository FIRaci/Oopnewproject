// File: DrawScreen.java
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class DrawScreen extends JPanel {
    private static final String SAVE_LABEL = "Lưu Bản Vẽ";
    private static final String BACK_LABEL = "Quay Lại";
    private static final String CLEAR_LABEL = "Xóa Hết";
    private static final String ERASER_LABEL = "Tẩy";
    private static final String PENCIL_LABEL = "Bút Vẽ"; // Changed for clarity
    private static final String SCAN_OCR_LABEL = "Quét Văn Bản";


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
    // private JPanel toolbarInnerPanel; // Sẽ không cần trực tiếp nữa nếu dùng GridBagLayout cho toolbarOuterPanel

    private Color currentColor = Color.BLACK;
    private int currentStrokeSize = 3;
    private boolean eraserMode = false;

    public DrawScreen(MainFrame mainFrame, NoteController controller) {
        this.mainFrame = mainFrame;
        this.controller = controller;
        initializeUI();
        setDrawingNote(null);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(15, 15)); // Increased gaps
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Overall padding

        // Top Panel: Title and Main Buttons
        JPanel topControlPanel = new JPanel(new BorderLayout(10, 5));
        topControlPanel.setBorder(BorderFactory.createEmptyBorder(0,0,10,0)); // Bottom margin

        JPanel titlePanel = new JPanel(new BorderLayout(5,0));
        JLabel titleLabel = new JLabel("Tiêu đề:");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titleField = new JTextField("Bản vẽ không tên");
        titleField.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        titlePanel.add(titleField, BorderLayout.CENTER);
        topControlPanel.add(titlePanel, BorderLayout.CENTER);


        JPanel mainButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton saveButton = new JButton(SAVE_LABEL);
        saveButton.setToolTipText("Lưu bản vẽ hiện tại");
        saveButton.addActionListener(e -> saveDrawing());
        JButton backButton = new JButton(BACK_LABEL);
        backButton.setToolTipText("Quay lại màn hình chính (Esc)");
        backButton.addActionListener(e -> mainFrame.showMainMenuScreen());
        mainButtonsPanel.add(saveButton);
        mainButtonsPanel.add(backButton);
        topControlPanel.add(mainButtonsPanel, BorderLayout.EAST);
        add(topControlPanel, BorderLayout.NORTH);

        // Toolbar Panel (West)
        // Sử dụng trực tiếp GridBagLayout cho panel chính của toolbar
        JPanel toolbarPanel = new JPanel(new GridBagLayout());
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Công cụ vẽ"),
                new EmptyBorder(5, 8, 5, 8) // Padding inside TitledBorder
        ));
        toolbarPanel.setPreferredSize(new Dimension(200, 0)); // Điều chỉnh chiều rộng nếu cần

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Mỗi component chiếm một hàng
        gbc.fill = GridBagConstraints.HORIZONTAL;    // Cho component giãn theo chiều ngang
        gbc.weightx = 1.0;                           // Cho phép giãn chiều ngang
        gbc.insets = new Insets(5, 0, 5, 0);    // Padding trên dưới cho mỗi component

        // Color Chooser Button
        colorButton = new JButton("Chọn Màu");
        colorButton.setOpaque(true); // Necessary for background to show
        setButtonColor(currentColor);
        colorButton.addActionListener(e -> chooseColor());
        toolbarPanel.add(colorButton, gbc);

        // Stroke Size Slider
        JLabel strokeTitleLabel = new JLabel("Kích thước nét:");
        // strokeTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Không cần với GridBagLayout
        toolbarPanel.add(strokeTitleLabel, gbc);

        JPanel strokePanel = new JPanel(new BorderLayout(5,0));
        strokePanel.setOpaque(false); // Make transparent if toolbar has own bg
        strokeSlider = new JSlider(JSlider.HORIZONTAL, 1, 50, currentStrokeSize);
        strokeSlider.setMajorTickSpacing(10);
        strokeSlider.setMinorTickSpacing(1);
        strokeSlider.setPaintTicks(true);
        currentStrokeLabel = new JLabel(String.valueOf(currentStrokeSize), SwingConstants.CENTER);
        currentStrokeLabel.setPreferredSize(new Dimension(25, currentStrokeLabel.getPreferredSize().height)); // Fixed width for label
        strokeSlider.addChangeListener(e -> {
            currentStrokeSize = strokeSlider.getValue();
            currentStrokeLabel.setText(String.valueOf(currentStrokeSize));
            drawingPanel.setCurrentStrokeSize(currentStrokeSize);
        });
        strokePanel.add(strokeSlider, BorderLayout.CENTER);
        strokePanel.add(currentStrokeLabel, BorderLayout.EAST);
        // strokePanel.setAlignmentX(Component.CENTER_ALIGNMENT); // Không cần
        toolbarPanel.add(strokePanel, gbc);

        // Pencil Button
        pencilButton = new JButton(PENCIL_LABEL);
        // pencilButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Không cần
        pencilButton.addActionListener(e -> setPencilMode());
        toolbarPanel.add(pencilButton, gbc);

        // Eraser Button
        eraserButton = new JButton(ERASER_LABEL);
        // eraserButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Không cần
        eraserButton.addActionListener(e -> setEraserMode());
        toolbarPanel.add(eraserButton, gbc);

        // Scan Button
        JButton scanButton = new JButton(SCAN_OCR_LABEL);
        // scanButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Không cần
        scanButton.addActionListener(e -> scanCanvasImageWithOCR());
        toolbarPanel.add(scanButton, gbc);


        // Clear Button - sẽ được đẩy xuống dưới cùng
        JButton clearButton = new JButton(CLEAR_LABEL);
        // clearButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Không cần

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

        // Để đẩy nút Clear xuống dưới, ta thêm một component co giãn ở giữa
        gbc.weighty = 1.0; // Cho phép component này co giãn theo chiều dọc
        gbc.fill = GridBagConstraints.VERTICAL;
        toolbarPanel.add(Box.createVerticalGlue(), gbc);
        gbc.weighty = 0; // Reset weighty
        gbc.fill = GridBagConstraints.HORIZONTAL; // Quay lại fill ngang cho nút Clear

        toolbarPanel.add(clearButton, gbc); // Thêm nút Clear ở cuối

        // Thêm toolbarPanel vào JScrollPane để có thể cuộn nếu nội dung quá dài
        JScrollPane toolbarScrollPane = new JScrollPane(toolbarPanel);
        toolbarScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        toolbarScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        toolbarScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Bỏ viền của JScrollPane
        add(toolbarScrollPane, BorderLayout.WEST);


        // Main Drawing Area
        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE); // Default background
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Border for drawing area
        add(new JScrollPane(drawingPanel) {{
            setBorder(BorderFactory.createEmptyBorder()); // Remove scroll pane's own border
        }}, BorderLayout.CENTER);

        updateToolStates();
    }

    private BufferedImage preprocessImageForOCR(BufferedImage input) {
        if (input == null) return null;
        int width = input.getWidth();
        int height = input.getHeight();
        if (width <= 0 || height <= 0) return null;


        // Convert to grayscale
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();

        // Get pixel data for Otsu's threshold
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int grayVal = gray.getRaster().getSample(x, y, 0);
                histogram[grayVal]++;
            }
        }

        // Compute Otsu's threshold
        int total = width * height;
        float sum = 0;
        for (int t = 0; t < 256; t++) sum += t * histogram[t];

        float sumB = 0, wB = 0, wF;
        float max = 0;
        int threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += histogram[t];
            if (wB == 0) continue;

            wF = total - wB;
            if (wF == 0) break;

            sumB += (float)(t * histogram[t]);
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;

            float between = wB * wF * (mB - mF) * (mB - mF);
            if (between > max) {
                max = between;
                threshold = t;
            }
        }

        // Apply binary threshold
        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int grayVal = gray.getRaster().getSample(x, y, 0);
                int binaryVal = grayVal > threshold ? 0xFFFFFF : 0x000000; // Trắng cho nền, đen cho chữ
                binary.setRGB(x, y, binaryVal);
            }
        }
        return binary;
    }


    private void setButtonColor(Color color) {
        colorButton.setBackground(color);
        colorButton.setForeground(getContrastColor(color));
    }

    private Color getContrastColor(Color color) {
        double y = (299 * color.getRed() + 587 * color.getGreen() + 114 * color.getBlue()) / 1000.0;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    // Đây là phương thức trong lớp DrawScreen (không phải static!)
    private void scanCanvasImageWithOCR() {
        BufferedImage canvasRaw = drawingPanel.getCanvasImage();
        if (canvasRaw == null || canvasRaw.getWidth() <= 0 || canvasRaw.getHeight() <= 0) {
            JOptionPane.showMessageDialog(this, "Bản vẽ trống hoặc không hợp lệ để quét.", "Lỗi OCR", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BufferedImage canvasForOcr = preprocessImageForOCR(canvasRaw);
        if (canvasForOcr == null) {
            JOptionPane.showMessageDialog(this, "Không thể tiền xử lý ảnh cho OCR.", "Lỗi OCR", JOptionPane.WARNING_MESSAGE);
            return;
        }


        try {
            Tesseract tesseract = new Tesseract();
            // Giả sử thư mục tessdata nằm cùng cấp với thư mục classes của bạn sau khi build
            // Hoặc bạn có thể đặt đường dẫn tuyệt đối hoặc cấu hình qua biến môi trường TESSDATA_PREFIX
            File tessDataFolder = new File("tessdata"); // Cần đảm bảo đường dẫn này đúng
            if (!tessDataFolder.exists() || !tessDataFolder.isDirectory()) {
                // Thử tìm trong resources nếu chạy từ IDE hoặc JAR
                try {
                    tessDataFolder = new File(getClass().getClassLoader().getResource("tessdata").toURI());
                } catch (Exception e) {
                    tessDataFolder = null; // Reset nếu không tìm thấy
                }
            }

            if (tessDataFolder == null || !tessDataFolder.exists()) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy thư mục tessdata. OCR không thể hoạt động.", "Lỗi Cấu Hình OCR", JOptionPane.ERROR_MESSAGE);
                return;
            }

            tesseract.setDatapath(tessDataFolder.getParentFile().getAbsolutePath()); // Tesseract cần thư mục cha của tessdata
            tesseract.setLanguage("eng+vie"); // Đặt ngôn ngữ

            String result = tesseract.doOCR(canvasForOcr);
            if (result.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không phát hiện văn bản.", "Kết quả OCR", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);

                JTextArea textArea = new JTextArea(result);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(500, 300));
                JOptionPane.showMessageDialog(this, scrollPane, "Kết quả OCR (đã copy vào clipboard)", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (TesseractException e) {
            JOptionPane.showMessageDialog(this, "Lỗi Tesseract OCR: " + e.getMessage() + "\nKiểm tra cấu hình Tesseract và đường dẫn tessdata.", "Lỗi OCR", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        catch (Exception e) { // Bắt các lỗi chung khác
            JOptionPane.showMessageDialog(this, "Lỗi không xác định khi quét OCR: " + e.getMessage(), "Lỗi OCR", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateToolStates() {
        pencilButton.setEnabled(eraserMode);
        eraserButton.setEnabled(!eraserMode);
        // Visually indicate active tool (e.g., thicker border or different background)
        pencilButton.setBorder(eraserMode ? UIManager.getBorder("Button.border") : BorderFactory.createLineBorder(Color.BLUE, 2));
        eraserButton.setBorder(!eraserMode ? UIManager.getBorder("Button.border") : BorderFactory.createLineBorder(Color.BLUE, 2));

    }

    private void setPencilMode() {
        eraserMode = false;
        drawingPanel.setEraserMode(false);
        drawingPanel.setCurrentColor(currentColor);
        updateToolStates();
    }

    private void setEraserMode() {
        eraserMode = true;
        drawingPanel.setEraserMode(true);
        updateToolStates();
    }


    private void chooseColor() {
        Color newColor = JColorChooser.showDialog(this, "Chọn màu vẽ", currentColor);
        if (newColor != null) {
            currentColor = newColor;
            setButtonColor(currentColor);
            if (!eraserMode) {
                drawingPanel.setCurrentColor(currentColor);
            }
        }
    }

    public void setDrawingNote(Note note) {
        this.currentDrawingNote = (note != null && note.getNoteType() == Note.NoteType.DRAWING) ?
                note :
                new Note("Bản vẽ " + System.currentTimeMillis()%10000, Note.NoteType.DRAWING, controller.getCurrentFolder());

        titleField.setText(this.currentDrawingNote.getTitle());
        if (this.currentDrawingNote.getDrawingData() != null && !this.currentDrawingNote.getDrawingData().isEmpty()) {
            try {
                drawingPanel.loadImageFromBase64(this.currentDrawingNote.getDrawingData());
            } catch (IOException e) {
                System.err.println("Lỗi khi tải dữ liệu bản vẽ: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Không thể tải dữ liệu bản vẽ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                drawingPanel.clearDrawing();
            }
        } else {
            drawingPanel.clearDrawing(); // Xóa canvas nếu không có dữ liệu hoặc note mới
        }

        currentColor = Color.BLACK;
        setButtonColor(currentColor);
        currentStrokeSize = 3;
        strokeSlider.setValue(currentStrokeSize);
        currentStrokeLabel.setText(String.valueOf(currentStrokeSize));
        setPencilMode(); // Đặt lại chế độ bút vẽ
        drawingPanel.setCurrentColor(currentColor); // Đảm bảo drawingPanel nhận màu mới
        drawingPanel.setCurrentStrokeSize(currentStrokeSize); // và kích thước nét mới
    }

    private void saveDrawing() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tiêu đề không được để trống.", "Lỗi Lưu", JOptionPane.WARNING_MESSAGE);
            titleField.requestFocus();
            return;
        }

        try {
            String base64Image = drawingPanel.getImageAsBase64("png");
            // Cho phép lưu bản vẽ trống nếu người dùng muốn, nhưng NoteController cần xử lý
            // if (base64Image == null || base64Image.isEmpty()) {
            //     JOptionPane.showMessageDialog(this, "Không có gì để lưu (bản vẽ trống).", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            //     return;
            // }

            boolean isNew = (currentDrawingNote.getId() == 0); // Kiểm tra ID để xác định note mới

            currentDrawingNote.setTitle(title);
            currentDrawingNote.setDrawingData(base64Image); // base64Image có thể là null nếu bản vẽ trống
            currentDrawingNote.setNoteType(Note.NoteType.DRAWING); // Đảm bảo đúng loại
            currentDrawingNote.setContent(null); // Bản vẽ không có content text

            if (isNew) {
                // Đảm bảo note mới được gán vào một thư mục
                if (currentDrawingNote.getFolderId() <= 0 && controller.getCurrentFolder() != null) {
                    currentDrawingNote.setFolder(controller.getCurrentFolder());
                } else if (currentDrawingNote.getFolderId() <= 0) {
                    // Cố gắng lấy thư mục Root nếu không có current folder
                    Folder rootFolder = controller.getFolderByName("Root").orElse(null);
                    if (rootFolder != null) {
                        currentDrawingNote.setFolder(rootFolder);
                    } else {
                        JOptionPane.showMessageDialog(this, "Không thể xác định thư mục. Vui lòng tạo thư mục 'Root'.", "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
                        return; // Không lưu nếu không có thư mục
                    }
                }
                controller.addNote(currentDrawingNote); // Controller sẽ xử lý việc lưu note mới
            } else {
                controller.updateExistingNote(currentDrawingNote.getId(), currentDrawingNote); // Controller cập nhật note
            }

            String message = isNew ? "Bản vẽ '" + title + "' đã được lưu!" : "Bản vẽ '" + title + "' đã được cập nhật!";
            JOptionPane.showMessageDialog(this, message, "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            mainFrame.showMainMenuScreen(); // Quay về màn hình chính sau khi lưu

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi lưu bản vẽ: " + e.getMessage(), "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) { // Bắt các lỗi khác có thể xảy ra từ controller
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Đã xảy ra lỗi không mong muốn khi lưu: " + e.getMessage(), "Lỗi Lưu", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Inner class DrawingPanel không thay đổi nhiều, giữ nguyên logic vẽ
    private static class DrawingPanel extends JPanel {
        private BufferedImage canvasImage;
        private Graphics2D g2dCanvas;
        private Color currentColor = Color.BLACK;
        private int currentStrokeSize = 3;
        private boolean isEraserMode = false;
        private Point lastPoint;
        private boolean hasDrawingContent = false; // Track if anything has been drawn

        public BufferedImage getCanvasImage() {
            return canvasImage;
        }

        public DrawingPanel() {
            setPreferredSize(new Dimension(800, 600)); // Default preferred size
            setBackground(Color.WHITE); // Explicitly set background

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastPoint = e.getPoint();
                    if (g2dCanvas != null) {
                        g2dCanvas.setColor(isEraserMode ? getBackground() : currentColor);
                        g2dCanvas.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        // Draw a small circle for a single click
                        g2dCanvas.fillOval(e.getX() - currentStrokeSize / 2, e.getY() - currentStrokeSize / 2, currentStrokeSize, currentStrokeSize);
                        hasDrawingContent = true;
                        repaint();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    lastPoint = null;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (g2dCanvas != null && lastPoint != null) {
                        g2dCanvas.setColor(isEraserMode ? getBackground() : currentColor);
                        g2dCanvas.setStroke(new BasicStroke(currentStrokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2dCanvas.drawLine(lastPoint.x, lastPoint.y, e.getX(), e.getY());
                        lastPoint = e.getPoint();
                        hasDrawingContent = true;
                        repaint();
                    }
                }
            });

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    if (getWidth() > 0 && getHeight() > 0) {
                        BufferedImage oldImage = canvasImage;
                        canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                        g2dCanvas = canvasImage.createGraphics();
                        setupGraphics2D(g2dCanvas);
                        if (oldImage != null) {
                            g2dCanvas.drawImage(oldImage, 0, 0, null);
                        }
                        repaint();
                    }
                }
            });
            // Ensure canvas is initialized on first paint if not resized yet
            SwingUtilities.invokeLater(this::initializeCanvas);
        }

        private void initializeCanvas() {
            if (getWidth() > 0 && getHeight() > 0 && canvasImage == null) {
                canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                g2dCanvas = canvasImage.createGraphics();
                setupGraphics2D(g2dCanvas);
                repaint();
            }
        }

        private void setupGraphics2D(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }


        public void setCurrentColor(Color color) { this.currentColor = color; }
        public void setCurrentStrokeSize(int size) { this.currentStrokeSize = Math.max(1, size); }
        public void setEraserMode(boolean isEraser) { this.isEraserMode = isEraser; }

        public void clearDrawing() {
            if (g2dCanvas != null && getWidth() > 0 && getHeight() > 0) {
                g2dCanvas.setColor(getBackground());
                g2dCanvas.fillRect(0, 0, getWidth(), getHeight());
                hasDrawingContent = false;
                repaint();
            } else if (getWidth() > 0 && getHeight() > 0) { // If g2dCanvas was null but panel has size
                initializeCanvas(); // Initialize and clear
                hasDrawingContent = false;
                repaint();
            }
        }

        public boolean isEffectivelyEmpty() {
            return !hasDrawingContent;
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvasImage == null && getWidth() > 0 && getHeight() > 0) {
                initializeCanvas();
            }
            if (canvasImage != null) {
                g.drawImage(canvasImage, 0, 0, this);
            }
        }

        public String getImageAsBase64(String formatName) throws IOException {
            if (canvasImage == null || !hasDrawingContent) return null; // Return null if nothing drawn

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvasImage, formatName, baos);
            byte[] imageBytes = baos.toByteArray();
            return (imageBytes.length == 0) ? null : Base64.getEncoder().encodeToString(imageBytes);
        }

        public void loadImageFromBase64(String base64Image) throws IOException {
            if (base64Image == null || base64Image.isEmpty()) {
                clearDrawing();
                return;
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage loadedImage = ImageIO.read(bais);

            if (loadedImage != null) {
                if (getWidth() <= 0 || getHeight() <= 0) { // Panel not yet sized
                    // Temporarily store and wait for resize, or use image's own size
                    setPreferredSize(new Dimension(loadedImage.getWidth(), loadedImage.getHeight()));
                    canvasImage = loadedImage; // This will be redrawn correctly on resize or first paint
                    // No g2dCanvas to draw on yet, will be created in componentResized or paintComponent
                } else {
                    // Panel is sized, create/recreate canvasImage and draw loadedImage onto it
                    canvasImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                    g2dCanvas = canvasImage.createGraphics();
                    setupGraphics2D(g2dCanvas); // Clears with background
                    g2dCanvas.drawImage(loadedImage, 0, 0, getWidth(), getHeight(), null); // Scale to fit
                }
                hasDrawingContent = true; // Assume loaded image has content
                repaint();
            } else {
                throw new IOException("Không thể giải mã dữ liệu ảnh từ Base64.");
            }
        }
    }
}