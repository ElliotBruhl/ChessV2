import java.util.ArrayList;

public class MoveGenV2 implements Player {

    private Board bestEndNode;
    private final int MAX_DEPTH;

    public MoveGenV2(int depth) {
        MAX_DEPTH = depth;
    }

    public int minimax(Board b, int depth, int alpha, int beta) {
        switch (b.gameState()) { //0 play on, 1 draw, 2 white win, 3 black win
            case 0 -> {if (depth <= 0) return staticEval(b);}
            case 1 -> {return 0;}
            case 2 -> {return Integer.MAX_VALUE/2+depth;} //prefer faster mates by using depth
            case 3 -> {return Integer.MIN_VALUE/2-depth;}
        }
        if (b.getMove()) {
            int bestVal = Integer.MIN_VALUE;
            Board bestNode = null;
            for (Board child : getChildren(b)) {
                int value = minimax(child, depth-1, alpha, beta);
                if (value > bestVal) {
                    bestVal = value;
                    bestNode = child;
                }
                alpha = Integer.max(alpha, bestVal);
                if (beta <= alpha) break;
            }
            this.bestEndNode = bestNode;
            return bestVal;
        }
        else {
            int bestVal = Integer.MAX_VALUE;
            Board bestNode = null;
            for (Board child : getChildren(b)) {
                int value = minimax(child, depth-1, alpha, beta);
                if (value < bestVal) {
                    bestVal = value;
                    bestNode = child;
                }
                beta = Integer.min(beta, bestVal);
                if (beta <= alpha) break;
            }
            this.bestEndNode = bestNode;
            return bestVal;
        }
    }
    private ArrayList<Board> getChildren(Board b) {

        int[] starts = b.getStartIndices(b.getMove());
        long[] allMoves = b.getLegalMoves(starts);
        //very few resizes will occur (only promotions cause resize)
        int expectedTotal = 0;
        for (long move : allMoves) {
            expectedTotal += Long.bitCount(move);
        }
        ArrayList<Board> children = new ArrayList<>(expectedTotal);

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
                    try { //these never throw becuase promoting is true - just here to make VS Code happy
                        Board newBoard = new Board(b, starts[i], 63-j, true);
                        newBoard.manageMove(starts[i], 63-j);
                        Board promoBoard1 = new Board(newBoard, starts[i], 63-j, true);
                        Board promoBoard2 = new Board(newBoard, starts[i], 63-j, true);
                        Board promoBoard3 = new Board(newBoard, starts[i], 63-j, true);

                        newBoard.managePromotion((63-j)%8, 8);
                        promoBoard1.managePromotion((63-j)%8, 2);
                        promoBoard2.managePromotion((63-j)%8, 4);
                        promoBoard3.managePromotion((63-j)%8, 6);

                        children.add(newBoard);
                        children.add(promoBoard1);
                        children.add(promoBoard2);
                        children.add(promoBoard3);
                    } catch (Exception e2) {
                        throw new Error("It threw an error anyway");
                    } 
                }
            }
        }
        return children;
    }
    private int staticEval(Board b) {
        int[] pieceValues = new int[]{1, -1, 5, -5, 3, -3, 3, -3, 9, -9, 100, -100};
        int total = 0;
        for (int i = 0; i < 12; i++) {
            total += (Long.bitCount(b.getBitboards()[i])*pieceValues[i]);
        }
        return total;
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