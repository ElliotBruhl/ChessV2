/*
 *Copyright (C) 2007 Pradyumna Kannan.
 *
 *This code is provided 'as-is', without any expressed or implied warranty.
 *In no event will the authors be held liable for any damages arising from
 *the use of this code. Permission is granted to anyone to use this
 *code for any purpose, including commercial applications, and to alter
 *it and redistribute it freely, subject to the following restrictions:
 *
 *1. The origin of this code must not be misrepresented; you must not
 *claim that you wrote the original code. If you use this code in a
 *product, an acknowledgment in the product documentation would be
 *appreciated but is not required.
 *
 *2. Altered source versions must be plainly marked as such, and must not be
 *misrepresented as being the original code.
 *
 *3. This notice may not be removed or altered from any source distribution.
 *
 * -------------------------------------------------------------------------
 * Converted from C to C# code by Rudy Alex Kohn, 2017 - for use in MUCI.
 * The same conditions as described above is *STILL* in effect.
 *
 * Converted to Java by Rudy Alex Kohn, 2020 - for general use.
 * The same conditions as described above is *STILL* in effect.
 * 
 * Modified 2024 by Elliot Bruhl - added simple move bitboards for other
 * pieces, added operations to return bitboards based on correct color, 
 * added xRay lookups for pin checking,
 * reversed some functions (my bitboards have most significant bit as a1).
 * The same conditions as described above are *STILL* in effect.
 * 
 * Original files: magicmoves.c and magicmoves.h
 * Conditional compile paths were removed.
 * The original algorithm and data were not changed in any way.
 *
 * WHATEVER YOU DO, DO NOT ALTER ANYTHING IN HERE!
 *
 */
import java.util.Arrays;

public abstract class MagicBB {

    private static final long ONE = 1L;
    private static final long FF = 0xFFL;
    private static final int MAGIC_BISHOP_DB_LENGTH = 512;
    private static final int MAGIC_ROOK_DB_LENGTH = 4096;

