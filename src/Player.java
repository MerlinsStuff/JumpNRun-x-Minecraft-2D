import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Player {
    enum PlayerState { IDLE, WALK, JUMP, FALL, PUNCH, SWORD_ATTACK }
    PlayerState currentState = PlayerState.IDLE;

    boolean jump = false, walkingLeft = false, walkingRight = false;
    boolean collidesTop = false, collidesDown = false, collidesLeft = false, collidesRight = false, collides = false;

    boolean facingLeft = false;

    Vec2 pos;
    Vec2 posLastFrame;
    Vec2 gravity;
    Vec2 maxSpeed;
    float padX;
    float padTop;
    int w;
    int h;

    public Vec2 lastValidPosition;

    float movementSpeed;

    BoundingBox boundingBox;
    int displayedAnimationState = 0;
    int moveCounter = 0;
    int points = 0;
    int numberOfLifes = 3;

    float jumpPower = 25.f;

    private boolean soundEnabled = true;

    // Animation-Listen
    private ArrayList<BufferedImage> idleTiles = new ArrayList<>();
    private ArrayList<BufferedImage> walkTiles = new ArrayList<>();
    private ArrayList<BufferedImage> jumpTiles = new ArrayList<>();
    private ArrayList<BufferedImage> fallTiles = new ArrayList<>();
    private ArrayList<BufferedImage> punchTiles = new ArrayList<>();
    private ArrayList<BufferedImage> swordAttackTiles = new ArrayList<>();

    Level l;

    Player(Level l) {
        this.pos = new Vec2(0, 0);
        this.posLastFrame = new Vec2(0, 0);
        this.gravity = new Vec2(0, 0.35f);
        this.maxSpeed = new Vec2(5, 10);
        this.movementSpeed = 3.5f;
        this.l = l;

        try {
            idleTiles.add(ImageIO.read(new File(Platformer.BasePath + "Steve/Idle.png")));
            for (int i = 1; i <= 4; i++) walkTiles.add(ImageIO.read(new File(Platformer.BasePath + "Steve/Walk" + i + ".png")));
            jumpTiles.add(ImageIO.read(new File(Platformer.BasePath + "Steve/Jump.png")));
            fallTiles.add(ImageIO.read(new File(Platformer.BasePath + "Steve/Fall.png")));
            for (int i = 1; i <= 6; i++) punchTiles.add(ImageIO.read(new File(Platformer.BasePath + "Steve/Punch" + i + ".png")));
            for (int i = 1; i <= 8; i++) swordAttackTiles.add(ImageIO.read(new File(Platformer.BasePath + "Steve/SwordAttack" + i + ".png")));
            swordAttackTiles.add(ImageIO.read(new File(Platformer.BasePath + "Steve/SwordAttackUp.png")));
            swordAttackTiles.add(ImageIO.read(new File(Platformer.BasePath + "Steve/SwordAttackDown.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int targetH = (int) (Tile.tileSize * 2.1f);
        scaleAllAnimations(targetH);

        this.w = idleTiles.get(0).getWidth();
        this.h = idleTiles.get(0).getHeight();
        this.padX = w * 0.12f;
        this.padTop = h * 0.05f;

        boundingBox = new BoundingBox(padX, 0, w - padX, h);
        updateBoundingBox();
        posLastFrame.x = pos.x;
        posLastFrame.y = pos.y;

        for(Tile t: l.tiles){
            if(!t.hasRigidCollision) continue;
            BoundingBox below = new BoundingBox(boundingBox.min.x, boundingBox.max.y + 1,
                                                boundingBox.max.x, boundingBox.max.y + 2);
            if (t.bb.intersect(below)) {
                collidesDown = true;
                lastValidPosition = new Vec2(pos.x, pos.y);
                break;
            }
        }
    }

    private void scaleAllAnimations(int targetH) {
        for (int i = 0; i < idleTiles.size(); i++) idleTiles.set(i, scaleToHeight(idleTiles.get(i), targetH));
        for (int i = 0; i < walkTiles.size(); i++) walkTiles.set(i, scaleToHeight(walkTiles.get(i), targetH));
        for (int i = 0; i < jumpTiles.size(); i++) jumpTiles.set(i, scaleToHeight(jumpTiles.get(i), targetH));
        for (int i = 0; i < fallTiles.size(); i++) fallTiles.set(i, scaleToHeight(fallTiles.get(i), targetH));
        for (int i = 0; i < punchTiles.size(); i++) punchTiles.set(i, scaleToHeight(punchTiles.get(i), targetH));
        for (int i = 0; i < swordAttackTiles.size(); i++) swordAttackTiles.set(i, scaleToHeight(swordAttackTiles.get(i), targetH));
    }

    private static BufferedImage scaleToHeight(BufferedImage src, int targetH) {
        double s = targetH / (double) src.getHeight();
        int w = (int) Math.round(src.getWidth() * s);
        BufferedImage dst = new BufferedImage(w, targetH, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = AffineTransform.getScaleInstance(s, s);
        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        op.filter(src, dst);
        return dst;
    }

    public void kill() {
        if (numberOfLifes > 0) {
            numberOfLifes--;
            pos.x = lastValidPosition.x;
            pos.y = lastValidPosition.y;
            posLastFrame.x = pos.x;
            posLastFrame.y = pos.y;
        }
    }

    public void update() {
        Vec2 vel = pos.sub(posLastFrame);

        if (walkingLeft) {
            vel.x -= movementSpeed / 5f;
            facingLeft = true;
        } else if (walkingRight) {
            vel.x += movementSpeed / 5f;
            facingLeft = false;
        } else if (collidesDown) {
            vel.x *= 0.5f;
            if (Math.abs(vel.x) < 0.1f) vel.x = 0;
        }

        if (jump && collidesDown) {
            vel.y = -jumpPower;
            collidesDown = false;
        }

        if (collidesDown) vel = vel.mul(0.92f);
        else { vel.x *= 0.85f; vel.y *= 0.92f; }

        posLastFrame = new Vec2(pos.x, pos.y);
        pos = pos.add(vel).add(gravity);

        if (pos.x < 0) pos.x = 0;
        if (pos.x > l.lvlSize.x - w) pos.x = l.lvlSize.x - w;
        if (pos.y > l.lvlSize.y - h) pos.y = l.lvlSize.y - h;

        updateBoundingBox();

        // State setzen, nur wenn keine Attacke läuft
        if (currentState != PlayerState.PUNCH && currentState != PlayerState.SWORD_ATTACK) {
            if (vel.y > 1.0f && !collidesDown) currentState = PlayerState.FALL;
            else if (walkingLeft || walkingRight) currentState = PlayerState.WALK;
            else if (!jump) currentState = PlayerState.IDLE;
        }

        // Attacke nach der Animation zurücksetzen
        if ((currentState == PlayerState.PUNCH || currentState == PlayerState.SWORD_ATTACK)
                && displayedAnimationState >= getCurrentAnimationFrames().size() - 1) {
            displayedAnimationState = 0;
            currentState = PlayerState.IDLE;
        }
    }

    public void updateBoundingBox() {
        boundingBox.min.x = pos.x + padX;
        boundingBox.min.y = pos.y + padTop;
        boundingBox.max.x = pos.x + w - padX;
        boundingBox.max.y = pos.y + h - padTop;
    }

    public BufferedImage getPlayerImage() {
        BufferedImage frame = getNextFrame();
        if (facingLeft) {
            BufferedImage flipped = new BufferedImage(frame.getWidth(), frame.getHeight(), frame.getType());
            Graphics2D g = flipped.createGraphics();
            g.drawImage(frame, frame.getWidth(), 0, -frame.getWidth(), frame.getHeight(), null);
            g.dispose();
            return flipped;
        }
        return frame;
    }

    private BufferedImage getNextFrame() {
        ArrayList<BufferedImage> anim = getCurrentAnimationFrames();

        // Bewegungsgesteuerte Animationsgeschwindigkeit
        if (currentState == PlayerState.IDLE) {
            moveCounter++;
            if (moveCounter >= 10) { displayedAnimationState++; moveCounter = 0; }
        } else {
            moveCounter++;
            if (moveCounter >= 5) { displayedAnimationState++; moveCounter = 0; }
        }

        if (displayedAnimationState >= anim.size()) displayedAnimationState = 0;
        return anim.get(displayedAnimationState);
    }

    private ArrayList<BufferedImage> getCurrentAnimationFrames() {
        return switch (currentState) {
            case WALK -> walkTiles;
            case JUMP -> jumpTiles;
            case FALL -> fallTiles;
            case PUNCH -> punchTiles;
            case SWORD_ATTACK -> swordAttackTiles;
            default -> idleTiles;
        };
    }

    public void startPunch() { 
        currentState = PlayerState.PUNCH; 
        displayedAnimationState = 0; 
        attackTiles(1);
    }
    public void startSwordAttack() { 
        currentState = PlayerState.SWORD_ATTACK; 
        displayedAnimationState = 0; 
        attackTiles(2);
    }

    private void attackTiles(int strength) {
        float attackWidth = Tile.tileSize * 0.6f;
        float attackHeight = h * 0.8f;

        float ax = facingLeft ? pos.x - attackWidth : pos.x + w;
        float ay = pos.y + h * 0.1f;

        BoundingBox attackBox = new BoundingBox(ax, ay, ax + attackWidth, ay + attackHeight);

        for (Tile tile : new ArrayList<>(l.tiles)) { // Kopie, da Tiles gelöscht werden könnten
            if (tile instanceof TileBreakable && tile.bb.intersect(attackBox)) {
                ((TileBreakable) tile).damage(l, strength);
                break; // nur 1 Tile pro Schlag
            }
        }
    }

    public void playSound(String path) {
        if (!soundEnabled) return;

        try {
            File soundFile = new File(path);
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(soundFile));
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void changeSoundEnabled(){
        this.soundEnabled = !this.soundEnabled;
    }


}
