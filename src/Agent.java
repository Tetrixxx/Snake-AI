
///McBuktam,h265832@stud.u-szeged.hu
import java.util.*;
import game.snake.Direction;
import game.snake.SnakeGame;
import game.snake.SnakePlayer;
import game.snake.utils.Cell;
import game.snake.utils.SnakeGameState;

/**
 * Az Agent osztaly egy kigyo jatekos ugynokot valosit meg.
 * V16: Optimalizalt verzio - finomhangolt parameterek,
 * gyorsabb etel eleres, erosebb tuleles.
 */
public class Agent extends SnakePlayer {

    private int boardRows;
    private int boardCols;

    public Agent(SnakeGameState gameState, int color, Random random) {
        super(gameState, color, random);
        this.boardRows = gameState.board.length;
        this.boardCols = gameState.board[0].length;
    }

    @Override
    public Direction getAction(long remainingTime) {
        Cell head = gameState.snake.peekFirst();
        Cell tail = gameState.snake.peekLast();
        int snakeLen = gameState.snake.size();

        // 1. Keressuk a legjobb etelt
        Cell food = findBestFood(head, snakeLen);

        // 2. Ha van etel es biztonsagos ut hozza
        if (food != null) {
            Node pathToFood = findPath(head, food, snakeLen);
            if (pathToFood != null && isSafeMove(pathToFood, true, snakeLen)) {
                return reconstructDirection(pathToFood);
            }
        }

        // 3. Farokkovetes
        if (tail != null) {
            Node pathToTail = findPath(head, tail, snakeLen);
            if (pathToTail != null) {
                Direction tailDir = reconstructDirection(pathToTail);
                Cell nextCell = getNextCell(head, tailDir);
                if (nextCell != null && isValidMove(nextCell)) {
                    int spaceAfter = countAccessibleSpace(nextCell, snakeLen * 2);
                    if (spaceAfter >= snakeLen) {
                        return tailDir;
                    }
                }
            }
        }

        // 4. Legjobb biztonsagos irany
        return findSafestDirection(head, tail, snakeLen);
    }

    /**
     * Megkeresi a legjobb etelt kombinalt ertekelssel.
     */
    private Cell findBestFood(Cell head, int snakeLen) {
        Cell bestFood = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        Cell closestFood = null;
        int minDist = Integer.MAX_VALUE;

        for (int i = 0; i < boardRows; i++) {
            for (int j = 0; j < boardCols; j++) {
                if (gameState.board[i][j] == SnakeGame.FOOD) {
                    Cell food = new Cell(i, j);
                    int distance = manhattanDistance(head, food);

                    if (distance < minDist) {
                        minDist = distance;
                        closestFood = food;
                    }

                    // Ter becslese
                    int spaceNearFood = countAccessibleSpace(food, snakeLen + 15);

                    // Egyensulyozott pontozas
                    double distWeight = Math.max(1.5, 3.0 - snakeLen / 40.0);
                    double spaceWeight = 1.0 + snakeLen / 30.0;
                    double score = spaceNearFood * spaceWeight - distance * distWeight;

                    if (score > bestScore) {
                        bestScore = score;
                        bestFood = food;
                    }
                }
            }
        }

        // Rovid kigyo: egyenesen az etelhez
        if (snakeLen < 12 && closestFood != null) {
            return closestFood;
        }

        return bestFood != null ? bestFood : closestFood;
    }

    private int manhattanDistance(Cell a, Cell b) {
        return Math.abs(a.i - b.i) + Math.abs(a.j - b.j);
    }

