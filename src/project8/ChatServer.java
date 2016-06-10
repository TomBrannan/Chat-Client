package project8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A Server for hosting a chat session. Allows clients to connect and message
 * each other.
 *
 * @author Tom Brannan
 * @date Oct 24, 2013
 */
public class ChatServer {

    private static HashMap<String, Socket> users;
    private final int PORT = 1500;
    private static ArrayList<ClientThread> clientThreads;
    private static ArrayList<String> usernames; //List of usernames, for convenience

    /**
     * Default constructor: Initializes the Map and ArrayLists.
     */
    public ChatServer() {
        users = new HashMap();
        clientThreads = new ArrayList();
        usernames = new ArrayList();
    }

    /**
     * Sends a message to everyone in the chat room. I.e. all clients connected
     * to this server.
     *
     * @param message The message
     */
    public static synchronized void broadcast(String message) {
        for (ClientThread ct : clientThreads) {
            ct.sendMessage(message);
        }
        System.out.println("Broadcasted \"" + message + "\" to "
                + clientThreads.size() + " clients.");
    }

    /**
     * Broadcasts a list of users to everyone in the chat room. The usernames
     * list (an ArrayList) is sent as-is. When the client receives this message,
     * the brackets [] are removed and the String is tokenized by commas.
     */
    public static synchronized void broadcastUsers() {
        broadcast("$UL" + usernames);   //"UL" = User List
    }

    /**
     * Starts the server, then loops forever to listen for clients connecting to
     * the server.
     */
    public void start() {
        //Try to register the ServerSocket
        try {
            ServerSocket ss = new ServerSocket(PORT);
            while (true) {
                System.out.println("Waiting for clients on port " + PORT);
                Socket s = ss.accept();

                //Create a thread of this client, add it to our 
                //list, and start it.
                ClientThread ct = new ClientThread(s);
                clientThreads.add(ct);
                ct.start();
                System.out.println("Added the connection!");
            }
        } catch (Exception e) {
            System.out.println("There was a problem starting the server.");
        }
    }

    //This thread exists for each unique client.
    //It listens constantly for messages from the client
    //And handles outputting messages to that client
    static class ClientThread extends Thread {

        private Socket socket;
        private BufferedReader input;
        private PrintWriter output;
        private String username;
        private String message;         //The message we received

        /**
         * Constructor: Initializes the thread with the given Socket And
         * establishes the IO streams. The username is the first thing sent from
         * the client to the server.
         *
         * @param socket
         */
        public ClientThread(Socket socket) {
            this.socket = socket;
            try {
                //Establish the IO streams
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                //Read the username from the client
                username = input.readLine();

                //Add it to our map and list
                users.put(username, socket);
                usernames.add(username);

                //Alert the rest of the chat room we joined
                System.out.println(username + " just connected.");
                broadcast(username + " has joined the room.");
                broadcastUsers();
                output.println("Welcome to the chatroom!");
                output.println("Type a message in the text box to talk to everyone.");
                output.println("Type /help for a listing of chat commands.\n");

                //This is necessary because this ClientThread hasn't
                //Been added to the list of ClientThreads yet.
                output.println("$UL" + usernames);
            } catch (Exception e) {
                System.out.println("There was a problem creating the "
                        + "thread for this client: " + e);
            }
        }

        /**
         * Sends a message to this client.
         *
         * @param message The message
         */
        public void sendMessage(String message) {
            try {
                output.println(message);
            } catch (Exception e) {
                System.out.println("The message could "
                        + "not be sent to " + username);
            }
        }

        /**
         * The run() thread constantly listens for messages From the client.
         * When we get a message, we determine its meaning and act accordingly.
         */
        @Override
        public void run() {
            while (true) {
                try {
                    message = input.readLine();
                    System.out.println(username + " sent: " + message);

                    //If they're trying to change their name
                    if (message.startsWith("/name")) {
                        try {
                            String[] parts = message.split(" ");
                            String newName = parts[1];

                            usernames.remove(username);
                            usernames.add(newName);

                            broadcastUsers();
                            broadcast(username + " has changed their "
                                    + "name to " + newName + ".");
                            username = newName;

                            users.put(username, socket);
                            continue;
                        } catch (Exception ex) {
                            sendMessage("You must supply a new name.");
                        }
                    } //If they need help
                    else if (message.startsWith("/help")) {
                        sendMessage("Type \"/name\" followed by a new name to change your username.");
                        sendMessage("Type \"/\" followed by a person's username to send a private message to them");
                        sendMessage("Or, double-click on their name in the list to the right.");
                        sendMessage("Close the window to disconnect.");
                        continue;
                    } //If they're doing a private message
                    else if (message.startsWith("/")) {
                        String temp = message.substring(1);
                        int spaceLoc = 0;
                        for (int i = 0; i < temp.length(); i++) {
                            if (temp.charAt(i) == ' ') {
                                spaceLoc = i;
                                break;
                            }
                        }
                        String sendTo = temp.substring(0, spaceLoc);
                        if (!users.containsKey(sendTo)) {
                            sendMessage("That person is not online.");
                            continue;
                        }
                        String messageContents = temp.substring(spaceLoc + 1);
                        try {
                            if (sendTo.equals(username)) {
                                sendMessage("Why are you talking to yourself?");
                                continue;
                            }
                            Socket tempSocket = users.get(sendTo);
                            PrintWriter o = output;
                            sendMessage("To " + sendTo + ": " + messageContents);
                            output = new PrintWriter(tempSocket.getOutputStream(), true);
                            sendMessage("From " + username + ": " + messageContents);
                            output = o;
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    } //Otherwise, it's just a message to everybody.
                    else {
                        broadcast(username + ": " + message);
                    }
                } catch (Exception e) {
                    clientThreads.remove(this);
                    usernames.remove(username);
                    users.remove(username);
                    broadcast(username + " has left the room.");
                    broadcastUsers();
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        ChatServer c = new ChatServer();
        c.start();
    }
}
