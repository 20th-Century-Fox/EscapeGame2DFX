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
import javafx.util.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;


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
    private int moveCount;
       
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
    private MediaPlayer bgmPlayer;

 // --- Win overlay ---
    private StackPane gameLayer;
    private ImageView winImage;
    
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
    
 // --- AUDIO ---
    private AudioClip lampSfx;       // short sound effect
    private AudioClip winSfx;        // short sound effect
    private boolean musicOn = true;

    
    @Override
    public void start(Stage stage) {
        this.stage = stage;

        // Build all screens once
        menuScene = buildMenuScene();
        instructionsScene = buildInstructionsScene();
        gameScene = buildGameScene();
        initAudio(); // start music + load sfx
        
        stage.setTitle("Escape the Room Within Lights");
        stage.setScene(menuScene);
        stage.show();
    }
 // =========================
    //  AUDIO SETUP
    // =========================
    private void initAudio() {
        // Background music (loops)
        var bgmUrl = getClass().getResource("/audio/song1.wav");
        
        if (bgmUrl != null) {
            try {
                Media bgm = new Media(bgmUrl.toExternalForm());
                bgmPlayer = new MediaPlayer(bgm);
                bgmPlayer.setVolume(0.25);

             // Loop the music 
             bgmPlayer.setOnEndOfMedia(() -> {
            	 bgmPlayer.seek(Duration.ZERO); 
                 bgmPlayer.play();
             });

             bgmPlayer.play();
             
            } catch (Exception ex) {
                System.out.println("BGM failed to load: " + ex.getMessage());
                bgmPlayer = null;
            }
        } else {
            System.out.println("Missing /audio/song1.wav (place it under src/main/resources/audio/)");
        }

        // Lamp toggle SFX
        var lampUrl = getClass().getResource("/audio/lamp.wav"); 
        if (lampUrl != null) {
            lampSfx = new AudioClip(lampUrl.toExternalForm());
            lampSfx.setVolume(0.7);
            
        } else {
            System.out.println("Missing /audio/lamp.wav");
        }

        // Win SFX
        var winUrl = getClass().getResource("/audio/win.wav"); 
        if (winUrl != null) {
            winSfx = new AudioClip(winUrl.toExternalForm());
            winSfx.setVolume(0.8);
           
        } else {
            System.out.println("Missing /audio/win.wav");
        }
    }
    private void toggleMusic(Button button) {
        musicOn = !musicOn;
        if (bgmPlayer != null) {
            if (musicOn) {
                bgmPlayer.play();
                button.setText("Music: On");
            } else {
                bgmPlayer.pause();
                button.setText("Music: Off");
            }
        } else {
            // No bgm loaded
            button.setText(musicOn ? "Music: On" : "Music: Off");
        }
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
        status = new Label("Click a lit neighbor tile to move. Click nearby L/S to interact. Reach Exit to Escape.");
        Button backToMenu = new Button("Menu");
        Button restart = new Button("Restart");
        Button musicToggle = new Button("Music: On");
        
        backToMenu.setOnAction(e -> {
        	if (winImage != null) winImage.setVisible(false);
            stage.setScene(menuScene);
        });

        restart.setOnAction(e -> {
            resetGame();
            refresh();
            status.setText("Restarted.");
        });
        
        musicToggle.setOnAction(e -> toggleMusic(musicToggle));
        HBox top = new HBox(12, backToMenu, restart, musicToggle, status);
     //   HBox top = new HBox(12, backToMenu, restart, status);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10));

        board = new GridPane();
        board.setPadding(new Insets(12));
        board.setHgap(2);
        board.setVgap(2);
        board.setAlignment(Pos.CENTER);

        var winUrl = getClass().getResource("/images/you_win.png");
        if (winUrl != null) {
            Image img = new Image(winUrl.toExternalForm());
            winImage = new ImageView(img);
            winImage.setPreserveRatio(true);
            winImage.setFitWidth(520);
            winImage.setVisible(false);
            winImage.setMouseTransparent(true); 
        } else {
            winImage = new ImageView();
            winImage.setVisible(false);
            System.out.println("Missing /images/you_win.png");
        }
        
        StackPane gameLayer = new StackPane(board,winImage);
        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(gameLayer);

     // Build the board UI once; we will fill it when we resetGame()
        // (tiles array depends on grid size, so we build after resetGame)
        resetGame();

        return new Scene(root, 720, 520);
    }

    private void resetGame() {
    	loadLevel(LEVEL1);
        recomputeLighting();
        moveCount = 0; 

        if (winImage != null) winImage.setVisible(false);
        lastPr = -1; lastPc = -1;

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

        cellPanes = new StackPane[grid.length][grid[0].length];
        tiles = new Rectangle[grid.length][grid[0].length];

        // Load player image once (from resources)
        var purl = getClass().getResource("/images/player.png");
        if (purl != null) {
            playerImage = new Image(purl.toExternalForm());
            playerView = new ImageView(playerImage);
            playerView.setPreserveRatio(true);
            playerView.setFitWidth(TILE * 0.9);
            playerView.setFitHeight(TILE * 0.9);
        } else {
            playerView = null; // fallback if image missing
            System.out.println("Missing /images/player.png");
        }

        // Load lamp images once
        var lOff = getClass().getResource("/images/lamp_off.png");
        var lOn  = getClass().getResource("/images/lamp_on.png");
        lampOffImage = (lOff == null) ? null : new Image(lOff.toExternalForm());
        lampOnImage  = (lOn  == null) ? null : new Image(lOn.toExternalForm());
        
        // Load door images once
        var dClosed = getClass().getResource("/images/door_closed.png");
        var dOpen   = getClass().getResource("/images/door_open.png");
        doorClosedImage = (dClosed == null) ? null : new Image(dClosed.toExternalForm());
        doorOpenImage   = (dOpen   == null) ? null : new Image(dOpen.toExternalForm());
        
        var sUrl = getClass().getResource("/images/switch.png");
        switchImage = (sUrl == null) ? null : new Image(sUrl.toExternalForm());
        
        var eUrl = getClass().getResource("/images/exit.png");
        exitImage = (eUrl == null) ? null : new Image(eUrl.toExternalForm());
        
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                Rectangle rect = new Rectangle(TILE, TILE);
                rect.setStroke(Color.gray(0.25));

                final int rr = r, cc = c;
                rect.setOnMouseClicked(e -> {
                    if (e.getButton() != MouseButton.PRIMARY) return;
                    handleClick(rr, cc);
                });

                StackPane cell = new StackPane(rect);
                cell.setAlignment(Pos.CENTER);

                tiles[r][c] = rect;
                cellPanes[r][c] = cell;

                board.add(cell, c, r);
            }
        }
    }

    
    
    private void handleClick(int r, int c) {
        // 1) Interact if clicked is on self/adjacent AND is an object
        if (isNeighborOrSelf(r, c, pr, pc)) {
            char t = grid[r][c];
            if (t == LAMP_OFF) {
                grid[r][c] = LAMP_ON;
                lampSfx.play();
                recomputeLighting();
                status.setText("Lamp turned ON.");
                refresh();
                return;
            }
            if (t == LAMP_ON) {
                grid[r][c] = LAMP_OFF;
                lampSfx.play();
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
        moveCount++;
        
        if (grid[pr][pc] == EXIT) {
            status.setText("YOU ESCAPED!");
            status.setText("You escaped in " + moveCount + " moves.");
            if (winSfx != null) winSfx.play();
            winImage.setVisible(true);
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
 // Helper: set/remove a tagged icon (lamp) in a cell
    private void setCellIcon(int r, int c, Image img, String tag) {
        cellPanes[r][c].getChildren().removeIf(n -> tag.equals(n.getUserData()));
        if (img == null) return;

        ImageView iv = new ImageView(img);
        iv.setMouseTransparent(true); // ✅ allow clicks to pass through
        iv.setPreserveRatio(true);
        iv.setFitWidth(TILE * 0.95);
        iv.setFitHeight(TILE * 0.95);
        iv.setUserData(tag);
        cellPanes[r][c].getChildren().add(iv);
    }
    
    private void refresh() {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                Rectangle rect = tiles[r][c];
                char t = grid[r][c];

                // Always clear lamp icons first
                setCellIcon(r, c, null, "lamp");
                setCellIcon(r, c, null, "door");
                setCellIcon(r, c, null, "switch");
                setCellIcon(r, c, null, "exit");


                // walls always visible
                if (t == WALL) {
                    rect.setFill(Color.rgb(75, 75, 90));
                    continue;
                }

                if (!lit[r][c]) {

                    // Keep doors visible even in darkness
                    if (t == DOOR_LOCKED) {
                        rect.setFill(Color.rgb(15, 15, 20));     // dark background
                        setCellIcon(r, c, doorClosedImage, "door");
                    }
                    else if (t == DOOR_OPEN) {
                        rect.setFill(Color.rgb(15, 15, 20));     // dark background
                        setCellIcon(r, c, doorOpenImage, "door");
                    }

                    // Keep lamps visible even in darkness 
                    else if (t == LAMP_OFF) {
                        rect.setFill(Color.rgb(245, 215, 120));
                        setCellIcon(r, c, lampOffImage, "lamp");
                    }
                    else if (t == LAMP_ON) {
                        rect.setFill(Color.rgb(255, 245, 170));
                        setCellIcon(r, c, lampOnImage, "lamp");
                    }
                    else if (t == SWITCH) {
                        rect.setFill(Color.rgb(15, 15, 20)); // dark background
                        setCellIcon(r, c, switchImage, "switch");
                    }

                    // Everything else stays dark
                    else {
                        rect.setFill(Color.rgb(75, 75, 90));
                    }

                    continue;
                }

                // lit colors
                if (t == EXIT) {
                    rect.setFill(Color.rgb(220, 220, 235)); // floor background
                    setCellIcon(r, c, exitImage, "exit");
                }
                else if (t == LAMP_OFF) rect.setFill(Color.rgb(245, 215, 120));
                else if (t == LAMP_ON) rect.setFill(Color.rgb(255, 245, 170));
                
                else rect.setFill(Color.rgb(220, 220, 235)); // floor

                // draw lamp icons on lit tiles
                if (t == LAMP_OFF) setCellIcon(r, c, lampOffImage, "lamp");
                else if (t == LAMP_ON) setCellIcon(r, c, lampOnImage, "lamp");
                
             // draw door icons
                if (t == DOOR_LOCKED) {
                    rect.setFill(Color.rgb(220, 220, 235)); // floor behind
                    setCellIcon(r, c, doorClosedImage, "door");
                }
                else if (t == DOOR_OPEN) {
                    rect.setFill(Color.rgb(220, 220, 235)); // floor behind
                    setCellIcon(r, c, doorOpenImage, "door");
                }
                else {
                    setCellIcon(r, c, null, "door");
                }  
                if (t == SWITCH) {
                    rect.setFill(Color.rgb(220, 220, 235)); // floor background
                    setCellIcon(r, c, switchImage, "switch");
                }
                else if (t == EXIT) {
                    rect.setFill(Color.rgb(15, 15, 20)); // dark background
                    setCellIcon(r, c, exitImage, "exit");
                }
            }
            
        }
        
        // Draw player sprite on top of the current cell
        if (playerView != null) {
            if (lastPr != -1 && lastPc != -1) {
                cellPanes[lastPr][lastPc].getChildren().remove(playerView);
            }
            cellPanes[pr][pc].getChildren().add(playerView);
            lastPr = pr;
            lastPc = pc;
        } else {
            tiles[pr][pc].setFill(Color.rgb(255, 190, 80));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
        
    
        
        
    
