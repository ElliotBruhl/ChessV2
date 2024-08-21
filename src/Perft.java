import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Perft {

    private final Board board;
    private final int depth;

    private Perft(Board b, int d) {
        this.board = b;
        this.depth = d;
    }

    private long countPositions() {
        if (this.depth == 0) return 1;

        long count = 0;
        int[][] moves = board.getLegalMoves();

        for (int[] move : moves) {
            Board newBoard = board.newBoard(); //create deep copy
            if (newBoard.manageMove(move[0], move[1])) { //theres a promotion
                for (int i = 2; i <= 8; i+=2) {
                    Board promoBoard = newBoard.newBoard();
                    promoBoard.managePromotion(move[1], i);
                    count += new Perft(promoBoard, this.depth-1).countPositions();
                }
            }
            else { //no promotion
                count += new Perft(newBoard, this.depth-1).countPositions();
            }
        }
        return count;
    }

    public static void runPerftTests(String filePath, int maxDepth) {
        int lineCounter = 1;
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                
                if (lineCounter%100 == 0) System.out.println("Parsing lines: "+lineCounter + " - " + (lineCounter+99));

                String[] splicedStr = line.split(",");
                Board board = new Board(splicedStr[0]);
                for (int i = 1; i<splicedStr.length && i<maxDepth; i++) {
                    long expected = Long.parseLong(splicedStr[i]);
                    long found = new Perft(board, i).countPositions();
                    if (expected != found) {
                        System.out.println("Test Failed on line "+lineCounter +": " + expected + " expected but " + found + " found");
                        System.out.println("FEN: " + splicedStr[0]);
                        System.out.println("Depth: " + i);
                    }
                }
                lineCounter++;
            }
            System.out.println("Test Finished");
        }
        catch (IOException e) {
            throw new Error("Error reading file");
        }
    }
    public static void runPerftTest(String FEN, int depth) {
        long startTime = System.nanoTime();
        long count = new Perft(new Board(FEN), depth).countPositions();
        long endTime = System.nanoTime();
        System.out.println(count + " positions at depth "+depth+" in "+((endTime-startTime)/1000000.0)+" ms");
    }
    public static void displayBitboard(long bitboard) { //for debugging
        String binaryString = Long.toBinaryString(bitboard);
        binaryString = String.format("%64s", binaryString).replace(' ', '0');
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                System.out.print(binaryString.charAt(index) + " ");
            }
            System.out.println();
        }
    }
}