public class Main {
    public static void main(String[] args) {

        String startPos = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        Board board = new Board(startPos);
        
        GameManager.runGuiGame(board, new Gui(board), new MoveGenV5(5), false); //modify depth in class vars (use depth 5-6 optimally)
        //GameManager.runGuiGame(board, new Gui(board));
        /*
        int[] rawScore = GameManager.runGame(new MoveGenV5(3), new MoveGenV5(3)); //1 is win for 2nd, 0 is draw, -1 is win for 1st
        int ones = 0;
        int zeros = 0;
        int negOnes = 0;
        for (int score : rawScore) {
            switch (score) {
                case -1 -> negOnes++;
                case 0 -> zeros++;
                case 1 -> ones++;
            }
        }
        System.out.println("For new version (2nd arg):\nW: " + ones + "\nD: " + zeros + "\nL: " + negOnes);
        */
    }
}