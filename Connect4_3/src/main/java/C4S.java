import com.mongodb.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Filters;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import com.mongodb.client.model.Sorts;

import java.util.*;
import java.util.function.Consumer;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.util.Pair;

public class C4S{
    // Name-PrintWriter Hashmap for unique name-outputStream pairs
    private static HashMap<String, PrintWriter> players = new HashMap<>();

    // To record chat history for all clients and refresh periodically
    private static ArrayList<String> chatHistory = new ArrayList<>();

    // ArrayList of games
//    private static ArrayList<C4Board> games = new ArrayList<>();
//    private static int gameIndex = 0;
    private static HashMap<String, C4Board> games = new HashMap<>();

    private static MongoClient mongoClient;
    private static MongoDatabase db;
    private static MongoCollection<Document> usersCollection;

    public static void main(String[] args) throws IOException {
        System.out.println("Connect4 server online");
        ExecutorService pool = Executors.newFixedThreadPool(500);

        mongoClient = MongoClients.create("<insert-mongogb+srv-here>");

        db = mongoClient.getDatabase("game");

        usersCollection = db.getCollection("users");

        System.out.println("Connected " + usersCollection.countDocuments());

        try (ServerSocket listener = new ServerSocket(2720)) {
            while (true) {
                pool.execute(new Player(listener.accept()));
            }
        }
    }
    private static class Player implements Runnable {
        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;

