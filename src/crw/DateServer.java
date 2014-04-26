package crw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

/**
 * A TCP server that runs on port 9090. When a client connects, it sends the
 * client the current date and time, then closes the connection with that
 * client. Arguably just about the simplest server you can write.
 */
public class DateServer {

    /**
     * Runs the server.
     */
    public static void main(String[] args) throws IOException {
        System.out.println("Working");
        ServerSocket listener = new ServerSocket(9090);
        try {
            Socket socket = listener.accept();
            System.out.println("Connected");
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(),
                        true);

                BufferedReader input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                while (true) {
                    String answer = input.readLine();
                    // JOptionPane.showMessageDialog(null, answer);
                    System.out.println("Read in: " + answer);
                    if (answer.compareTo("quit") == 0) {
                        out.println("quit");
                        break;
                    } else if (answer.compareTo("getpath") == 0) {
                        Scanner scanner = null;
                        try {
                            scanner = new Scanner(new File("/home/shawn/src/boat_gui/sami-crw/lat_long.txt"), "latin1");
                            while (scanner.hasNextLine()) {
                                String line = scanner.nextLine();
                                out.println(line);
                            }
                            out.println("points end");
                        } catch (FileNotFoundException e) {
                            System.err.println("Cannot find the file");
                        } finally {
                            if (scanner != null) {
                                scanner.close();
                            }
                        }
                    } else {
                        System.out.println(answer);
                        //out.println(answer);
                    }
                }
            } finally {
                System.out.println("Closed socket");
                socket.close();
            }

        } finally {
            System.out.println("Closed listener");
            listener.close();
        }
    }
}
