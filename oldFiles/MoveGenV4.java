import java.util.ArrayList;
import java.util.HashMap;

public class MoveGenV4 implements Player {

    private Board bestEndNode;
    private final HashMap<Long, TableEntry> transpositionTable = new HashMap<>();
    private final int MAX_DEPTH;

    public MoveGenV4(int depth) {
        MAX_DEPTH = depth;
    }

    private final int[] mgValues = new int[]{82, 337, 365, 477, 1025, 0}; //overall values P, K, B, R, Q, K
    private final int[] egValues = new int[]{94, 281, 297, 512, 936, 0};
    private final int[] phases = new int[]{0, 1, 1, 2, 4, 0}; //phase weights - same order
    private final int[][] mgTable = new int[][]{ //index directly for black - do index XOR 56 to flip for white
        {  // mg_pawn_table
            0,   0,   0,   0,   0,   0,  0,   0,
           98, 134,  61,  95,  68, 126, 34, -11,
           -6,   7,  26,  31,  65,  56, 25, -20,
          -14,  13,   6,  21,  23,  12, 17, -23,
          -27,  -2,  -5,  12,  17,   6, 10, -25,
          -26,  -4,  -4, -10,   3,   3, 33, -12,
          -35,  -1, -20, -23, -15,  24, 38, -22,
            0,   0,   0,   0,   0,   0,  0,   0
        },
        {  // mg_knight_table
          -167, -89, -34, -49,  61, -97, -15, -107,
           -73, -41,  72,  36,  23,  62,   7,  -17,
           -47,  60,  37,  65,  84, 129,  73,   44,
            -9,  17,  19,  53,  37,  69,  18,   22,
           -13,   4,  16,  13,  28,  19,  21,   -8,
           -23,  -9,  12,  10,  19,  17,  25,  -16,
           -29, -53, -12,  -3,  -1,  18, -14,  -19,
          -105, -21, -58, -33, -17, -28, -19,  -23
        },
        {  // mg_bishop_table
          -29,   4, -82, -37, -25, -42,   7,  -8,
          -26,  16, -18, -13,  30,  59,  18, -47,
          -16,  37,  43,  40,  35,  50,  37,  -2,
           -4,   5,  19,  50,  37,  37,   7,  -2,
           -6,  13,  13,  26,  34,  12,  10,   4,
            0,  15,  15,  15,  14,  27,  18,  10,
            4,  15,  16,   0,   7,  21,  33,   1,
          -33,  -3, -14, -21, -13, -12, -39, -21
        },
        {  // mg_rook_table
           32,  42,  32,  51, 63,  9,  31,  43,
           27,  32,  58,  62, 80, 67,  26,  44,
           -5,  19,  26,  36, 17, 45,  61,  16,
          -24, -11,   7,  26, 24, 35,  -8, -20,
          -36, -26, -12,  -1,  9, -7,   6, -23,
          -45, -25, -16, -17,  3,  0,  -5, -33,
          -44, -16, -20,  -9, -1, 11,  -6, -71,
          -19, -13,   1,  17, 16,  7, -37, -26
        },
        {  // mg_queen_table
          -28,   0,  29,  12,  59,  44,  43,  45,
          -24, -39,  -5,   1, -16,  57,  28,  54,
          -13, -17,   7,   8,  29,  56,  47,  57,
          -27, -27, -16, -16,  -1,  17,  -2,   1,
           -9, -26,  -9, -10,  -2,  -4,   3,  -3,
          -14,   2, -11,  -2,  -5,   2,  14,   5,
          -35,  -8,  11,   2,   8,  15,  -3,   1,
           -1, -18,  -9,  10, -15, -25, -31, -50
        },
        {  // mg_king_table
          -65,  23,  16, -15, -56, -34,   2,  13,
           29,  -1, -20,  -7,  -8,  -4, -38, -29,
           -9,  24,   2, -16, -20,   6,  22, -22,
          -17, -20, -12, -27, -30, -25, -14, -36,
          -49,  -1, -27, -39, -46, -44, -33, -51,
          -14, -14, -22, -46, -44, -30, -15, -27,
            1,   7,  -8, -64, -43, -16,   9,   8,
          -15,  36,  12, -54,   8, -28,  24,  14
        }
    };
    
