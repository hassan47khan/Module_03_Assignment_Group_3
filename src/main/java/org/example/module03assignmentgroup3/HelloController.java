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
import javafx.scene.control.TabPane;
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

    // Pane used for Maze 2.    @FXML
    private Pane maze2Pane;

    // Tab Pane created in order to include maze2
    @FXML
    private TabPane tabPane;

    // The maze image shown in the background.
    @FXML
    private ImageView mazeView;

    @FXML
    private ImageView maze2View;

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

    // Maze 2 starting and exit positions.
    private static final double MAZE2_START_X = 30;
    private static final double MAZE2_START_Y = 40;

    private static final double MAZE2_EXIT_X = 566;
    private static final double MAZE2_EXIT_Y = 412;


    // Grid spacing while searching for a path.
    private static final int SOLVE_STEP = 4;

    // Pixels moved per animation tick.
    private static final double ANIMATION_SPEED = 3;


    // Currently active maze image used for pixel collision detection.
    private Image mazeImage;

    // Keeps track of which maze is currently selected.
    private boolean maze2Active = false;

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

        // Initialize the car and maze image for the initially selected tab.
        if (tabPane.getSelectionModel().getSelectedIndex() == 1) {

            maze2Active = true;

            gamePane.getChildren().remove(car);
            maze2Pane.getChildren().add(car);

            mazeImage = maze2View.getImage();

            car.setLayoutX(MAZE2_START_X);
            car.setLayoutY(MAZE2_START_Y);
            car.setDirection("RIGHT");
        }

        // Switch the car and maze image when the selected tab changes.
        tabPane.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldTab, newTab) -> {

                    if (newTab.getText().equals("Maze 2")) {
                        maze2Active = true;

                        gamePane.getChildren().remove(car);
                        maze2Pane.getChildren().add(car);

                        mazeImage = maze2View.getImage();

                        car.setLayoutX(MAZE2_START_X);
                        car.setLayoutY(MAZE2_START_Y);
                        car.setDirection("RIGHT");

                    } else {
                        maze2Active = false;

                        maze2Pane.getChildren().remove(car);
                        gamePane.getChildren().add(car);

                        mazeImage = mazeView.getImage();

                        car.setLayoutX(START_X);
                        car.setLayoutY(START_Y);
                        car.setDirection("RIGHT");
                    }
                });

        // Listen for arrow keys at the Scene level so both maze tabs can receive input.
        Platform.runLater(() -> {
            tabPane.getScene().addEventFilter(
                    KeyEvent.KEY_PRESSED,
                    this::moveCar
            );
        });

        // Clicking the maze returns keyboard focus.
        gamePane.setOnMouseClicked(
                event -> gamePane.requestFocus()
        );

        Platform.runLater(() -> tabPane.requestFocus());
    }

    private void moveCar(KeyEvent event) {

        statusLabel.setText("KEY: " + event.getCode());

        // Ignore non-arrow keys and ignore keys while auto-driving.
        if (animating || !MOVEMENT_KEYS.contains(event.getCode())) {
            return;
        }

        // Begin with the car's current position.
        double nextX = car.getLayoutX();
        double nextY = car.getLayoutY();

        // Change one coordinate depending on arrow key.

        String direction = "";

        switch (event.getCode()) {

            case UP -> {
                nextY -= STEP;
                direction = "UP";
            }

            case DOWN -> {
                nextY += STEP;
                direction = "DOWN";
            }

            case LEFT -> {
                nextX -= STEP;
                direction = "LEFT";
            }

            case RIGHT -> {
                nextX += STEP;
                direction = "RIGHT";
            }

            default -> {
            }
        }

        String oldDirection = car.getDirection();

        car.setDirection(direction);

        // Then Check if the car can move in that direction.
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
        } else {

            car.setDirection(oldDirection);
            statusLabel.setText("BLOCKED: " + event.getCode());
        }

        event.consume();
    }

    private boolean canOccupy(double x, double y) {

        double carWidth = 24;
        double carHeight = 11;

        double left = x;
        double top = y;
        double right = x + carWidth;
        double bottom = y + carHeight;

        if (left < 0
                || top < 0
                || right >= MAZE_WIDTH
                || bottom >= MAZE_HEIGHT) {

            return false;
        }

        double[][] samplePoints = {
                {left + 2, top + 2},
                {right - 2, top + 2},
                {left + 2, bottom - 2},
                {right - 2, bottom - 2},

                {(left + right) / 2, top + 2},
                {(left + right) / 2, bottom - 2},

                {left + 2, (top + bottom) / 2},
                {right - 2, (top + bottom) / 2},

                {(left + right) / 2, (top + bottom) / 2}
        };

        for (double[] point : samplePoints) {

            if (isBlue(point[0], point[1])) {
                return false;
            }
        }

        return true;
    }

    private boolean isBlue(double x, double y) {

        if (x < 0
                || y < 0
                || x >= MAZE_WIDTH
                || y >= MAZE_HEIGHT) {

            return true;
        }

        Image image = mazeImage;

        int imageX =
                (int) (x * image.getWidth() / MAZE_WIDTH);

        int imageY =
                (int) (y * image.getHeight() / MAZE_HEIGHT);

        javafx.scene.paint.Color color =
                image.getPixelReader().getColor(
                        imageX,
                        imageY
                );

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

        if (tabPane.getSelectionModel().getSelectedIndex() == 1) {
            mazeImage = maze2View.getImage();
        } else {
            mazeImage = mazeView.getImage();
        }

        Point2D from =
                new Point2D(
                        car.getLayoutX(),
                        car.getLayoutY()
                );

        // Use the correct exit position for the selected maze.
        boolean maze2 = tabPane.getSelectionModel().getSelectedIndex() == 1;

        double exitX = maze2 ? MAZE2_EXIT_X : EXIT_X;
        double exitY = maze2 ? MAZE2_EXIT_Y : EXIT_Y;

        Point2D to =
                new Point2D(
                        exitX,
                        exitY
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

        // Reset the car to the starting position of the selected maze.
        if (maze2Active) {
            car.setLayoutX(MAZE2_START_X);
            car.setLayoutY(MAZE2_START_Y);
        } else {
            car.setLayoutX(START_X);
            car.setLayoutY(START_Y);
        }

        car.setDirection("RIGHT");


        statusLabel.setText(
                "Use the arrow keys, or press Start Animation."
        );


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

        // Change the car's direction while it follows the path.
        if (Math.abs(dx) > Math.abs(dy)) {

            if (dx > 0) {
                car.setDirection("RIGHT");
            } else {
                car.setDirection("LEFT");
            }

        } else if (Math.abs(dy) > 0) {

            if (dy > 0) {
                car.setDirection("DOWN");
            } else {
                car.setDirection("UP");
            }
        }

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
                start.getY())) {

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

