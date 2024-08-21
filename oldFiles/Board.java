import java.util.HashMap;
import java.util.Map;

public final class Board {
    
    private final long[] bitboards; //indices: [0-w pawns, 1-b pawns, 2-w rooks, 3-b rooks, 4-w knights, 5-b knights, 6-w bishop, 7-b bishop, 8-w queen, 9-b queen, 10-w king, 11-b king]
    private boolean move; //true for white and false for black
    private int enPassantTarget; //index of pawn than can be enPassanted
    private int halfmoves; //for 50 move rule
    private int fullmoves;
    private final boolean[] castlingRights;
    private final HashMap<Long, Integer> pastMoves;

    private final Board parent;
    private final int[] parentMove;

    //constuctors
    public Board(String FEN) { //for initial/current board state
        this.bitboards = new long[12];
        this.enPassantTarget = -1;
        this.castlingRights = new boolean[]{false, false, false, false}; //Kingside, Queenside, kingside, queenside (white then black)
        this.pastMoves = new HashMap<>();
        this.parent = null;
        this.parentMove = null;

        try {readFEN(FEN);}
        catch (Exception e) {throw new Error("Error reading FEN");}
    }
    @SuppressWarnings("unchecked")
    public Board(Board b, int start, int end, boolean promoting) throws Exception { //for generating future board states
        this.pastMoves = (HashMap<Long, Integer>) b.getPastMoves().clone(); //essentially a deep copy
        this.bitboards = b.getBitboards().clone(); //same thing
        this.move = b.getMove();
        this.enPassantTarget = b.getEnPassantTarget();
        this.castlingRights = b.getCastlingRights().clone();
        this.halfmoves = b.getHalfmoves();
        this.fullmoves = b.getFullmoves();
        this.parent = b;
        this.parentMove = new int[]{start, end};

        if (!promoting) {
            if (start != -1 && this.manageMove(start, end)) { //throw exception to signal a promotion
                throw new Exception();
            }
        }
    }
    //getters
    public long[] getBitboards() {
        return this.bitboards;
    }
    public boolean getMove() {
        return this.move;
    }
    public int getEnPassantTarget() {
        return this.enPassantTarget;
    }
    public int getHalfmoves() {
        return this.halfmoves;
    }
    public int getFullmoves() {
        return this.fullmoves;
    }
    public boolean[] getCastlingRights() {
        return this.castlingRights;
    }
    public HashMap<Long, Integer> getPastMoves() {
        return this.pastMoves;
    }
    public Board getParent() {
        return this.parent;
    }
    public int[] getParentMove() {
        return this.parentMove;
    }
    //utility functions
    public long getAllofColor(boolean m) { //if true return all of white's pieces and vice versa
        if (m)
            return this.bitboards[0] | this.bitboards[2] | this.bitboards[4] | this.bitboards[6] | this.bitboards[8] | this.bitboards[10];
        return this.bitboards[1] | this.bitboards[3] | this.bitboards[5] | this.bitboards[7] | this.bitboards[9] | this.bitboards[11];
    }
    public int getBBfromIndex(int index) {
        for (int i = 0; i < 12; i++) {
            if ((this.bitboards[i] & (1L << (63-index))) != 0) {
                return i;
            }
        }
        return -1;
    }
    public int[] getStartIndices(boolean m) {
        long friendly = getAllofColor(m);
        int[] starts = new int[Long.bitCount(friendly)];
        int nextIndex = 0;
        for (int i = 0; i < 64; i++) {
            if ((friendly & (1L << (63-i))) != 0) {
                starts[nextIndex] = i;
                nextIndex++;
            }
        }
        return starts;
    }
    public boolean hasLegalMove() {
        long[] allMoves = getLegalMoves(getStartIndices(this.move));
        for (long pieceMoves : allMoves) {
            if (pieceMoves != 0) return true;
        }
        return false;
    }
    public boolean fastInCheck(boolean m) {
        long blockers = this.bitboards[0]|this.bitboards[1]|this.bitboards[2]|this.bitboards[3]|this.bitboards[4]| //blockers except kings
        this.bitboards[5]|this.bitboards[6]|this.bitboards[7]|this.bitboards[8]|this.bitboards[9];
        int kIndex;
        if (m) {
            blockers |= this.bitboards[11];
            kIndex = Long.numberOfLeadingZeros(this.bitboards[10]);
        }
        else {
            blockers |= this.bitboards[10];
            kIndex = Long.numberOfLeadingZeros(this.bitboards[11]);
        }
        return ((pawnCheck(kIndex)|rookCheck(kIndex, blockers)|bishopCheck(kIndex, blockers)|knightCheck(kIndex)) != 0);
    }
    //game management
    public int gameState() { //0 play on, 1 draw, 2 white win, 3 black win
        //stalemate and checkmate
        if (!hasLegalMove()) {
            if (fastInCheck(this.move)) {
                return this.move ? 3 : 2;
            }
            return 1;
        }
        //comment out below this for perft
        if (this.halfmoves >= 100) { //50 move rule
            return 1;
        }
        if (Long.bitCount(getAllofColor(true) | getAllofColor(false)) < 4 && //less than 4 pieces
        this.bitboards[0] == 0L && this.bitboards[1] == 0L && //no pawns
        this.bitboards[2] == 0L && this.bitboards[3] == 0L && //no rooks
        this.bitboards[8] == 0L && this.bitboards[9] == 0L) {//no queens
            return 1;
        }
        for (int i : this.pastMoves.values()) { //repitition
            if (i > 2)
                return 1;
        }
        return 0;
    }
    public boolean manageMove(int start, int end) {
        boolean promotion = false; //true if promotion occurs
        int startBBIndex = getBBfromIndex(start); //type of piece
        switch (startBBIndex) {
            case 0 -> {
                if (end == this.enPassantTarget) { //for white remove black pawn
                    this.bitboards[1] &= ~(1L << (63-(end - 8)));
                }
                else if (end >= 56) {
                    promotion = true; //promotion flag set
                    this.bitboards[0] &= ~(1L << (63-start)); //remove pawn
                }
                this.halfmoves = -1;
                this.pastMoves.clear();
            }
            case 1 -> {
                if (end == this.enPassantTarget) { //for black remove white pawn
                    this.bitboards[0] &= ~(1L << (63-(end + 8)));
                }
                else if (end <= 7) {
                    promotion = true; //promotion flag set
                    this.bitboards[1] &= ~(1L << (63-start)); //remove pawn
                }
                this.halfmoves = -1;
                this.pastMoves.clear();
            }
            case 2 -> {
                if (start == 7)
                    this.castlingRights[0] = false;
                else if (start == 0)
                    this.castlingRights[1] = false;
            }
            case 3 -> {
                if (start == 63)
                    this.castlingRights[2] = false;
                else if (start == 56)
                    this.castlingRights[3] = false;
            }
            case 10 -> {
                if (start == 4) { // White king
                    if (end == 6) {
                        this.bitboards[2] &= ~(1L << 56); // Kingside
                        this.bitboards[2] |= (1L << 58);
                    }
                    else if (end == 2) {
                        this.bitboards[2] &= ~(1L << 63); // Queenside
                        this.bitboards[2] |= (1L << 60);
                    }
                }
                this.castlingRights[0] = false;
                this.castlingRights[1] = false;
            }
            case 11 -> {
                if (start == 60) {
                    if (end == 62) { //kingside
                        this.bitboards[3] &= ~(1L);
                        this.bitboards[3] |= (1L << 2);
                    }
                    else if (end == 58) { //queenside
                        this.bitboards[3] &= ~(1L << 7);
                        this.bitboards[3] |= (1L << 4);
                    }
                }
                this.castlingRights[2] = false;
                this.castlingRights[3] = false;
            }
        }
        // Manage en passant target
        this.enPassantTarget = -1;
        if (end - start == 16 && startBBIndex == 0) {
            this.enPassantTarget = start + 8;
        }
        else if (start - end == 16 && startBBIndex == 1) {
            this.enPassantTarget = start - 8;
        }
        // Manage halfmoves and past positions
        int capturedType = getBBfromIndex(end); //is there a piece on ending square
        if (capturedType != -1) { //irreversible move
            this.halfmoves = -1;
            this.pastMoves.clear();
            this.bitboards[capturedType] &= ~(1L << (63-end));
        }
        //normal piece movement
        if (!promotion) {
            this.bitboards[startBBIndex] &= ~(1L << (63-start));
            this.bitboards[startBBIndex] |= (1L << (63-end));
        }
        //change move and update past moves
        this.pastMoves.merge(Zobrist.getHash(this), 1, Integer::sum); //adds (key, value) or increment value if present
        this.move = !this.move;
        this.halfmoves++;
        this.fullmoves = this.move ? this.fullmoves+1 : this.fullmoves;

        return promotion;
    }
    public void managePromotion(int file, int bbNum) { //bbNum: pass in -1 to choose piece (GUI), computer passes in 2, 4, 6, 8 for rook, knight, bishop, queen, respectively
        if (bbNum == -1) {
            switch (Gui.selectPromotionPiece()) {
                case "Queen" -> bbNum = 8;
                case "Rook" -> bbNum = 2;
                case "Knight" -> bbNum = 4;
                case "Bishop" -> bbNum = 6;
            }
        }
        //promotion is managed after move change
        if (this.move) { //for black
            this.bitboards[bbNum + 1] |= (1L << 63-file);
            this.bitboards[1] &= ~(1L << 63-file);
        }
        else { //for white
            this.bitboards[bbNum] |= (1L << 7-file);
            this.bitboards[0] &= ~(1L << 7-file);
        }
    }
    //move generation
    public long[] getLegalMoves(int[] starts) {
        //initialization info
        long[] allMoves = new long[starts.length];
        long friendly = getAllofColor(this.move);
        long opposing = getAllofColor(!this.move);
        long blockers = friendly|opposing;
        long attackMask = getAttackMask();
        //detect and handle a check
        int kIndex = (Long.numberOfLeadingZeros(this.move ? this.bitboards[10] : this.bitboards[11]));
        long rookCMask = rookCheck(kIndex, blockers);
        long bishopCMask = bishopCheck(kIndex, blockers);
        long checkerMask = pawnCheck(kIndex)|knightCheck(kIndex);
        if (rookCMask != 0L) { //move the checker into its own mask
            checkerMask |= (rookCMask&blockers);
            rookCMask &= ~checkerMask;
        }
        if (bishopCMask != 0L) {
            checkerMask |= (bishopCMask&blockers);
            bishopCMask &= ~checkerMask;
        }
        long checkTraceMask = rookCMask|bishopCMask; //check trace for ray pieces
        int checkNum = Long.bitCount(checkerMask);
        if (checkNum == 0) { //no checks
            checkTraceMask = ~0L;
            checkerMask = ~0L;
        }
        else if (checkNum > 1) { //double check
            checkTraceMask = 0L;
            checkerMask = 0L;
        }
        //handle pins
        long HVPinMask = getPinnersHV(kIndex);
        long DiagPinMask = getPinnersDiag(kIndex);

        for (int i = 0; i<starts.length; i++) {
            int start = starts[i];
            long legalMoves = 0L;
            int BBIndex = getBBfromIndex(start);
            long tempHVPinMask = (HVPinMask & (1L << (63 - start))) != 0L ? HVPinMask : ~0L;
            long tempDiagPinMask = (DiagPinMask & (1L << (63 - start))) != 0L ? DiagPinMask : ~0L;

            switch (BBIndex) {
                case 0 -> { //causes problems if pawns are on last rank (shouldn't be possible)
                    if ((blockers & (1L << (55-start))) == 0 && tempDiagPinMask == ~0L) { //no forward when pinned diag
                        legalMoves |= ((1L << (55-start))&checkTraceMask&tempHVPinMask);
                        if ((blockers & (1L << (47-start))) == 0 && start/8 == 1) {
                            legalMoves |= ((1L << (47-start))&checkTraceMask&tempHVPinMask);
                        }
                    }
                    if (tempHVPinMask == ~0L) { //no captures when pinned HV
                        if ((opposing & (1L << (56-start))) != 0 && start%8 != 0) { //captures
                            legalMoves |= ((1L << (56-start))&checkerMask&tempDiagPinMask);
                        }
                        if ((opposing & (1L << (54-start))) != 0 && start%8 != 7) {
                            legalMoves |= ((1L << (54-start))&checkerMask&tempDiagPinMask);
                        }
                        if ((this.enPassantTarget == start+7 && start%8 != 0) || (this.enPassantTarget == start+9 && start%8 != 7)) { //enPassant is evaluated rarely (doesn't have to be very optimized)
                            if (((checkerMask & (1L << (63-(this.enPassantTarget-8)))) != 0) || //not in check or in check from pawn being taken or
                            ((checkTraceMask & (1L << (63-this.enPassantTarget))) != 0)) { //will block the check
                                if ((bishopCheck(kIndex, (blockers&~(1L << (63-(this.enPassantTarget-8))))) == 0) && //opponent pawn is not pinned diagonally and
                                (rookCheck(kIndex, ((blockers&~(1L << (63-(this.enPassantTarget-8))))&~(1L << (63-start))|(1L << (63-this.enPassantTarget)))) == 0)) {  //both pawns are not pinned horizontally
                                    legalMoves |= ((1L << (63-this.enPassantTarget))&tempDiagPinMask);
                                }
                            }
                        }
                    }
                    allMoves[i] = legalMoves;
                }
                case 1 -> {
                    if ((blockers & (1L << (71-start))) == 0 && tempDiagPinMask == ~0L) { //no forward when pinned diag
                        legalMoves |= ((1L << (71-start))&checkTraceMask&tempHVPinMask);
                        if ((blockers & (1L << (79-start))) == 0 && start / 8 == 6) {
                            legalMoves |= ((1L << (79-start))&checkTraceMask&tempHVPinMask);
                        }
                    }
                    if (tempHVPinMask == ~0L) { //no captures when pinned HV
                        if ((opposing & (1L << (72-start))) != 0 && start % 8 != 0) { //captures
                            legalMoves |= ((1L << (72-start))&checkerMask&tempDiagPinMask);
                        }
                        if ((opposing & (1L << (70-start))) != 0 && start % 8 != 7) {
                            legalMoves |= ((1L << (70-start)) & checkerMask&tempDiagPinMask);
                        }
                        if ((this.enPassantTarget == start-9 && start%8 != 0) || (this.enPassantTarget == start-7 && start%8 != 7)) { //enPassant is evaluated rarely (doesn't have to be very optimized)
                            if (((checkerMask & (1L << (63-(this.enPassantTarget+8)))) != 0) || //not in check or in check from pawn being taken or
                            ((checkTraceMask & (1L << (63-this.enPassantTarget))) != 0)) { //will block the check
                                if ((bishopCheck(kIndex, (blockers&~(1L << (63-(this.enPassantTarget+8))))) == 0) && //opponent pawn is not pinned diagonally and
                                (rookCheck(kIndex, ((blockers&~(1L << (63-(this.enPassantTarget+8))))&~(1L << (63-start))|(1L << (63-this.enPassantTarget)))) == 0)) {  //both pawns are not pinned horizontally
                                    legalMoves |= ((1L << (63-this.enPassantTarget))&tempDiagPinMask);
                                }
                            }
                        }
                    }
                    allMoves[i] = legalMoves;
                }
                case 2, 3-> {
                    if (tempDiagPinMask == ~0L) //not pinned diag
                        allMoves[i] = (MagicBB.RookAttacks(63-start, blockers)&(checkerMask|checkTraceMask)&~friendly&tempHVPinMask);
                }
                case 4, 5 -> {
                    if ((tempDiagPinMask&tempHVPinMask) == ~0L)
                        allMoves[i] = (MagicBB.KnightAttacks(start)&(checkerMask|checkTraceMask)&~friendly);
                }
                case 6, 7 -> {
                    if (tempHVPinMask == ~0L)
                        allMoves[i] = (MagicBB.BishopAttacks(63-start, blockers)&(checkerMask|checkTraceMask)&~friendly&tempDiagPinMask);
                }
                case 8, 9 -> {
                    if (tempDiagPinMask != ~0L) //pinned on diag
                        allMoves[i] = (MagicBB.BishopAttacks(63-start, blockers)&(checkerMask|checkTraceMask)&~friendly&tempDiagPinMask);
                    else if (tempHVPinMask != ~0L) //pinned on HV
                        allMoves[i] = (MagicBB.RookAttacks(63-start, blockers)&(checkerMask|checkTraceMask)&~friendly&tempHVPinMask);
                    else
                        allMoves[i] = (MagicBB.QueenAttacks(63-start, blockers)&(checkerMask|checkTraceMask)&~friendly);
                }
                case 10 -> {
                    legalMoves |= (MagicBB.KingAttacks(start) & ~attackMask & ~friendly); //normal moves
                    if (start == 4 && (attackMask & (1L << 59)) == 0) { //castling
                        if (this.castlingRights[0] && ((attackMask|blockers) & (1L << 58)) == 0 && ((attackMask|blockers) & (1L << 57)) == 0 && (this.bitboards[2] & (1L << 56)) != 0) {
                            legalMoves |= (1L << 57);
                        }
                        if (this.castlingRights[1] && ((attackMask|blockers) & (1L << 60)) == 0 && ((attackMask|blockers) & (1L << 61)) == 0 && (blockers & (1L << 62)) == 0 && (this.bitboards[2] & (1L << 63)) != 0) {
                            legalMoves |= (1L << 61);
                        }
                    }
                    allMoves[i] = legalMoves;
                }
                case 11 -> {
                    legalMoves |= (MagicBB.KingAttacks(start) & ~getAttackMask() & ~friendly);
                    if (start == 60 && (attackMask & (1L << 3)) == 0) {
                        if (this.castlingRights[2] && ((attackMask|blockers) & (1L << 2)) == 0 && ((attackMask|blockers) & (1L << 1)) == 0 && (this.bitboards[3] & (1L)) != 0) {
                            legalMoves |= (1L << 1);
                        }
                        if (this.castlingRights[3] && ((attackMask|blockers) & (1L << 4)) == 0 && ((attackMask|blockers) & (1L << 5)) == 0 && (blockers & (1L << 6)) == 0 && (this.bitboards[3] & (1L << 7)) != 0) {
                            legalMoves |= (1L << 5);
                        }
                    }
                    allMoves[i] = legalMoves;
                }
                default -> throw new Error("Bad BBIndex indicator: "+BBIndex);
            }
        }
        return allMoves;
    }
    private long pawnCheck(int kIndex) {
        if ((kIndex > 47 && this.move) || (kIndex < 16 && !this.move)) return 0L;
        long pawnMask = 0L;
        switch (kIndex%8) {
            case 0 -> {
                pawnMask |= this.move ? (1L << (54-kIndex)) : (1L << (70-kIndex));
            }
            case 7 -> {
                pawnMask |= this.move ? (1L << (56-kIndex)) : (1L << (72-kIndex));
            }
            default -> {
                pawnMask |= this.move ? (1L << (56-kIndex)) : (1L << (72-kIndex));
                pawnMask |= this.move ? (1L << (54-kIndex)) : (1L << (70-kIndex));
            }
        }
        return pawnMask & (this.move ? this.bitboards[1] : this.bitboards[0]);
    }
    private long rookCheck(int kIndex, long blockers) { //traces path of check or 0L if no check
        long rookMask = MagicBB.RookAttacks(63-kIndex, blockers);
        long oppRQPos = this.move ? (this.bitboards[3]|this.bitboards[9]) : (this.bitboards[2]|this.bitboards[8]);
        int checkIndex = Long.numberOfLeadingZeros(rookMask & oppRQPos);
        if (checkIndex == 64) return 0L; //no checking rook/queen as rook
        if (Long.bitCount(rookMask&oppRQPos) == 2) { //wild special case where promotion allows for two rook checks
            int checkIndex2 = Long.numberOfLeadingZeros(rookMask&oppRQPos&~(1L << (63-checkIndex)));
            rookMask &= (MagicBB.RookAttacks(63-checkIndex, blockers)|MagicBB.RookAttacks(63-checkIndex2, blockers));
            rookMask |= (1L << (63-checkIndex));
            rookMask |= (1L << (63-checkIndex2));
            return rookMask;
        }
        rookMask &= MagicBB.RookAttacks(63-checkIndex, blockers); //remove directions other than checking direction
        rookMask |= (1L << (63-checkIndex));
        return rookMask;
    }
    private long bishopCheck(int kIndex, long blockers) { //traces path of check or 0L if no check
        long bishopMask = MagicBB.BishopAttacks(63-kIndex, blockers);
        long oppBQPos = this.move ? (this.bitboards[7]|this.bitboards[9]) : (this.bitboards[6]|this.bitboards[8]);
        int checkIndex = Long.numberOfLeadingZeros(bishopMask& oppBQPos);
        if (checkIndex == 64) return 0L; //no checking bishop/queen as bishop
        bishopMask &= MagicBB.BishopAttacks(63-checkIndex, blockers); //remove other directions
        bishopMask |= (1L << (63-checkIndex));
        return bishopMask;
    }
    private long knightCheck(int kIndex) {
        return this.move ? 
        (MagicBB.KnightAttacks(kIndex) & this.bitboards[5]) :
        (MagicBB.KnightAttacks(kIndex) & this.bitboards[4]);
    }
    private long getPinnersHV(int kIndex) {
        long oppRQPos = this.move ? (this.bitboards[3]|this.bitboards[9]) : (this.bitboards[2]|this.bitboards[8]);
        long blockers = getAllofColor(true)|getAllofColor(false);
        long xRayHV = MagicBB.xrayHV(kIndex);
        long possiblePinners = xRayHV&oppRQPos; //R/Q is lined up with king with (?) pieces in between
        if (possiblePinners == 0L) return 0L;
        long pinMask = 0L;

        for (int i = 0; i < 64; i++) { //loop through each possible pinner
            if ((possiblePinners & (1L << (63-i))) != 0) {
                long pinnedPiece = (MagicBB.RookAttacks(63-kIndex, blockers)&MagicBB.RookAttacks(63-i, blockers)&getAllofColor(this.move)); //both can see same piece of correct color on same line
                if (pinnedPiece != 0L) { //one is found
                    pinMask |= ((MagicBB.RookAttacks(63-Long.numberOfLeadingZeros(pinnedPiece), blockers) & xRayHV)); //make a pinmask with the info
                    pinMask |= pinnedPiece;
                }
            }
        }
        return pinMask;
    }
    private long getPinnersDiag(int kIndex) {
        long oppBQPos = this.move ? (this.bitboards[7]|this.bitboards[9]) : (this.bitboards[6]|this.bitboards[8]);
        long blockers = getAllofColor(true)|getAllofColor(false);
        long xRayDiag = MagicBB.xrayDiag(kIndex);
        long possiblePinners = xRayDiag&oppBQPos;
        if (possiblePinners == 0L) return 0L;
        long pinMask = 0L;

        for (int i = 0; i < 64; i++) {
            if ((possiblePinners & (1L << (63-i))) != 0) {
                long pinnedPiece = (MagicBB.BishopAttacks(63-kIndex, blockers)&MagicBB.BishopAttacks(63-i, blockers)&getAllofColor(this.move)); //both can see same piece of correct color
                if (pinnedPiece != 0L) {
                    pinMask |= ((MagicBB.BishopAttacks(63-Long.numberOfLeadingZeros(pinnedPiece), blockers) & xRayDiag)); //make a pinmask with the info
                    pinMask |= pinnedPiece;
                }
            }
        }
        return pinMask;
    }
    private long getAttackMask() {
        //pseudo legal generation except:
            //opposing kings are pierced from ray attacks
            //own pieces are added as attack squares
            //enPassant/castling/forward pawn moves excluded
        long attackMask = 0L;
        for (int start : getStartIndices(!this.move)) {
            switch (getBBfromIndex(start)) {
                case 0-> { //wPawns
                    if (start%8 != 0)
                        attackMask |= (1L << (56-start));
                    if (start%8 != 7)
                        attackMask |= (1L << (54-start));
                }
                case 1 -> { //bPawns
                    if (start%8 != 0)
                        attackMask |= (1L << (72-start));
                    if (start%8 != 7)
                        attackMask |= (1L << (70-start));
                }
                case 2 -> { //wRooks
                    attackMask |= MagicBB.RookAttacks(63-start, ((getAllofColor(true)|getAllofColor(false)) & ~this.bitboards[11]));
                }
                case 3 -> { //bRooks
                    attackMask |= MagicBB.RookAttacks(63-start, ((getAllofColor(true)|getAllofColor(false)) & ~this.bitboards[10]));
                }
                case 4-> { //wKnights
                    attackMask |= MagicBB.KnightAttacks(start);
                }
                case 5 -> { //bKnights
                    attackMask |= MagicBB.KnightAttacks(start);
                }
                case 6 -> { //wBishops
                    attackMask |= MagicBB.BishopAttacks(63-start, ((getAllofColor(true)|getAllofColor(false)) & ~this.bitboards[11]));
                }
                case 7 -> { //bBishops
                    attackMask |= MagicBB.BishopAttacks(63-start, ((getAllofColor(true)|getAllofColor(false)) & ~this.bitboards[10]));
                }
                case 8 -> { //wQueen
                    attackMask |= MagicBB.QueenAttacks(63-start, ((getAllofColor(true)|getAllofColor(false)) & ~this.bitboards[11]));
                }
                case 9 -> { //bQueen
                    attackMask |= MagicBB.QueenAttacks(63-start, ((getAllofColor(true)|getAllofColor(false)) & ~this.bitboards[10]));
                }
                case 10 -> { //wKing
                    attackMask |= MagicBB.KingAttacks(start);
                }
                case 11 -> { //bKing
                    attackMask |= MagicBB.KingAttacks(start);
                }
                default -> throw new Error("Bad bitboard index indicator");
            }
        }
        return attackMask;
    }
    //read fens
    private void readFEN(String FEN) {
        final Map<Character, Integer> FENtoBBindices = new HashMap<>() {{
            put('P', 0);  // White pawns
            put('p', 1);  // Black pawns
            put('R', 2);  // White rooks
            put('r', 3);  // Black rooks
            put('N', 4);  // White knights
            put('n', 5);  // Black knights
            put('B', 6);  // White bishops
            put('b', 7);  // Black bishops
            put('Q', 8);  // White queen
            put('q', 9);  // Black queen
            put('K', 10); // White king
            put('k', 11); // Black king
        }};
        String[] splitFEN = FEN.split(" "); //splits fen into the 6 pieces
        //-----------------------------handles position part of FEN-----------------------------
        int current_square = 0;
        int file = 0;
        for (int i = 0; i < splitFEN[0].length(); i++) {
            char c = splitFEN[0].charAt(i);
            if (Character.isDigit(c)) { // If it's a digit, it represents empty squares
                int emptySquares = Character.getNumericValue(c);
                current_square += emptySquares;
                file += emptySquares;
            }
            else if (c == '/') { // If '/' move to the next rank
                current_square += (8-file);
                file = 0;
            }
            else { // There's a piece there
                this.bitboards[FENtoBBindices.get(c)] |= (1L << (63-((7 - current_square / 8) * 8 + current_square % 8))); //put piece on correct bitboard and index
                current_square++;
                file++;
            }
        }
        //-----------------------------handles move(w/b) part of FEN-----------------------------
        this.move = (splitFEN[1].charAt(0) == 'w');
        //-----------------------------handles castling rights part of FEN-----------------------------
        if (splitFEN[2].contains("K"))
            this.castlingRights[0] = true;
        if (splitFEN[2].contains("Q"))
            this.castlingRights[1] = true;
        if (splitFEN[2].contains("k"))
            this.castlingRights[2] = true;
        if (splitFEN[2].contains("q"))
            this.castlingRights[3] = true;
        //-----------------------------handles enPassant target part of FEN-----------------------------
        if (splitFEN[3].length() == 2) {
            this.enPassantTarget = ((splitFEN[3].charAt(1) - '1') * 8) + (splitFEN[3].charAt(0) - 'a'); //converts fram algebraic notation to index
        }
        //-----------------------------handles halfmove part of FEN-----------------------------
        try {
            this.halfmoves = Integer.parseInt(splitFEN[4]);
        }
        catch (IndexOutOfBoundsException e) {
            this.halfmoves = 0;
        }
        //-----------------------------handles fullmove part of FEN---------------------------------
        try {
            this.fullmoves = Integer.parseInt(splitFEN[5]);
        }
        catch (IndexOutOfBoundsException e) {
            this.fullmoves = 1;
        }
    }
}