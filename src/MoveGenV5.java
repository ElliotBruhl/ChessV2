import java.util.Arrays;
import java.util.HashMap;

public class MoveGenV5 implements Player {

    private record MoveNode (Board board, int[] move){} //used to compress board and move preformed into a single object so it can be returned as array
    private record TTEntry(int eval, int depth, int flag) {}  // flag: 0 = exact, -1 = upper bound, 1 = lower bound
    private final int MAX_DEPTH; //hard limit on depth - replace with time constraint later
    private int curDepth; //used to maintain indexing in lines across deepenings
    private int[][] PV; //stores principle variation - should be tried first
    private int[][] CV; //stores current variation so full line can be copied to PV if new best found
    private final HashMap<Long, TTEntry> TTable;
    public MoveGenV5(int depth) {
        this.MAX_DEPTH = depth;
        PV = new int[MAX_DEPTH][2];
        CV = new int[MAX_DEPTH][2];
        TTable = new HashMap<>();
    }
    
    private int miniMax(Board b, int depth, int alpha, int beta) {
        //transpositions
        long hash = Zobrist.getHash(b);
        if (TTable.containsKey(hash)) {
            TTEntry entry = TTable.get(hash);
            if (entry.depth() >= depth) {
                if (entry.flag() == 0) return entry.eval(); // exact
                if (entry.flag() == 1 && entry.eval() > alpha) alpha = entry.eval(); // lower bound
                if (entry.flag() == -1 && entry.eval() < beta) beta = entry.eval(); // upper bound
                if (alpha >= beta) return entry.eval(); // Cut-off
            }
        }
        //leaf nodes
        Integer gameState = b.gameState();
        if (gameState != null) {
            int eval;
            switch (gameState) {
                case 0 -> eval = 0;
                case Integer.MAX_VALUE/2 -> eval = gameState - depth;
                case Integer.MIN_VALUE/2 -> eval = gameState + depth;
                default -> throw new Error("Case " + gameState + " not found");
            }
            TTable.put(hash, new TTEntry(eval, depth, 0));
            return eval;
        }
        if (depth == 0) {
            int eval = evaluate(b);
            TTable.put(hash, new TTEntry(eval, depth, 0));
            return eval;
        }
        //recurse for all other nodes
        int bestEval;
        int flag;
        if (b.getMove()) {
            bestEval = Integer.MIN_VALUE;
            for (MoveNode node : sortNextMoves(b, depth)) {
                CV[curDepth-depth] = node.move();
                int eval = miniMax(node.board(), depth-1, alpha, beta);
                if (eval > bestEval) {
                    PV[curDepth-depth] = node.move();
                    bestEval = eval;
                }
                if (bestEval > alpha)
                    alpha = bestEval;
                if (beta <= alpha) break;
            }
            flag = alpha >= beta ? 1 : 0;
        }
        else {
            bestEval = Integer.MAX_VALUE;
            for (MoveNode node : sortNextMoves(b, depth)) {
                CV[curDepth-depth] = node.move();
                int eval = miniMax(node.board(), depth-1, alpha, beta);
                if (eval < bestEval) {
                    PV[curDepth-depth] = node.move();
                    bestEval = eval;
                }
                if (bestEval < beta)
                    beta = bestEval;
                if (beta <= alpha) break;
            }
            flag = alpha >= beta ? -1 : 0;
        }
        TTable.put(hash, new TTEntry(bestEval, depth, flag));
        return bestEval;
    }
    private int evaluate(Board b) { //put real function later
        int[] pieceValues = new int[]{1, -1, 5, -5, 3, -3, 3, -3, 9, -9};
        int total = 0;
        for (int i = 0; i < 10; i++) {
            total += (Long.bitCount(b.getBitboards()[i])*pieceValues[i]);
        }
        return total;
    }
    private MoveNode[] sortNextMoves(Board b, int depth) { //put more sorting (captures, checks, etc) later
        int[][] allMoves = b.getLegalMoves();
        MoveNode[] allNodes = new MoveNode[allMoves.length];

        if (PV[curDepth-depth] != null) {
            for (int i = 0; i < allMoves.length; i++) { //order moves
                if (allMoves[i] == PV[curDepth-depth]) {
                    int[] temp = allMoves[i];
                    allMoves[i] = allMoves[0];
                    allMoves[0] = temp;
                    break;
                }
            }
        }
        for (int i = 0; i < allMoves.length; i++) { //create nodes
            Board newBoard = b.newBoard();
            if (newBoard.manageMove(allMoves[i][0], allMoves[i][1]))
                newBoard.managePromotion(allMoves[i][1], 8);
            allNodes[i] = new MoveNode(newBoard, allMoves[i]);
        }
        return allNodes;
    }
    @Override
    public int[] getMove(Board b) {
        curDepth = MAX_DEPTH;
        System.out.println("eval: " + miniMax(b, MAX_DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE) + " with " + Arrays.deepToString(PV));
        return new int[]{PV[0][0], PV[0][1], 8};
    }
}
