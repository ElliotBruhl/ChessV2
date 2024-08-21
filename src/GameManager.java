import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.SwingWorker;

public abstract class GameManager {

    public static int[] runGame(Player oldBot, Player newBot) { //no human players
        int[] move;
        int[] result = new int[202];
        try {
            List<String> lines = Files.readAllLines(Paths.get("files/TrainingOpeningDB.txt"));
            for (int i = 0; i < lines.size(); i++) { //each starting fen in the training db
                System.out.println(i + " iterations");

                Board b = new Board(lines.get(i));
                while (b.gameState() == null) { //game with oldBot as white

                    move = b.getMove() ? oldBot.getMove(b) : newBot.getMove(b);

                    if (b.manageMove(move[0], move[1]))
                        b.managePromotion(move[1], move[2]);
                }
                
                switch (b.gameState()) { //convert values because players switch sides
                    case 0 -> {result[i*2] = 0;}
                    case Integer.MAX_VALUE/2 -> {result[i*2] = -1;}
                    case Integer.MIN_VALUE/2 -> {result[i*2] = 1;}
                    default -> {throw new Error("Play on in update");}
                }

                b = new Board(lines.get(i));
                while (b.gameState() == null) { //game with newBot as white

                    move = b.getMove() ? newBot.getMove(b) : oldBot.getMove(b);
                    if (b.manageMove(move[0], move[1]))
                        b.managePromotion(move[1], move[2]);
                }
                
                switch (b.gameState()) { //convert values because players switch sides
                    case 0 -> {result[i*2+1] = 0;}
                    case Integer.MAX_VALUE/2 -> {result[i*2+1] = 1;}
                    case Integer.MIN_VALUE/2 -> {result[i*2+1] = -1;}
                    default -> {throw new Error("Play on in update");}
                }
            }
        }
        catch (IOException e) {
            throw new Error("Error reading file");
        }

        return result;
    }
    public static void runGuiGame(Board board, Gui boardGui) { //both human players
        boardGui.updateBoard();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() { //Gui and game logic must operate on seperate threads
                int[] move;

                while (board.gameState() == null) {

                    boardGui.waitForValidInput(); //proceeds when valid start and end are inputed
                    move = boardGui.getClicks();
                    boardGui.resetStartEnd(); //clears the Gui temp variables so new input is ready

                    boolean legal = false;
                    for (int[] i : board.getLegalMoves()) { //check for legality becuase human is playing
                        if (i[0] == move[0] && i[1] == move[1]) {
                            legal = true;
                            break;
                        }
                    }
                    if (!legal) continue;

                    if (board.manageMove(move[0], move[1]))
                        board.managePromotion(move[1], move[2]);

                    boardGui.updateBoard();
                }
                return null;
            }
            @Override
            protected void done() { //called when doInBackground finished (game over)
                Gui.displayGameOver(board);
            } 
        }.execute();
    }
    public static void runGuiGame(Board board, Gui boardGui, Player computer, boolean computerTurn) { //one human player
        boardGui.updateBoard();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() { //Gui and game logic must operate on seperate threads
                int[] move;

                while (board.gameState() == null) {

                    if (board.getMove() == computerTurn) {
                        long start = System.currentTimeMillis();
                        move = computer.getMove(board);
                        System.out.println("Move found in " + (System.currentTimeMillis()-start) + "ms");
                    }
                    else {
                        boardGui.waitForValidInput(); //proceeds when valid start and end are inputed
                        move = boardGui.getClicks();
                        boardGui.resetStartEnd(); //clears the Gui temp variables so new input is ready

                        boolean legal = false;
                        for (int[] i : board.getLegalMoves()) { //check for legality becuase human is playing
                            if (i[0] == move[0] && i[1] == move[1]) {
                                legal = true;
                                break;
                            }
                        }
                        if (!legal) continue;
                    }

                    if (board.manageMove(move[0], move[1])) //do the move
                        board.managePromotion(move[1], move[2]);

                    boardGui.updateBoard();
                }
                return null;
            }
            @Override
            protected void done() { //called when doInBackground finished (game over)
                Gui.displayGameOver(board);
            } 
        }.execute();
    }
}
