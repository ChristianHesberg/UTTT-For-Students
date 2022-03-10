package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.*;

public class SpicyCarlosBot implements IBot{
    final int moveTimeMs = 1000;
    private String BOT_NAME = getClass().getSimpleName();
    private static double totalMoves = 0;
    
    public static void addTotalMove()
    {
        SpicyCarlosBot.totalMoves++;
    }

    @Override
    public IMove doMove(IGameState state) {

        List<IMove> moves = state.getField().getAvailableMoves();
        PotentialMove[] potentialMoves = new PotentialMove[moves.size()];
        SpicyCarlosBot.totalMoves = 0;
        double highestValue = 0;
        IMove bestMove = null;

        for(int i = 0; i < potentialMoves.length; i++)
        {
            potentialMoves[i] = new PotentialMove(moves.get(i), 0, 0);
        }

        MySpicyRunnable runnable1 = new MySpicyRunnable(state, potentialMoves);
        MySpicyRunnable runnable2 = new MySpicyRunnable(state, potentialMoves);
        MySpicyRunnable runnable3 = new MySpicyRunnable(state, potentialMoves);

        Thread thread1 = new Thread(runnable1);
        Thread thread2 = new Thread(runnable2);
        Thread thread3 = new Thread(runnable3);

        thread1.start();
        thread2.start();
        thread3.start();
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(PotentialMove potMove : potentialMoves)
        {
            if(potMove.nNodeMoves > highestValue)
            {
                highestValue = potMove.nNodeMoves;
                bestMove = potMove.move;
            }
        }
        return bestMove;
        
        //calculateWinningMove(state, moveTimeMs);
    }


       /* double mostNodeVisits = 0;
        IMove returnedMove = null;
        for(PotentialMove potentialMove: potentialMoves)
        {
            double nMoves = potentialMove.getnNodeMoves();
            if(nMoves > mostNodeVisits)
            {
                mostNodeVisits = nMoves;
                returnedMove = potentialMove.getPotentialMove();
            }
        }
        return returnedMove;
        
        */


    /*
        The code below is a simulator for simulation of gameplay. This is needed for AI.

        It is put here to make the Bot independent of the GameManager and its subclasses/enums

        Now this class is only dependent on a few interfaces: IMove, IField, and IGameState

        You could say it is self-contained. The drawback is that if the game rules change, the simulator must be
        changed accordingly, making the code redundant.

     */


    @Override
    public String getBotName() {
        return BOT_NAME;
    }

    public enum GameOverState {
        Active,
        Win,
        Tie
    }

    public class Move implements IMove {
        int x = 0;
        int y = 0;

        public Move(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Move move = (Move) o;
            return x == move.x && y == move.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }


    }

    class GameSimulator {
        private final IGameState currentState;
        int currentPlayer = 0; //player0 == 0 && player1 == 1
        private volatile GameOverState gameOver = GameOverState.Active;

        public void setGameOver(GameOverState state) {
            gameOver = state;
        }

        public GameOverState getGameOver() {
            return gameOver;
        }

        public void setCurrentPlayer(int player) {
            currentPlayer = player;
        }

        public IGameState getCurrentState() {
            return currentState;
        }

        public GameSimulator(IGameState currentState) {
            this.currentState = currentState;
        }

        public Boolean updateGame(IMove move) {
            if (!verifyMoveLegality(move))
                return false;

            updateBoard(move);
            currentPlayer = (currentPlayer + 1) % 2;

            return true;
        }

        private Boolean verifyMoveLegality(IMove move) {
            IField field = currentState.getField();
            boolean isValid = field.isInActiveMicroboard(move.getX(), move.getY());

            if (isValid && (move.getX() < 0 || 9 <= move.getX())) isValid = false;
            if (isValid && (move.getY() < 0 || 9 <= move.getY())) isValid = false;

            if (isValid && !field.getBoard()[move.getX()][move.getY()].equals(IField.EMPTY_FIELD))
                isValid = false;

            return isValid;
        }

        private void updateBoard(IMove move) {
            String[][] board = currentState.getField().getBoard();
            board[move.getX()][move.getY()] = currentPlayer + "";
            currentState.setMoveNumber(currentState.getMoveNumber() + 1);
            if (currentState.getMoveNumber() % 2 == 0) {
                currentState.setRoundNumber(currentState.getRoundNumber() + 1);
            }
            checkAndUpdateIfWin(move);
            updateMacroboard(move);

        }

        private void checkAndUpdateIfWin(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            int macroX = move.getX() / 3;
            int macroY = move.getY() / 3;

            if (macroBoard[macroX][macroY].equals(IField.EMPTY_FIELD) ||
                    macroBoard[macroX][macroY].equals(IField.AVAILABLE_FIELD)) {

                String[][] board = getCurrentState().getField().getBoard();

                if (isWin(board, move, "" + currentPlayer))
                    macroBoard[macroX][macroY] = currentPlayer + "";
                else if (isTie(board, move))
                    macroBoard[macroX][macroY] = "TIE";

                //Check macro win
                if (isWin(macroBoard, new Move(macroX, macroY), "" + currentPlayer))
                    gameOver = GameOverState.Win;
                else if (isTie(macroBoard, new Move(macroX, macroY)))
                    gameOver = GameOverState.Tie;
            }

        }

        private boolean isTie(String[][] board, IMove move) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            for (int i = startX; i < startX + 3; i++) {
                for (int k = startY; k < startY + 3; k++) {
                    if (board[i][k].equals(IField.AVAILABLE_FIELD) ||
                            board[i][k].equals(IField.EMPTY_FIELD))
                        return false;
                }
            }
            return true;
        }


