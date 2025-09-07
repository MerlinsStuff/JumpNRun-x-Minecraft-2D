import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

public class Creeper {
    enum State { IDLE, WALK, CHASE, HURT, EXPLODE }

    State currentState = State.IDLE;

    Vec2 pos;
    Vec2 posLastFrame;
    BoundingBox bb;
    boolean facingLeft = false;
    boolean exploded = false;

    int w, h;
    float movementSpeed = 2.0f;
    float roamRange = 200f; // How far he roams from spawn
    Vec2 spawnPos;

    int displayedAnimationState = 0;
    int moveCounter = 0;
    int health = 6;

    float explodeRange = 150f;
    int explodeTimer = -1; // countdown to explode once in range
    int explodeDelay = 60; // frames before exploding

    ArrayList<BufferedImage> idleTiles = new ArrayList<>();
    ArrayList<BufferedImage> walkTiles = new ArrayList<>();
    ArrayList<BufferedImage> hurtTiles = new ArrayList<>();
    ArrayList<BufferedImage> explodeTiles = new ArrayList<>();

    Random rand = new Random();

    Level l;

    public Creeper(Level l, float x, float y) {
        this.l = l;
        pos = new Vec2(x, y);
        posLastFrame = new Vec2(x, y);
        spawnPos = new Vec2(x, y);

        try {
            idleTiles.add(ImageIO.read(new File(Platformer.BasePath + "Enemies/Creeper/Idle.png")));
            for(int i=1;i<=4;i++) walkTiles.add(ImageIO.read(new File(Platformer.BasePath + "Enemies/Creeper/Walk" + i + ".png")));
            for(int i=1;i<=4;i++) hurtTiles.add(ImageIO.read(new File(Platformer.BasePath + "Enemies/Creeper/Hurt" + i + ".png")));
            for(int i=1;i<=6;i++) explodeTiles.add(ImageIO.read(new File(Platformer.BasePath + "/Enemies/Creeper/Explode" + i + ".png")));
        } catch(IOException e) { e.printStackTrace(); }

        this.h = idleTiles.get(0).getHeight();
        this.w = idleTiles.get(0).getWidth();

        bb = new BoundingBox(pos.x, pos.y, pos.x + w, pos.y + h);
    }

    public void update(Player p) {
        // --- AI / velocity calculation ---
        float vx = 0; // horizontal velocity
        float vy = pos.y - posLastFrame.y; // vertical velocity from last frame

        // Distance to player
        float dx = (p.pos.x + p.w/2) - (pos.x + w/2);
        float dy = (p.pos.y + p.h/2) - (pos.y + h/2);
        float dist = (float)Math.sqrt(dx*dx + dy*dy);

        boolean seesPlayer = dist < 400; 
        boolean inExplodeRange = dist < explodeRange;

        // --- State machine ---
        switch(currentState) {
            case IDLE -> { if(seesPlayer) currentState = State.WALK; }
            case WALK -> {
                if(!seesPlayer) {
                    float dir = rand.nextFloat()*2 - 1; // -1..1
                    vx = dir * movementSpeed;
                    facingLeft = vx < 0;
                    // clamp to spawn range
                    if(pos.x < spawnPos.x - roamRange) vx = Math.abs(vx);
                    if(pos.x > spawnPos.x + roamRange) vx = -Math.abs(vx);
                } else { currentState = State.CHASE; }
            }
            case CHASE -> {
                vx = (dx < 0) ? -movementSpeed : movementSpeed;
                facingLeft = vx < 0;
                if(!seesPlayer) currentState = State.WALK;
                if(inExplodeRange && explodeTimer < 0) explodeTimer = explodeDelay;
            }
            case HURT -> {
                vx *= 0.5f;
                if(displayedAnimationState >= hurtTiles.size()-1) {
                    currentState = State.WALK;
                    displayedAnimationState = 0;
                }
            }
            case EXPLODE -> {
                vx = 0;
                if(displayedAnimationState >= explodeTiles.size()-1) {
                    explode();
                    return;
                }
            }
        }

        // --- Explode timer ---
        if(explodeTimer >= 0) {
            if(!inExplodeRange) {
                explodeTimer = -1;
                currentState = State.CHASE;
            } else {
                explodeTimer--;
                if(explodeTimer == 0) currentState = State.EXPLODE;
            }
        }

        // --- Gravity ---
        vy += 0.5f;
        if(vy > 8) vy = 8;

        // --- Apply velocity ---
        posLastFrame = new Vec2(pos.x, pos.y);
        pos.x += vx;
        pos.y += vy;
        updateBoundingBox();

        // --- Collision with tiles (player-style overlap) ---
        for (Tile tile : new ArrayList<>(l.tiles)) {
            Vec2 overlap = tile.bb.OverlapSize(bb);
            if (overlap.x > 0 && overlap.y > 0) {
                if (tile.hasRigidCollision) {
                    boolean resolveY = overlap.y < overlap.x; // smallest penetration first
                    if (resolveY) {
                        float centerC = (bb.min.y + bb.max.y) * 0.5f;
                        float centerT = (tile.bb.min.y + tile.bb.max.y) * 0.5f;
                        if (centerC > centerT) { // from below
                            pos.y += overlap.y;
                            posLastFrame.y = pos.y;
                        } else { // from above
                            pos.y -= overlap.y;
                            posLastFrame.y = pos.y;
                            vy = 0; // landed
                        }
                    } else {
                        float centerC = (bb.min.x + bb.max.x) * 0.5f;
                        float centerT = (tile.bb.min.x + tile.bb.max.x) * 0.5f;
                        if (centerC > centerT) { // from right
                            pos.x += overlap.x;
                            posLastFrame.x = pos.x;
                        } else { // from left
                            pos.x -= overlap.x;
                            posLastFrame.x = pos.x;
                        }
                    }
                }
                //tile.onCollision(this); // optional if creeper triggers tile effects
                updateBoundingBox();
            }
        }

        // --- Animation ---
        moveCounter++;
        int frameDelay = (currentState==State.IDLE) ? 20 : 5;
        if(moveCounter >= frameDelay) { displayedAnimationState++; moveCounter = 0; }
        ArrayList<BufferedImage> frames = getCurrentAnimationFrames();
        if(displayedAnimationState >= frames.size()) displayedAnimationState = 0;
    }



