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
        if (depth == 0) return 1;

        long count = 0;
        int[] starts = board.getStartIndices(board.getMove());
        long[] allMoves = board.getLegalMoves(starts);

        for (int i = 0; i < starts.length; i++) {
            long moveBitmap = allMoves[i];
            while (moveBitmap != 0) {
                int j = Long.numberOfTrailingZeros(moveBitmap);
                moveBitmap &= ~(1L << j);

                try {
                    Board newBoard = new Board(this.board, starts[i], 63-j, false); //throws if promotion (throw invalidates construction of object)
                    count += new Perft(newBoard, this.depth - 1).countPositions();
                }
                catch (Exception e) {
                    try { //these never throw becuase promoting is true - just here to make VS Code happy
                        Board newBoard = new Board(this.board, starts[i], 63-j, true);
                        newBoard.manageMove(starts[i], 63-j);
                        Board promoBoard1 = new Board(newBoard, starts[i], 63-j, true);
                        Board promoBoard2 = new Board(newBoard, starts[i], 63-j, true);
                        Board promoBoard3 = new Board(newBoard, starts[i], 63-j, true);

                        newBoard.managePromotion((63-j)%8, 8);
                        promoBoard1.managePromotion((63-j)%8, 2);
                        promoBoard2.managePromotion((63-j)%8, 4);
                        promoBoard3.managePromotion((63-j)%8, 6);
                        
                        count += new Perft(newBoard, this.depth - 1).countPositions();
                        count += new Perft(promoBoard1, this.depth-1).countPositions();
                        count += new Perft(promoBoard2, this.depth-1).countPositions();
                        count += new Perft(promoBoard3, this.depth-1).countPositions();
                    } catch (Exception e2) {
                        throw new Error("It threw an error anyway");
                    } 
                }
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
    public static void d1PerftDivide(String FEN) {
        Board board = new Board(FEN);
        int[] starts = board.getStartIndices(board.getMove());
        long[] moves = board.getLegalMoves(starts);
        for (int i = 0; i < starts.length; i++) {
            System.out.println("\nStart: " + starts[i]);
            displayBitboard(moves[i]);
        }
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