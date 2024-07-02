package org.example;

import org.apache.kafka.clients.producer.ProducerRecord;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;

public class Chat extends JFrame {

    private JTextPane chatView;
    private JPanel mainPanel;
    private JTextField message;
    private JButton odpiszProszeButton;
    private JButton loginButton;
    private JList<String> usersList;
    private JTextField loginField;

    private final MessageConsumer messageConsumer;
    private final MessageProducer messageProducer;
    private final Set<String> loggedUsers = new HashSet<>();
    private boolean isLoggedIn = false;
    private final List<String> messageBuffer = new ArrayList<>();
    private final Map<String, Color> userColors = new HashMap<>();
    private final List<Color> availableColors = Arrays.asList(Color.RED, Color.BLUE, Color.MAGENTA, Color.ORANGE, Color.PINK);

    private static final String LOGIN_PREFIX = "[LOGIN]";
    private static final String LOGOUT_PREFIX = "[LOGOUT]";

    public Chat(String id, String topic, String userStatusTopic) throws HeadlessException {
        messageConsumer = new MessageConsumer(id, topic, userStatusTopic);
        messageProducer = new MessageProducer();
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.add(mainPanel);
        this.setVisible(true);
        this.setTitle(String.valueOf(id));
        this.pack();

        message.setEnabled(false);
        odpiszProszeButton.setEnabled(false);

        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                messageConsumer.kafkaConsumer.poll(Duration.of(1, ChronoUnit.SECONDS)).forEach(
                        m -> {
                            String messageText = m.value();
                            System.out.println("Received message: " + messageText);
                            if (messageText.startsWith(LOGIN_PREFIX)) {
                                String username = messageText.substring(LOGIN_PREFIX.length());
                                loggedUsers.add(username);
                                updateUsersList();
                                appendToPane(chatView, "User " + username + " logged in.", Color.GREEN);
                                System.out.println("User logged in: " + username);
                            } else if (messageText.startsWith(LOGOUT_PREFIX)) {
                                String username = messageText.substring(LOGOUT_PREFIX.length());
                                loggedUsers.remove(username);
                                updateUsersList();
                                appendToPane(chatView, "User " + username + " logged out.", Color.GREEN);
                                System.out.println("User logged out: " + username);
                            } else {
                                Color userColor = extractColorFromMessage(messageText);
                                String cleanMessageText = messageText.replaceAll("\\[color=#[0-9a-fA-F]{6}\\]", "");
                                if (isLoggedIn) {
                                    appendToPane(chatView, cleanMessageText, userColor);
                                } else {
                                    messageBuffer.add(cleanMessageText);
                                }
                            }
                        }
                );
            }
        });

        odpiszProszeButton.addActionListener(e -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String formattedTime = LocalDateTime.now().format(formatter);
            String username = loginField.getText().trim();
            String messageText = message.getText();
            Color userColor = getColorForUser(username);
            String userColorHex = String.format("#%02x%02x%02x", userColor.getRed(), userColor.getGreen(), userColor.getBlue());
            String fullMessage = formattedTime + " - " + username + ": " + messageText + " [color=" + userColorHex + "]";
            messageProducer.send(new ProducerRecord<>(topic, fullMessage));
            System.out.println("Sent message: " + fullMessage);
            message.setText("");
        });

        loginButton.addActionListener(e -> {
            String username = loginField.getText().trim();
            if (loginButton.getText().equals("Login")) {
                if (!username.isEmpty() && !loggedUsers.contains(username)) {
                    loggedUsers.add(username);
                    Color assignedColor = getRandomColor();
                    userColors.put(username.toLowerCase(), assignedColor);
                    updateUsersList();
                    messageProducer.sendUserStatus(userStatusTopic, LOGIN_PREFIX + username);
                    System.out.println("Logging in user: " + username);
                    loginButton.setText("Logout");
                    isLoggedIn = true;
                    message.setEnabled(true);
                    odpiszProszeButton.setEnabled(true);
                    processAndSendMessageBuffer(topic);
                }
            } else {
                if (loggedUsers.contains(username)) {
                    loggedUsers.remove(username);
                    userColors.remove(username);
                    updateUsersList();
                    messageProducer.sendUserStatus(userStatusTopic, LOGOUT_PREFIX + username);
                    System.out.println("Logging out user: " + username);
                    loginButton.setText("Login");
                    isLoggedIn = false;
                    message.setEnabled(false);
                    odpiszProszeButton.setEnabled(false);
                }
            }
        });
    }

    private void processAndSendMessageBuffer(String topic) {
        for (String msg : messageBuffer) {
            String userId = extractUserIdFromMessage(msg);
            appendToPane(chatView, msg, getColorForUser(userId));
            messageProducer.send(new ProducerRecord<>(topic, msg));
            System.out.println("Processed buffered message: " + msg);
        }
        messageBuffer.clear();
    }

    private void appendToPane(JTextPane tp, String msg, Color c) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = tp.getDocument().getLength();
        tp.setCaretPosition(len);
        tp.setCharacterAttributes(aset, false);
        tp.replaceSelection(msg + "\n");
    }

    private String extractUserIdFromMessage(String message) {
        String[] parts = message.split(" - ", 2);
        if (parts.length > 1) {
            String[] subParts = parts[1].split(": ", 2);
            if (subParts.length > 1) {
                return subParts[0];
            }
        }
        return "unknown";
    }

    private Color extractColorFromMessage(String message) {
        int startIndex = message.indexOf("[color=") + 7;
        int endIndex = message.indexOf("]", startIndex);
        if (startIndex > 6 && endIndex > startIndex) {
            String colorPart = message.substring(startIndex, endIndex);
            Color color = Color.decode(colorPart);
            return color;
        }
        return Color.BLACK;
    }

    private Color getRandomColor() {
        Random rand = new Random();
        return availableColors.get(rand.nextInt(availableColors.size()));
    }

    private Color getColorForUser(String userId) {
        Color color = userColors.get(userId.toLowerCase());
        return color != null ? color : Color.BLACK;
    }

    private void updateUsersList() {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        loggedUsers.forEach(listModel::addElement);
        usersList.setModel(listModel);
    }
}
