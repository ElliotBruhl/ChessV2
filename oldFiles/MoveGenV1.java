import java.util.Random;

public class MoveGenV1 implements Player {

    final static Random rand = new Random();

    @Override
    public int[] getMove(Board b) {
        int start = -1;
        int end = -1;
        int[] startIndices = b.getStartIndices(b.getMove());
        while (start == -1) {
            int tempIndexS = rand.nextInt(startIndices.length);
            int startIndex = startIndices[tempIndexS];
            long legalMoves = b.getLegalMoves(new int[]{startIndex})[0];
            if (legalMoves != 0L) {
                start = startIndex;
                int tempIndexE = rand.nextInt(Long.bitCount(legalMoves));
                int count = 0;
                for (int i = 0; i < 64; i++) {
                    if ((legalMoves & (1L << 63-i)) != 0L) {
                        if (count++ == tempIndexE) {
                            end = i;
                            break;
                        }
                    }
                }
            }
        }
        return new int[]{start, end};
    }

    @Override
    public int getPromotionBB(Board b) {
        return rand.nextInt(4)*2+2;
    }
}