package C4A2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class C4Server {
    public static void main(String[] args) throws Exception {
        try (ServerSocket listener = new ServerSocket(2720)) {
            System.out.println("Connect4 server online...");
            ExecutorService pool = Executors.newFixedThreadPool(200);
            while (true) {
                Game game = new Game();
                pool.execute(game.new Player(listener.accept(), "\u001B[31m\u3007\u001B[0m"));  // Red Token
                pool.execute(game.new Player(listener.accept(), "\u001B[33m\u3007\u001B[0m"));  // Yellow Token
            }
        }
    }
}

class Game {
    private ArrayList<ArrayList<Player>> grid = new ArrayList<>();
    private ArrayList<String> chatHistory = new ArrayList<>();
    Player currentPlayer;
    private final int width = 8, height = 6;
    private int lastCol = -1, lastRow = -1, moves = 0;

    public Game() {
        for (int i = 0; i < height; i++) {
            grid.add(new ArrayList<>());
            for (int j = 0; j < width; j++) {
                grid.get(i).add(j, new Player(null, "\u3007"));
            }
        }
    }

    public void printBoard( PrintWriter output, Player opponent) {
        opponent.output.print("\033[H\033[2J");
        opponent.output.flush();
        for (int i = 0; i < height; i++) {
            opponent.output.println("\n\n");
            for (int j = 0; j < width; j++) {
                opponent.output.print(grid.get(i).get(j).mark + "\t");
            }
        }
        opponent.output.println("\n\n");
    }

    public String horizontal() {
        StringBuilder x = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            x.append(grid.get(lastRow).get(i).mark);
        }
        return x.toString();
    }

    public String vertical() {
        StringBuilder y = new StringBuilder(height);
        for (int i = 0; i < height; i++)
            y.append(grid.get(i).get(lastCol).mark);
        return y.toString();
    }

    public String forwardSlash() {
        StringBuilder fSlash = new StringBuilder(height);
        for (int h = 0; h < height; h++) {
            int w = lastCol + lastRow - h;
            if (0 <= w && w < width)
                fSlash.append(grid.get(h).get(w).mark);
        }
        return fSlash.toString();
    }

    public String backSlash() {
        StringBuilder bSlash = new StringBuilder(height);
        for (int h = 0; h < height; h++) {
            int w = lastCol - lastRow + h;
            if (0 <= w && w < width)
                bSlash.append(grid.get(h).get(w).mark);
        }
        return bSlash.toString();
    }

    public static boolean isStreak(String str, String substr) {
        return str.contains(substr);
    }

    public Boolean isWinningMove() {
        String token = grid.get(lastRow).get(lastCol).mark;
        String streak = String.format("%s%s%s%s", token, token, token, token);
        return isStreak(horizontal(), streak) || isStreak(vertical(), streak) || isStreak(forwardSlash(), streak) || isStreak(backSlash(), streak);
    }

    public boolean boardFilledUp() {
        return (moves == 48);
    }

    public synchronized void move(int location, Player player) {
        if (player != currentPlayer)
            throw new IllegalStateException("Not your turn!");
        else if (player.opponent == null)
            throw new IllegalStateException("You don't have an opponent");
        else {
            for (int h = height - 1; h >= 0; h--) {
                if (grid.get(h).get(location).mark.equals("\u3007")) {
                    lastRow = h;
                    lastCol = location;
                    grid.get(h).set(location, new Player(player.socket, player.mark));
                    moves++;
                    currentPlayer = currentPlayer.opponent;
                    return;
                }
            }
            throw new IllegalStateException("Column "+location+" is already full!");
        }
    }

    class Player implements Runnable {
        String mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        public Player(Socket socket, String mark) {
            this.mark = mark;
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                setup();
                processCommands();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (opponent != null && opponent.output != null)
                    opponent.output.println("\nOTHER_PLAYER_LEFT");
                try {
                    socket.close();
                }
                catch (IOException ignored) {}
            }
        }

        private void setup() throws IOException {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            output.println("WELCOME " + mark);
            if (mark.equals("\u001B[31m\u3007\u001B[0m")) {
                currentPlayer = this;
                output.println("MESSAGE Waiting for opponent to connect");
            }
            else {
                output.println("MESSAGE Waiting for opponent to make their move");
                opponent = currentPlayer;
                opponent.opponent = this;
                printBoard(output, opponent);
                opponent.output.println("MESSAGE Your turn");
            }
        }

        private void processCommands() throws IOException {
            String command;
            while ((command = input.readLine()) != null) {
                if (command.startsWith("QUIT"))
                    return;
                else if (command.startsWith("MOVE")) {
                    processMoveCommand(Integer.parseInt(command.substring(5)));
                }
                else if (command.startsWith("CHAT")) {
                    chatHistory.add(mark + ": " + command.substring(5));
                    opponent.output.println(mark + ": " + command.substring(5));
                }
            }
        }

        private void processMoveCommand(int location) {
            try {
                move(location, this);
                printBoard(output,opponent.opponent);
                printBoard(output,opponent);
                for (String chat: chatHistory) {
                    opponent.output.println(chat);
                    output.println(chat);
                }
                opponent.output.println("\nMESSAGE Your turn");
                if (isWinningMove()) {
                    output.println("\n\nVICTORY");
                    opponent.output.println("\n\nDEFEAT");
                } else if (boardFilledUp()) {
                    output.println("TIE");
                    opponent.output.println("TIE");
                }
            } catch (IllegalStateException e) {
                output.println("MESSAGE " + e.getMessage());
            }
        }
    }
}