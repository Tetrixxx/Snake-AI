///McBuktam,h265832@stud.u-szeged.hu
import java.util.*;
import game.snake.Direction;
import game.snake.SnakeGame;
import game.snake.SnakePlayer;
import game.snake.utils.Cell;
import game.snake.utils.SnakeGameState;

/**
 * Az Agent osztaly egy kigyo jatekos ugynokot valosit meg, amely a SnakePlayer osztalybol szarmazik.
 * Az ugynok celja az elelmiszer megtalalasa es elkerulni az oncsapdakat.
 */
public class Agent extends SnakePlayer {

    /**
     * Agent konstruktor.
     *
     * @param gameState a jatek aktualis allapota
     * @param color a kigyo szine
     * @param random veletlenszam-generátor
     */
    public Agent(SnakeGameState gameState, int color, Random random) {
        super(gameState, color, random);
    }

    /**
     * Meghatarozza az ugynok kovetkezo lepeset.
     * Elso sorban az elelmiszerhez vezeto utvonalat keresi meg, ha nincs veszely.
     * Ha nincs elerheto elelmiszer, egy biztonsagos teruletre probal navigalni.
     *
     * @param remainingTime a hatralevo ido milliszekundumban
     * @return a kovetkezo irany, amelybe az ugynok mozog
     */
    @Override
    public Direction getAction(long remainingTime) {
        Cell food = findClosestCell(SnakeGame.FOOD);
        if (food != null) {
            Node pathToFood = findPath(gameState.snake.peekFirst(), food);
            if (pathToFood != null && !causesSelfTrap(pathToFood)) {
                return reconstructDirection(pathToFood);
            }
        }

        Cell safeZone = findClosestCell(SnakeGame.EMPTY);
        if (safeZone != null) {
            Node pathToSafeZone = findPath(gameState.snake.peekFirst(), safeZone);
            if (pathToSafeZone != null) {
                return reconstructDirection(pathToSafeZone);
            }
        }

        return gameState.direction; // Alapertelemezett irany
    }