        public Player(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Welcome to Connect4! Login or create an account to continue!");

                // Authentication loop
                while (true) {
                    out.println("\n(L)ogin or (R)egister?: ");
                    char lr = in.nextLine().charAt(0);

                    out.println("Username: ");
                    String username = in.nextLine();
                    if (username == null) {
                        return;
                    }

                    synchronized (usersCollection) {
                        Document userMatch = usersCollection.find(eq("username", username)).first();
                        if (lr == 'L') {
                            if (players.containsKey(username)) {
                                out.println("\033[H\033[2J");
                                out.flush();
                                out.println("User already logged in from separate session!");
                            }
                            else if (userMatch != null) {
                                out.println("Password: ");
                                String password = in.nextLine();
                                if (password == null) {
                                    return;
                                }

                                Document passMatch = usersCollection.find(and(eq("username", username), eq("password", password))).first();

                                if (passMatch != null) {
                                    name = username;
//                                    names.add(name);
                                    players.put(name, out);
                                    break;
                                }
                                else {
                                    out.println("\033[H\033[2J");
                                    out.flush();
                                    out.println("Could not find a user with these credentials, please try again!");
                                }
                            }
                            else {
                                out.println("\033[H\033[2J");
                                out.flush();
                                out.println("Could not find a user with this username, please try again!");
                            }
                        }
                        else {
                            if (userMatch != null) {
                                out.println("\033[H\033[2J");
                                out.flush();
                                out.println("User already exists, please try logging in instead");
                            }
                            else {
                                out.println("Password: ");
                                String password = in.nextLine();
                                if (password == null) {
                                    return;
                                }
                                if (usersCollection.insertOne( new Document("username", username).append("password", password)).wasAcknowledged()) {
                                    name = username;
//                                    names.add(name);
                                    players.put(name, out);
                                    break;
                                }
                                else {
                                    out.println("\033[H\033[2J");
                                    out.flush();
                                    out.println("Registration error, please try again");
                                }
                            }
                        }
                    }
                }
                out.println("\033[H\033[2J");
                out.flush();

                for (PrintWriter writer: players.values()) {
                    ChatLoop(writer);

                    if (writer.equals(out))
                        writer.println("Authentication successful. You have now joined the lobby, " + name);
                    else
                        writer.println(name + " has joined");
                }

                // Accept messages from this client and broadcast them.
                while (true) {
                    String input = in.nextLine();
                    if (input.toLowerCase().startsWith("#quit")) {
                        return;
                    }
                    if (input.toLowerCase().startsWith("#challenge")) {
                        String challenged = input.split(" ")[1];
                        System.out.println(challenged);
                        PrintWriter getOpp = players.get(challenged);
                        if (getOpp != null) {
                            System.out.println(getOpp);
                            getOpp.println(name + " has challenged you to a game");
                            getOpp.println("Reply with '#accept " + name + "' to accept request");
                            C4Board newGame = new C4Board(getOpp, out);
                            games.put(name, newGame);

                            C4Board game = games.get(name);

                            while (true) {
                                if (gameLoop(name, getOpp, challenged, name)) break;
                            }
                        }
                    }
                    else if (input.toLowerCase().startsWith("#accept")) {
                        String challenger = input.split(" ")[1];
                        PrintWriter getOpp = players.get(challenger);

                        if (getOpp != null) {

                            players.remove(challenger);
                            players.remove(name);

                            for (PrintWriter writer: players.values()) {
                                ChatLoop(writer);
                            }

                            C4Board game = games.get(challenger);
                            game.printBoard();

                            while (true) {
                                if (gameLoop(name, getOpp, challenger, challenger)) break;
                            }
                        }
                    }
                    else if (input.toLowerCase().startsWith("#stats")) {
                        usersCollection
                        .find(eq("username", name))
                        .projection(fields(include("matches"), excludeId()))
                        .forEach(new Consumer<Document>() {
                            @Override
                            public void accept(Document document) {
                                ArrayList<Document> res = new ArrayList<>();
                                res = (ArrayList<Document>) document.get("matches");
                                out.println("INDEX\tRESULTS\tMOVES");
                                int index = 1;
                                for (Document result : res) {
                                    out.println(index++ + "\t" + result.get("result") + "\t" + result.get("moves"));
                                }
                            }
                        });
                    }
                    else {
                        chatHistory.add(name + ": " + input);
                        for (PrintWriter writer: players.values()) {
                            ChatLoop(writer);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } finally {
                if (name != null) {
                    System.out.println(name + " is leaving");
                    players.remove(name);
                    for (PrintWriter writer: players.values()) {
                        writer.println("MESSAGE " + name + " has left");
                    }
                    mongoClient.close();
                }
                try {
                    socket.close();
                } catch (IOException ignored) {
                }

            }
        }

        private boolean gameLoop(String me, PrintWriter getOpp, String opp, String gameOwner) {
            C4Board game = games.get(gameOwner);
            String gameIp = in.nextLine();
            if (gameIp.toLowerCase().startsWith("#endgame")) {
                if (game == null) {
                    players.put(me, out);
                    for (PrintWriter writer: players.values()) {
                        ChatLoop(writer);
                    }
                    return true;
                }
                games.replace(gameOwner, null);
                players.put(me, out);
                for (PrintWriter writer: players.values()) {
                    ChatLoop(writer);
                }
                if (!game.isGameOver()) {
                    out.println("You forfeited the previous game. This will be counted as a loss");
                    getOpp.println(me + " forfeited. You've won the game");
                    usersCollection
                    .updateOne(
                        eq("username", me),
                        new Document("$push", new Document(
                            "matches", new Document("result", "LOSS").append("moves", game.getMoves())
                        ))
                    );
                    usersCollection
                    .updateOne(
                        eq("username", opp),
                        new Document("$push", new Document(
                            "matches", new Document("result", "WIN").append("moves", game.getMoves())
                        ))
                    );
                }
                return true;
            }
            else if (gameIp.toLowerCase().startsWith("#move") && !game.isGameOver()) {
                if (!game.getCurrentPlayer().equals(out)) {
                    out.println("C4ALERT: NOT YOUR MOVE!");
                }
                else {
                    int move = Integer.parseInt(gameIp.split(" ")[1]);
                    game.choice(move);
                    if (game.isWinningMove()) {
                        //gameOver = true;
                        game.setGameOver(true);
                        out.println("You've won the game. Enter #endgame to go back to the lobby");
                        getOpp.println("You've lost the game. Enter #endgame to go back to the lobby");
                        usersCollection
                        .updateOne(
                            eq("username", me),
                            new Document("$push", new Document(
                                "matches", new Document("result", "WIN").append("moves", game.getMoves())
                            ))
                        );
                        usersCollection
                        .updateOne(
                            eq("username", opp),
                            new Document("$push", new Document(
                                "matches", new Document("result", "LOSS").append("moves", game.getMoves())
                            ))
                        );
                    }
                }
            } else {
                game.setChatHistory(name + ": " + gameIp);
                game.printBoard();
            }
            games.replace(gameOwner, game);
            return false;
        }

        private void ChatLoop(PrintWriter writer) {
            writer.println("\033[H\033[2J");
            writer.flush();

            writer.println("Commands: ");
            writer.println("#challenge <player_name> to challenge");
            writer.println("#accept <player_name> to accept");
            writer.println("#stats to view statistics");
            writer.println("#quit to leave game\n");

            writer.println("Others in lobby: ");

            players.keySet().forEach(writer::println);

            writer.println("\n----------------------------------------");
            writer.println("Global chat");

            chatHistory.forEach(writer::println);
        }

    }
}