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

    // The main screen that receives focus and keyboard input.
    @FXML
    private Pane gamePane;

    // The maze image shown in the background.
    @FXML
    private ImageView mazeView;

    // The car that moves around the maze.
    private Car car;

    // Step 2 Controls
    @FXML
    private Button startButton;

    @FXML
    private Button resetButton;

    @FXML
    private Label statusLabel;

    // Number of pixels moved for each arrow-key press.
    private static final double STEP = 8;

    // Size of the maze image on the screen.
    private static final double MAZE_WIDTH = 609;
    private static final double MAZE_HEIGHT = 430;

    // Starting position.
    private static final double START_X = 0;
    private static final double START_Y = 258;

    // Exit position.
    private static final double EXIT_X = 580;
    private static final double EXIT_Y = 243;

    // Grid spacing while searching for a path.
    private static final int SOLVE_STEP = 4;

    // Pixels moved per animation tick.
    private static final double ANIMATION_SPEED = 3;

    // Original maze image used to check wall colors.
    private Image mazeImage;

    // Only these four keys can move the car.
    private static final Set<KeyCode> MOVEMENT_KEYS =
            new HashSet<>(Arrays.asList(
                    KeyCode.UP,
                    KeyCode.DOWN,
                    KeyCode.LEFT,
                    KeyCode.RIGHT
            ));

    // Animation path information.
    private List<Point2D> animationPath;
    private int pathIndex;
    private Timeline animationTimeline;

    private boolean animating = false;

    @FXML
    @SuppressWarnings("unused")
    private void initialize() {

        // Keep a reference to the exact maze image used on screen.
        mazeImage = mazeView.getImage();

        // Create the car and add it on top of the maze.
        car = new Car();
        gamePane.getChildren().add(car);

        // Starting position.
        car.setLayoutX(START_X);
        car.setLayoutY(START_Y);

        // Listen for arrow keys.
        gamePane.sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (newScene != null) {
                        newScene.addEventHandler(
                                KeyEvent.KEY_PRESSED,
                                this::moveCar
                        );
                    }
                }
        );

        // Clicking the maze returns keyboard focus.
        gamePane.setOnMouseClicked(
                event -> gamePane.requestFocus()
        );

        Platform.runLater(gamePane::requestFocus);
    }

    private void moveCar(KeyEvent event) {

        // Ignore non-arrow keys and ignore keys while auto-driving.
        if (animating || !MOVEMENT_KEYS.contains(event.getCode())) {
            return;
        }

        // Begin with the car's current position.
        double nextX = car.getLayoutX();
        double nextY = car.getLayoutY();

        // Change one coordinate depending on arrow key.
        switch (event.getCode()) {

            case UP -> nextY -= STEP;

            case DOWN -> nextY += STEP;

            case LEFT -> nextX -= STEP;

            case RIGHT -> nextX += STEP;

            default -> {
            }
        }

        // Move only when the new position is inside the white path.
        if (canOccupy(nextX, nextY)) {

            car.setLayoutX(nextX);
            car.setLayoutY(nextY);

            statusLabel.setText(
                    String.format(
                            "x=%.0f, y=%.0f",
                            nextX,
                            nextY
                    )
            );
        }

        event.consume();
    }

    private boolean canOccupy(double x, double y) {

        // Get the size of the car.
        double carWidth = car.getBoundsInLocal().getWidth();
        double carHeight = car.getBoundsInLocal().getHeight();

        // Check corners and center so the car cannot overlap a wall.
        double[][] samplePoints = {

                {x + 2, y + 2},

                {x + carWidth - 2, y + 2},

                {x + 2, y + carHeight - 2},

                {x + carWidth - 2, y + carHeight - 2},

                {x + carWidth / 2, y + carHeight / 2}
        };

        for (double[] point : samplePoints) {

            // A blue pixel means this point is on a wall.
            if (isBlue(point[0], point[1])) {
                return false;
            }
        }

        // Prevent the car from leaving the maze.
        return x >= 0
                && y >= 0
                && x + carWidth <= MAZE_WIDTH
                && y + carHeight <= MAZE_HEIGHT;
    }

    private boolean isBlue(double x, double y) {

        // Treat positions outside the maze as blocked.
        if (x < 0
                || y < 0
                || x >= MAZE_WIDTH
                || y >= MAZE_HEIGHT) {

            return true;
        }

        // Convert screen position to a pixel in the original image.
        Image image = mazeImage;

        int imageX =
                (int) (x * image.getWidth() / MAZE_WIDTH);

        int imageY =
                (int) (y * image.getHeight() / MAZE_HEIGHT);

        // Read pixel color.
        javafx.scene.paint.Color color =
                image.getPixelReader().getColor(
                        imageX,
                        imageY
                );

        // Blue pixels are maze walls.
        return color.getBlue() > 0.35
                && color.getBlue()
                > color.getRed() * 1.5;
    }

    // -------------------------------------------------
    // Step 2: Automatic Drive
    // -------------------------------------------------

    @FXML
    @SuppressWarnings("unused")
    private void onStartAnimation() {

        if (animating) {
            return;
        }

        Point2D from =
                new Point2D(
                        car.getLayoutX(),
                        car.getLayoutY()
                );

        Point2D to =
                new Point2D(
                        EXIT_X,
                        EXIT_Y
                );

        List<Point2D> rawPath =
                findPath(
                        from,
                        to,
                        SOLVE_STEP
                );

        if (rawPath == null) {

            statusLabel.setText(
                    "No path found from the current position to the exit."
            );

            return;
        }

        // Collapse straight runs so the car changes direction only at corners.
        animationPath = compress(rawPath);

        pathIndex = 0;
        animating = true;

        startButton.setDisable(true);

        statusLabel.setText(
                "Driving to the exit..."
        );

        if (animationTimeline == null) {

            animationTimeline =
                    new Timeline(
                            new KeyFrame(
                                    Duration.millis(16),
                                    event -> stepAnimation()
                            )
                    );

            animationTimeline.setCycleCount(
                    Timeline.INDEFINITE
            );
        }

        animationTimeline.playFromStart();
    }

    @FXML
    @SuppressWarnings("unused")
    private void onReset() {

        if (animationTimeline != null) {
            animationTimeline.stop();
        }

        animating = false;

        startButton.setDisable(false);

        car.setLayoutX(START_X);
        car.setLayoutY(START_Y);

        statusLabel.setText(
                "Use the arrow keys, or press Start Animation."
        );

        gamePane.requestFocus();
    }

    private void stepAnimation() {

        if (pathIndex >= animationPath.size()) {

            animationTimeline.stop();

            animating = false;

            startButton.setDisable(false);

            statusLabel.setText(
                    "Reached the exit!"
            );

            return;
        }

        Point2D target =
                animationPath.get(pathIndex);

        double dx =
                target.getX()
                        - car.getLayoutX();

        double dy =
                target.getY()
                        - car.getLayoutY();

        double dist =
                Math.hypot(dx, dy);

        if (dist <= ANIMATION_SPEED) {

            car.setLayoutX(
                    target.getX()
            );

            car.setLayoutY(
                    target.getY()
            );

            pathIndex++;

        } else {

            car.setLayoutX(
                    car.getLayoutX()
                            + dx / dist
                            * ANIMATION_SPEED
            );

            car.setLayoutY(
                    car.getLayoutY()
                            + dy / dist
                            * ANIMATION_SPEED
            );
        }

        statusLabel.setText(
                String.format(
                        "x=%.0f y=%.0f (driving)",
                        car.getLayoutX(),
                        car.getLayoutY()
                )
        );
    }

    // Breadth-first search over the same canOccupy test
    // used by manual movement.
    private List<Point2D> findPath(
            Point2D start,
            Point2D goal,
            int step) {

        if (!canOccupy(
                start.getX(),
                start.getY())
                || !canOccupy(
                goal.getX(),
                goal.getY())) {

            return null;
        }

        Map<Point2D, Point2D> cameFrom =
                new HashMap<>();

        Deque<Point2D> queue =
                new ArrayDeque<>();

        queue.add(start);

        cameFrom.put(
                start,
                null
        );

        int[][] moves = {
                {step, 0},
                {-step, 0},
                {0, step},
                {0, -step}
        };

        Point2D hit = null;

        while (!queue.isEmpty()) {

            Point2D current =
                    queue.poll();

            if (Math.abs(
                    current.getX()
                            - goal.getX())
                    + Math.abs(
                    current.getY()
                            - goal.getY())
                    <= step) {

                hit = current;
                break;
            }

            for (int[] move : moves) {

                Point2D next =
                        new Point2D(
                                current.getX()
                                        + move[0],

                                current.getY()
                                        + move[1]
                        );

                if (!cameFrom.containsKey(next)
                        && canOccupy(
                        next.getX(),
                        next.getY())) {

                    cameFrom.put(
                            next,
                            current
                    );

                    queue.add(next);
                }
            }
        }

        if (hit == null) {
            return null;
        }

        List<Point2D> path =
                new ArrayList<>();

        for (Point2D p = hit;
             p != null;
             p = cameFrom.get(p)) {

            path.add(p);
        }

        Collections.reverse(path);

        path.add(goal);

        return path;
    }

    // Keeps only points where direction changes.
    private List<Point2D> compress(
            List<Point2D> path) {

        if (path.size() <= 3) {
            return path;
        }

        List<Point2D> corners =
                new ArrayList<>();

        corners.add(
                path.getFirst()
        );

        for (int i = 1;
             i < path.size() - 1;
             i++) {

            Point2D a =
                    path.get(i - 1);

            Point2D b =
                    path.get(i);

            Point2D c =
                    path.get(i + 1);

            int dx1 =
                    (int) Math.signum(
                            b.getX()
                                    - a.getX()
                    );

            int dy1 =
                    (int) Math.signum(
                            b.getY()
                                    - a.getY()
                    );

            int dx2 =
                    (int) Math.signum(
                            c.getX()
                                    - b.getX()
                    );

            int dy2 =
                    (int) Math.signum(
                            c.getY()
                                    - b.getY()
                    );

            if (dx1 * dy2
                    != dy1 * dx2) {

                corners.add(b);
            }
        }

        corners.add(
                path.getLast()
        );

        return corners;
    }
}

