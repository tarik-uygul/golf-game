package bots;

import java.util.*;
import io.CourseInputModuleStorage;
import model.obstacles.Obstacle;
import model.obstacles.Water;

public class PathPlanner {
    // This is a helper class for MazeBot to compute the path to the hole using A* algorithm.
    // It will take a bird's-eye view of the maze as input and return the optimal path.

    // 1. FIXED: Added 'implements Comparable<Node>'
    private static class Node implements Comparable<Node> {
        int gridX, gridY; // FIXED: Renamed to match the rest of your code
        double gCost, hCost, fCost;
        double worldX, worldY;
        Node parent;

        public Node(int gridX, int gridY, double worldX, double worldY) {
            this.gridX = gridX;
            this.gridY = gridY;
            this.worldX = worldX;
            this.worldY = worldY;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fCost, other.fCost);
        }
    }

    public static List<double[]> findCheckPoints(CourseInputModuleStorage course, double[] start, double[] target) {
        double gridSize = 0.5; // size of each grid cell in world coordinates
        int width = (int) Math.ceil(course.getCourseWidth() / gridSize);
        int height = (int) Math.ceil(course.getCourseHeight() / gridSize);

        Node startNode = new Node((int)(start[0]/gridSize), (int)(start[1]/gridSize), start[0], start[1]);
        Node targetNode = new Node((int)(target[0]/gridSize), (int)(target[1]/gridSize), target[0], target[1]);

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        boolean[][] closedSet = new boolean[width + 1][height + 1];

        // FIXED: Added a tracker for the best gCost to each cell to fix the A* logic
        double[][] bestCosts = new double[width + 1][height + 1];
        for (double[] row : bestCosts) {
            Arrays.fill(row, Double.MAX_VALUE);
        }

        // Initialize the start node
        startNode.gCost = 0;
        startNode.hCost = getDistance(startNode, targetNode);
        startNode.fCost = startNode.hCost;
        openSet.add(startNode);
        bestCosts[startNode.gridX][startNode.gridY] = 0;

        Node current = null;

        // A* search loop
        while (!openSet.isEmpty()) {
            current = openSet.poll();

            // If we are close enough to the target, stop searching
            if (getDistance(current, targetNode) < 1.0) {
                break;
            }

            // Skip if we already evaluated a better path to this node
            if (closedSet[current.gridX][current.gridY]) continue;
            closedSet[current.gridX][current.gridY] = true;

            // Check 8 neighbors (Up, Down, Left, Right, Diagonals)
            int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};

            for (int[] dir : directions) {
                int nx = current.gridX + dir[0];
                int ny = current.gridY + dir[1];

                if (nx >= 0 && nx <= width && ny >= 0 && ny <= height && !closedSet[nx][ny]) {
                    double worldX = nx * gridSize;
                    double worldY = ny * gridSize;

                    if (isSafe(worldX, worldY, course)) {

                        Node tempNeighbor = new Node(nx, ny, worldX, worldY);
                        double newCost = current.gCost + getDistance(current, tempNeighbor);

                        if (newCost < bestCosts[nx][ny]) {
                            bestCosts[nx][ny] = newCost;

                            Node neighbor = new Node(nx, ny, worldX, worldY);
                            neighbor.parent = current;
                            neighbor.gCost = newCost;
                            neighbor.hCost = getDistance(neighbor, targetNode);
                            neighbor.fCost = neighbor.gCost + neighbor.hCost;

                            openSet.add(neighbor);
                        }
                    }
                }
            }
        }

        return extractCheckPoints(current);
    }

    private static boolean isSafe(double x, double y, CourseInputModuleStorage course) {
        // A 35cm buffer ensures the ball's physical radius never touches a hazard or a wall,
        // and protects against physics "drift" when rolling down slopes.
        final double SAFETY_BUFFER = 0.35; 

        // 1. OUT OF BOUNDS PROTECTION (The "Wall Buffer")
        // We use the course dimensions but shrink the "safe zone" by 35cm on all sides.
        // This prevents the bot from pathing exactly on the map's edge.
        if (x < SAFETY_BUFFER || x > course.getCourseWidth() - SAFETY_BUFFER || 
            y < SAFETY_BUFFER || y > course.getCourseHeight() - SAFETY_BUFFER) {
            return false; 
        }

        // 2. NATIVE TERRAIN WATER (Mathematical Height Function)
        if (course.getHeight(x, y) < 0) {
            return false;
        }

        // 3. ALL USER-PLACED OBSTACLES (Trees, Water, Sand)
        if (course.getObstacles() != null) {
            for (model.obstacles.Obstacle obstacle : course.getObstacles()) {
                
                // First, check if the exact point is inside the obstacle (matches physics engine)
                if (obstacle.contains(x, y)) {
                    return false;
                }

                // Second, apply the Safety Buffer to ALL obstacles universally
                double dx = x - obstacle.getX();
                double dy = y - obstacle.getY();
                if (Math.hypot(dx, dy) < obstacle.getRadius() + SAFETY_BUFFER) {
                    return false;
                }
            }
        }
        
        // If it survived all checks, this coordinate is 100% safe to traverse!
        return true;
    }

    private static double getDistance(Node a, Node b) {
        return Math.sqrt(Math.pow(a.worldX - b.worldX, 2) + Math.pow(a.worldY - b.worldY, 2));
    }

    private static List<double[]> extractCheckPoints(Node endNode) {
        List<double[]> path = new ArrayList<>();
        Node current = endNode;

        while (current != null) {
            path.add(0, new double[] { current.worldX, current.worldY });
            current = current.parent;
        }

        List<double[]> sparseCheckPoints = new ArrayList<>();
        for (int i = 0; i < path.size(); i += 2) { // Tighter checkpoints prevent corner-cutting
            sparseCheckPoints.add(path.get(i));
        }

        if (endNode != null) {
            sparseCheckPoints.add(new double[] { endNode.worldX, endNode.worldY }); // ensure the last point is the target
        }

        return sparseCheckPoints;
    }
}
