import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.util.Properties;

public class SshTunnelManager {

    private static Session sshSession;

    /**
     * Thiết lập một SSH tunnel.
     *
     * @param sshUser User để đăng nhập SSH.
     * @param sshPassword Mật khẩu SSH.
     * @param sshHost Hostname hoặc IP của SSH server.
     * @param sshPort Port của SSH server (thường là 22).
     * @param remoteDbHost Hostname hoặc IP của Database server (từ góc nhìn của SSH server, thường là 'localhost' nếu DB cùng máy SSH).
     * @param localForwardPort Port trên máy local sẽ được forward.
     * @param remoteDbPort Port của Database server trên máy remote.
     * @throws JSchException Nếu có lỗi khi thiết lập tunnel.
     */
    public static void connect(String sshUser, String sshPassword, String sshHost, int sshPort,
                               String remoteDbHost, int localForwardPort, int remoteDbPort) throws JSchException {
        if (sshSession != null && sshSession.isConnected()) {
            System.out.println("SSH tunnel đã được kết nối.");
            return;
        }

        try {
            JSch jsch = new JSch();
            sshSession = jsch.getSession(sshUser, sshHost, sshPort);
            sshSession.setPassword(sshPassword);

            // Bỏ qua kiểm tra host key nghiêm ngặt (chỉ cho môi trường dev/test, không khuyến khích cho production)
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(config);

            System.out.println("Đang kết nối SSH đến " + sshHost + ":" + sshPort + " với user " + sshUser + "...");
            sshSession.connect(); // Timeout mặc định
            System.out.println("Kết nối SSH thành công.");

            // Thiết lập port forwarding: localPort:remoteDbHost:remoteDbPort
            // Ví dụ: localhost:9999 sẽ được forward đến remoteDbHost:5432 qua SSH tunnel
            int assignedPort = sshSession.setPortForwardingL(localForwardPort, remoteDbHost, remoteDbPort);
            System.out.println("SSH tunnel được thiết lập: localhost:" + assignedPort + " -> " + remoteDbHost + ":" + remoteDbPort);
            System.out.println("Bây giờ bạn có thể kết nối database qua localhost:" + assignedPort);

        } catch (JSchException e) {
            System.err.println("Lỗi khi thiết lập SSH tunnel: " + e.getMessage());
            sshSession = null; // Reset session nếu lỗi
            throw e;
        }
    }

    public static void disconnect() {
        if (sshSession != null && sshSession.isConnected()) {
            System.out.println("Đang đóng SSH tunnel...");
            sshSession.disconnect();
            sshSession = null;
            System.out.println("SSH tunnel đã đóng.");
        }
    }

    public static boolean isConnected() {
        return sshSession != null && sshSession.isConnected();
    }
}