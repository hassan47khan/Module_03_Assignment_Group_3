package org.example.module03assignmentgroup3;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    //Number of pixels moved for each arrow-key press.
    private static final double STEP = 8;

    //Size of the maze image on the screen.
    private static final double MAZE_WIDTH = 609;
    private static final double MAZE_HEIGHT = 430;

    //The original maze image is used to check wall colors.
    private Image mazeImage;

    //Only these four keys can move the robot.
    private static final Set<KeyCode> MOVEMENT_KEYS = new HashSet<>(Arrays.asList(KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT));

    @FXML
    @SuppressWarnings("unused")
    private void initialize() 
    {
        //Keep a reference to the exact maze image used on screen.
        mazeImage = mazeView.getImage();

        //Start the robot inside the opening on the left side of the maze.
        robotView.setLayoutX(0);
        robotView.setLayoutY(258);

        //Listen for arrow keys on the whole window, not just one control.
        gamePane.sceneProperty().addListener((observable, oldScene, newScene) -> 
        {
            if (newScene != null) 
            {
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, this::moveRobot);
            }
        });

        //Clicking the maze gives keyboard focus back to the game.
        gamePane.setOnMouseClicked(event -> gamePane.requestFocus());

        //Request focus after the window has been displayed.
        //Copilot Recommendation
        Platform.runLater(gamePane::requestFocus);
    }

    private void moveRobot(KeyEvent event) 
    {
        //Ignore keys that are not arrow keys.
        //Copilot Recommendation
        if (!MOVEMENT_KEYS.contains(event.getCode())) 
        {
            return;
        }

        //Begin with the robot's current position.
        double nextX = robotView.getLayoutX();
        double nextY = robotView.getLayoutY();

        //Change only one coordinate for each arrow key.
        switch (event.getCode()) 
        {
            case UP -> nextY -= STEP;
            case DOWN -> nextY += STEP;
            case LEFT -> nextX -= STEP;
            case RIGHT -> nextX += STEP;
            default -> { }
        }

        //Move only when the new position is inside the white path.
        if (canOccupy(nextX, nextY)) 
        {
            robotView.setLayoutX(nextX);
            robotView.setLayoutY(nextY);
        }

        //Stop this key event from being handled again elsewhere.
        //Copilot Recommendation
        event.consume();
    }

    private boolean canOccupy(double x, double y) 
    {
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

        for (double[] point : samplePoints) 
        {
            //A blue pixel means this point is on a wall.
            if (isBlue(point[0], point[1])) 
            {
                return false;
            }
        }

        //Also prevent the robot from leaving the image area.
        //Copilot Recommendation
        return x >= 0 && y >= 0
                && x + robotWidth <= MAZE_WIDTH
                && y + robotHeight <= MAZE_HEIGHT;
    }

    private boolean isBlue(double x, double y) 
    {
        //Treat positions outside the maze as blocked.
        //Copilot Recommendation
        if (x < 0 || y < 0 || x >= MAZE_WIDTH || y >= MAZE_HEIGHT) 
        {
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
}
