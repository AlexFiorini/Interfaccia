package com.mycompany.interfaccia;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChatGUI extends JFrame {

    private final DefaultListModel<Contact> contactsListModel;
    private final JList<Contact> contactsList;
    private final JTextArea messageArea;
    private final JTextField messageField;
    private final JPanel messagePanel;
    private Contact selectedContact;
    private final Map<Contact, StringBuilder> messagesMap;
    
    // Componenti per il login
    private final JTextField emailField;
    private final JPanel loginPanel;
    private static final int PORT = 12345;
    private BufferedReader in;
    private PrintWriter out;
    public String serverAddress = "192.168.178.196"; // Indirizzo IP del server, modificare se necessario
    public String senderEmail;

    public ChatGUI() {
        setTitle("Chatta liberamente");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // Centra la finestra al centro dello schermo

        messagesMap = new HashMap<>();

        // Pannello dei contatti
        JPanel contactsPanel = new JPanel(new BorderLayout());
        contactsPanel.setBackground(Color.GRAY);
        contactsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        contactsListModel = new DefaultListModel<>();
        contactsList = new JList<>(contactsListModel);
        contactsList.setCellRenderer(new ContactListRenderer());
        contactsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane contactsScrollPane = new JScrollPane(contactsList);
        contactsPanel.add(contactsScrollPane, BorderLayout.CENTER);

        JPanel contactsButtonPanel = new JPanel(new FlowLayout());
        contactsButtonPanel.setBackground(Color.GRAY);
        JButton addButton = new JButton("Aggiungi Contatto");
        JButton removeButton = new JButton("Rimuovi Contatto");
        contactsButtonPanel.add(addButton);
        contactsButtonPanel.add(removeButton);
        contactsPanel.add(contactsButtonPanel, BorderLayout.SOUTH);

        // Pannello dei messaggi
        messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(Color.LIGHT_GRAY);
        messagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messagePanel.add(messageScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(Color.LIGHT_GRAY);
        messageField = new JTextField();
        JButton sendButton = new JButton("Invia");
        sendButton.setBackground(new Color(37, 211, 102)); // Colore verde simile a WhatsApp
        sendButton.setBorder(new EmptyBorder(5, 10, 5, 10)); // Spaziatura intorno al pulsante
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        messagePanel.add(inputPanel, BorderLayout.SOUTH);

        // Divisione principale tra contatti e messaggi
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contactsPanel, messagePanel);
        splitPane.setResizeWeight(0.2);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        // Aggiunta dei componenti per il login
        loginPanel = new JPanel(new FlowLayout());
        JLabel emailLabel = new JLabel("Email:");
        emailField = new JTextField(20);
        JButton loginButton = new JButton("Login");
        loginPanel.add(emailLabel);
        loginPanel.add(emailField);
        loginPanel.add(loginButton);
        getContentPane().add(loginPanel, BorderLayout.NORTH);

        // Nasconde il pannello dei contatti e dei messaggi finché l'utente non effettua il login
        contactsPanel.setVisible(false);
        messagePanel.setVisible(false);

        // Listener per il login
        loginButton.addActionListener(e -> {
            String email = emailField.getText();

            if(validaEmail(email)) {
                senderEmail = email;
                try {
                    Socket socket = new Socket(serverAddress, PORT);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    out.println(emailField.getText());
                    loadMessagesFromFile();
                    Thread receiverThread = new Thread(new MessageReceiver(senderEmail));
                    receiverThread.start();

                    // Puoi anche chiudere il socket quando non ne hai più bisogno
                    // socket.close();

                    // Effettua il login utilizzando l'email
                    JOptionPane.showMessageDialog(ChatGUI.this, "Accesso effettuato con successo!");

                    // Mostra il pannello dei contatti e dei messaggi dopo il login
                    contactsPanel.setVisible(true);
                    messagePanel.setVisible(true);
                    loginPanel.setVisible(false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ChatGUI.this, "Errore durante la connessione al server.", "Errore", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(ChatGUI.this, "Inserisci un indirizzo email valido!", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Aggiunta dei listener per i contatti
        contactsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedContact = contactsList.getSelectedValue();
                if (selectedContact != null) {
                    setTitle("Chat con " + selectedContact.name());
                    updateMessageArea(selectedContact);
                }
            }
        });

        // Carica i contatti salvati all'avvio dell'applicazione
        loadContactsFromFile();

        contactsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedContact = contactsList.getSelectedValue();
                if (selectedContact != null) {
                    setTitle("Chat con " + selectedContact.getName());
                    updateMessageArea(selectedContact);
                }
            }
        });

        // Listener per il pulsante Aggiungi Contatto
        addButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(ChatGUI.this, "Inserisci il nome del nuovo contatto:");
            String email = JOptionPane.showInputDialog(ChatGUI.this, "Inserisci l'email del nuovo contatto:");
            if (name != null && !name.trim().isEmpty() && validaEmail(email)) {
                Contact newContact = new Contact(name, email);
                contactsListModel.addElement(newContact);
                messagesMap.put(newContact, new StringBuilder());
            }
        });

        // Listener per il pulsante Rimuovi Contatto
        removeButton.addActionListener(e -> {
            if (selectedContact != null) {
                int choice = JOptionPane.showConfirmDialog(ChatGUI.this, "Sei sicuro di voler rimuovere il contatto?", "Conferma Rimozione", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    contactsListModel.removeElement(selectedContact);
                    messagesMap.remove(selectedContact);
                    messageArea.setText("");
                }
            }
        });

        // Listener per la chiusura della finestra
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveContactsToFile();
            }
        });
    }



    // Metodo principale per avviare l'applicazione
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            ChatGUI app = new ChatGUI();
            app.setVisible(true);
        });
    }

    // Invia un messaggio al contatto selezionato
    private void sendMessage() {
        String message = messageField.getText();
        if (selectedContact != null && !message.isEmpty()) {
            messageArea.append("Tu: " + message + "\n");
            StringBuilder messages = messagesMap.getOrDefault(selectedContact, new StringBuilder());
            messages.append("\nTu: ").append(message).append("\n");
            messagesMap.put(selectedContact, messages);
            out.println(selectedContact.email() + " - " + senderEmail + " - " + message);
            messageField.setText("");
            saveMessage(selectedContact, senderEmail, message);
        } else {
            JOptionPane.showMessageDialog(this, "Seleziona un contatto e inserisci un messaggio.");
        }
    }

    // Aggiorna l'area dei messaggi con i messaggi per il contatto selezionato
    private void updateMessageArea(Contact contact) {
        messageArea.setText(""); // Resetta l'area di messaggio

        // Trova il contatto selezionato da contactsListModel
        for (int i = 0; i < contactsListModel.getSize(); i++) {
            Contact storedContact = contactsListModel.getElementAt(i);
            if (storedContact != null && storedContact.equals(contact)) {
                StringBuilder messages = messagesMap.getOrDefault(storedContact, new StringBuilder());
                // Fai append per il contatto selezionato
                for (String message : messages.toString().split("\n")) {
                    if(messages.length() > 0) {
                        if(message.contains("Tu:")) {
                            messageArea.append(message + "\n");
                        } else {
                            messageArea.append(storedContact.getName() + ": " + message + "\n");
                        }
                    }
                }
                break; // Match
            }
        }
    }

    // Metodo per la validazione dell'email utilizzando un'espressione regolare
    private boolean validaEmail(String email) {
        // Espressione regolare per la validazione dell'email
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    // Carica i contatti da un file all'avvio dell'applicazione
    private void loadContactsFromFile() {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("contatti.txt"))) {
            while (true) {
                try {
                    Object obj = inputStream.readObject();
                    if (obj instanceof Contact contact) {
                        contactsListModel.addElement(contact);
                        messagesMap.put(contact, new StringBuilder());
                    }
                } catch (EOFException e) {
                    // End of file reached
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    // Salva i contatti su un file
    private void saveContactsToFile() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("contatti.txt"))) {
            for (int i = 0; i < contactsListModel.getSize(); i++) {
                outputStream.writeObject(contactsListModel.getElementAt(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveMessage(Contact contact, String sender, String message) {
        if(!contact.getEmail().equals(sender)) {
            try (PrintWriter writer = new PrintWriter(new FileWriter("messaggi.txt", true))) {
                writer.println("Contatto: " + contact.getName());
                writer.println("Da: " + sender);
                writer.println("Messaggio: " + message);
                writer.println(); // Aggiungi una riga vuota per separare i messaggi
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadMessagesFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("messaggi.txt"))) {
            String line;
            StringBuilder message = new StringBuilder();
            Contact currentContact = null;
            String sender = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Contatto: ")) {
                    String contactName = line.substring("Contatto: ".length());
                    currentContact = findContactByName(contactName);
                } else if (line.startsWith("Da: ")) {
                    sender = line.substring("Da: ".length());
                } else if (line.startsWith("Messaggio: ")) {
                    message.append(line.substring("Messaggio: ".length())).append("\n");
                } else if (line.isEmpty()) {
                    if (currentContact != null && sender != null && !message.toString().isEmpty()) {
                        // Aggiungi il messaggio alla mappa dei messaggi
                        StringBuilder messages = messagesMap.getOrDefault(currentContact, new StringBuilder());
                        if(sender.equals(senderEmail)) {
                            message.insert(0, "Tu: ");
                            messages.append(message);
                        } else {
                            messages.append(message);
                        }
                        messagesMap.put(currentContact, messages);

                        currentContact = null;
                        sender = null;
                        message = new StringBuilder();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Contact findContactByName(String name) {
        for (int i = 0; i < contactsListModel.getSize(); i++) {
            Contact contact = contactsListModel.getElementAt(i);
            if (contact.getName().equals(name)) {
                return contact;
            }
        }
        return null;
    }

    private class MessageReceiver implements Runnable {
        String mail;

        MessageReceiver(String mail) {
            this.mail = mail;
        }
        public void run() {
            while (true) {
                try {
                    String line = in.readLine();
                    if (line != null) {
                        if(line.split(" - ").length == 3) {
                            String senderEmail = line.split(" - ", 3)[1];
                            String message = line.split(" - ", 3)[2];
                            String nome = "null";
                            for (int i = 0; i < contactsListModel.getSize(); i++) {
                                Contact contatto = contactsListModel.getElementAt(i);
                                if (contatto != null && Objects.equals(contatto.getEmail(), senderEmail)) {
                                    nome = contatto.getName();
                                    break; // Match
                                }
                            }
                            Contact sender = new Contact(nome, senderEmail);
                            StringBuilder messaggiprecedenti = messagesMap.get(sender);
                            StringBuilder messaggio = messaggiprecedenti.append(message);
                            messagesMap.put(sender, messaggio);
                            saveMessage(sender, senderEmail, message);
                            if(Objects.equals(contactsList.getSelectedValue().getEmail(), senderEmail)) {
                                //Non mostrare doppioni se si mandano messaggi a sé stessi
                                if(!mail.equals(senderEmail)) {
                                    messageArea.append(nome + ": " + message + "\n");
                                }
                            }
                        }
                    } else {
                        break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}