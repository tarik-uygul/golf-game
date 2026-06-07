package bots;

import java.util.*;
import io.CourseInputModuleStorage;

public class PathPlanner {

    private static class Node implements Comparable<Node> {
        int gridX, gridY;
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

        // Cache the heavy `isSafe` evaluations so we don't recalculate the same grid tile 8 times during neighbor checks
        Boolean[][] safeCache = new Boolean[width + 1][height + 1];

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

                    // Retrieve or compute the safety of this tile only ONCE
                    if (safeCache[nx][ny] == null) {
                        safeCache[nx][ny] = isSafe(worldX, worldY, course);
                    }

                    if (safeCache[nx][ny]) {

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
        // A 35cm buffer ensures the ball's physical radius never touches a hazard
        final double SAFETY_BUFFER = 0.35; 

        // OUT OF BOUNDS PROTECTION
        if (x < SAFETY_BUFFER || x > course.getCourseWidth() - SAFETY_BUFFER || 
            y < SAFETY_BUFFER || y > course.getCourseHeight() - SAFETY_BUFFER) {
            return false; 
        }

        // WATER CHECK (Negative Heights)
        if (course.getHeight(x, y) < 0) {
            return false;
        }

        //GRAVITY & SLOPE CHECK
        // Calculate the steepness of the terrain at this exact grid coordinate
        double slopeX = course.getSlopeX(x, y);
        double slopeY = course.getSlopeY(x, y);
        double slopeGradient = Math.hypot(slopeX, slopeY);
        
        // If the slope pull is stronger than static friction, the ball will roll away. 
        // We CANNOT use this grid cell as a safe stopping point!
        if (slopeGradient > course.getStaticFriction()) {
            return false;
        }

        // 4. ALL USER-PLACED OBSTACLES (Trees, Water, Sand)
        if (course.getObstacles() != null) {
            for (model.obstacles.Obstacle obstacle : course.getObstacles()) {
                if (obstacle.contains(x, y)) {
                    return false;
                }
                double dx = x - obstacle.getX();
                double dy = y - obstacle.getY();
                if (Math.hypot(dx, dy) < obstacle.getRadius() + SAFETY_BUFFER) {
                    return false;
                }
            }
        }
        
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