    /**
     * Legjobb biztonsagos irany.
     */
    private Direction findSafestDirection(Cell head, Cell tail, int snakeLen) {
        Direction bestDirection = gameState.direction;
        double maxScore = Double.NEGATIVE_INFINITY;

        for (Direction dir : SnakeGame.DIRECTIONS) {
            if (isOppositeDirection(dir, gameState.direction))
                continue;

            Cell nextCell = getNextCell(head, dir);
            if (nextCell == null || !isValidMove(nextCell))
                continue;

            int space = countAccessibleSpace(nextCell, snakeLen * 2);

            // Minimum ter: a kigyo 55%-a
            if (space < snakeLen * 0.55)
                continue;

            double score = space * 10.0;

            // Farok kozeliti bonusz
            if (tail != null) {
                int headToTail = manhattanDistance(head, tail);
                int nextToTail = manhattanDistance(nextCell, tail);
                if (nextToTail < headToTail) {
                    score += 10.0;
                }
            }

            // Kozepre torekves hosszabb kigyo eseten
            if (snakeLen > 25) {
                int distToCenter = Math.abs(nextCell.i - boardRows / 2) + Math.abs(nextCell.j - boardCols / 2);
                score -= distToCenter * 0.4;
            }

            // Fal es test elkerulese
            if (!isNearWall(nextCell))
                score += 4.0;
            if (!isNearSnake(nextCell))
                score += 5.0;

            if (score > maxScore) {
                maxScore = score;
                bestDirection = dir;
            }
        }

        // Fallback
        if (maxScore == Double.NEGATIVE_INFINITY) {
            for (Direction dir : SnakeGame.DIRECTIONS) {
                if (!isOppositeDirection(dir, gameState.direction)) {
                    Cell next = getNextCell(head, dir);
                    if (next != null && isValidMove(next)) {
                        return dir;
                    }
                }
            }
        }

        return bestDirection;
    }

    private boolean isOppositeDirection(Direction dir1, Direction dir2) {
        return (dir1 == SnakeGame.UP && dir2 == SnakeGame.DOWN) ||
                (dir1 == SnakeGame.DOWN && dir2 == SnakeGame.UP) ||
                (dir1 == SnakeGame.LEFT && dir2 == SnakeGame.RIGHT) ||
                (dir1 == SnakeGame.RIGHT && dir2 == SnakeGame.LEFT);
    }