    /**
     * Megkeresi a legkozelebbi cellat adott tipus alapjan.
     *
     * @param targetType a keresett cella tipusa (elelmiszer vagy ures cella)
     * @return a legkozelebbi cella, amely megfelel a targetType-nak, vagy null ha nincs talalat
     */
    private Cell findClosestCell(int targetType) {
        Cell snakeHead = gameState.snake.peekFirst();
        Cell closest = null;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < gameState.board.length; i++) {
            for (int j = 0; j < gameState.board[i].length; j++) {
                if (gameState.board[i][j] == targetType) {
                    int distance = Math.abs(snakeHead.i - i) + Math.abs(snakeHead.j - j);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closest = new Cell(i, j);
                    }
                }
            }
        }
        return closest;
    }

    private Node findPath(Cell start, Cell goal) {
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<Cell, Double> gScoreMap = new HashMap<>();
        Set<Cell> closedSet = new HashSet<>();

        gScoreMap.put(start, 0.0);
        openList.add(new Node(start, null, 0, heuristic(start, goal)));

        while (!openList.isEmpty()) {
            Node current = openList.poll();
            if (current.cell.equals(goal)) {
                return current; // Found the goal
            }

            closedSet.add(current.cell);

            for (Cell neighbor : current.cell.neighbors()) {
                if (!gameState.isOnBoard(neighbor) || gameState.getValueAt(neighbor) == SnakeGame.SNAKE || closedSet.contains(neighbor)) {
                    continue; // Skip invalid or already processed cells
                }

                double tentativeGScore = gScoreMap.getOrDefault(current.cell, Double.MAX_VALUE) + 1;

                if (tentativeGScore < gScoreMap.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    gScoreMap.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + heuristic(neighbor, goal);
                    openList.add(new Node(neighbor, current, tentativeGScore, fScore));
                }
            }
        }

        return null; // No path found
    }
    private boolean causesSelfTrap(Node path) {
        // Simulate the snake's movement and ensure it has enough free space
        Set<Cell> pathCells = new HashSet<>();
        Node current = path;
        while (current != null) {
            pathCells.add(current.cell);
            current = current.parent;
        }

        for (Cell cell : pathCells) {
            for (Cell neighbor : cell.neighbors()) {
                if (!pathCells.contains(neighbor) && gameState.isOnBoard(neighbor) && gameState.getValueAt(neighbor) != SnakeGame.SNAKE) {
                    return false; // Safe space available
                }
            }
        }

        return true; // Trap detected
    }

    /**
     * Heuristic function to estimate the distance between two cells with penalties for dangerous areas.
     *
     * @param a the starting cell
     * @param b the goal cell
     * @return the Manhattan distance with danger penalties
     */
    private double manhattanDistance(Cell a, Cell b) {
        return Math.abs(a.i - b.i) + Math.abs(a.j - b.j);
    }
    private double dynamicHeuristic(Cell a, Cell b) {
        double baseDistance = manhattanDistance(a, b);

        double safetyScore = 0.0;
        if (isNearSnake(a)) safetyScore += 2.0;
        if (isNearObstacle(a)) safetyScore += 3.0;

        return baseDistance + safetyScore;
    }

    private double weightedHeuristic(Cell a, Cell b) {
        double baseDistance = dynamicHeuristic(a, b); // Use preferred base heuristic
        double dangerPenalty = 1.0;

        if (isNearSnake(a)) {
            dangerPenalty += 1.5;
        }

        if (isNearObstacle(a)) {
            dangerPenalty += 2.0;
        }

        return baseDistance * dangerPenalty;
    }
    private double heuristic(Cell a, Cell b) {
        // Calculate the Euclidean distance between cell a and cell b
        double distance = manhattanDistance(a, b);

        // Weight factors for danger zones (near snake or walls)
        double dangerWeight = 1.0; // Default weight for normal cells

        // Increase weight for dangerous areas (e.g., near snake body)
        if (isNearSnake(a)) {
            dangerWeight = 1.5; // Heuristic penalty for being near snake body
        }

        // Increase weight for walls or edges (if needed)
        if (isNearObstacle(a)) {
            dangerWeight = 2.0; // Heuristic penalty for being near obstacles or walls
        }

        // Return the weighted Euclidean distance
        return distance * dangerWeight;
    }

    /**
     * Checks if a cell is near the snake's body.
     * This would be used to apply a higher penalty for cells near the snake.
     *
     * @param cell the cell to check
     * @return true if the cell is near the snake's body
     */
    private boolean isNearSnake(Cell cell) {
        // Check if the cell is occupied by the snake (could be part of the snake or its body)
        if (gameState.getValueAt(cell) == SnakeGame.SNAKE) {
            return true; // It's part of the snake
        }

        // Check neighboring cells for proximity to the snake (snake body cells)
        for (Cell neighbor : cell.neighbors()) {
            // Ensure the neighbor is within the valid board boundaries
            if (isValidCell(neighbor) && gameState.getValueAt(neighbor) == SnakeGame.SNAKE) {
                return true; // It's adjacent to the snake body
            }
        }

        return false; // Not near the snake
    }

    /**
     * Validates if a cell is within the bounds of the game board.
     *
     * @param cell the cell to check
     * @return true if the cell is within the bounds of the board
     */
    private boolean isValidCell(Cell cell) {
        // Check if the cell is within the valid range of the board's rows and columns
        return cell.i >= 0 && cell.i < gameState.board.length && cell.j >= 0 && cell.j < gameState.board[0].length;
    }


    /**
     * Checks if a cell is near an obstacle (like walls).
     *
     * @param cell the cell to check
     * @return true if the cell is near an obstacle
     */
    private boolean isNearObstacle(Cell cell) {
        // For simplicity, let's assume obstacles are any areas outside the board or walls.
        // You might need to adjust this depending on your game setup.
        if (!gameState.isOnBoard(cell)) {
            return true; // Outside the board (acting as an obstacle)
        }

        // Additional checks for more specific obstacles (if your game has more complex objects)
        // For example, walls could be a special cell type, but here we use the SnakeGame.SNAKE value as an obstacle.
        if (gameState.getValueAt(cell) == SnakeGame.SNAKE) {
            return true; // Treat the snake body as an obstacle
        }

        return false; // No obstacles near
    }


    /**
     * Rekonstrualja az iranyt az utvonal vegpontja alapjan.
     *
     * @param path az utvonal
     * @return a rekonstrualt irany, amelybe az ugynok mozog
     */
    private Direction reconstructDirection(Node path) {
        while (path.parent != null && path.parent.parent != null) {
            path = path.parent;
        }
        return gameState.snake.peekFirst().directionTo(path.cell);
    }

    /**
     * A Node osztaly egy cellat es az utvonalon levo szulo csomopontot tarolja.
     *
     * cell az aktualis cella
     * parent az eloz csomopont az utvonalban
     * gScore a kezdoponttol valo tenyleges tavolsag
     * fScore a celpontig tarto becsult ossztavolsag (gScore + heuristic)
     */
    private static class Node {
        Cell cell;
        Node parent;
        double gScore;
        double fScore;

        Node(Cell cell, Node parent, double gScore, double fScore) {
            this.cell = cell;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }
}
