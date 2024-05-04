import java.io.*;

public class Client2 {
    private static Client client;

    public static void main(String[] args) throws IOException {
        client = new Client("localhost", 12345);
    }
}


