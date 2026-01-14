package game;

package game;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.Arrays;

public class EscapeLightsFX extends Application {

    // --- Level symbols ---
    // # wall, . floor, @ player, L lamp(off), * lamp(on), S switch, D locked door, / open door, E exit
    private static final String[] LEVEL1 = {
            "###########",
            "#..*....#..#",
            "#.#####.#..#",
            "#...@...#..#",
            "#####.#####E",
            "#.........##",
            "###########"
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

    // --- UI ---
    private GridPane board;
    private Rectangle[][] tiles;
    private Label status;

    private static final int TILE = 36;

    @Override
    public void start(Stage stage) {
        loadLevel(LEVEL1);
        recomputeLighting();

        status = new Label("Click a lit neighbor to move. Click nearby lamp/switch to interact. Reach E.");
        HBox top = new HBox(status);
        top.setPadding(new Insets(10));

        board = new GridPane();
        board.setPadding(new Insets(12));
        board.setHgap(2);
        board.setVgap(2);
        board.setAlignment(Pos.CENTER);

        tiles = new Rectangle[grid.length][grid[0].length];
        buildBoard();

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(board);

        refresh();

        Scene scene = new Scene(root);
        stage.setTitle("Escape the Room Within Lights (JavaFX)");
        stage.setScene(scene);
        stage.show();
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
        if (!lit[nr][nc]) return;

        pr = nr; pc = nc;

        if (grid[pr][pc] == EXIT) {
            status.setText("Room Complete! You reached the exit.");
        } else {
            status.setText("Moved.");
        }
    }

    private void recomputeLighting() {
        for (int r = 0; r < lit.length; r++) Arrays.fill(lit[r], false);

        ArrayDeque<int[]> q = new ArrayDeque<>();

        // Light sources: all lamps that are ON
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
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                Rectangle rect = tiles[r][c];
                char t = grid[r][c];

                // darkness
                if (!lit[r][c] && t != WALL) {
                    rect.setFill(Color.rgb(15, 15, 20));
                    continue;
                }

                // basic tile colors
                if (t == WALL) rect.setFill(Color.rgb(80, 80, 90));
                else if (t == EXIT) rect.setFill(Color.rgb(160, 230, 160));
                else if (t == LAMP_OFF) rect.setFill(Color.rgb(245, 215, 120));
                else if (t == LAMP_ON) rect.setFill(Color.rgb(255, 245, 170));
                else if (t == SWITCH) rect.setFill(Color.rgb(140, 200, 255));
                else if (t == DOOR_LOCKED) rect.setFill(Color.rgb(240, 150, 150));
                else if (t == DOOR_OPEN) rect.setFill(Color.rgb(210, 210, 210));
                else rect.setFill(Color.rgb(220, 220, 235)); // floor
            }
        }

        // draw player highlight
        tiles[pr][pc].setFill(Color.rgb(255, 200, 80));
    }
}
