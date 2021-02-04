import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class C4Board {

    private static final String[] TOKENS = {"\u001B[31m\u3007\u001B[0m", "\u001B[33m\u3007\u001B[0m"};   // RED and YELLOW TOKENS
    private final int width = 8, height = 6;
    private final ArrayList<ArrayList<String>> grid = new ArrayList<ArrayList<String>>();
    private ArrayList<String> chatHistory = new ArrayList<>();
    private int lastCol = -1, lastRow = -1, moves = 0; // to store last moves
    private PrintWriter [] players; // to store players' PrintWriters
    private boolean gameOver = false;

    public C4Board() {
        for (int i = 0; i < height; i++) {
            grid.add(new ArrayList<String>());
            for (int j = 0; j < width; j++) {
                grid.get(i).add(j, "\u3007");
            }
        }
    }

    public C4Board(PrintWriter red, PrintWriter yellow) {
        players = new PrintWriter[]{red, yellow};
        for (int i = 0; i < height; i++) {
            grid.add(new ArrayList<String>());
            for (int j = 0; j < width; j++) {
                grid.get(i).add(j, "\u3007");
            }
        }
    }

    public C4Board(PrintWriter out) {
        players = new PrintWriter[]{out};
        for (int i = 0; i < height; i++) {
            grid.add(new ArrayList<String>());
            for (int j = 0; j < width; j++) {
                grid.get(i).add(j, "\u3007");
            }
        }
    }

    public void printBoard() {
        for (PrintWriter out: players) {
            out.println("\033[H\033[2J");
            out.flush();
            for (int i = 0; i < height; i++) {
                out.println("\n\n");
                StringBuilder str = new StringBuilder("");
                for (int j = 0; j < width; j++) {
                    str.append(grid.get(i).get(j)).append("\t");
                }
                out.println(str);
            }
            out.println("\n\n");
            chatHistory.forEach(out::println);
        }
        players[moves%2].println("\nYOUR MOVE");
    }

    public String horizontal() {
        StringBuilder x = new StringBuilder(width);
        for (int i = 0; i < width; i++) {
            x.append(grid.get(lastRow).get(i));
        }
        return x.toString();
    }

    public String vertical() {
        StringBuilder y = new StringBuilder(height);
        for (int i = 0; i < height; i++)
            y.append(grid.get(i).get(lastCol));
        return y.toString();
    }

    public String forwardSlash() {
        StringBuilder fSlash = new StringBuilder(height);
        for (int h = 0; h < height; h++) {
            int w = lastCol + lastRow - h;
            if (0 <= w && w < width)
                fSlash.append(grid.get(h).get(w));
        }
        return fSlash.toString();
    }

    public String backSlash() {
        StringBuilder bSlash = new StringBuilder(height);
        for (int h = 0; h < height; h++) {
            int w = lastCol - lastRow + h;
            if (0 <= w && w < width)
                bSlash.append(grid.get(h).get(w));
        }
        return bSlash.toString();
    }

    public static boolean isStreak(String str, String subStr) {
        return str.contains(subStr);
    }

    // Check victory
    public boolean isWinningMove() {
        String token = grid.get(lastRow).get(lastCol);
        String streak = String.format("%s%s%s%s", token, token, token, token);
        return isStreak(horizontal(), streak) || isStreak(vertical(), streak) || isStreak(forwardSlash(), streak) || isStreak(backSlash(), streak);
    }

    public void choice(int col) {
        if (col < 0 || col > 7) {
            players[moves%2].println("Column must be between 0 and 7");
            return;
        }

        for (int h = height - 1; h >= 0 ; h--) {
            if (grid.get(h).get(col).equals("\u3007")) {
                lastRow = h;
                lastCol = col;
                grid.get(h).set(col, TOKENS[moves%2]);
                moves++;
                printBoard();
                return;
            }
        }

        players[moves%2].println("Column " + col + " is full!");
    }

    //public int getMoves() { return moves; }
    public PrintWriter getCurrentPlayer() { return players[moves%2]; }

    public int getMoves() {
        return moves;
    }

    public void setChatHistory(String chat) {
        chatHistory.add(chat);
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}
