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
    float roamRange = 100f; // How far he roams from spawn
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
        Vec2 vel = pos.sub(posLastFrame);

        // Distance to player
        float dx = (p.pos.x + p.w/2) - (pos.x + w/2);
        float dy = (p.pos.y + p.h/2) - (pos.y + h/2);
        float dist = (float)Math.sqrt(dx*dx + dy*dy);

        boolean seesPlayer = dist < 400; // can “see” player
        boolean inExplodeRange = dist < explodeRange;

        // State machine
        switch(currentState) {
            case IDLE -> {
                if(seesPlayer) {
                    currentState = State.WALK;
                }
            }
            case WALK -> {
                // roam randomly around spawn
                if(!seesPlayer) {
                    float dir = rand.nextFloat()*2 -1; // -1..1
                    vel.x = dir * movementSpeed;
                    facingLeft = vel.x<0;
                    // clamp to spawn range
                    if(pos.x < spawnPos.x - roamRange) vel.x = Math.abs(vel.x);
                    if(pos.x > spawnPos.x + roamRange) vel.x = -Math.abs(vel.x);
                } else {
                    currentState = State.CHASE;
                }
            }
            case CHASE -> {
                // move towards player
                if(dx<0) { vel.x = -movementSpeed; facingLeft = true; }
                else { vel.x = movementSpeed; facingLeft = false; }

                if(!seesPlayer) currentState = State.WALK;

                if(inExplodeRange && explodeTimer<0) {
                    explodeTimer = explodeDelay;
                }
            }
            case HURT -> {
                // simple knockback
                vel.x *= 0.5f;
                if(displayedAnimationState >= hurtTiles.size()-1) {
                    currentState = State.WALK;
                    displayedAnimationState = 0;
                }
            }
            case EXPLODE -> {
                // countdown animation
                vel.x = 0;
                if(displayedAnimationState >= explodeTiles.size()-1) {
                    explode(); // do damage and remove creeper
                    return;
                }
            }
        }

        posLastFrame = new Vec2(pos.x,pos.y);
        pos = pos.add(vel);

        updateBoundingBox();

        if(explodeTimer>=0) {
            explodeTimer--;
            if(explodeTimer==0) currentState = State.EXPLODE;
        }

        // Animation counter
        moveCounter++;
        int frameDelay = (currentState==State.IDLE) ? 20 : 5;
        if(moveCounter>=frameDelay) { displayedAnimationState++; moveCounter=0; }

        ArrayList<BufferedImage> frames = getCurrentAnimationFrames();
        if(displayedAnimationState>=frames.size()) displayedAnimationState=0;
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
