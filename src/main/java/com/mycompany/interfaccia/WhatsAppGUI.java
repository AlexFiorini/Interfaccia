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

public class WhatsAppGUI extends JFrame {

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

    public WhatsAppGUI() {
        setTitle("WhatsApp Web");
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
                try {
                    Socket socket = new Socket(serverAddress, PORT);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    out.println(emailField.getText());

                    Thread receiverThread = new Thread(new MessageReceiver());
                    receiverThread.start();

                    // Puoi anche chiudere il socket quando non ne hai più bisogno
                    // socket.close();

                    // Effettua il login utilizzando l'email
                    JOptionPane.showMessageDialog(WhatsAppGUI.this, "Accesso effettuato con successo!");

                    // Mostra il pannello dei contatti e dei messaggi dopo il login
                    contactsPanel.setVisible(true);
                    messagePanel.setVisible(true);
                    loginPanel.setVisible(false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(WhatsAppGUI.this, "Errore durante la connessione al server.", "Errore", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(WhatsAppGUI.this, "Inserisci un indirizzo email valido!", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Aggiunta dei listener per i contatti
        contactsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedContact = contactsList.getSelectedValue();
                if (selectedContact != null) {
                    setTitle("WhatsApp Web - " + selectedContact.name());
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
                    setTitle("WhatsApp Web - " + selectedContact.getName());
                    updateMessageArea(selectedContact);
                }
            }
        });

        // Listener per il pulsante Aggiungi Contatto
        addButton.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(WhatsAppGUI.this, "Inserisci il nome del nuovo contatto:");
            String email = JOptionPane.showInputDialog(WhatsAppGUI.this, "Inserisci l'email del nuovo contatto:");
            if (name != null && !name.trim().isEmpty() && validaEmail(email)) {
                Contact newContact = new Contact(name, email);
                contactsListModel.addElement(newContact);
                messagesMap.put(newContact, new StringBuilder());
            }
        });

        // Listener per il pulsante Rimuovi Contatto
        removeButton.addActionListener(e -> {
            if (selectedContact != null) {
                int choice = JOptionPane.showConfirmDialog(WhatsAppGUI.this, "Sei sicuro di voler rimuovere il contatto?", "Conferma Rimozione", JOptionPane.YES_NO_OPTION);
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
            WhatsAppGUI app = new WhatsAppGUI();
            app.setVisible(true);
        });
    }

    // Invia un messaggio al contatto selezionato
    private void sendMessage() {
        String message = messageField.getText();
        if (selectedContact != null && !message.isEmpty()) {
            messageArea.append("Tu: " + message + "\n");
            StringBuilder messages = messagesMap.getOrDefault(selectedContact, new StringBuilder());
            messages.append("Tu: ").append(message).append("\n");
            messagesMap.put(selectedContact, messages);
            out.println(selectedContact.email() + " - " + message);
            messageField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Seleziona un contatto e inserisci un messaggio.");
        }
    }

    // Aggiorna l'area dei messaggi con i messaggi per il contatto selezionato
    private void updateMessageArea(Contact contact) {
        StringBuilder messages = messagesMap.getOrDefault(contact, new StringBuilder());
        messageArea.setText(contact.name() + "\n" + messages.toString());
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

    private class MessageReceiver implements Runnable {
        public void run() {
            while (true) {
                try {
                    String line = in.readLine();
                    if (line != null) {
                        System.out.println(line);
                        messageArea.append(line + "\n");
                    } else {
                        break; // Exit the loop if the stream is closed
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}