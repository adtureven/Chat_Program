import java.io.*;

public class Client1 {
    private static Client client;

    public static void main(String[] args) throws IOException {
        client = new Client("121.43.55.5", 3389);
        //client = new Client("localhost", 3389);
    }
}

