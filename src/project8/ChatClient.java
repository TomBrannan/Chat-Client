package project8;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A graphical chat client that connects to a chat server (chat room).
 *
 * @author Tom Brannan
 * @date Oct 24, 2013
 */
public final class ChatClient {

    //Client attributes
    private Socket socket;
    private String username;
    private final String host = "localhost";
    private final int port = 1500;
    private PrintWriter output;     //This is our output stream to send
    //Messages to the server

    private BufferedReader input;   //This is our input stream to get
    //Messages from the server

    //GUI attributes
    private JFrame frame;
    private JTextArea chat;
    private JTextArea userEntry;
    private JList userList;
    private JScrollPane listPane, entryPane;
    private JScrollPane chatPane;

    /**
     * Default constructor: Initializes the graphics, functionality, connects to
     * the server and establishes the input/output streams. Starts a thread to
     * listen for messages from the server, and prompts the user to enter a
     * username.
     */
    public ChatClient() {
        initGUI();               //Initialize all the graphics
        connect();               //Connect to server
        initStreams();           //Initialize the streams
        initListeners();         //Add listener functionality
        new ListenerThread().start(); //Start listening
        promptUsername();        //Prompt the user for name
    }

    /**
     * Initializes all of the graphics
     */
    public void initGUI() {
        frame = new JFrame();
        frame.setSize(600, 350);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chat = new JTextArea();
        chat.setEditable(false);
        chat.setFont(new Font("Tahoma", 0, 12));

        userList = new JList();
        userList.setFixedCellWidth(120);

        userEntry = new JTextArea();

        chatPane = new JScrollPane(chat);
        listPane = new JScrollPane(userList);
        entryPane = new JScrollPane(userEntry);

        frame.add(listPane, BorderLayout.EAST);
        frame.add(chatPane);
        frame.add(entryPane, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    /**
     * Initializes the action listeners
     */
    public void initListeners() {
        //Activated when we send a message by pressing enter
        userEntry.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    if (!userEntry.getText().equals("")) {
                        sendMessage(userEntry.getText());
                        userEntry.setText("");
                        userEntry.requestFocus();
                    }
                }
            }
        });

        //Activated when we close the window
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    disconnect();
                } catch (Exception ex) {
                    System.out.println("Problem disconnecting.");
                    System.out.println(ex.getMessage());
                }
            }
        });

        /*
         * Listens for double-clicks on our list of users.
         * If we double click on a name, it prepares the text box
         * for us to send a private message to that person.
         * (Of course this can be done manually as well)
         */
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = userList.getSelectedIndex();
                    String name = (String) userList.getSelectedValue();
                    userEntry.setText("/" + name + " "
                            + userEntry.getText());
                    userEntry.requestFocus();
                }
            }
        });
    }

    /**
     * Prompts the user to enter a username, then sends it to the server.
     */
    public void promptUsername() {
        String s = JOptionPane.showInputDialog("Enter your username: ");
        username = s;
        frame.setTitle(s + "'s Chat Box");
        sendMessage(username);
        System.out.println("Entered " + s + " as name");
    }

    /**
     * Connects us to the server (establishes the socket)
     */
    public void connect() {
        try {
            socket = new Socket(host, port);
        } catch (Exception e) {
            System.out.println("Error connecting to server " + e);
            return;
        }
        System.out.println("Connection successful!");
    }

    /**
     * Attempt to initialize the data streams.
     */
    public void initStreams() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException ioe) {
            System.out.println("There was an error creating the IO streams");
        }
    }

    /**
     * Sends a message to the server.
     *
     * @param message the message to be sent to the server
     */
    public void sendMessage(String message) {
        try {
            output.println(message.trim());
        } catch (Exception e) {
            System.out.println("There was a problem sending the message.");
        }
    }

    /**
     * Disconnects us from the server. Closes the streams and socket, assuming
     * they exist.
     */
    public void disconnect() {
        try {
            if (output != null) {
                output.close();
            }
            if (input != null) {
                input.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            System.out.println("Failed to disconnect?");
        }
    }

    /**
     * Displays a message to the chat window.
     *
     * @param message The message
     */
    public void display(String message) {
        chat.append(message);
    }

    /**
     * A static class representing a Thread that constantly listens for messages
     * from the server.
     */
    class ListenerThread extends Thread {

        @Override
        public void run() {
            while (true) {
                //Try to get a message from the server
                try {
                    System.out.println("Waiting for a message from the server");
                    String message = input.readLine();
                    System.out.println("Got message: " + message);

                    //Check what kind of message it is
                    //If we're updating the users ("UL = User List")
                    if (message.startsWith("$UL")) {
                        String temp = message.substring(3);
                        temp = temp.replace("[", "");
                        temp = temp.replace("]", "");
                        String[] users = temp.split(", ");
                        userList.setListData(users);
                        continue; //Don't display this message
                    }
                    display(message + "\n");
                } catch (Exception e) {
                    System.out.println("Disconnected.");
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        ChatClient c = new ChatClient();
    }
}
