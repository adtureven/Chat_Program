import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JFrame loginFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private int audiocount;
    private boolean isRecording;
    public Client(String ip, int port) throws IOException {
        connectToServer(ip, port);
    }

    private void createLoginGUI() {
        loginFrame = new JFrame("Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(300, 150);
        loginFrame.setLayout(new GridLayout(3, 2));
        isRecording=false;
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField();
        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField();

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                out.println(username);
                out.println(password);
            }
        });

        loginFrame.add(usernameLabel);
        loginFrame.add(usernameField);
        loginFrame.add(passwordLabel);
        loginFrame.add(passwordField);
        loginFrame.add(loginButton);

        loginFrame.setLocationRelativeTo(null); // 将窗口居中显示
    }

    private void connectToServer(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        System.out.println("Connected to server: " + socket);

        createLoginGUI();
        loginFrame.setVisible(true);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String response = in.readLine();
        System.out.println("Server response: "+ response);

        if (response.equals("Authentication successful")) {
            loginFrame.dispose();
        }
        createAndShowGUI();

    }
    private void createAndShowGUI() {
        JFrame frame = new JFrame("Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        audiocount=0;
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // 将消息显示区域放置在滚动窗格中
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        JPanel inputPanel = new JPanel(new BorderLayout());
        // 创建语音输入按钮
        JButton voiceButton = new JButton("Voice");
        voiceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isRecording) {
                    // 开始录制语音
                    startRecording();
                    voiceButton.setText("Stop");
                    SwingUtilities.invokeLater(() -> {
                        messageArea.append("You start a Voice Chat...\n\n");
                    });
                } else {
                    // 结束录制语音并发送
                    stopRecordingAndSend();
                    voiceButton.setText("Voice");
                    SwingUtilities.invokeLater(() -> {
                        messageArea.append("Voice Chat is over...\n\n");
                    });
                }
            }
        });
        inputPanel.add(voiceButton, BorderLayout.WEST);

        JTextField inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText();
                sendMessage(message);
                SwingUtilities.invokeLater(() -> {
                    messageArea.append("You: " + message + "\n\n");
                });
                inputField.setText("");
            }
        });
        inputPanel.add(inputField, BorderLayout.CENTER);

        // 创建上传文件按钮
        JButton uploadButton = new JButton("Upload File");
        uploadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    // 处理上传文件的逻辑
                    sendFile(selectedFile);
                }
            }
        });

        inputPanel.add(uploadButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        // 开启一个单独的线程读取服务器消息并更新图形界面
        Thread readerThread = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    receiveMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        readerThread.start();
    }
    public void sendMessage(String message) {
        out.println("Text:"+message);
    }
    private void startRecording() {
        try {
            // 创建音频格式
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

            // 获取音频输入设备
            TargetDataLine line = AudioSystem.getTargetDataLine(format);
            line.open(format);
            line.start();
            out.println("Audio:"+ audiocount);
            out.flush();
            audiocount+=1;
            System.out.println("Audio:"+ audiocount);
            // 创建输出流，用于发送音频数据
            OutputStream outputStream = socket.getOutputStream();
            isRecording=true;
            // 创建线程用于录制音频并发送
            Thread recordingThread = new Thread(() -> {
                try {
                    System.out.println("Start recording...");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while (isRecording && (bytesRead = line.read(buffer, 0, buffer.length)) != -1) {
                        // 将音频数据发送到服务器
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                        System.out.println(bytesRead);
                    }

                    byte specialSymbol = '$';
                    byte[] endMarker = new byte[1024];
                    Arrays.fill(endMarker, (byte) specialSymbol); // 填充1024个特殊符号
                    outputStream.write(endMarker);
                    outputStream.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // 关闭音频输入设备和输出流
                    line.stop();
                    line.close();
                }
            });
            recordingThread.start();
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }
    private void stopRecordingAndSend() {
        System.out.println("Stop recording and send...");
        isRecording = false;
    }
    private void receiveMessage(String message) {
        try {
            System.out.println(message);
            if (message.startsWith("Text:")) {
                // 接收到文本消息
                String textMessage = message.substring(5);
                SwingUtilities.invokeLater(() -> {
                    messageArea.append("Client: " + textMessage + "\n\n");
                });
            } else if (message.startsWith("File:")) {
                // 接收到文件消息
                String[] parts = message.split(":");
                String fileName = parts[1];
                long fileSize = Long.parseLong(parts[2]);
                //String fileName = message.substring(5);
                receiveFile(fileName,fileSize);
            } else if(message.startsWith("Audio:")) {
                String audioname = message.substring(5);
                SwingUtilities.invokeLater(() -> {
                    messageArea.append("You received a Voice Chat...\n\n");
                });
                receiveAudio(audioname);
            }
            // 处理逻辑..
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void receiveAudio(String audioname){
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            int flag = 0;
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

            // 获取音频输出设备
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

            // 打开音频输出设备并开始播放
            line.open(format);
            line.start();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // 处理接收到的音频数据，例如播放音频
                // 这里假设你有一个播放音频的函数 playAudio(byte[] audioData, int length)
                byte specialSymbol = '$'; // 设置特殊符号
                for (byte b : buffer) {
                    if (b == specialSymbol) {
                        flag = 1;
                    } else break;
                }
                if (flag == 1) break;
                line.write(buffer, 0, bytesRead);
                //System.out.println("going...");
                //System.out.println(bytesRead);
            }
            // 等待音频播放完成
            line.drain();
            // 关闭音频输出设备
            line.close();
            SwingUtilities.invokeLater(() -> {
                messageArea.append("Voice is played over...\n\n");
            });
        }catch (LineUnavailableException | IOException e){
            e.printStackTrace();
        }
    }
    private void sendFile(File file) {
        try {
            // 读取文件内容并发送
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(buffer, 0, buffer.length);

            // 发送文件名
            out.println("File:" + file.getName()+":"+file.length());
            out.flush();

            // 发送文件内容
            OutputStream os = socket.getOutputStream();
            os.write(buffer, 0, buffer.length);
            os.flush();

            // 关闭流
            fis.close();
            bis.close();

            // 在图形界面中显示文件传输信息
            SwingUtilities.invokeLater(() -> {
                messageArea.append("File sent: " + file.getName() + file.length()+ "\n\n");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void receiveFile(String fileName,long filesize) {
        try {
            // 创建文件输出流
            File receivedFile = new File(fileName);
            FileOutputStream fos = new FileOutputStream(receivedFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            // 从输入流中读取文件内容
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                bos.flush();
                if(bytesRead != 8192) break;
            }

            // 关闭流
            bos.close();
            fos.close();

            String filePath = receivedFile.getAbsolutePath();

            // 在图形界面中显示文件接收信息
            SwingUtilities.invokeLater(() -> {
                messageArea.append("File received: " + fileName +"---"+ receivedFile.length()+"\n");
                messageArea.append("File saved to :" + filePath + "\n\n");
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}
