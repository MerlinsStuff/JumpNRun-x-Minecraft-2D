import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

public class Level {
	BufferedImage levelImg, resultingLevelImg, backgroundImage, cloudImage1;
	public Player player;
	Vec2 lvlSize;
	float offsetX;
	ArrayList<Tile> tiles;
	public ArrayList<Creeper> creepers;

	public Level(String levelMapPath, String levelBackgroundMapPath) {
		try {
			backgroundImage = ImageIO.read(new File(levelBackgroundMapPath));
			tiles = new ArrayList<>();
			lvlSize = new Vec2(0, 0);
			offsetX = 0.0f;
			creepers = new ArrayList<Creeper>();

			try {
				// Level image
				levelImg = ImageIO.read(new File(levelMapPath));

				// Cloud image
				cloudImage1 = ImageIO.read(new File("./assets/Items/cloud1.png"));

				// Tile images /
				Tile.images.add(ImageIO.read(new File("./assets/Tiles/liquidWaterTop_mid.png")));
				Tile.images.add(ImageIO.read(new File("./assets/Tiles/signRight.png")));
				Tile.images.add(ImageIO.read(new File("./assets/Tiles/signExit.png")));
				Tile.images.add(ImageIO.read(new File("./assets/Items/coinGold.png")));
				Tile.images.add(ImageIO.read(new File("./assets/Items/springboardDown.png")));
				Tile.images.add(ImageIO.read(new File("./assets/Items/springboardUp.png")));
			} catch (IOException e) {

			}
			initLevel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update() {

		//update camera offset
		float diff = (player.boundingBox.max.x + player.boundingBox.min.x)*0.5f-500  - offsetX;

		int noMoveZone = 100;

		if(Math.abs(diff)>noMoveZone){
			if(diff<0)
				diff+=noMoveZone;
			else
				diff-=noMoveZone;
			offsetX += diff;
		}


		if (offsetX < 0)
			offsetX = 0;

		if (offsetX > resultingLevelImg.getWidth() - 1000)
			offsetX = resultingLevelImg.getWidth() - 1000;
	}

	public void initLevel() {
		// Levelgröße berechnen
		lvlSize.x = Tile.tileSize * levelImg.getWidth();
		lvlSize.y = Tile.tileSize * levelImg.getHeight();

		resultingLevelImg = new BufferedImage((int) lvlSize.x, (int) lvlSize.y, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = resultingLevelImg.createGraphics();

		// Hintergrund + Wolken zeichnen
		drawBackground(g2d);

		// Tiles zurücksetzen
		tiles.clear();

		// Tiles erstellen
		for (int y = 0; y < levelImg.getHeight(); y++) {
			for (int x = 0; x < levelImg.getWidth(); x++) {
				Color color = new Color(levelImg.getRGB(x, y));
				Tile t = createTileByColor(color, x * Tile.tileSize, y * Tile.tileSize);
				if (t != null) {
					tiles.add(t);
					t.drawStatic(g2d, 0, 0);
				}
			}
		}

		g2d.dispose();
	}

	// Hintergrund und Wolken
	private void drawBackground(Graphics2D g2d) {
		int width = resultingLevelImg.getWidth();
		int bgWidth = backgroundImage.getWidth();

		// Hintergrund kacheln
		for (int x = 0; x < width; x += bgWidth) {
			g2d.drawImage(backgroundImage, null, x, 0);
			BufferedImage flipped = flipHorizontally(backgroundImage);
			backgroundImage = flipped; // optional spiegeln
		}

		// Wolken
		Random r = new Random();
		for (int x = 0; x < width; x += cloudImage1.getWidth() * 2) {
			g2d.drawImage(cloudImage1, null, x + r.nextInt(250), r.nextInt(250) + 50);
		}
	}

	// Tile anhand von Farbe erstellen
	private Tile createTileByColor(Color color, float x, float y) {
		switch (color.getRGB()) {
			case 0xFF0000FF: // Blau - Wasser
				return new TileWater(0, x, y);
			case 0xFF000000: // Schwarz - Mittel Grass
				return new TileBreakable("./assets/Tiles/grassMid/", "GrassMid", x, y);
			case 0xFF00FF00: // Grün - Sign Right
				return new Tile(1, x, y, false);
			case 0xFFFF0000: // Rot - Exit Sign
				return new Tile(2, x, y, false);
			case 0xFFFFFF00: // Gelb - Rechts Grass
				return new TileBreakable("./assets/Tiles/grassLeft/", "GrassLeft", x, y, true); // gespiegelt
			case 0xFF808080: // Grau - Links Grass
				return new TileBreakable("./assets/Tiles/grassLeft/", "GrassLeft", x, y); // links
			case 0xFFFFA500: // Orange - Coins
				return new TileCoin(3, x, y);
			case 0xFF00FFFF: // Cyan - Springboard
				return new TileSpringBoard(4, x, y);
			case 0xFFFF00FF: // Magenta - Center
				return new TileBreakable("./assets/Tiles/grassCenter/", "GrassCenter", x, y);
			case 0xFFFFAFAF: // Pink - Grass
				return new TileBreakable("./assets/Tiles/grass/", "Grass", x, y);
			case 0xFF006400:
				Creeper c = new Creeper(this, x - Tile.tileSize, y - Tile.tileSize);
				System.out.println("Creeper spawned at " + c.pos.x + ", " + c.pos.y);
				creepers.add(c);

				return null;
			default:
				return null;
		}
	}

	// Horizontal flip Hilfsmethode
	private static BufferedImage flipHorizontally(BufferedImage img) {
		BufferedImage flipped = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
		Graphics2D g = flipped.createGraphics();
		g.drawImage(img, img.getWidth(), 0, -img.getWidth(), img.getHeight(), null);
		g.dispose();
		return flipped;
	}


	public Image getResultingImage() {
		return resultingLevelImg;
	}

	public int getSizeX() {
		return resultingLevelImg.getWidth();
	}

	public int getSizeY() {
		return resultingLevelImg.getHeight();
	}
}
