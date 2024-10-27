package com.buaisociety.pacman.entity.behavior;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.buaisociety.pacman.maze.Maze;
import com.buaisociety.pacman.sprite.DebugDrawing;
import com.cjcrafter.neat.Client;
import com.buaisociety.pacman.entity.Direction;
import com.buaisociety.pacman.entity.Entity;
import com.buaisociety.pacman.entity.PacmanEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

import java.util.concurrent.ThreadLocalRandom;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;

import com.buaisociety.pacman.maze.Tile;




public class NeatPacmanBehavior implements Behavior {

    /*private final Queue<Direction> lastDirections = new LinkedList<>();
    private static final int DIRECTION_HISTORY_LIMIT = 4; // Number of recent directions to track
    private static final int MAX_CIRCLE_COUNT = 3; // Number of circular detections before killing Pac-Man
    private int circleCount = 0; // Counter for consecutive circular patterns
*/
    private final @NotNull Client client;
    private @Nullable PacmanEntity pacman;
    private float lastPositionX = -1; // Store the last X position
    private float lastPositionY = -1; // Store the last Y position
    private int stuckCounter = 0; // Counter for how long Pac-Man has been stuck
    private static final int STUCK_LIMIT = 60 * 3; // Number of updates to be considered stuck (adjust as necessary)



    // Score modifiers help us maintain "multiple pools" of points.
    // This is great for training, because we can take away points from
    // specific pools of points instead of subtracting from all.
    private int scoreModifier = 0;

    private int numberUpdatesSinceLastScore =0;
    private int lastScore = 0;
    private int pacytimer = 0;

    private static final int MAX_HISTORY = 60 * 15; // Number of frames to track
    private final Deque<Vector2> positionHistory = new ArrayDeque<>(MAX_HISTORY);
    private final Set<Vector2i> visitedTiles = new HashSet<>();
    

    public NeatPacmanBehavior(@NotNull Client client) {
        this.client = client;
    }

    public float getDisplacement() {
        if (positionHistory.size() < MAX_HISTORY) {
            // Not enough history to calculate displacement
            return 0f; // Or throw an exception, based on your needs
        }
    
        // Get the position from 300 frames ago
        Vector2 position300FramesAgo = positionHistory.peekFirst(); // Get the oldest position
    
        // Get the current position
        Vector2 currentPosition = new Vector2(pacman.getTilePosition().x(), pacman.getTilePosition().y());
    
        // Calculate the displacement
        float displacementX = currentPosition.x - position300FramesAgo.x;
        float displacementY = currentPosition.y - position300FramesAgo.y;
    
        // Return the magnitude of the displacement
        return (float) Math.sqrt(displacementX * displacementX + displacementY * displacementY);
    }

    