        public boolean isWin(String[][] board, IMove move, String currentPlayer) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            //check col
            for (int i = startY; i < startY + 3; i++) {
                if (!board[move.getX()][i].equals(currentPlayer))
                    break;
                if (i == startY + 3 - 1) return true;
            }

            //check row
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][move.getY()].equals(currentPlayer))
                    break;
                if (i == startX + 3 - 1) return true;
            }

            //check diagonal
            if (localX == localY) {
                //we're on a diagonal
                int y = startY;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][y++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }

            //check anti diagonal
            if (localX + localY == 3 - 1) {
                int less = 0;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][(startY + 2) - less++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }
            return false;
        }

        private void updateMacroboard(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            for (int i = 0; i < macroBoard.length; i++)
                for (int k = 0; k < macroBoard[i].length; k++) {
                    if (macroBoard[i][k].equals(IField.AVAILABLE_FIELD))
                        macroBoard[i][k] = IField.EMPTY_FIELD;
                }

            int xTrans = move.getX() % 3;
            int yTrans = move.getY() % 3;

            if (macroBoard[xTrans][yTrans].equals(IField.EMPTY_FIELD))
                macroBoard[xTrans][yTrans] = IField.AVAILABLE_FIELD;
            else {
                // Field is already won, set all fields not won to avail.
                for (int i = 0; i < macroBoard.length; i++)
                    for (int k = 0; k < macroBoard[i].length; k++) {
                        if (macroBoard[i][k].equals(IField.EMPTY_FIELD))
                            macroBoard[i][k] = IField.AVAILABLE_FIELD;
                    }
            }
        }
    }

    class PotentialMove
    {
        double nNodeMoves;
        double nTotalWins;
        IMove move;

        public PotentialMove(IMove move, double nNodeMoves, double nTotalWins)
        {
            this.move = move;
            this.nTotalWins = nTotalWins;
            this.nNodeMoves = nNodeMoves;
        }
    }

    class MySpicyRunnable implements Runnable {
        final int moveTimeMs;
        IGameState gameState;
        PotentialMove[] moves;

        public MySpicyRunnable(IGameState state, PotentialMove[] potentialMoves) {
            this.gameState = state;
            this.moves = potentialMoves;
            this.moveTimeMs = state.getTimePerMove();
        }

        @Override
        public void run() {
            calculateWinningMove(gameState, moveTimeMs, moves);
        }

        public double calculateUCT(double nNodeMoves, double nTotalWins, double nTotalMoves) {
            return (nTotalWins / nNodeMoves) + (1.41 * Math.sqrt(Math.log(nTotalMoves) / nNodeMoves));
        }

        public void updateUCTWin(PotentialMove move) {
            move.nNodeMoves++;
            move.nTotalWins++;
            addTotalMove();
        }

        public void updateUCTLoss(PotentialMove move) {
            move.nNodeMoves++;
            addTotalMove();
        }

        public PotentialMove calculateHighestUCT(PotentialMove[] potentialMoves) {
            double highestUCTvalue = -1;
            PotentialMove highestUCTmove = null;
            for (PotentialMove potMove : potentialMoves) {
                if (potMove.nNodeMoves == 0) {
                    return potMove;
                }
                double uct = calculateUCT(potMove.nNodeMoves, potMove.nTotalWins, SpicyCarlosBot.totalMoves);

                if (uct > highestUCTvalue) {
                    highestUCTvalue = uct;
                    highestUCTmove = potMove;
                }
            }
            return highestUCTmove;
        }

        // Plays single games until it wins and returns the first move for that. If iterations reached with no clear win, just return random valid move
        private void calculateWinningMove(IGameState state, int maxTimeMs, PotentialMove[] potentialMoves) {
            long time = System.currentTimeMillis();
            Random rand = new Random();

            while (System.currentTimeMillis() < time + maxTimeMs) { // check how much time has passed, stop if over maxTimeMs
                GameSimulator simulator = createSimulator(state);
                IGameState gs = simulator.getCurrentState();
                List<IMove> moves;
                PotentialMove highestUCTmove = calculateHighestUCT(potentialMoves);
                IMove randomMovePlayer = highestUCTmove.move;
                int currentPlayer = simulator.currentPlayer;

                while (simulator.getGameOver() == GameOverState.Active) {
                    simulator.updateGame(randomMovePlayer);

                    // Opponent plays randomly
                    if (simulator.getGameOver() == GameOverState.Active) { // game still going
                        moves = gs.getField().getAvailableMoves();
                        IMove randomMoveOpponent = moves.get(rand.nextInt(moves.size()));
                        simulator.updateGame(randomMoveOpponent);
                    }
                    if (simulator.getGameOver() == GameOverState.Active) { // game still going
                        moves = gs.getField().getAvailableMoves();
                        randomMovePlayer = moves.get(rand.nextInt(moves.size()));
                    }
                }
                if (simulator.getGameOver() == GameOverState.Win) {
                    if (simulator.currentPlayer != currentPlayer) {
                        updateUCTWin(highestUCTmove);
                    }
                    updateUCTLoss(highestUCTmove);
                }
            }
        }

        private GameSimulator createSimulator(IGameState state) {
            GameSimulator simulator = new GameSimulator(new GameState());
            simulator.setGameOver(GameOverState.Active);
            simulator.setCurrentPlayer(state.getMoveNumber() % 2);
            simulator.getCurrentState().setRoundNumber(state.getRoundNumber());
            simulator.getCurrentState().setMoveNumber(state.getMoveNumber());
            simulator.getCurrentState().getField().setBoard(state.getField().getBoard());
            simulator.getCurrentState().getField().setMacroboard(state.getField().getMacroboard());
            return simulator;
        }
    }
}