    private final int[][] egTable = new int[][]{
        {  // eg_pawn_table
            0,   0,   0,   0,   0,   0,   0,   0,
          178, 173, 158, 134, 147, 132, 165, 187,
           94, 100,  85,  67,  56,  53,  82,  84,
           32,  24,  13,   5,  -2,   4,  17,  17,
           13,   9,  -3,  -7,  -7,  -8,   3,  -1,
            4,   7,  -6,   1,   0,  -5,  -1,  -8,
           13,   8,   8,  10,  13,   0,   2,  -7,
            0,   0,   0,   0,   0,   0,   0,   0
        },
        {  // eg_knight_table
          -58, -38, -13, -28, -31, -27, -63, -99,
          -25,  -8, -25,  -2,  -9, -25, -24, -52,
          -24, -20,  10,   9,  -1,  -9, -19, -41,
          -17,   3,  22,  22,  22,  11,   8, -18,
          -18,  -6,  16,  25,  16,  17,   4, -18,
          -23,  -3,  -1,  15,  10,  -3, -20, -22,
          -42, -20, -10,  -5,  -2, -20, -23, -44,
          -29, -51, -23, -15, -22, -18, -50, -64
        },
        {  // eg_bishop_table
          -14, -21, -11,  -8, -7,  -9, -17, -24,
          -8,  -4,   7, -12, -3,  -13, -4, -14,
            2,  -8,   0,  -1, -2,   6,   0,   4,
           -3,   9,  12,   9, 14,  10,   3,   2,
           -6,   3,  13,  19,  7,  10,  -3,  -9,
          -12,  -3,   8,  10, 13,   3,  -7,  -15,
          -14, -18,  -7,  -1,   4,  -9, -15, -27,
          -23,  -9, -23,  -5, -9, -16,  -5, -17
        },
        {  // eg_rook_table
           13,  10,  18,  15,  12,  12,   8,   5,
           11,  13,  13,  11,  -3,   3,   8,   3,
            7,   7,   7,   5,   4,  -3,  -5,  -3,
            4,   3,  13,   1,   2,   1,  -1,   2,
            3,   5,   8,   4,  -5,  -6,  -8, -11,
           -4,   0,  -5,  -1,  -7, -12,  -8, -16,
           -6,   0,   2,  -9,  -9,  -4,   3,  -19,
           -9,   2,   3,  -1,  -5,  -13,   4,  -20
        },
        {  // eg_queen_table
          -9,  22,  22,  27,  27,  19,  10,  20,
          -17,  20,  32,  41,  58,  25,  30,   0,
          -20,   6,   9,  49,  47,  35,  19,   9,
            3,  22,  24,  45,  57,  40,  57,  36,
          -18,  28,  19,  47,  31,  34,  39,  23,
          -16, -27,  15,   6,   9,  17,  10,   5,
          -22,  -23,  -30,  -16,  -16,  -23,  -36, -32,
          -33,  -28,  -22,  -43,  -5, -32, -20, -41
        },
        {  // eg_king_table
          -74, -35, -18, -18, -11,  15,   4, -17,
          -12,  17,  14,  17,  17,  38,  23,  11,
           10,  17,  23,  15,  20,  45,  44,  13,
          -8,  22,  24,  27,  26,  33,  26,   3,
          -18, -18,   0,  25,  25,  20,   6,  -8,
          -23,  -9, -11,  -3,  -5,  -16,   4, -20,
          -29, -53, -12,  -3,  -1,  18, -14,  -19,
          -105, -21, -58, -33, -17, -28, -19,  -23
        }
    };

