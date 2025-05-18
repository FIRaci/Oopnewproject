import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

public class MouseEventDispatcher {
    private final ImageSpinner imageSpinner;
    private final Component rootComponent;
    private final MouseMotionListener mouseMotionListener;

    public MouseEventDispatcher(ImageSpinner imageSpinner, Component rootComponent) {
        this.imageSpinner = imageSpinner;
        this.rootComponent = rootComponent;
        this.mouseMotionListener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Chuyển vị trí chuột sang tọa độ tương đối với imageSpinner
                Point mousePos = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), imageSpinner);
                imageSpinner.updateMousePosition(mousePos);
            }
        };
    }

    // Thêm MouseMotionListener vào một component và tất cả các thành phần con của nó
    public void addMouseMotionListener(Component component) {
        if (component instanceof JComponent) {
            ((JComponent) component).addMouseMotionListener(mouseMotionListener);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                addMouseMotionListener(child);
            }
        }
    }

    // Thêm listener vào dialog hoặc frame
    public void addMouseMotionListenerToWindow(Window window) {
        if (window instanceof JFrame) {
            addMouseMotionListener(((JFrame) window).getContentPane());
        } else if (window instanceof JDialog) {
            addMouseMotionListener(((JDialog) window).getContentPane());
        }
        window.addMouseMotionListener(mouseMotionListener);
    }

    // Gỡ listener nếu cần (không bắt buộc trong trường hợp này)
    public void removeMouseMotionListener(Component component) {
        if (component instanceof JComponent) {
            ((JComponent) component).removeMouseMotionListener(mouseMotionListener);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                removeMouseMotionListener(child);
            }
        }
    }
}