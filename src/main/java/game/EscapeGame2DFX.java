package game;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.Arrays;

public class EscapeGame2DFX extends Application {

    // --- Level symbols ---
    // # wall, . floor, @ player, L lamp(off), * lamp(on), S switch, D locked door, / open door, E exit
    private static final String[] LEVEL1 = {
    		"@.*###########",
            "#.....D..L..##",
            "#.##..##..#.D#",
            "#...#.L....#.#",
            "#LSD.#...#.#.E",
            "#.......#.#..#",
            "#.##L..#L#D..#",
            "#D.#.#.....L.#",
            "##############"
    };

    private static final char WALL = '#';
    private static final char FLOOR = '.';
    private static final char EXIT = 'E';
    private static final char LAMP_OFF = 'L';
    private static final char LAMP_ON = '*';
    private static final char SWITCH = 'S';
    private static final char DOOR_LOCKED = 'D';
    private static final char DOOR_OPEN = '/';

    // --- Model ---
    private char[][] grid;
    private boolean[][] lit;
    private int pr, pc; // player row/col

       
 // --- UI (game screen) ---
    private GridPane board;
    private Rectangle[][] tiles;
    private StackPane[][] cellPanes;
    private Label status;

    private static final int TILE = 38;
    
 // --- Stage + Scenes ---
    private Stage stage;
    private Scene menuScene;
    private Scene instructionsScene;
    private Scene gameScene;

 // --- Player sprite ---
    private ImageView playerView;
    private Image playerImage;
    private int lastPr = -1, lastPc = -1;

    // --- Lamp sprite ---
    private Image lampOffImage;
    private Image lampOnImage;

 // --- Door sprite ---
    private Image doorClosedImage;
    private Image doorOpenImage;
    
 // --- Switch sprite ---
    private Image switchImage;
    
 // --- Exit sprite ---
    private Image exitImage;

    
    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // Build all screens once
        menuScene = buildMenuScene();
        instructionsScene = buildInstructionsScene();
        gameScene = buildGameScene();

