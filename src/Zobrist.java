import java.util.Random;

public abstract class Zobrist {

    private static final long[][] SQUARE_RANDS = new long[12][64];
    private static final long MOVE_RAND;
    private static final long[] CASTLING_RANDS = new long[4];
    private static final long[] ENPASSANT_RANDS = new long[8];

    private static final Random rand = new Random(0x016069317E428CA9L);

    static {
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 64; j++) {
                SQUARE_RANDS[i][j] = rand.nextLong();
            }
        }
        MOVE_RAND = rand.nextLong();
        for (int i = 0; i < 4; i++) {
            CASTLING_RANDS[i] = rand.nextLong();
        }
        for (int i = 0; i < 8; i++) {
            ENPASSANT_RANDS[i] = rand.nextLong();
        }
    }
    public static long getHash(Board b) {
        long hash = 0L;

        //piece positions
        long[] bitboards = b.getBitboards();
        for (int i = 0; i < 12; i++) {
            long bb = bitboards[i];
            for (int j = 0; j < 64; j++) {
                if ((bb & (1L << (63-j))) != 0)
                    hash ^= SQUARE_RANDS[i][j];
            }
        }
        //move
        if (b.getMove()) {
            hash ^= MOVE_RAND;
        }
        //castling rights
        boolean[] castlingRights = b.getCastlingRights();
        for (int i = 0; i < 4; i++) {
            if (castlingRights[i])
                hash ^= CASTLING_RANDS[i];
        }
        //enPassant target
        if (b.getEnPassantTarget() != -1)
            hash ^= ENPASSANT_RANDS[b.getEnPassantTarget()%8];

        return hash;
    }
}