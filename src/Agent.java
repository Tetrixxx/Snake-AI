
///McBuktam,h265832@stud.u-szeged.hu
import java.util.*;
import game.snake.Direction;
import game.snake.SnakeGame;
import game.snake.SnakePlayer;
import game.snake.utils.Cell;
import game.snake.utils.SnakeGameState;

/**
 * Az Agent osztaly egy kigyo jatekos ugynokot valosit meg.
 * V10: V1 alap + finom parameterhangolas + jobb farok kovetes.
 */
public class Agent extends SnakePlayer {

    public Agent(SnakeGameState gameState, int color, Random random) {
        super(gameState, color, random);
    }

    @Override
    public Direction getAction(long remainingTime) {
        Cell head = gameState.snake.peekFirst();
        Cell food = findClosestCell(SnakeGame.FOOD);

        // 1. Ha van etel es biztonsagos ut hozza
        if (food != null) {
            Node pathToFood = findPath(head, food);
            if (pathToFood != null && isSafeMove(pathToFood)) {
                return reconstructDirection(pathToFood);
            }
        }

        // 2. Farokkovetes - kovessuk a sajat farkunkat
        Cell tail = gameState.snake.peekLast();
        if (tail != null) {
            Node pathToTail = findPath(head, tail);
            if (pathToTail != null) {
                Direction tailDir = reconstructDirection(pathToTail);
                Cell nextCell = getNextCell(head, tailDir);
                if (nextCell != null && isValidMove(nextCell)) {
                    return tailDir;
                }
            }
        }

        // 3. Legjobb biztonsagos irany
        return findSafestDirection(head);
    }

    private Direction findSafestDirection(Cell head) {
        Direction bestDirection = gameState.direction;
        int maxSpace = -1;

        for (Direction dir : SnakeGame.DIRECTIONS) {
            if (isOppositeDirection(dir, gameState.direction))
                continue;

            Cell nextCell = getNextCell(head, dir);
            if (nextCell != null && isValidMove(nextCell)) {
                int space = countAccessibleSpace(nextCell);
                if (space > maxSpace) {
                    maxSpace = space;
                    bestDirection = dir;
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

    private int countAccessibleSpace(Cell start) {
        if (!isValidMove(start))
            return 0;

        Set<Cell> visited = new HashSet<>();
        Queue<Cell> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);
        int count = 0;
        int snakeLength = gameState.snake.size();
        int maxSearch = Math.min(snakeLength * 2, gameState.board.length * gameState.board[0].length);

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

    private boolean isSafeMove(Node path) {
        Cell endCell = path.cell;
        int accessibleSpace = countAccessibleSpaceAfterMove(endCell, path);
        int snakeLength = gameState.snake.size();

        // Biztonsagos, ha legalabb annyi hely van, mint a kigyo hossza
        return accessibleSpace >= snakeLength;
    }

    private int countAccessibleSpaceAfterMove(Cell endCell, Node path) {
        Set<Cell> pathCells = new HashSet<>();
        Node current = path;
        while (current != null) {
            pathCells.add(current.cell);
            current = current.parent;
        }

        Set<Cell> visited = new HashSet<>(pathCells);
        Queue<Cell> queue = new LinkedList<>();
        queue.add(endCell);
        int count = 0;
        int snakeLength = gameState.snake.size();
        int maxSearch = snakeLength * 2;

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
                return current;
            }

            closedSet.add(current.cell);

            for (Cell neighbor : current.cell.neighbors()) {
                if (!gameState.isOnBoard(neighbor) ||
                        gameState.getValueAt(neighbor) == SnakeGame.SNAKE ||
                        closedSet.contains(neighbor)) {
                    continue;
                }

                double tentativeGScore = gScoreMap.getOrDefault(current.cell, Double.MAX_VALUE) + 1;

                // Sulyozzuk: kerulj a veszelyes teruleteket
                if (isNearSnake(neighbor)) {
                    tentativeGScore += 0.5;
                }
                if (isNearWall(neighbor)) {
                    tentativeGScore += 0.3;
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

    private boolean isNearSnake(Cell cell) {
        for (Cell neighbor : cell.neighbors()) {
            if (gameState.isOnBoard(neighbor) && gameState.getValueAt(neighbor) == SnakeGame.SNAKE) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearWall(Cell cell) {
        return cell.i <= 0 || cell.i >= gameState.board.length - 1 ||
                cell.j <= 0 || cell.j >= gameState.board[0].length - 1;
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
