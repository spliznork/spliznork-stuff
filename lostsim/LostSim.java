import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Simulation regarding {@link http://www.reddit.com/r/askscience/comments/35uljq/if_i_wanted_to_randomly_find_someone_in_an/cr8nuqa}.
 */
public class LostSim {

  private static class World {
    private static final int POSITION_BITS = 10;
    private static final int POSITION_MASK = (1 << POSITION_BITS) - 1;

    private final int worldSize;
    private final int visitSize;
    private final long[] lastVisitedTick;
    private final Random random = new Random();
    private long currentTick;

    public World(int worldSize, int visitSize) {
      this.worldSize = worldSize;
      this.visitSize = visitSize;
      this.lastVisitedTick = new long[worldSize * worldSize];
      this.currentTick = worldSize * worldSize;
      initializeLastVisitedTicks();
    }

    private void initializeLastVisitedTicks() {
      List<Integer> ticks = new ArrayList<>(lastVisitedTick.length);
      for (int i = 1; i <= lastVisitedTick.length; i++) {
        ticks.add(i);
      }
      Collections.shuffle(ticks);
      int i = 0;
      for (int y = 0; y < worldSize; y++) {
        for (int x = 0; x < worldSize; x++) {
          lastVisitedTick[y * worldSize + x] = encodeOpaque(x, y, ticks.get(i++));
        }
      }
    }

    private long encodeOpaque(int x, int y, long t) {
      return (t << (2 * POSITION_BITS)) + (y << POSITION_BITS) + x;
    }

    public void visit(int x, int y) {
      long t = ++currentTick;
      int jmin = Math.max(y - visitSize, 0);
      int imin = Math.max(x - visitSize, 0);
      int jmax = Math.min(y + visitSize, worldSize - 1);
      int imax = Math.min(x + visitSize, worldSize - 1);
      for (int j = jmin; j <= jmax; j++) {
        for (int i = imin; i <= imax; i++) {
          lastVisitedTick[y * worldSize + x] = encodeOpaque(x, y, t);
        }
      }
    }

    /** Returns opaque value; get position with decodeX and decodeY */
    public long pickOld(double oldRatio) {
      long[] copy = Arrays.copyOf(lastVisitedTick, lastVisitedTick.length);
      Arrays.sort(copy);
      return copy[random.nextInt(1 + (int) (oldRatio * copy.length))];
    }

    /** Returns opaque value; get position with decodeX and decodeY */
    public long pickYoung(double youngRatio) {
      long[] copy = Arrays.copyOf(lastVisitedTick, lastVisitedTick.length);
      Arrays.sort(copy);
      int length = copy.length;
      return copy[length - 1 - random.nextInt(1 + (int) (youngRatio * length))];
    }

    public int decodeX(long opaque) {
      return (int) (opaque & POSITION_MASK);
    }

    public int decodeY(long opaque) {
      return (int) ((opaque >> POSITION_BITS) & POSITION_MASK);
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      for (int y = 0; y < worldSize; y++) {
        for (int x = 0; x < worldSize; x++) {
          long t = lastVisitedTick[(y * worldSize) + x] >> (2 * POSITION_BITS);
          sb.append(String.format("  (%d:x %d:y %2d:t)", x, y, t));
        }
        sb.append("\n");
      }
      return sb.toString();
    }
  }

  private static class Wanderer {
    private final World world;
    private final double oldRatio;
    private final Random random;
    private int currentX;
    private int currentY;
    private int targetX;
    private int targetY;

    public Wanderer(int worldSize, int visitSize, double oldRatio) {
      this.world = new World(worldSize, visitSize);
      this.oldRatio = oldRatio;
      this.random = new Random();
      pickCurrent();
      pickTarget();
    }

    private void pickCurrent() {
      long opaque = world.pickYoung((oldRatio < 0.5) ? oldRatio : (1 - oldRatio));
      currentX = world.decodeX(opaque);
      currentY = world.decodeY(opaque);
      world.visit(currentX, currentY);
    }

    private void pickTarget() {
      long opaque = world.pickOld(oldRatio);
      targetX = world.decodeX(opaque);
      targetY = world.decodeY(opaque);
    }

    public void tick() {
      if ((currentX == targetX) && (currentY == targetY)) {
        pickTarget();
      }
      int dx = targetX - currentX;
      int dy = targetY - currentY;
      boolean moveX;
      if ((dx != 0) && (dy != 0)) {
        moveX = random.nextBoolean();
      } else {
        moveX = (dx != 0);
      }
      if (moveX) {
        if (dx < 0) {
          currentX--;
        } else {
          currentX++;
        }
      } else {
        if (dy < 0) {
          currentY--;
        } else {
          currentY++;
        }
      }
      world.visit(currentX, currentY);
    }

    public int getCurrentX() {
      return currentX;
    }

    public int getCurrentY() {
      return currentY;
    }
  }

  private static boolean isFound(Wanderer seeker, Wanderer tourist, int radius) {
    int dx = seeker.getCurrentX() - tourist.getCurrentX();
    int dy = seeker.getCurrentY() - tourist.getCurrentY();
    int negRadius = -radius;
    return (negRadius <= dx) && (dx <= radius) && (negRadius <= dy) && (dy < radius);
  }

  public static void main(String... args) {
    double oldRatio = 0.1;
    int visitSize = 10;
    int maxTrials = 1000;
    long[] foundTicks = new long[maxTrials];
    String resultFmt = "%8s %8s %8s %8s %8s %8s %8s %8s %8s\n";
    System.out.format("oldRatio = %f\n", oldRatio);
    System.out.format("visitSize = %d\n", visitSize);
    System.out.format("maxTrials = %d\n", maxTrials);
    System.out.println();
    System.out.format(
        resultFmt, "World", "Seeker", "2%ile", "10%ile", "25%ile", "50%ile", "75%ile", "90%ile", "98%ile");
    String sep = "--------";
    System.out.format(resultFmt, sep, sep, sep, sep, sep, sep, sep, sep, sep);
    for (int worldSize : Arrays.asList(20, 40, 80, 160, 320)) {
      for (boolean moveSeeker : Arrays.asList(false, true)) {
        Arrays.fill(foundTicks, 0);
        for (int trial = 0; trial < maxTrials; trial++) {
          Wanderer seeker = new Wanderer(worldSize, visitSize, oldRatio);
          Wanderer tourist = new Wanderer(worldSize, visitSize, oldRatio);
          long ticks = 0;
          while (!isFound(seeker, tourist, visitSize)) {
            if (moveSeeker) {
              seeker.tick();
            }
            tourist.tick();
            ticks++;
          }
          foundTicks[trial] = ticks;
        }
        Arrays.sort(foundTicks);
        System.out.format(
            resultFmt,
            String.format("%dx%d", worldSize, worldSize),
            moveSeeker ? "wanders" : "stands",
            foundTicks[(int) (0.02 * maxTrials)],
            foundTicks[(int) (0.10 * maxTrials)],
            foundTicks[(int) (0.25 * maxTrials)],
            foundTicks[(int) (0.50 * maxTrials)],
            foundTicks[(int) (0.75 * maxTrials)],
            foundTicks[(int) (0.90 * maxTrials)],
            foundTicks[(int) (0.98 * maxTrials)]);
      }
    }
  }
}