    /**
     * Hozzaferheto ter szamolas.
     */
    private int countAccessibleSpace(Cell start, int maxSearch) {
        if (!isValidMove(start))
            return 0;

        Set<Cell> visited = new HashSet<>();
        Deque<Cell> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        int count = 0;

        while (!queue.isEmpty() && count < maxSearch) {
            Cell current = queue.poll();
            count++;

            for (Cell neighbor : current.neighbors()) {
                if (!visited.contains(neighbor) && isValidMove(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return count;
    }

    /**
     * Biztonsag ellenorzes.
     */
    private boolean isSafeMove(Node path, boolean goingToFood, int snakeLen) {
        Cell endCell = path.cell;
        int pathLength = getPathLength(path);
        int accessibleSpace = countAccessibleSpaceAfterMove(endCell, path, snakeLen * 2 + 10);

        int requiredSpace = goingToFood ? snakeLen + 1 : snakeLen;

        if (accessibleSpace < requiredSpace) {
            return false;
        }

        // Farok elerhetoseg hosszu kigyo eseten
        if (snakeLen > 18 && pathLength < snakeLen) {
            Cell tail = gameState.snake.peekLast();
            if (tail != null) {
                for (Cell neighbor : tail.neighbors()) {
                    if (gameState.isOnBoard(neighbor) &&
                            gameState.getValueAt(neighbor) != SnakeGame.SNAKE) {
                        return true;
                    }
                }
                return accessibleSpace >= snakeLen * 1.15;
            }
        }

        return true;
    }

    private int getPathLength(Node path) {
        int length = 0;
        Node current = path;
        while (current != null) {
            length++;
            current = current.parent;
        }
        return length;
    }

    private int countAccessibleSpaceAfterMove(Cell endCell, Node path, int maxSearch) {
        Set<Cell> pathCells = new HashSet<>();
        Node current = path;
        while (current != null) {
            pathCells.add(current.cell);
            current = current.parent;
        }

        Set<Cell> visited = new HashSet<>(pathCells);
        Deque<Cell> queue = new ArrayDeque<>();
        queue.add(endCell);
        int count = 0;

        while (!queue.isEmpty() && count < maxSearch) {
            Cell cell = queue.poll();
            count++;

            for (Cell neighbor : cell.neighbors()) {
                if (!visited.contains(neighbor) && isValidMove(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return count;
    }

    private Cell getNextCell(Cell cell, Direction dir) {
        int newI = cell.i, newJ = cell.j;
        if (dir == SnakeGame.UP)
            newI--;
        else if (dir == SnakeGame.DOWN)
            newI++;
        else if (dir == SnakeGame.LEFT)
            newJ--;
        else if (dir == SnakeGame.RIGHT)
            newJ++;
        return new Cell(newI, newJ);
    }

    private boolean isValidMove(Cell cell) {
        return gameState.isOnBoard(cell) && gameState.getValueAt(cell) != SnakeGame.SNAKE;
    }

    private boolean isNearWall(Cell cell) {
        return cell.i <= 1 || cell.i >= boardRows - 2 ||
                cell.j <= 1 || cell.j >= boardCols - 2;
    }

    private boolean isNearSnake(Cell cell) {
        for (Cell neighbor : cell.neighbors()) {
            if (gameState.isOnBoard(neighbor) && gameState.getValueAt(neighbor) == SnakeGame.SNAKE) {
                return true;
            }
        }
        return false;
    }

    /**
     * A* utvonalkereso optimalizalt sulyokkal.
     */
    private Node findPath(Cell start, Cell goal, int snakeLen) {
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<Cell, Double> gScoreMap = new HashMap<>();
        Set<Cell> closedSet = new HashSet<>();

        gScoreMap.put(start, 0.0);
        openList.add(new Node(start, null, 0, heuristic(start, goal)));

        // Finomhangolt sulyok
        double snakeFactor = snakeLen / 35.0;
        double snakePenalty = 0.4 + snakeFactor;
        double wallPenalty = 0.25 + snakeFactor * 0.4;

        int maxIterations = boardRows * boardCols;
        int iterations = 0;

        while (!openList.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node current = openList.poll();

            if (current.cell.equals(goal)) {
                return current;
            }

            if (closedSet.contains(current.cell))
                continue;
            closedSet.add(current.cell);

            for (Cell neighbor : current.cell.neighbors()) {
                if (!gameState.isOnBoard(neighbor) ||
                        gameState.getValueAt(neighbor) == SnakeGame.SNAKE ||
                        closedSet.contains(neighbor)) {
                    continue;
                }

                double tentativeGScore = gScoreMap.getOrDefault(current.cell, Double.MAX_VALUE) + 1;

                if (isNearSnake(neighbor)) {
                    tentativeGScore += snakePenalty;
                }
                if (isNearWall(neighbor)) {
                    tentativeGScore += wallPenalty;
                }

                // Szuk hely elkerulese hosszabb kigyo eseten
                if (snakeLen > 22) {
                    int space = countAccessibleSpace(neighbor, snakeLen);
                    if (space < snakeLen) {
                        tentativeGScore += (snakeLen - space) * 0.04;
                    }
                }

                if (tentativeGScore < gScoreMap.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    gScoreMap.put(neighbor, tentativeGScore);
                    double fScore = tentativeGScore + heuristic(neighbor, goal);
                    openList.add(new Node(neighbor, current, tentativeGScore, fScore));
                }
            }
        }

        return null;
    }

    private double heuristic(Cell a, Cell b) {
        return Math.abs(a.i - b.i) + Math.abs(a.j - b.j);
    }

    private Direction reconstructDirection(Node path) {
        while (path.parent != null && path.parent.parent != null) {
            path = path.parent;
        }
        return gameState.snake.peekFirst().directionTo(path.cell);
    }

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
