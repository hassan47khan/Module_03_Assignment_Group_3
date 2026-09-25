package org.example.module03assignmentgroup3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;

public class HelloController {
    //The main screen that receives focus and keyboard input.
    @FXML
    private Pane gamePane;

    //The maze image shown in the background.
    @FXML
    private ImageView mazeView;

    //The robot image that we move around the maze.
    @FXML
    private ImageView robotView;

    //Step 2 Controls

    @FXML
    private Button startButton;
    @FXML
    private Button resetButton;
    @FXML
    private Label statusLabel;

    //Number of pixels moved for each arrow-key press.
    private static final double STEP = 8;

    //Size of the maze image on the screen.
    private static final double MAZE_WIDTH = 609;
    private static final double MAZE_HEIGHT = 430;

    //Where the robot starts, inside the opening on the left side of the maze.
    private static final double START_X = 0;
    private static final double START_Y = 258;

    //Where the robot is heading: the opening on the right side of the maze.
    private static final double EXIT_X = 580;
    private static final double EXIT_Y = 243;


    //Grid spacing while searching for a path; differ than this and the search gets slow.
    private static final int SOLVE_STEP = 4;

    //Pixels moved per animation tick
    private static final double ANIMATION_SPEED = 3;

    //The original maze image is used to check wall colors.
    private Image mazeImage;

    //Only these four keys can move the robot.
    private static final Set<KeyCode> MOVEMENT_KEYS = new HashSet<>(Arrays.asList(KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT));

    //The waypoints from the robot's start to the exit, and where we are along them.
    private List<Point2D> animationPath;
    private int pathIndex;
    private Timeline animationTimeline;


    @FXML
    @SuppressWarnings("unused")
    private void initialize() {
        //Keep a reference to the exact maze image used on screen.
        mazeImage = mazeView.getImage();

        //Start the robot inside the opening on the left side of the maze.
        robotView.setLayoutX(START_X);
        robotView.setLayoutY(START_Y);

        //Listen for arrow keys on the whole window, not just one control.
        gamePane.sceneProperty().addListener((observable, oldScene, newScene) ->
        {
            if (newScene != null) {
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, this::moveRobot);
            }
        });

        //Clicking the maze gives keyboard focus back to the game.
        gamePane.setOnMouseClicked(event -> gamePane.requestFocus());

