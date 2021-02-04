import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mongodb.client.model.Filters.*;

public class C4Server {
    // Name-PrintWriter Hashmap for unique name-outputStream pairs
    private static HashMap<String, PrintWriter> players = new HashMap<>();

    // To record chat history for all clients and refresh periodically
    private static ArrayList<String> chatHistory = new ArrayList<>();

    // Name-Game Hashmap for unique name-game pairs
    private static HashMap<String, C4Board> games = new HashMap<>();

    private static MongoClient mongoClient;
    private static MongoDatabase db;
    private static MongoCollection<Document> usersCollection;

    public static void main(String[] args) throws IOException {
        System.out.println("Connect 4 server online");
        ExecutorService pool = Executors.newFixedThreadPool(500);

        mongoClient = MongoClients.create("<insert-mongogb+srv-here>");

        db = mongoClient.getDatabase("game");

        usersCollection = db.getCollection("users");

        System.out.println("Connected to MongoDB Cluster. Docs obtained: " + usersCollection.countDocuments());

        try (ServerSocket listener = new ServerSocket(2720)) {
            while (true) {
                pool.execute(new Player(listener.accept()));
            }
        }
    }

    private static class Player implements Runnable {
        private String name;
        private PrintWriter opponent;
        private String gameId;
        private final Socket socket;
        private Scanner in;
        private PrintWriter out;

        public Player(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);
                while (in.hasNextLine()) {
                    String command = in.nextLine();
                    this
                    .getClass()
                    .getMethod(command.split(" ")[0], String.class)
                    .invoke(this, command);
                }
            } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            finally {
                System.out.println("Initiating forced shutdown");
                out.println("FORCELOGOUT 0");
            }
        }

        public void LOGIN (String command) {
            String [] creds = command.split(" ");
            String username = creds[1];
            String password = creds[2];
            if (players.containsKey(username)) {
                out.println("AUTHERROR 1");
                return;
            }
            Document check = usersCollection.find(and(eq("username", username), eq("password", password))).first();
            if (check == null) {
                out.println("AUTHERROR 2");
            }
            else {
                userAuth(username);
            }
        }

        public void REGISTER (String command) {
            String [] creds = command.split(" ");
            String username = creds[1];
            String password = creds[2];
            if (players.containsKey(username)) {
                out.println("AUTHERROR 1");
                return;
            }
            Document userExists = usersCollection.find(eq("username", username)).first();
            if (userExists != null) {
                out.println("AUTHERROR 3");
                return;
            }
            if (usersCollection.insertOne( new Document("username", username).append("password", password)).wasAcknowledged()) {
                userAuth(username);
            }
            else {
                out.println("AUTHERROR 4");
            }
        }

        public void LOGOUT (String command) {
            players.remove(name);
            players.values().forEach(writer -> writer.println("PLAYERLEFT " + name));
        }

        public void GSEND (String command) {
            String msg = command.substring(6);
            chatHistory.add(msg);
            players.values().forEach(writer -> writer.println("GRECV " + msg));
        }

        public void CHALLENGE (String command) {
            String challenged = command.split(" ")[1];
            opponent = players.get(challenged);
            gameId = new ObjectId().toString();
            players.get(challenged).println("REQUESTED " + name + " " + gameId);
        }

        public void ACCEPT (String command) {
            String [] parts = command.split(" ");
            String challenger = parts[1];
            String gameId = parts[2];
            this.gameId = gameId;
            opponent = players.get(challenger);
            games.put(gameId, new C4Board());
            players.get(challenger).println("ACCEPTED " + name + " " + gameId);
        }

        public void REJECT (String command) {
            String challenger = command.split(" ")[1];
            players.get(challenger).println("REJECTED " + name);
        }

        public void MOVE (String command) {
            int move = Integer.parseInt(command.split(" ")[1]);
            C4Board game = games.get(gameId);
            int pos = game.choice(move);
            String op = "MOVED " + pos;
            out.println(op);
            opponent.println(op);
            if (game.isWinningMove()) {
                out.println("GAMEOVER " + Boolean.toString(true));
                opponent.println("GAMEOVER " + Boolean.toString(false));
            }
        }

        public void TIMEOUT (String command) {
            games.get(gameId).incrementMoves();
            out.println("MOVED -1");
            opponent.println("MOVED -1");
        }

        public void LSEND (String command) {
            out.println("LRECV " + command.substring(6));
            opponent.println("LRECV " + command.substring(6));
        }

        public void FORFEIT (String command) {
            opponent.println("FORFEITED " + name);
        }

        public void userAuth(String username) {
            name = username;
            players.put(name, out);
            StringBuffer activePlayers = new StringBuffer("AUTHSUCCESS");
            players.keySet().forEach(player -> {
                if (!player.equals(name))
                    activePlayers.append(" ").append(player);
            });
            out.println(activePlayers);
            StringBuffer messages = new StringBuffer("GRECV");
            chatHistory.forEach(msg -> messages.append(" ").append(msg));
            out.println(messages);

            players.values().forEach(writer -> {
                if (!writer.equals(out))
                    writer.println("PLAYERJOINED " + name);
            });
        }

    }
}