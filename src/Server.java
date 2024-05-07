import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Server {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            System.out.println("Waiting for client...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket);

            // 创建客户端处理器
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            clients.add(clientHandler);
            clientHandler.start();
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // 读取用户名和密码
                String username = in.readLine();
                String password = in.readLine();

                // 验证用户信息
                boolean authenticated = authenticateUser(username, password);

                // 发送验证结果给客户端
                if (authenticated) {
                    out.println("Authentication successful");
                    System.out.println("Authentication successful for client: " + clientSocket);
                } else {
                    out.println("Authentication failed");
                    System.out.println("Authentication failed for client: " + clientSocket);
                }

                // 等待客户端通信
                receiveMessage();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // 关闭连接
                try {
                    stopConnection();
                    System.out.println("Client disconnected: " + clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        private void receiveMessage() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Text:")) {
                        // 接收到文本消息
                        String textMessage = message.substring(5);
                        System.out.println("Message from client: " + textMessage);
                        // 转发消息给其他客户端
                        for (ClientHandler client : clients) {
                            if (client != this) {
                                client.sendMessage(textMessage);
                            }
                        }
                    } else if (message.startsWith("File:")) {
                        // 接收到文件消息
                        String[] parts = message.split(":");
                        String fileName = parts[1];
                        long fileSize = Long.parseLong(parts[2]);
                        //String fileName = message.substring(5);
                        System.out.println("File:" + fileName + fileSize);
                        receiveFile(fileName,fileSize);
                    } else if (message.startsWith("Audio:")) {
                        String[] parts = message.split(":");
                        String audioname = parts[1];
                        long fileSize = Long.parseLong(parts[2]);
                        System.out.println("Audio from client.."+fileSize);
                        receiveAudio(audioname,fileSize);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void receiveAudio(String audioname,long filesize) throws IOException {
            // 创建输入流，用于接收客户端发送的音频数据
            InputStream inputStream = clientSocket.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1460];
            int bytesRead;
            //int flag=0;
            while (true) {
                // 将音频数据写入文件
                bytesRead = inputStream.read(buffer);
                outputStream.write(buffer, 0, bytesRead);
//                byte specialSymbol = '$'; // 设置特殊符号
//                for (byte b : buffer) {
//                    if (b == specialSymbol) {
//                        flag = 1;
//                    } else break;
//                }
//                if (flag == 1) break;
                filesize-=bytesRead;
                System.out.println(filesize);
                if(filesize<=0) break;
            }
            for (ClientHandler client : clients) {
                if (client != this) { // 不转发给当前客户端
                    client.sendAudio(audioname,outputStream);
                }
            }
        }
        private void sendAudio(String audioname,ByteArrayOutputStream output) throws IOException {
            out.println("Audio:"+audioname+":"+output.size());
            out.flush();

            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(output.toByteArray());
            outputStream.flush();

            System.out.println("over transfer");
        }
        private void receiveFile(String fileName,long filesize) {
            try {
                // 创建文件输出流
                File receivedFile = new File(fileName);
                FileOutputStream fos = new FileOutputStream(receivedFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                // 从输入流中读取文件内容
                InputStream inputStream = clientSocket.getInputStream();

                byte[] buffer = new byte[1460];
                int bytesRead;
                //int flag=0;
                byte specialSymbol = '$';
                byte[] endMarker = new byte[1460];
                Arrays.fill(endMarker, (byte) specialSymbol);
                while (true) {
                    bytesRead = inputStream.read(buffer);
                    bos.write(buffer, 0, bytesRead);
                    bos.flush();
                    //System.out.println(filesize);
                    filesize-=bytesRead;
                    if(filesize<=0) break;
                }
                // 关闭流
                bos.close();
                fos.close();

                // 在服务端显示文件接收信息
                System.out.println("File received: " + fileName+" " +receivedFile.length());

                // 转发文件给其他客户端
                broadcastFile(receivedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void broadcastFile(File file) {
            try {
                // 读取文件内容
                byte[] buffer = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(buffer, 0, buffer.length);

                // 转发文件给其他客户端
                for (ClientHandler client : clients) {
                    if (client != this) { // 不转发给当前客户端
                        client.sendFile(file.getName(), buffer);
                    }
                }

                // 关闭流
                bis.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void sendFile(String fileName, byte[] fileData) {
            try {
                // 发送文件名
                out.println("File:" + fileName+":"+fileData.length);
                out.flush();

                // 发送文件内容
                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(fileData, 0, fileData.length);
                outputStream.flush();

//                byte specialSymbol = '$';
//                byte[] endMarker = new byte[1024];
//                Arrays.fill(endMarker, (byte) specialSymbol); // 填充1024个特殊符号
//                outputStream.write(endMarker);
//                outputStream.flush();

                System.out.println("send File:" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void stopConnection() throws IOException {
            in.close();
            out.close();
            clientSocket.close();
            clients.remove(this);
        }
        private boolean authenticateUser(String username, String password) {
            // 在实际情况下，你需要在这里编写验证用户信息的逻辑
            // 这里只是一个简单的示例，始终返回true，表示认证成功
            if(Objects.equals(username, "user1") && Objects.equals(password, "123456")){
                    return true;
            }
            if(Objects.equals(username, "user2") && Objects.equals(password, "123")){
                return true;
            }
            return false;
            //return true;
        }
        private void sendMessage(String message) {
            out.println("Text:"+message);
        }
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start(3389);
    }
}
