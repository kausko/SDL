import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class C4Board {

    private static final String[] TOKENS = {"\u001B[31m\u3007\u001B[0m", "\u001B[33m\u3007\u001B[0m"};   // RED and YELLOW TOKENS
    private final int width = 8, height = 6;
    private final ArrayList<ArrayList<String>> grid = new ArrayList<ArrayList<String>>();
    private int lastCol = -1, lastRow = -1, moves = 0; // to store last moves

    public C4Board() {
        for (int i = 0; i < height; i++) {
            grid.add(new ArrayList<String>());
            for (int j = 0; j < width; j++) {
                grid.get(i).add(j, "\u3007");
            }
        }
    }

    public C4Board(PrintWriter red, PrintWriter yellow) {
        for (int i = 0; i < height; i++) {
            grid.add(new ArrayList<String>());
            for (int j = 0; j < width; j++) {
                grid.get(i).add(j, "\u3007");
            }
        }
    }

    public C4Board(PrintWriter out) {
        for (int i = 0; i < height; i++) {
            grid.add(new ArrayList<String>());
            for (int j = 0; j < width; j++) {
                grid.get(i).add(j, "\u3007");
            }
        }
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

    public int choice(int col) {
        int h;
        for (h = height - 1; h >= 0 ; h--) {
            if (grid.get(h).get(col).equals("\u3007")) {
                lastRow = h;
                lastCol = col;
                grid.get(h).set(col, TOKENS[moves%2]);
                moves++;
                break;
            }
        }
        return (8*h) + col;
    }

    public void incrementMoves() { moves++; }
}