    public void updateBoundingBox() {
        bb.min.x = pos.x;
        bb.min.y = pos.y;
        bb.max.x = pos.x + w;
        bb.max.y = pos.y + h;
    }

    private ArrayList<BufferedImage> getCurrentAnimationFrames() {
        return switch(currentState) {
            case IDLE -> idleTiles;
            case WALK, CHASE -> walkTiles;
            case HURT -> hurtTiles;
            case EXPLODE -> explodeTiles;
        };
    }

    public void draw(Graphics2D g2d, float offsetX, float offsetY) {
        BufferedImage frame = getCurrentAnimationFrames().get(displayedAnimationState);
        if(facingLeft) {
            BufferedImage flipped = new BufferedImage(frame.getWidth(), frame.getHeight(), frame.getType());
            Graphics2D g = flipped.createGraphics();
            g.drawImage(frame, frame.getWidth(),0,-frame.getWidth(),frame.getHeight(),null);
            g.dispose();
            frame = flipped;
        }
        g2d.drawImage(frame, (int)(pos.x-offsetX), (int)(pos.y-offsetY), null);
    }

    public void hurt(Player p, int damage, boolean fromLeft) {
        health -= damage;
        currentState = State.HURT;
        displayedAnimationState = 0;
        // knockback
        pos.x += fromLeft ? 20 : -20;
        updateBoundingBox();
        if(health <= 0) currentState = State.EXPLODE;
    }

    private void explode() {
        if(exploded) return;
        float cx = pos.x + w / 2;
        float cy = pos.y + h / 2;

        // --- Damage player ---
        float px = l.player.pos.x + l.player.w / 2;
        float py = l.player.pos.y + l.player.h / 2;
        float dx = cx - px;
        float dy = cy - py;
        float distanceToPlayer = (float)Math.sqrt(dx*dx + dy*dy);
        

        // --- Destroy breakable tiles safely ---
        ArrayList<TileBreakable> tilesToDestroy = new ArrayList<>();
        for(Tile t : l.tiles) {
            if(t instanceof TileBreakable) {
                TileBreakable tb = (TileBreakable) t;
                float tx = (tb.bb.min.x + tb.bb.max.x)/2;
                float ty = (tb.bb.min.y + tb.bb.max.y)/2;
                float dist = (float)Math.sqrt((cx - tx)*(cx - tx) + (cy - ty)*(cy - ty));
                if(dist < explodeRange) {
                    tilesToDestroy.add(tb);
                }
            }
        }

        // Apply destruction AFTER iterating
        for(TileBreakable tb : tilesToDestroy) {
            tb.damage(l, 10); // this may remove it from l.tiles safely
        }

        // --- Remove creeper from level ---
        l.creepers.remove(this);
        if(distanceToPlayer < explodeRange) {
            l.player.numberOfLifes = Math.max(0, l.player.numberOfLifes - 1);
            if(px < cx) l.player.pos.x -= 20;
            else l.player.pos.x += 20;
            l.player.updateBoundingBox();
        }
        exploded = true;
    }


}
