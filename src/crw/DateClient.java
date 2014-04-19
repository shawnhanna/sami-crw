package crw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JOptionPane;

/**
 * Trivial client for the date server.
 */
public class DateClient {

    /**
     * Runs the client as an application. First it displays a dialog box asking
     * for the IP address or hostname of a host running the date server, then
     * connects to it and displays the date that it serves.
     */
    public static void main(String[] args) throws IOException {
        String serverAddress = JOptionPane
                .showInputDialog("Enter IP Address of a machine that is\n"
                        + "running the date service on port 9090:");
        Socket s = new Socket(serverAddress, 9090);
        Scanner scanner = new Scanner(System.in);
        PrintWriter out
                = new PrintWriter(s.getOutputStream(), true);

        BufferedReader input = new BufferedReader(new InputStreamReader(
                s.getInputStream()));
        ArrayList<Double> X = new ArrayList<Double>();
        ArrayList<Double> Y = new ArrayList<Double>();
        while (true) {
            String in = scanner.nextLine();
            out.println(in);
            //String[] array=in.split(",");
            if (in.compareTo("get path") == 0) {
                while (true) {
                    String answer = input.readLine();
                    if (answer.equals("points end")) {
                        break;
                    } else {
                        String[] array = answer.split(" ");
                        X.add(Double.parseDouble(array[0]));
                        Y.add(Double.parseDouble(array[1]));
                    }
                }
                System.out.println(X);
                System.out.println(Y);
            }
            //X.add(Double.parseDouble(array[0]));
            //Y.add(Double.parseDouble(array[1]));

            if (in.compareTo("quit") == 0) {
                break;
            }
        }
        scanner.close();
        s.close();
        System.exit(0);
    }
}