    public int minimax(Board b, int depth, int alpha, int beta) {
        //check table
        long zobristKey = Zobrist.getHash(b);
        if (transpositionTable.containsKey(zobristKey)) {
            TableEntry entry = transpositionTable.get(zobristKey);
            if (entry.depth >= depth) {
                switch (entry.flag) {
                    case 0 -> {
                        return entry.value; //exact
                    }
                    case 1 -> { //lower bound
                        if (entry.value > alpha)
                            alpha = entry.value;
                    }
                    case 2 -> { //upper bound
                        if (entry.value < beta)
                            beta = entry.value;
                    }
                }
                if (alpha >= beta) return entry.value;
            }
        }

        switch (b.gameState()) { //0 play on, 1 draw, 2 white win, 3 black win
            case 0 -> { if (depth <= 0) return staticEval(b); }
            case 1 -> { return 0; }
            case 2 -> { return Integer.MAX_VALUE / 2 + depth; } // prefer faster mates by using depth
            case 3 -> { return Integer.MIN_VALUE / 2 - depth; }
        }

        int originalAlpha = alpha;
        int bestVal;
        Board bestNode = null;

        if (b.getMove()) {
            bestVal = Integer.MIN_VALUE;
            for (Board child : getChildren(b)) {
                int value = minimax(child, depth - 1, alpha, beta);
                if (value > bestVal) {
                    bestVal = value;
                    bestNode = child;
                }
                alpha = Integer.max(alpha, bestVal);
                if (beta <= alpha) break;
            }
        }
        else {
            bestVal = Integer.MAX_VALUE;
            for (Board child : getChildren(b)) {
                int value = minimax(child, depth - 1, alpha, beta);
                if (value < bestVal) {
                    bestVal = value;
                    bestNode = child;
                }
                beta = Integer.min(beta, bestVal);
                if (beta <= alpha) break;
            }
        }

        int flag;
        if (bestVal <= originalAlpha) //upper bound
            flag = 2; 
        else if (bestVal >= beta) //lower bound
            flag = 1; 
        else //exact
            flag = 0; 
        if (!transpositionTable.containsKey(zobristKey) || transpositionTable.get(zobristKey).depth < depth) {
            transpositionTable.put(zobristKey, new TableEntry(bestVal, depth, flag));
        }

        this.bestEndNode = bestNode;
        return bestVal;
    }
    private ArrayList<Board> getChildren(Board b) {
        int[] starts = b.getStartIndices(b.getMove());
        long[] allMoves = b.getLegalMoves(starts);
        int expectedTotal = 0;
        for (long move : allMoves) {
            expectedTotal += Long.bitCount(move);
        }
        ArrayList<Board> children = new ArrayList<>(expectedTotal); //very few resizes will occur (only promotions cause resize)

        for (int i = 0; i < starts.length; i++) {
            long currentMoves = allMoves[i];
            while (currentMoves != 0) {
                int j = Long.numberOfTrailingZeros(currentMoves);
                currentMoves &= ~(1L << j);
                try {
                    Board newBoard = new Board(b, starts[i], 63-j, false); //throws if promotion (throw invalidates construction of object)
                    children.add(newBoard);
                }
                catch (Exception e) {
                    handlePromotion(b, starts[i], 63-j, children);
                }
            }
        }
        return children;
    }
    private void handlePromotion(Board b, int start, int end, ArrayList<Board> children) {
        try {
            Board newBoard = new Board(b, start, end, true);
            newBoard.manageMove(start, end);
            Board promoBoard1 = new Board(newBoard, start, end, true);
            Board promoBoard2 = new Board(newBoard, start, end, true);
            Board promoBoard3 = new Board(newBoard, start, end, true);
            newBoard.managePromotion(end%8, 8);
            promoBoard1.managePromotion(end%8, 2);
            promoBoard2.managePromotion(end%8, 4);
            promoBoard3.managePromotion(end%8, 6);
            children.add(newBoard);
            children.add(promoBoard1);
            children.add(promoBoard2);
            children.add(promoBoard3);
        }
        catch (Exception e2) {
            throw new Error("Promotion handling failed");
        }
    }
    private int staticEval(Board b) {
        long[] BBs = b.getBitboards();
        int mgEval = 0;
        int egEval = 0;

        for (int i = 0; i < 12; i++) {
            long bb = BBs[i];
            int mgPieceValue = mgValues[i / 2];
            int egPieceValue = egValues[i / 2];
            int[] mgPieceTable = mgTable[i / 2];
            int[] egPieceTable = egTable[i / 2];

            while (bb != 0) {
                int j = Long.numberOfLeadingZeros(bb);
                bb &= ~(1L << (63-j));

                if (i%2 == 0) { //white piece
                    mgEval += (mgPieceValue + mgPieceTable[j^56]);
                    egEval += (egPieceValue + egPieceTable[j^56]);
                }
                else { //black piece
                    mgEval -= (mgPieceValue + mgPieceTable[j]);
                    egEval -= (egPieceValue + egPieceTable[j]);
                }
            }
        }
        //taper eval
        int phase = 24; //max phase - 24 is most endgame and 0 is least endgame
        for (int i = 0; i < 12; i+=2) {
            phase -= (Long.bitCount(BBs[i]|BBs[i+1]) * phases[i/2]);
        }

        return (mgEval*(24-phase) + egEval*phase) / 24;
    }
    @Override
    public int[] getMove(Board b) {
        minimax(b, MAX_DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE);
        if (bestEndNode == null || bestEndNode.getParent() == null) throw new Error("Null pointer for best node");
        while (bestEndNode.getParent().getParent() != null) {
            bestEndNode = bestEndNode.getParent();
        }
        return bestEndNode.getParentMove();
    }
    @Override
    public int getPromotionBB(Board b) {
        if (bestEndNode == null || bestEndNode.getParent() == null) throw new Error("Null pointer for best node - promotion");
        while (bestEndNode.getParent().getParent() != null) {
            bestEndNode = bestEndNode.getParent();
        }
        return (b.getMove() ? (bestEndNode.getBBfromIndex(bestEndNode.getParentMove()[1])-1) : bestEndNode.getBBfromIndex(bestEndNode.getParentMove()[1]));
    }
}
