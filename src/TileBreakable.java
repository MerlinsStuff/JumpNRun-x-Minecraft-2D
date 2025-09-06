import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class TileBreakable extends Tile {

    private ArrayList<BufferedImage> breakStages = new ArrayList<>();
    private int currentStage = 0;
    private boolean destroyed = false;

    public TileBreakable(String basePath, String tileName, float x, float y) {
        super(0, x, y); // imageIndex nicht relevant
        hasRigidCollision = true;

        try {
            // Lade alle Stufen GrassMidBreaking0..n
            int stage = 0;
            while (true) {
                File f = new File(basePath + tileName + "Breaking" + stage + ".png");
                if (!f.exists()) break;
                breakStages.add(ImageIO.read(f));
                stage++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TileBreakable(String basePath, String tileName, float x, float y, boolean flip) {
        super(0, x, y);
        hasRigidCollision = true;

        try {
            int stage = 0;
            while (true) {
                File f = new File(basePath + tileName + "Breaking" + stage + ".png");
                if (!f.exists()) break;

                BufferedImage img = ImageIO.read(f);
                if (flip) img = flipHorizontally(img);
                breakStages.add(img);
                stage++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hilfsmethode zum horizontalen Spiegeln
    private static BufferedImage flipHorizontally(BufferedImage img) {
        BufferedImage flipped = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        Graphics2D g = flipped.createGraphics();
        g.drawImage(img, img.getWidth(), 0, -img.getWidth(), img.getHeight(), null);
        g.dispose();
        return flipped;
    }


    /** Spieler hat den Block getroffen */
    public void damage(Level l, int strength) {
        if (destroyed) return;

        currentStage += strength;
        if (currentStage >= breakStages.size()) {
            destroyed = true;
            l.tiles.remove(this); // Block verschwindet komplett
        }
    }

    @Override
    public void drawStatic(Graphics2D g2d, float offsetX, float offsetY) {
        // zerstörbare Tiles werden dynamisch gezeichnet
    }

    @Override
    public void draw(Graphics2D g2d, float offsetX, float offsetY) {
        if (!destroyed) {
            g2d.drawImage(
                breakStages.get(currentStage),
                (int) (bb.min.x - offsetX),
                (int) (bb.min.y - offsetY),
                null
            );
        }
    }
}