        //Request focus after the window has been displayed.
        //Copilot Recommendation
        Platform.runLater(gamePane::requestFocus);
    }
    private boolean animating = false;

    private void moveRobot(KeyEvent event) {

        //Ignore keys that are not arrow keys, and ignore all keys while auto-driving.
        if (animating || !MOVEMENT_KEYS.contains(event.getCode())) {
            return;
        }

        //Begin with the robot's current position.
        double nextX = robotView.getLayoutX();
        double nextY = robotView.getLayoutY();

        //Change only one coordinate for each arrow key.
        switch (event.getCode()) {
            case UP -> nextY -= STEP;
            case DOWN -> nextY += STEP;
            case LEFT -> nextX -= STEP;
            case RIGHT -> nextX += STEP;
            default -> {
            }
        }

        //Move only when the new position is inside the white path.
        if (canOccupy(nextX, nextY)) {
            robotView.setLayoutX(nextX);
            robotView.setLayoutY(nextY);
            statusLabel.setText(String.format("x=%.0f,y=%.0f", nextX, nextY));
        }

        //Stop this key event from being handled again elsewhere.
        //Copilot Recommendation
        event.consume();
    }

    private boolean canOccupy(double x, double y) {
        //Get the robot's current size, including its image bounds.
        double robotWidth = robotView.getBoundsInParent().getWidth();
        double robotHeight = robotView.getBoundsInParent().getHeight();

        //Check the corners and center so the robot cannot overlap a wall.
        double[][] samplePoints =
                {
                        {x + 2, y + 2},
                        {x + robotWidth - 2, y + 2},
                        {x + 2, y + robotHeight - 2},
                        {x + robotWidth - 2, y + robotHeight - 2},
                        {x + robotWidth / 2, y + robotHeight / 2}
                };

        for (double[] point : samplePoints) {
            //A blue pixel means this point is on a wall.
            if (isBlue(point[0], point[1])) {
                return false;
            }
        }

        //Also prevent the robot from leaving the image area.
        //Copilot Recommendation
        return x >= 0 && y >= 0
                && x + robotWidth <= MAZE_WIDTH
                && y + robotHeight <= MAZE_HEIGHT;
    }

    private boolean isBlue(double x, double y) {
        //Treat positions outside the maze as blocked.
        //Copilot Recommendation
        if (x < 0 || y < 0 || x >= MAZE_WIDTH || y >= MAZE_HEIGHT) {
            return true;
        }

        //Convert the screen position to a pixel in the original image.
        Image image = mazeImage;
        int imageX = (int) (x * image.getWidth() / MAZE_WIDTH);
        int imageY = (int) (y * image.getHeight() / MAZE_HEIGHT);

        //Read the pixel color. Blue pixels are maze walls.
        javafx.scene.paint.Color color = image.getPixelReader().getColor(imageX, imageY);
        return color.getBlue() > 0.35 && color.getBlue() > color.getRed() * 1.5;
    }
    //--------------------------------------- Step 2: auto-drive

    @FXML
    @SuppressWarnings("unused")
    private void onStartAnimation() {
        if (animating) {
            return;
        }

        Point2D from = new Point2D(robotView.getLayoutX(), robotView.getLayoutY());
        Point2D to = new Point2D(EXIT_X, EXIT_Y);

        List<Point2D> rawPath = findPath(from, to, SOLVE_STEP);
        if (rawPath == null) {
            statusLabel.setText("No path found from the current position to the exit.");
            return;
        }

        //Collapse straight runs so the robot only changes direction at corners
        animationPath = compress(rawPath);
        pathIndex = 0;
        animating = true;
        startButton.setDisable(true);
        statusLabel.setText("Driving to the exit...");

        if (animationTimeline == null) {
            animationTimeline = new Timeline(new KeyFrame(Duration.millis(16), event -> stepAnimation()));
            animationTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        animationTimeline.playFromStart();
    }

    @FXML
    @SuppressWarnings("unused")
    private void onReset() {

        if (animationTimeline != null)

            animationTimeline.stop();

        animating = false;
        startButton.setDisable(false);
        robotView.setLayoutX(START_X);
        robotView.setLayoutY(START_Y);
        statusLabel.setText("Use the arrow keys, or press Start Animation.");
        gamePane.requestFocus();
    }

    private void stepAnimation() {
        if (pathIndex >= animationPath.size()) {
            animationTimeline.stop();
            animating = false;
            startButton.setDisable(false);
            statusLabel.setText("Reached the exit!");
            return;
        }

        Point2D target = animationPath.get(pathIndex);
        double dx = target.getX() - robotView.getLayoutX();
        double dy = target.getY() - robotView.getLayoutY();
        double dist = Math.hypot(dx, dy);

        if (dist <= ANIMATION_SPEED) {
            robotView.setLayoutX(target.getX());
            robotView.setLayoutY(target.getY());
            pathIndex++;

        } else {
            robotView.setLayoutX(robotView.getLayoutX() + dx / dist * ANIMATION_SPEED);
            robotView.setLayoutY(robotView.getLayoutY() + dy / dist * ANIMATION_SPEED);
        }

        statusLabel.setText(String.format("x=%.0f  y=%.0f  (driving)", robotView.getLayoutX(), robotView.getLayoutY()));
    }
    //Breath-first search over the same canOccupy test the manual movemenet uses,
    //so a position reachable by animation is exactly one reachable by hand

    private List<Point2D> findPath(Point2D start, Point2D goal, int step)
    {
        if (!canOccupy(start.getX(), start.getY()) || !canOccupy(goal.getX(), goal.getY())) {
            return null;
        }

        Map<Point2D, Point2D> cameFrom = new HashMap<>();
        Deque<Point2D> queue = new ArrayDeque<>();
        queue.add(start);
        cameFrom.put(start, null);

        int[][] moves = {{step, 0}, {-step, 0}, {0, step}, {0, -step}};
        Point2D hit = null;

        while (!queue.isEmpty()) {
            Point2D current = queue.poll();
            if (Math.abs(current.getX() - goal.getX()) + Math.abs(current.getY() - goal.getY()) <= step) {
                hit = current;
                break;
            }
            for (int[] move : moves) {
                Point2D next = new Point2D(current.getX() + move[0], current.getY() + move[1]);
                if (!cameFrom.containsKey(next) && canOccupy(next.getX(), next.getY()))
                {
                    cameFrom.put(next, current);
                    queue.add(next) ;
                }
            }
}
        if (hit == null) {
            return null;
        }

        List<Point2D> path = new ArrayList<>();
        for (Point2D p = hit; p != null; p = cameFrom.get(p)) {
            path.add(p);
        }
        Collections.reverse(path);
        path.add(goal);
        return path;
        }

        //Keeps only the point where directions changes, so the robot drives in straight lines.
    private List<Point2D> compress(List<Point2D> path)
    {
        if (path.size() <= 3)
        {
            return path;
        }

        List<Point2D> corners  = new ArrayList<>();
        corners.add(path.getFirst());

        for (int i = 1; i < path.size() - 1; i++) {
            Point2D a = path.get(i - 1);
            Point2D b = path.get(i);
            Point2D c = path.get(i + 1);

            int dx1 = (int) Math.signum(b.getX() - a.getX());
            int dy1 = (int) Math.signum(b.getY() - a.getY());
            int dx2 = (int) Math.signum(c.getX() - b.getX());
            int dy2 = (int) Math.signum(c.getY() - b.getY());

            if (dx1 * dy2 != dy1 * dx2) {
                corners.add(b);
            }
        }
        corners.add(path.getLast());
        return corners;

    }
    }

