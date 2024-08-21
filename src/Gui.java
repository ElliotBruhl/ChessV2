import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

public class Gui { 
    //should only be one instance so I can be messy with these variables
    private JFrame frame;
    private JPanel panel;
    private JButton[] buttons;
    private final String[] pathArr;
    private int startClick = -1;
    private int endClick = -1;
    private boolean validStartEnd = false;
    private final Board board;
    private final Color lightSquare = new Color(206, 166, 125);
    private final Color darkSquare = new Color(153, 102, 51);
    private final Color highlight = new Color(143, 207, 235);
    private final Color selected = new Color(255, 0, 0);

    public Gui(Board b) {
        pathArr = new String[]{
            "files/Chess_plt45.png", "files/Chess_pdt45.png", 
            "files/Chess_rlt45.png", "files/Chess_rdt45.png", 
            "files/Chess_nlt45.png", "files/Chess_ndt45.png",
            "files/Chess_blt45.png", "files/Chess_bdt45.png", 
            "files/Chess_qlt45.png", "files/Chess_qdt45.png", 
            "files/Chess_klt45.png", "files/Chess_kdt45.png"
        };
        board = b;
        initializeFrame();
        initializeBoard();
        frame.setVisible(true);
    }
    private void initializeFrame() {
        frame = new JFrame("Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 450);
        panel = new JPanel();
        panel.setLayout(new GridLayout(8, 8));
        frame.add(panel);
    }
    private void initializeBoard() {
        buttons = new JButton[64];
        for (int i = 0; i < 64; i++) {
            buttons[i] = new JButton();
            buttons[i].setPreferredSize(new Dimension(45, 45));
            panel.add(buttons[i]);
            buttons[i].addActionListener(createButtonActionListener(i));
            if ((i/8 + i%8) % 2 == 0)
                buttons[i].setBackground(lightSquare);
            else
                buttons[i].setBackground(darkSquare);
        }
    }
    public void updateBoard() {
        for (JButton button : buttons) { // Clear pieces first
            button.setIcon(null);
        }
        for (int i = 0; i < 12; i++) {
            long bitboard = board.getBitboards()[i];
            for (int j = 0; j < 64; j++) {
                if ((bitboard & (1L << j)) != 0) {
                    buttons[(j/8)*8 + (7-(j%8))].setIcon(new ImageIcon(pathArr[i])); //(7-row)*8 + col to get from swing button indexing to normal indexing
                }
            }
        }
        if (board.getMove()) {
            frame.setTitle("Chess - White to Move");
        }
        else {
            frame.setTitle("Chess - Black to Move");
        }
    }
    private void resetSquareColors() {
        for (int i = 0; i < 64; i++) {
            if ((i/8 + i%8) % 2 == 0)
                buttons[i].setBackground(lightSquare); //light square color
            else
                buttons[i].setBackground(darkSquare); //dark square color
        }
    }
    private void highlightLegalMoves(int start) {
        ArrayList<Integer> legalSquares = new ArrayList<>();
        for (int[] pair : board.getLegalMoves()) { //find squares the piece can move to
            if (pair[0] == start) legalSquares.add(pair[1]);
        }
        for (int square : legalSquares) { //apply highlights
            buttons[(((7 - (square/8))*8 + (square%8)))].setBackground(highlight);
        }
    }
    public static void displayGameOver(Board b) {
        String message;
        switch (b.gameState()) {
            case null -> {
                throw new Error("Play on in game over message");
            }
            case 0 -> {
                message = "Draw.";
            }
            case Integer.MAX_VALUE/2 -> {
                message = "White Wins!";
            }
            case Integer.MIN_VALUE/2 -> {
                message = "Black Wins!";
            }
            default -> throw new IllegalStateException("Unexpected value of gameState");
        }
        JOptionPane.showMessageDialog(null, message, "Game Over", JOptionPane.PLAIN_MESSAGE);
    }
    public static String selectPromotionPiece() {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};

        String selectedValue = (String) JOptionPane.showInputDialog(null, "Select the piece to promote to:", "Pawn Promotion", 
            JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (selectedValue == null) // default to queen if canceled
            return "Queen"; 

        return selectedValue;
    }
    public int[] getClicks() {
        return new int[]{startClick, endClick, -1};
    }
    public void resetStartEnd() {
        startClick = -1;
        endClick = -1;
        validStartEnd = false;
    }
    public void waitForValidInput() {
        while (!validStartEnd) {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException ignored) {}
        }
    }
    private void manageClick(int index) {
        if (startClick == -1) { //no start square selected yet
            if ((board.getAllofColor(board.getMove()) & (1L << 63-index)) != 0) { //valid start square
                startClick = index;
                resetSquareColors(); //remove highlights
                buttons[(((7 - (index/8))*8 + (index%8)))].setBackground(selected);
                highlightLegalMoves(index); //highlight legal moves
            }
        }
        else { // Start square already selected
            endClick = index;
            resetSquareColors(); //remove highlights
            validStartEnd = true; //main can now proceed
        }
    }
    private ActionListener createButtonActionListener(int i) {
        return e -> manageClick(((7 - (i/8))*8 + (i%8)));
    }
}
