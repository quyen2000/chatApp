package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;


public class Server {
    static Map<String, ClientHandler> listWait = new LinkedHashMap<>();
    static Map<String, ClientHandler> listMatched = new LinkedHashMap<>();
    static String lastEnter;
    
    public static void main(String args[]){
        try(ServerSocket serverSocket = new ServerSocket(999);){
            Socket clienSocket ;
            while(true){
                System.out.println("Server waiting for client");
                // 22-28 luồng 1
                clienSocket = serverSocket.accept();
                System.out.println("New client request received : " + clienSocket);
                ObjectOutputStream ous = new ObjectOutputStream(clienSocket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(clienSocket.getInputStream());
                ClientHandler newClient = new ClientHandler(clienSocket,ois,ous);
                Thread thread = new Thread(newClient);
                thread.start();
            }
        }
        catch (IOException e) {
        	e.printStackTrace();
        }

    }
}