    /**
     * Returns the desired direction that the entity should move towards.
     *
     * @param entity the entity to get the direction for
     * @return the desired direction for the entity
     */
    @NotNull
    @Override
    public Direction getDirection(@NotNull Entity entity) {
        if (pacman == null) {
            pacman = (PacmanEntity) entity;
        }

        // SPECIAL TRAINING CONDITIONS
        // TODO: Make changes here to help with your training...

        int newScore = pacman.getMaze().getLevelManager().getScore();
        if (newScore > lastScore) {
            lastScore = newScore;
            numberUpdatesSinceLastScore = 0;
        }

        if (numberUpdatesSinceLastScore++ > 60 * 10) {
            pacman.kill();
            return Direction.UP;
        }

        pacytimer = pacytimer + 1;


        // END OF SPECIAL TRAINING CONDITIONS

        // We are going to use these directions a lot for different inputs. Get them all once for clarity and brevity
        Direction forward = pacman.getDirection();
        Direction left = pacman.getDirection().left();
        Direction right = pacman.getDirection().right();
        Direction behind = pacman.getDirection().behind();

        Vector2i currentTilePosition = pacman.getTilePosition();
        visitedTiles.add(currentTilePosition);



        // Input nodes 1, 2, 3, and 4 show if the pacman can move in the forward, left, right, and behind directions
        boolean canMoveForward = pacman.canMove(forward);
        boolean canMoveLeft = pacman.canMove(left);
        boolean canMoveRight = pacman.canMove(right);
        boolean canMoveBehind = pacman.canMove(behind);

    
        
        pacman.getTilePosition().x();
        float pacmanPositionX = pacman.getTilePosition().x();
        float pacmanPositionY = pacman.getTilePosition().y();
        float pelletTileX = pacman.getMaze().getClosestPellet().getPosition().x();
        float pelletTileY = pacman.getMaze().getClosestPellet().getPosition().y();
        float pelletRemainingNode = pacman.getMaze().getPelletsRemaining();
        float currentScore = pacman.getMaze().getLevelManager().getScore();
        float displll = getDisplacement();
        float randomNumber = ThreadLocalRandom.current().nextFloat();



        positionHistory.addLast(new Vector2(pacmanPositionX, pacmanPositionY));
        // If the history exceeds the maximum size, remove the oldest position
        if (positionHistory.size() > MAX_HISTORY) {
            positionHistory.removeFirst();
        }

        float[] outputs = client.getCalculator().calculate(new float[]{
            canMoveForward ? 1f : 0f,
            canMoveLeft ? 1f : 0f,
            canMoveRight ? 1f : 0f,
            canMoveBehind ? 1f : 0f,
            //pacmanPositionX,
            //pacmanPositionY,
            pelletTileY,
            pelletTileX,
            pelletRemainingNode,
            currentScore,
             // displll,
            randomNumber,
            pacytimer,


            
        }).join();




        int index = 0;
        float max = outputs[0];
        for (int i = 1; i < outputs.length; i++) {
            if (outputs[i] > max) {
                max = outputs[i];
                index = i;
            }
        }

        Direction newDirection = switch (index) {
            case 0 -> pacman.getDirection();
            case 1 -> pacman.getDirection().left();
            case 2 -> pacman.getDirection().right();
            case 3 -> pacman.getDirection().behind();
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };

        // Track the last few directions to detect circular movement
        /*lastDirections.add(newDirection);
        if (lastDirections.size() > DIRECTION_HISTORY_LIMIT) {
            lastDirections.poll(); // Maintain a fixed size for the direction history
        }

        // Check for circular movement pattern
        if (lastDirections.stream().distinct().count() <= 2) { // Only two distinct directions in history
          circleCount++; // Increment the circular movement counter
        } else {
            circleCount = 0; // Reset counter if Pac-Man breaks out of the circle
        }

        if (circleCount >= MAX_CIRCLE_COUNT) {
            pacman.kill(); // Kill Pac-Man if circular movement persists
            return Direction.UP; // Return an arbitrary direction after Pac-Man is killed
        } */
        
        // Direction[] directionList;
        // directionList = directionList + pacman.getDirection();

        Direction currentDirection = pacman.getDirection();

        if ((newDirection == Direction.DOWN && currentDirection == Direction.UP) ||
        (newDirection == Direction.UP && currentDirection == Direction.DOWN)) {
         newDirection = currentDirection; // Keep current direction if there is a conflict
        }

        // Similarly, handle left and right conflicts if needed
        if ((newDirection == Direction.LEFT && currentDirection == Direction.RIGHT) ||
            (newDirection == Direction.RIGHT && currentDirection == Direction.LEFT)) {
            newDirection = currentDirection; // Keep current direction if there's a conflict
        }





        client.setScore(pacman.getMaze().getLevelManager().getScore() + scoreModifier);
        return newDirection;
    }

    @Override
    public void render(@NotNull SpriteBatch batch) {
        // TODO: You can render debug information here
        /*
        if (pacman != null) {
            DebugDrawing.outlineTile(batch, pacman.getMaze().getTile(pacman.getTilePosition()), Color.RED);
            DebugDrawing.drawDirection(batch, pacman.getTilePosition().x() * Maze.TILE_SIZE, pacman.getTilePosition().y() * Maze.TILE_SIZE, pacman.getDirection(), Color.RED);
        }
         */
    }
}
