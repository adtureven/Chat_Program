import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class test {
    private JFrame loginFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;

    public test() {
        createLoginGUI();
    }

    private void createLoginGUI() {
        loginFrame = new JFrame("Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(300, 150);
        loginFrame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        JLabel usernameLabel = new JLabel("             Username:");
        usernameField = new JTextField();
        JLabel passwordLabel = new JLabel("             Password:");
        passwordField = new JPasswordField();

        inputPanel.add(usernameLabel);
        inputPanel.add(usernameField);
        inputPanel.add(passwordLabel);
        inputPanel.add(passwordField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton loginButton = new JButton("Login");

        buttonPanel.add(loginButton);

        statusLabel = new JLabel("", JLabel.CENTER);

        loginFrame.add(inputPanel, BorderLayout.CENTER);
        loginFrame.add(buttonPanel, BorderLayout.SOUTH);
        loginFrame.add(statusLabel, BorderLayout.NORTH);

        loginFrame.setLocationRelativeTo(null); // 将窗口居中显示
        loginFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new test();
            }
        });
    }
}
