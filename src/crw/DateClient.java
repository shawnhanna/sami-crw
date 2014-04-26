package crw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import sami.path.Location;
import sami.path.UTMCoordinate;
import sami.service.pathplanning.PlanningService;

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
        String serverAddress = "localhost";
        Socket socket = new Socket(serverAddress, 9090);
        if (socket.isConnected()) {
            System.out.println("Connected to waypoint server");
        }

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader input = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));

        try {
            Thread.currentThread().sleep(100);
        } catch (Exception e) {
        }

        if (socket.isConnected()) {
            String s = "getpath 10.43 50.42324234234234 0.434 15402.0\n";
            System.out.print("Sending 'get path request': " + s);
            out.print(s);
            out.flush();
        }

        ArrayList<Double> latitude = new ArrayList<Double>();
        ArrayList<Double> longitude = new ArrayList<Double>();
        try {
            while (true) {
                String response = "";
//                                if (input.ready()) {
                response = input.readLine();
                System.out.println("Got response: "+response);
//                                } else {
//                                    System.out.println("Socket input stream not ready");
//                                }s
                if (response.equals("points end")) {
                    break;
                } else {
                    String[] array = response.split(" ");
                    latitude.add(Double.parseDouble(array[0]));
                    longitude.add(Double.parseDouble(array[1]));
                    System.out.println("lat_long: " + latitude.get(latitude.size() - 1) + " " + longitude.get(longitude.size() - 1));
                }
            }
            System.out.println(latitude);
            System.out.println(longitude);
        } catch (IOException ex) {
            System.out.println("IO Exception: "+ex);
        } finally {
            System.out.println("Closing connection to socket");
            if (socket != null) {
                try {
                    out.close();
                    input.close();
                    socket.close();
                } catch (IOException ex) {
                    Logger.getLogger(PlanningService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