        stage.setTitle("Escape the Room Within Lights");
        stage.setScene(menuScene);
        stage.show();
    }

    // =========================
    //  Screen 1: Main Menu
    // =========================
    private Scene buildMenuScene() {
        Label title = new Label("Escape the Room Within Lights");
        title.setFont(Font.font(24));

        Button start = new Button("Start Game");
        Button instructions = new Button("Instructions");
        Button exit = new Button("Exit");
        
        start.setPrefWidth(220);
        instructions.setPrefWidth(220);
        exit.setPrefWidth(220);

        start.setOnAction(e -> {
            resetGame();                 // start fresh each time
            stage.setScene(gameScene);
        });

        instructions.setOnAction(e -> stage.setScene(instructionsScene));
        exit.setOnAction(e -> stage.close());

        VBox box = new VBox(14, title, start, instructions, exit);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));

        BorderPane root = new BorderPane(box);
        root.setPadding(new Insets(20));

        return new Scene(root, 720, 520);
    }
    
 // =========================
    //  Screen 2: Instructions
    // =========================
    private Scene buildInstructionsScene() {
        Label header = new Label("HOW TO PLAY");
        header.setFont(Font.font(22));

        Label body = new Label(
                "- Use the mouse to play.\n" +
                "- You can only move onto LIT tiles.\n" +
                "- Click a neighboring tile to move.\n" +
                "- Click a lamp (L) next to you to turn it ON/OFF.\n" +
                "- Click a switch (S) next to you to toggle doors.\n" +
                "- Reach the exit (E) to clear the room.\n\n" +
                "Tile meanings:\n" +
                "  # = wall (blocks movement + light)\n" +
                "  L = lamp (off)      * = lamp (on)\n" +
                "  S = switch          D = locked door    / = open door\n" +
                "  E = exit\n"
        );
        body.setFont(Font.font(16));
        
        Button back = new Button("Back to Menu");
        back.setOnAction(e -> stage.setScene(menuScene));

        VBox box = new VBox(16, header, body, back);
        box.setPadding(new Insets(30));
        box.setAlignment(Pos.TOP_LEFT);

        BorderPane root = new BorderPane(box);
        return new Scene(root, 720, 520);
    }

    // =========================
    //  Screen 3: Game Screen
    // =========================
    private Scene buildGameScene() {
        // status bar + buttons
        status = new Label("Click a lit neighbor tile to move. Click nearby L/S to interact. Reach E to win.");
        Button backToMenu = new Button("Menu");
        Button restart = new Button("Restart");

        backToMenu.setOnAction(e -> stage.setScene(menuScene));
        restart.setOnAction(e -> {
            resetGame();
            refresh();
            status.setText("Restarted.");
        });

        HBox top = new HBox(12, backToMenu, restart, status);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10));

        board = new GridPane();
        board.setPadding(new Insets(12));
        board.setHgap(2);
        board.setVgap(2);
        board.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(board);
        
     // Build the board UI once; we will fill it when we resetGame()
        // (tiles array depends on grid size, so we build after resetGame)
        resetGame();

        return new Scene(root, 720, 520);
    }

    private void resetGame() {
        loadLevel(LEVEL1);
        recomputeLighting();

        // rebuild grid UI if needed (first time or after a different-size level)
        tiles = new Rectangle[grid.length][grid[0].length];
        buildBoard();

        refresh();
    }

    private void loadLevel(String[] level) {
        int rows = level.length;
        int cols = level[0].length();
        grid = new char[rows][cols];
        lit = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                char ch = level[r].charAt(c);
                if (ch == '@') {
                    pr = r; pc = c;
                    grid[r][c] = FLOOR;
                } else {
                    grid[r][c] = ch;
                }
            }
        }
    }
    
    private void buildBoard() {
        board.getChildren().clear();
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                Rectangle rect = new Rectangle(TILE, TILE);
                rect.setStroke(Color.gray(0.25));

                final int rr = r, cc = c;
                rect.setOnMouseClicked(e -> {
                    if (e.getButton() != MouseButton.PRIMARY) return;
                    handleClick(rr, cc);
                });

                tiles[r][c] = rect;
                board.add(rect, c, r);
            }
        }
    }
    
    private void handleClick(int r, int c) {
        // 1) Interact if clicked is on self/adjacent AND is an object
        if (isNeighborOrSelf(r, c, pr, pc)) {
            char t = grid[r][c];
            if (t == LAMP_OFF) {
                grid[r][c] = LAMP_ON;
                recomputeLighting();
                status.setText("Lamp turned ON.");
                refresh();
                return;
            }
            if (t == LAMP_ON) {
                grid[r][c] = LAMP_OFF;
                recomputeLighting();
                status.setText("Lamp turned OFF.");
                refresh();
                return;
            }
            if (t == SWITCH) {
                toggleDoors();
                recomputeLighting();
                status.setText("Switch toggled doors.");
                refresh();
                return;
            }
        }
        
     // 2) Otherwise try move if clicked is a 4-neighbor tile
        if (is4Neighbor(r, c, pr, pc)) {
            tryMove(r, c);
            refresh();
        }
    }

    private void tryMove(int nr, int nc) {
        if (!inBounds(nr, nc)) return;
        char t = grid[nr][nc];
        if (blocksMovement(t)) return;

        // must be lit to step on
        if (!lit[nr][nc]) {
            status.setText("That tile is dark. Turn on a lamp to light a path.");
            return;
        }

        pr = nr; pc = nc;
        
        if (grid[pr][pc] == EXIT) {
            status.setText("Room Complete! You reached the exit. (Menu → Start again)");
        } else {
            status.setText("Moved.");
        }
    }

    private void recomputeLighting() {
        for (int r = 0; r < lit.length; r++) Arrays.fill(lit[r], false);

        ArrayDeque<int[]> q = new ArrayDeque<>();

        // Start BFS from all LAMP_ON tiles
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] == LAMP_ON) {
                    lit[r][c] = true;
                    q.add(new int[]{r, c});
                }
            }
        }
        
        while (!q.isEmpty()) {
            int[] cur = q.removeFirst();
            int r = cur[0], c = cur[1];

            int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int[] d : dirs) {
                int nr = r + d[0], nc = c + d[1];
                if (!inBounds(nr, nc)) continue;
                if (lit[nr][nc]) continue;
                if (blocksLight(grid[nr][nc])) continue;

                lit[nr][nc] = true;
                q.addLast(new int[]{nr, nc});
            }
        }
    }
    
    private void toggleDoors() {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] == DOOR_LOCKED) grid[r][c] = DOOR_OPEN;
                else if (grid[r][c] == DOOR_OPEN) grid[r][c] = DOOR_LOCKED;
            }
        }
    }

    private boolean blocksMovement(char t) {
        return t == WALL || t == DOOR_LOCKED;
    }

    private boolean blocksLight(char t) {
        return t == WALL || t == DOOR_LOCKED;
    }
    
    private boolean inBounds(int r, int c) {
        return r >= 0 && r < grid.length && c >= 0 && c < grid[0].length;
    }

    private boolean is4Neighbor(int r, int c, int pr, int pc) {
        return (Math.abs(r - pr) + Math.abs(c - pc)) == 1;
    }

    private boolean isNeighborOrSelf(int r, int c, int pr, int pc) {
        return Math.abs(r - pr) <= 1 && Math.abs(c - pc) <= 1;
    }
    
    private void refresh() {
        // draw tiles
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                Rectangle rect = tiles[r][c];
                char t = grid[r][c];

                // walls always visible
                if (t == WALL) {
                    rect.setFill(Color.rgb(75, 75, 90));
                    continue;
                }

                // dark = almost black
                if (!lit[r][c]) {
                    rect.setFill(Color.rgb(15, 15, 20));
                    continue;
                }

                // lit colors
                if (t == EXIT) rect.setFill(Color.rgb(160, 230, 160));
                else if (t == LAMP_OFF) rect.setFill(Color.rgb(245, 215, 120));
                else if (t == LAMP_ON) rect.setFill(Color.rgb(255, 245, 170));
                else if (t == SWITCH) rect.setFill(Color.rgb(140, 200, 255));
                else if (t == DOOR_LOCKED) rect.setFill(Color.rgb(240, 150, 150));
                else if (t == DOOR_OPEN) rect.setFill(Color.rgb(210, 210, 210));
                else rect.setFill(Color.rgb(220, 220, 235)); // floor
            }
        }

        // player highlight
        tiles[pr][pc].setFill(Color.rgb(255, 190, 80));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
        
    
        
        
    
