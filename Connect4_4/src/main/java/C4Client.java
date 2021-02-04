import com.formdev.flatlaf.FlatDarkLaf;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.*;
import java.util.Timer;

public class C4Client extends JFrame implements ActionListener {

    JPanel panel;

    JPanel authPanel;
    JTextField userField;
    JPasswordField passField;
    JPasswordField confField;
    JLabel confLabel;
    JLabel errLabel;
    JRadioButton login;
    JRadioButton register;
    ButtonGroup lr;
    JButton submit;

    JPanel globalNorthPanel;
    JButton logout;
    JTable players;
    JScrollPane playersPanel;
    JTextArea globalChatArea;
    JScrollPane globalChatPanel;
    JScrollPane globalMessagePanel;
    JPanel globalSouthPanel;
    JTextArea globalMessageBox;
    JButton globalSendButton;
    ListSelectionModel select;

    JPanel gameNorthPanel;
    JButton time;
    java.util.Timer timer;
    JButton forfeit;
    JPanel gameBoardPanel;
    ArrayList<JButton> gameBoardButtons;
    JTextArea gameChatArea;
    JScrollPane gameChatPanel;
    JPanel gameSouthPanel;
    JTextArea gameMessageBox;
    JScrollPane gameMessagePanel;
    JButton gameSendButton;

    Socket socket;
    Scanner in;
    PrintWriter out;
    String myName;
    String gameId;
    Boolean turn;
    HashSet<String> playerNames;
    Color token;
    Color oppToken;