    private static final long[] XRAY_HV = {
        0x7f80808080808080L, 0xbf40404040404040L, 0xdf20202020202020L, 0xef10101010101010L, 0xf708080808080808L, 0xfb04040404040404L, 0xfd02020202020202L, 0xfe01010101010101L, 
        0x807f808080808080L, 0x40bf404040404040L, 0x20df202020202020L, 0x10ef101010101010L, 0x8f7080808080808L, 0x4fb040404040404L, 0x2fd020202020202L, 0x1fe010101010101L,
        0x80807f8080808080L, 0x4040bf4040404040L, 0x2020df2020202020L, 0x1010ef1010101010L, 0x808f70808080808L, 0x404fb0404040404L, 0x202fd0202020202L, 0x101fe0101010101L,
        0x8080807f80808080L, 0x404040bf40404040L, 0x202020df20202020L, 0x101010ef10101010L, 0x80808f708080808L, 0x40404fb04040404L, 0x20202fd02020202L, 0x10101fe01010101L,
        0x808080807f808080L, 0x40404040bf404040L, 0x20202020df202020L, 0x10101010ef101010L, 0x8080808f7080808L, 0x4040404fb040404L, 0x2020202fd020202L, 0x1010101fe010101L,
        0x80808080807f8080L, 0x4040404040bf4040L, 0x2020202020df2020L, 0x1010101010ef1010L, 0x808080808f70808L, 0x404040404fb0404L, 0x202020202fd0202L, 0x101010101fe0101L,
        0x8080808080807f80L, 0x404040404040bf40L, 0x202020202020df20L, 0x101010101010ef10L, 0x80808080808f708L, 0x40404040404fb04L, 0x20202020202fd02L, 0x10101010101fe01L,
        0x808080808080807fL, 0x40404040404040bfL, 0x20202020202020dfL, 0x10101010101010efL, 0x8080808080808f7L, 0x4040404040404fbL, 0x2020202020202fdL, 0x1010101010101feL
    };
    private static final long[] XRAY_DIAGONAL = {
        0x40201008040201L, 0xa0100804020100L, 0x50880402010000L, 0x28448201000000L, 0x14224180000000L, 0xa112040800000L, 0x5081020408000L, 0x2040810204080L, 
        0x4000402010080402L, 0xa000a01008040201L, 0x5000508804020100L, 0x2800284482010000L, 0x1400142241800000L, 0xa000a1120408000L, 0x500050810204080L, 0x200020408102040L,
        0x2040004020100804L, 0x10a000a010080402L, 0x8850005088040201L, 0x4428002844820100L, 0x2214001422418000L, 0x110a000a11204080L, 0x805000508102040L, 0x402000204081020L,
        0x1020400040201008L, 0x810a000a0100804L, 0x488500050880402L, 0x8244280028448201L, 0x4122140014224180L, 0x20110a000a112040L, 0x1008050005081020L, 0x804020002040810L,
        0x810204000402010L, 0x40810a000a01008L, 0x204885000508804L, 0x182442800284482L, 0x8041221400142241L, 0x4020110a000a1120L, 0x2010080500050810L, 0x1008040200020408L,
        0x408102040004020L, 0x2040810a000a010L, 0x102048850005088L, 0x1824428002844L, 0x80412214001422L, 0x804020110a000a11L, 0x4020100805000508L, 0x2010080402000204L,
        0x204081020400040L, 0x102040810a000a0L, 0x1020488500050L, 0x18244280028L, 0x804122140014L, 0x804020110a000aL, 0x8040201008050005L, 0x4020100804020002L,
        0x102040810204000L, 0x102040810a000L, 0x10204885000L, 0x182442800L, 0x8041221400L, 0x804020110a00L, 0x80402010080500L, 0x8040201008040200L
    };
    private static final long[] KnightMovesBB = {
        0x20400000000000L, 0x10A00000000000L, 0x88500000000000L, 0x44280000000000L, 0x22140000000000L, 0x110A0000000000L, 0x8050000000000L, 0x4020000000000L,
        0x2000204000000000L, 0x100010A000000000L, 0x8800885000000000L, 0x4400442800000000L, 0x2200221400000000L, 0x1100110A00000000L, 0x800080500000000L, 0x400040200000000L,
        0x4020002040000000L, 0xA0100010A0000000L, 0x5088008850000000L, 0x2844004428000000L, 0x1422002214000000L, 0xA1100110A000000L, 0x508000805000000L, 0x204000402000000L,
        0x40200020400000L, 0xA0100010A00000L, 0x50880088500000L, 0x28440044280000L, 0x14220022140000L, 0xA1100110A0000L, 0x5080008050000L, 0x2040004020000L,
        0x402000204000L, 0xA0100010A000L, 0x508800885000L, 0x284400442800L, 0x142200221400L, 0xA1100110A00L, 0x50800080500L, 0x20400040200L,
        0x4020002040L, 0xA0100010A0L, 0x5088008850L, 0x2844004428L, 0x1422002214L, 0xA1100110AL, 0x508000805L, 0x204000402L,
        0x40200020L, 0xA0100010L, 0x50880088L, 0x28440044L, 0x14220022L, 0xA110011L, 0x5080008L, 0x2040004L,
        0x402000L, 0xA01000L, 0x508800L, 0x284400L, 0x142200L, 0xA1100L, 0x50800L, 0x20400L
    };
    private static final long[] KingMovesBB = {
        0x40C0000000000000L, 0xA0E0000000000000L, 0x5070000000000000L, 0x2838000000000000L, 0x141C000000000000L, 0x0A0E000000000000L, 0x0507000000000000L, 0x0203000000000000L, 
        0xC040C00000000000L, 0xE0A0E00000000000L, 0x7050700000000000L, 0x3828380000000000L, 0x1C141C0000000000L, 0x0E0A0E0000000000L, 0x0705070000000000L, 0x0302030000000000L, 
        0x00C040C000000000L, 0x00E0A0E000000000L, 0x0070507000000000L, 0x0038283800000000L, 0x001C141C00000000L, 0x000E0A0E00000000L, 0x0007050700000000L, 0x0003020300000000L, 
        0x0000C040C0000000L, 0x0000E0A0E0000000L, 0x0000705070000000L, 0x0000382838000000L, 0x00001C141C000000L, 0x00000E0A0E000000L, 0x0000070507000000L, 0x0000030203000000L, 
        0x000000C040C00000L, 0x000000E0A0E00000L, 0x0000007050700000L, 0x0000003828380000L, 0x0000001C141C0000L, 0x0000000E0A0E0000L, 0x0000000705070000L, 0x0000000302030000L, 
        0x00000000C040C000L, 0x00000000E0A0E000L, 0x0000000070507000L, 0x0000000038283800L, 0x000000001C141C00L, 0x000000000E0A0E00L, 0x0000000007050700L, 0x0000000003020300L, 
        0x0000000000C040C0L, 0x0000000000E0A0E0L, 0x0000000000705070L, 0x0000000000382838L, 0x00000000001C141CL, 0x00000000000E0A0EL, 0x0000000000070507L, 0x0000000000030203L, 
        0x000000000000C040L, 0x000000000000E0A0L, 0x0000000000007050L, 0x0000000000003828L, 0x0000000000001C14L, 0x0000000000000E0AL, 0x0000000000000705L, 0x0000000000000302L
    };
    private static final long[][] MagicBishopDb = new long[64][];
    private static final long[] MagicmovesBMagics = {
            0x0002020202020200L, 0x0002020202020000L, 0x0004010202000000L, 0x0004040080000000L, 0x0001104000000000L, 0x0000821040000000L, 0x0000410410400000L, 0x0000104104104000L,
            0x0000040404040400L, 0x0000020202020200L, 0x0000040102020000L, 0x0000040400800000L, 0x0000011040000000L, 0x0000008210400000L, 0x0000004104104000L, 0x0000002082082000L,
            0x0004000808080800L, 0x0002000404040400L, 0x0001000202020200L, 0x0000800802004000L, 0x0000800400A00000L, 0x0000200100884000L, 0x0000400082082000L, 0x0000200041041000L,
            0x0002080010101000L, 0x0001040008080800L, 0x0000208004010400L, 0x0000404004010200L, 0x0000840000802000L, 0x0000404002011000L, 0x0000808001041000L, 0x0000404000820800L,
            0x0001041000202000L, 0x0000820800101000L, 0x0000104400080800L, 0x0000020080080080L, 0x0000404040040100L, 0x0000808100020100L, 0x0001010100020800L, 0x0000808080010400L,
            0x0000820820004000L, 0x0000410410002000L, 0x0000082088001000L, 0x0000002011000800L, 0x0000080100400400L, 0x0001010101000200L, 0x0002020202000400L, 0x0001010101000200L,
            0x0000410410400000L, 0x0000208208200000L, 0x0000002084100000L, 0x0000000020880000L, 0x0000001002020000L, 0x0000040408020000L, 0x0004040404040000L, 0x0002020202020000L,
            0x0000104104104000L, 0x0000002082082000L, 0x0000000020841000L, 0x0000000000208800L, 0x0000000010020200L, 0x0000000404080200L, 0x0000040404040400L, 0x0002020202020200L
    };
    private static final long[] MagicmovesBMask = {
            0x0040201008040200L, 0x0000402010080400L, 0x0000004020100A00L, 0x0000000040221400L, 0x0000000002442800L, 0x0000000204085000L, 0x0000020408102000L, 0x0002040810204000L,
            0x0020100804020000L, 0x0040201008040000L, 0x00004020100A0000L, 0x0000004022140000L, 0x0000000244280000L, 0x0000020408500000L, 0x0002040810200000L, 0x0004081020400000L,
            0x0010080402000200L, 0x0020100804000400L, 0x004020100A000A00L, 0x0000402214001400L, 0x0000024428002800L, 0x0002040850005000L, 0x0004081020002000L, 0x0008102040004000L,
            0x0008040200020400L, 0x0010080400040800L, 0x0020100A000A1000L, 0x0040221400142200L, 0x0002442800284400L, 0x0004085000500800L, 0x0008102000201000L, 0x0010204000402000L,
            0x0004020002040800L, 0x0008040004081000L, 0x00100A000A102000L, 0x0022140014224000L, 0x0044280028440200L, 0x0008500050080400L, 0x0010200020100800L, 0x0020400040201000L,
            0x0002000204081000L, 0x0004000408102000L, 0x000A000A10204000L, 0x0014001422400000L, 0x0028002844020000L, 0x0050005008040200L, 0x0020002010080400L, 0x0040004020100800L,
            0x0000020408102000L, 0x0000040810204000L, 0x00000A1020400000L, 0x0000142240000000L, 0x0000284402000000L, 0x0000500804020000L, 0x0000201008040200L, 0x0000402010080400L,
            0x0002040810204000L, 0x0004081020400000L, 0x000A102040000000L, 0x0014224000000000L, 0x0028440200000000L, 0x0050080402000000L, 0x0020100804020000L, 0x0040201008040200L
    };
    private static final long[][] MagicRookDb = new long[64][];
    private static final long[] MagicmovesRMagics = {
            0x0080001020400080L, 0x0040001000200040L, 0x0080081000200080L, 0x0080040800100080L, 0x0080020400080080L, 0x0080010200040080L, 0x0080008001000200L, 0x0080002040800100L,
            0x0000800020400080L, 0x0000400020005000L, 0x0000801000200080L, 0x0000800800100080L, 0x0000800400080080L, 0x0000800200040080L, 0x0000800100020080L, 0x0000800040800100L,
            0x0000208000400080L, 0x0000404000201000L, 0x0000808010002000L, 0x0000808008001000L, 0x0000808004000800L, 0x0000808002000400L, 0x0000010100020004L, 0x0000020000408104L,
            0x0000208080004000L, 0x0000200040005000L, 0x0000100080200080L, 0x0000080080100080L, 0x0000040080080080L, 0x0000020080040080L, 0x0000010080800200L, 0x0000800080004100L,
            0x0000204000800080L, 0x0000200040401000L, 0x0000100080802000L, 0x0000080080801000L, 0x0000040080800800L, 0x0000020080800400L, 0x0000020001010004L, 0x0000800040800100L,
            0x0000204000808000L, 0x0000200040008080L, 0x0000100020008080L, 0x0000080010008080L, 0x0000040008008080L, 0x0000020004008080L, 0x0000010002008080L, 0x0000004081020004L,
            0x0000204000800080L, 0x0000200040008080L, 0x0000100020008080L, 0x0000080010008080L, 0x0000040008008080L, 0x0000020004008080L, 0x0000800100020080L, 0x0000800041000080L,
            0x0000102040800101L, 0x0000102040008101L, 0x0000081020004101L, 0x0000040810002101L, 0x0001000204080011L, 0x0001000204000801L, 0x0001000082000401L, 0x0000002040810402L
    };
    private static final long[] MagicmovesRMask = {
            0x000101010101017EL, 0x000202020202027CL, 0x000404040404047AL, 0x0008080808080876L, 0x001010101010106EL, 0x002020202020205EL, 0x004040404040403EL, 0x008080808080807EL,
            0x0001010101017E00L, 0x0002020202027C00L, 0x0004040404047A00L, 0x0008080808087600L, 0x0010101010106E00L, 0x0020202020205E00L, 0x0040404040403E00L, 0x0080808080807E00L,
            0x00010101017E0100L, 0x00020202027C0200L, 0x00040404047A0400L, 0x0008080808760800L, 0x00101010106E1000L, 0x00202020205E2000L, 0x00404040403E4000L, 0x00808080807E8000L,
            0x000101017E010100L, 0x000202027C020200L, 0x000404047A040400L, 0x0008080876080800L, 0x001010106E101000L, 0x002020205E202000L, 0x004040403E404000L, 0x008080807E808000L,
            0x0001017E01010100L, 0x0002027C02020200L, 0x0004047A04040400L, 0x0008087608080800L, 0x0010106E10101000L, 0x0020205E20202000L, 0x0040403E40404000L, 0x0080807E80808000L,
            0x00017E0101010100L, 0x00027C0202020200L, 0x00047A0404040400L, 0x0008760808080800L, 0x00106E1010101000L, 0x00205E2020202000L, 0x00403E4040404000L, 0x00807E8080808000L,
            0x007E010101010100L, 0x007C020202020200L, 0x007A040404040400L, 0x0076080808080800L, 0x006E101010101000L, 0x005E202020202000L, 0x003E404040404000L, 0x007E808080808000L,
            0x7E01010101010100L, 0x7C02020202020200L, 0x7A04040404040400L, 0x7608080808080800L, 0x6E10101010101000L, 0x5E20202020202000L, 0x3E40404040404000L, 0x7E80808080808000L
    };
    static {
        for (int i = 0; i < MagicBishopDb.length; i++)
            MagicBishopDb[i] = new long[MAGIC_BISHOP_DB_LENGTH];

        for (int i = 0; i < MagicRookDb.length; ++i)
            MagicRookDb[i] = new long[MAGIC_ROOK_DB_LENGTH];

        final int[] initMagicMovesDb = {
            63, 0, 58, 1, 59, 47, 53, 2,
            60, 39, 48, 27, 54, 33, 42, 3,
            61, 51, 37, 40, 49, 18, 28, 20,
            55, 30, 34, 11, 43, 14, 22, 4,
            62, 57, 46, 52, 38, 26, 32, 41,
            50, 36, 17, 19, 29, 10, 13, 21,
            56, 45, 25, 31, 35, 16, 9, 12,
            44, 24, 15, 8, 23, 7, 6, 5
        };

        final int[] squares = new int[64];
        int numSquares;

        for (int i = 0; i < squares.length; ++i) {
            numSquares = 0;
            long temp = MagicmovesBMask[i];
            while (temp != 0) {
                final long bit = (temp & -temp);
                squares[numSquares++] = initMagicMovesDb[(int) ((bit * 0x07EDD5E59A4E28C2L) >>> 58)];
                temp ^= bit;
            }

            for (temp = 0; temp < ONE << numSquares; ++temp) {
                final long tempocc = InitmagicmovesOcc(squares, numSquares, temp);
                MagicBishopDb[i][(int) ((tempocc * MagicmovesBMagics[i]) >>> 55)] = InitmagicmovesBmoves(i, tempocc);
            }
        }

        Arrays.fill(squares, 0);

        for (int i = 0; i < squares.length; ++i) {
            numSquares = 0;
            long temp = MagicmovesRMask[i];
            while (temp != 0) {
                final long bit = (temp & -temp);
                squares[numSquares++] = initMagicMovesDb[(int) ((bit * 0x07EDD5E59A4E28C2L) >>> 58)];
                temp ^= bit;
            }

            for (temp = 0; temp < ONE << numSquares; ++temp) {
                final long tempocc = InitmagicmovesOcc(squares, numSquares, temp);
                MagicRookDb[i][(int) ((tempocc * MagicmovesRMagics[i]) >>> 52)] = InitmagicmovesRmoves(i, tempocc);
            }
        }
    }
    public static long BishopAttacks(int square, final long blockers) {
        return (MagicBishopDb[square][(int) (((blockers & MagicmovesBMask[square]) * MagicmovesBMagics[square]) >>> 55)]);
    }
    public static long RookAttacks(int square, final long blockers) {
        return (MagicRookDb[square][(int) (((blockers & MagicmovesRMask[square]) * MagicmovesRMagics[square]) >>> 52)]);
    }
    public static long QueenAttacks(final int square, final long blockers) {
        return BishopAttacks(square, blockers) | RookAttacks(square, blockers);
    }
    public static long KnightAttacks(final int square) {
        return (KnightMovesBB[square]);
    }
    public static long KingAttacks(final int square) {
        return (KingMovesBB[square]);
    }
    public static long xrayDiag(final int square) {
        return XRAY_DIAGONAL[square];
    }
    public static long xrayHV(final int square) {
        return XRAY_HV[square];
    }
    private static long InitmagicmovesOcc(final int[] squares, final int squareNumber, final long linocc) {
        long ret = 0L;
        for (int i = 0; i < squareNumber; ++i)
            if ((linocc & (ONE << i)) != 0)
                ret |= ONE << squares[i];

        return ret;
    }
    private static long InitmagicmovesRmoves(final int square, final long occ) {
        long ret = 0L;
        final long rowbits = FF << (8 * (square / 8));

        long bit = ONE << square;
        do {
            bit <<= 8;
            ret |= bit;
        }
        while (bit > 0 && (bit & occ) == 0);

        bit = ONE << square;
        do {
            bit >>>= 8;
            ret |= bit;
        }
        while (bit != 0 && (bit & occ) == 0);

        bit = ONE << square;
        do {
            bit <<= 1;
            if ((bit & rowbits) != 0)
                ret |= bit;
            else
                break;
        }
        while ((bit & occ) == 0);

        bit = ONE << square;
        do {
            bit >>>= 1;
            if ((bit & rowbits) != 0)
                ret |= bit;
            else
                break;
        }
        while ((bit & occ) == 0);

        return ret;
    }
    private static long InitmagicmovesBmoves(final int square, final long occ) {
        long ret = 0L;
        final long rowbits = FF << (8 * (square / 8));

        long bit = ONE << square;
        long bit2 = bit;
        do {
            bit <<= 8 - 1;
            bit2 >>>= 1;
            if ((bit2 & rowbits) != 0)
                ret |= bit;
            else
                break;
        }
        while (bit != 0 && (bit & occ) == 0);

        bit = ONE << square;
        bit2 = bit;
        do {
            bit <<= 8 + 1;
            bit2 <<= 1;
            if ((bit2 & rowbits) != 0)
                ret |= bit;
            else
                break;
        }
        while (bit != 0 && (bit & occ) == 0);

        bit = ONE << square;
        bit2 = bit;
        do {
            bit >>>= 8 - 1;
            bit2 <<= 1;
            if ((bit2 & rowbits) != 0)
                ret |= bit;
            else
                break;
        }
        while (bit != 0 && (bit & occ) == 0);

        bit = ONE << square;
        bit2 = bit;
        do {
            bit >>>= 8 + 1;
            bit2 >>= 1;
            if ((bit2 & rowbits) != 0)
                ret |= bit;
            else
                break;
        }
        while (bit > 0 && (bit & occ) == 0);

        return ret;
    }
}