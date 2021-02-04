import java.util.ArrayList;
import java.util.Scanner;

public class C4A1 {

    private static final String[] PLAYERS = {"\u001B[31m\u3007\u001B[0m", "\u001B[33m\u3007\u001B[0m"};   // RED and YELLOW TOKENS
    private final int width = 8, height = 6;
    private final ArrayList<ArrayList<String>> grid = new ArrayList<ArrayList<String>>();
    private int lastCol = -1, lastRow = -1; // to store last moves

    public C4A1() {
        for (int i = 0; i < height; i++) {
            grid.add(new ArrayList<String>());
            for (int j = 0; j < width; j++) {
                grid.get(i).add(j, "\u3007");
            }
        }
    }

    public void printBoard() {
        for (int i = 0; i < height; i++) {
            System.out.println();
            System.out.println();
            System.out.println();
            for (int j = 0; j < width; j++) {
                System.out.print(grid.get(i).get(j) + "\t");
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

    public void choice(String token, Scanner input) {
        do {
            System.out.print("\n\n" + token + "'s turn: ");
            int col = input.nextInt();

            if (col < 0 || col > 7) {
                System.out.println("Column must be between 0 and 7");
                continue;
            }

            for (int h = height - 1; h >= 0 ; h--) {
                if (grid.get(h).get(col).equals("\u3007")) {
                    lastRow = h;
                    lastCol = col;
                    grid.get(h).set(col, token);
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    return;
                }
            }

            System.out.println("Column " + col + " is full!");
        } while (true);
    }

    public static void main(String[] args) {
	// write your code here
        int moves = 48;
        C4A1 board = new C4A1();
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter a number between 0 and 7 (inclusive) to choose a column.");
        board.printBoard();
        for (int player = 0; moves-- > 0; player = 1 - player) {
            String currPlayer = PLAYERS[player];
            board.choice(currPlayer, sc);
            board.printBoard();
            if (board.isWinningMove()) {
                System.out.println("\n\nGAME OVER");
                System.out.println("\nPlayer " + currPlayer + " wins!");
                return;
            }
        }
        System.out.println("Game ended in a tie.");
    }
}