    public static void main(String[] args) throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        FlatDarkLaf.install();
        C4Client c4Client = new C4Client();
        c4Client.gameLoop();
    }

    public void gameLoop() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        while (in.hasNextLine()) {
            String command = in.nextLine();
            this
            .getClass()
            .getMethod(command.split(" ")[0], String.class)
            .invoke(this, command);
        }
    }

    public void AUTHERROR(String command) {
        int code = Integer.parseInt(command.split(" ")[1]);
        if (code == 1)
            errLabel.setText("User already logged in!");
        else if (code == 2)
            errLabel.setText("Invalid credentials");
        else if (code == 3)
            errLabel.setText("User already exists");
        else
            errLabel.setText("Could not connect to database, please try again later");
        repaint();
    }

    public void AUTHSUCCESS(String command) {
        myName = userField.getText();
        playerNames = new HashSet<>();
        if (!command.equals("AUTHSUCCESS"))
            playerNames.addAll(Arrays.asList(command.substring(12).split(" ")));
        renderGlobal();
    }

    public void FORCELOGOUT(String command) {
        System.exit(0);
    }

    public void PLAYERJOINED(String command) {
        playerNames.add(command.substring(13));
        players.setModel(new DefaultTableModel(new Object[][]{playerNames.toArray()}, new String[]{"Active Players"}));
        players.setCellSelectionEnabled(true);

        select = players.getSelectionModel();
        select.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        select.addListSelectionListener(listSelectionEvent -> {
            if (listSelectionEvent.getValueIsAdjusting()) {
                String data1 = null;
                int[] rows = players.getSelectedRows();
                int[] columns = players.getSelectedColumns();
                for (int row : rows) {
                    for (int col : columns) {
                        data1 = (String) players.getValueAt(row, col);
                    }
                }
                int res = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to challenge " + data1,
                        "CHALLENGE",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (res == JOptionPane.YES_OPTION)
                    out.println("CHALLENGE " + data1);
            }
        });
        repaint();
    }

    public void PLAYERLEFT(String command) {
        playerNames.remove(command.substring(13));
        if (playerNames.isEmpty())
            players.setModel(new DefaultTableModel(new Object[][]{{"Lobby is empty"}}, new String[]{"Active Players"}));
        else {
            players.setModel(new DefaultTableModel(new Object[][]{playerNames.toArray()}, new String[]{"Active Players"}));
            players.setCellSelectionEnabled(true);

            select = players.getSelectionModel();
            select.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            select.addListSelectionListener(listSelectionEvent -> {
                if (listSelectionEvent.getValueIsAdjusting()) {
                    String data1 = null;
                    int[] rows = players.getSelectedRows();
                    int[] columns = players.getSelectedColumns();
                    for (int row : rows) {
                        for (int col : columns) {
                            data1 = (String) players.getValueAt(row, col);
                        }
                    }
                    int res = JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to challenge " + data1,
                            "CHALLENGE",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );
                    if (res == JOptionPane.YES_OPTION)
                        out.println("CHALLENGE " + data1);
                }
            });
        }
        repaint();
    }

    public void GRECV(String command) {
        if (!command.equals("GRECV"))
            globalChatArea.append(command.substring(6) + "\n");
        repaint();
    }

    public void REQUESTED(String command) {
        String [] recv = command.split(" ");
        int response = JOptionPane.showConfirmDialog(
                this,
                recv[1] + " has challenged you to a game! Accept challenge?",
                "New Challenge",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (response == JOptionPane.NO_OPTION) {
            out.println("REJECT " + recv[1]);
        }
        else {
            gameId = recv[2];
            out.println("ACCEPT " + recv[1] + " " + recv[2]);
            token = new Color(239, 83, 80);
            oppToken = new Color(255, 160, 0);
            turn = true;
            renderGame();
        }
    }

    public void ACCEPTED(String command) {
        String [] recv = command.split(" ");
        JOptionPane.showMessageDialog(
                this,
                recv[1] + " has accepted your request, your match is being started",
                "Challenge accepted",
                JOptionPane.INFORMATION_MESSAGE
        );
        gameId = recv[2];
        token = new Color(255, 160, 0);
        oppToken = new Color(239, 83, 80);
        turn = false;
        renderGame();
    }

    public void REJECTED(String command) {
        String [] recv = command.split(" ");
        JOptionPane.showMessageDialog(
            this,
            recv[1] + " has rejected your request.",
            "Challenge rejected",
            JOptionPane.WARNING_MESSAGE
        );
    }

    public void FORFEITED(String command) {
//        timer.cancel();
        String [] recv = command.split(" ");
        int res = JOptionPane.showConfirmDialog(
                this,
                recv[1] + " has forfeited and you have won the match. Close to return to lobby",
                "Match Forfeited",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );
        renderGlobal();
    }

    public void GAMEOVER(String command) {
        String [] recv = command.split(" ");
        int res = JOptionPane.showConfirmDialog(
                this,
                Boolean.parseBoolean(recv[1]) ? "You have won this match" : "You have lost this match",
                "GAME OVER",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );
        renderGlobal();
    }

    public void MOVED(String command) {
        System.out.println(command);
        String [] recv = command.split(" ");
        int pos = Integer.parseInt(recv[1]);
            panel.remove(gameBoardPanel);
            gameBoardPanel.removeAll();
            for (int i = 0; i < 56; i++) {
                JButton jButton = gameBoardButtons.get(i);
                if (i < 8) {
                    jButton.setEnabled(!turn);
                }
                else if (i == pos + 8) {
                    jButton.setBackground(turn ? token : oppToken);
                }
                gameBoardButtons.set(i, jButton);
                gameBoardPanel.add(jButton);
            }
            turn = !turn;
            panel.add(gameBoardPanel, BorderLayout.WEST);
            setContentPane(panel);
            repaint();
    }

    public void LRECV(String command) {
        gameChatArea.append(command.substring(6) + "\n");
    }

    C4Client() throws IOException {
        socket = new Socket("localhost", 2720);
        in = new Scanner(socket.getInputStream());
        out = new PrintWriter(socket.getOutputStream(), true);
        myName = null;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (myName != null)
                    out.println("LOGOUT 0");
                System.exit(0);
            }
        });
        setTitle("Connect 4");
        setSize(new Dimension(960, 768));
        setLocationRelativeTo(null);
        setResizable(false);
        panel = new JPanel(new MigLayout("fillx"));
        renderAuth();
        setVisible(true);
    }

    void renderAuth() {
        panel.removeAll();
        panel.setLayout(new MigLayout("fillx"));
        panel.setMinimumSize(new Dimension(1024, 768));
        JLabel C4 = new JLabel("Connect 4");
        C4.setFont(new Font("Arial", Font.PLAIN, 128));

        login = new JRadioButton("Login", true);
        register = new JRadioButton("Register", false);
        lr = new ButtonGroup();

        lr.add(login);
        lr.add(register);
        login.addActionListener(this);
        register.addActionListener(this);

        authPanel = new JPanel(new MigLayout("fillx"));
        JLabel userLabel = new JLabel("Username");
        JLabel passLabel = new JLabel("Password");
        confLabel = new JLabel("Confirm");
        errLabel = new JLabel("");
        errLabel.setForeground(new Color(239, 83, 80));
        userField = new JTextField();
        passField = new JPasswordField();
        confField = new JPasswordField();

        confLabel.setVisible(false);
        confField.setVisible(false);

        submit = new JButton("Submit");
        submit.addActionListener(this);

        authPanel.setMinimumSize(new Dimension(400, 400));
        authPanel.add(userLabel);
        authPanel.add(userField, "span, grow");
        authPanel.add(passLabel);
        authPanel.add(passField, "span, grow");
        authPanel.add(confLabel, "gapbottom 10");
        authPanel.add(confField, "span, grow, gapbottom 15, wrap");
        authPanel.add(errLabel, "center, span, gapbottom 15, wrap");
        authPanel.add(login, "center");
        authPanel.add(register, "center, wrap");
        authPanel.add(submit, "center, span, gaptop 30");
        panel.add(C4, "center, span, gaptop 50");
        panel.add(authPanel, "center, span, gaptop 50");
        setContentPane(panel);
        repaint();
    }

    void renderGlobal() {
        authPanel.removeAll();
        panel.removeAll();
        panel.setLayout(new BorderLayout(5,1));

        String [] column = {"Active players"};

        if (playerNames.isEmpty()) {
            players = new JTable(new Object[][]{{"Lobby is empty"}}, column);
        }
        else {
            players = new JTable(new Object[][]{ playerNames.toArray() }, column);
            players.setCellSelectionEnabled(true);

            select = players.getSelectionModel();
            select.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            select.addListSelectionListener(listSelectionEvent -> {
                if (listSelectionEvent.getValueIsAdjusting()) {
                    String data1 = null;
                    int [] rows = players.getSelectedRows();
                    int [] columns = players.getSelectedColumns();
                    for (int row : rows) {
                        for (int col : columns) {
                            data1 = (String) players.getValueAt(row, col);
                        }
                    }
                    int res = JOptionPane.showConfirmDialog(
                            this,
                            "Are you sure you want to challenge " + data1,
                            "CHALLENGE",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE
                    );
                    if (res == JOptionPane.YES_OPTION)
                        out.println("CHALLENGE " + data1);
                }
            });
        }

        playersPanel = new JScrollPane(players);
        playersPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        new EmptyBorder(0,5,0,0), new EtchedBorder()
                )
        );
        globalChatArea = new JTextArea();
        globalChatArea.setEditable(false);
        globalChatArea.setLineWrap(true);
        globalChatPanel = new JScrollPane(globalChatArea);
        globalChatPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        new EmptyBorder(0,0,0,5), new EtchedBorder()
                )
        );
        globalChatPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        globalChatPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        globalMessageBox = new JTextArea();
        globalMessageBox.setColumns(100);
        globalMessageBox.setLineWrap(true);
        globalMessagePanel = new JScrollPane(globalMessageBox);
        globalMessagePanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        globalMessagePanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        globalSendButton = new JButton("Send");

        globalSendButton.addActionListener(this);

        globalSouthPanel = new JPanel(new MigLayout("fill"));
        globalSouthPanel.add(globalMessagePanel);
        globalSouthPanel.add(globalSendButton);

        logout = new JButton("Logout");
        logout.addActionListener(this);
        globalNorthPanel = new JPanel(new MigLayout("fillx"));
        globalNorthPanel.add(logout, "right");

        panel.add(globalNorthPanel, BorderLayout.NORTH);
        panel.add(playersPanel, BorderLayout.WEST);
        panel.add(globalChatPanel, BorderLayout.CENTER);
        panel.add(globalSouthPanel, BorderLayout.SOUTH);
        setContentPane(panel);
        repaint();
    }


    void renderGame() {
        panel.removeAll();
        panel.setLayout(new BorderLayout(5,1));

//        time = new JButton("00:00:20");
//        time.setEnabled(false);
//        timer = new Timer();
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                String sec = time.getText();
//                int parsec = Integer.parseInt(sec.split(":")[2]);
//                if (parsec == 0) {
//                    parsec = 21;
//                    out.println("TIMEOUT 0");
//                }
//                parsec = parsec - 1;
//                if (parsec < 10)
//                    sec = "00:00:0" + Integer.toString(parsec);
//                else
//                    sec = "00:00:" + Integer.toString(parsec);
//                time.setText(sec);
//            }
//        },0,1000);
        forfeit = new JButton("Forfeit");
        forfeit.addActionListener(this);

        gameNorthPanel = new JPanel(new MigLayout("fillx"));
//        gameNorthPanel.add(time);
        gameNorthPanel.add(forfeit, "right");

        gameBoardPanel = new JPanel(new GridLayout(7, 8, 5, 5));

        gameBoardButtons = new ArrayList<>();
        for (int i = 0; i < 56; i++) {
            JButton jButton = new JButton();
            if (i < 8) {
                jButton.setText("Drop " + (i+1));
                int finalI = i;
                jButton.setEnabled(turn);
                jButton.addActionListener(actionEvent -> {
                    out.println("MOVE " + finalI);
//                    System.out.println(finalI);
//                    System.out.println(gameBoardPanel.getComponent(finalI));
                });
            }
            else {
                jButton.setText(" ");
                jButton.setOpaque(true);
            }
            gameBoardButtons.add(jButton);
            gameBoardPanel.add(jButton);
        }

        gameBoardPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        new EmptyBorder(0,5,0,0), new EtchedBorder()
                )
        );

        gameChatArea = new JTextArea();
        gameChatArea.setEditable(false);
        gameChatArea.setLineWrap(true);
        gameChatPanel = new JScrollPane(gameChatArea);
        gameChatPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        new EmptyBorder(0,0,0,5), new EtchedBorder()
                )
        );
        gameChatPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gameChatPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        gameMessageBox = new JTextArea();
        gameMessageBox.setColumns(100);
        gameMessageBox.setLineWrap(true);
        gameMessagePanel = new JScrollPane(gameMessageBox);
        gameMessagePanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gameMessagePanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        gameSendButton = new JButton("Send");

        gameSendButton.addActionListener(this);

        gameSouthPanel = new JPanel(new MigLayout("fill"));
        gameSouthPanel.add(gameMessagePanel);
        gameSouthPanel.add(gameSendButton);

        panel.add(gameNorthPanel, BorderLayout.NORTH);
        panel.add(gameBoardPanel, BorderLayout.WEST);
        panel.add(gameChatPanel, BorderLayout.CENTER);
        panel.add(gameSouthPanel, BorderLayout.SOUTH);
        setContentPane(panel);
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(login)) {
            confLabel.setVisible(false);
            confField.setVisible(false);
        }
        if (actionEvent.getSource().equals(register)) {
            confLabel.setVisible(true);
            confField.setVisible(true);
        }
        if (actionEvent.getSource().equals(submit)) {
            if (
                userField.getText().length() == 0 ||
                passField.getPassword().length == 0
            )
                errLabel.setText("All fields must be filled");
            else if (confField.isVisible()) {
                if (passField.getText().equals(confField.getText()))
                    errLabel.setText("Passwords don't match");
                else {
                    out.println("REGISTER " + userField.getText() + " " + passField.getText());
                }
            }
            else {
                out.println("LOGIN " + userField.getText() + " " + passField.getText());
            }
        }
        if (actionEvent.getSource().equals(logout)) {
            out.println("LOGOUT 0");
            panel.removeAll();
            renderAuth();
        }
        if (actionEvent.getSource().equals(globalSendButton)) {
            if (globalMessageBox.getText().length() != 0) {
                out.println("GSEND " + myName + ": " + globalMessageBox.getText());
                globalMessageBox.setText("");
            }
        }
        if (actionEvent.getSource().equals(forfeit)) {
            int res = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to forfeit? This will count as a loss",
                    "FORFEIT GAME",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (res == JOptionPane.YES_OPTION) {
                out.println("FORFEIT 0");
                renderGlobal();
            }
//            panel.removeAll();
//            timer.cancel();
//            renderGlobal();
        }
        if (actionEvent.getSource().equals(gameSendButton)) {
            if (gameMessageBox.getText().length() != 0) {
                out.println("LSEND " + myName + ": " + gameMessageBox.getText());
                gameMessageBox.setText("");
            }
        }
    }
}