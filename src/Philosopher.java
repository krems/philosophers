import java.util.concurrent.PriorityBlockingQueue;

public class Philosopher implements Runnable {
    // Time spent on
    public static long thinkTime;
    public static long eatTime;
    public final int id;
    // Number of times eating
    private int timesAte;
    // Changed by Waiter
    private volatile boolean hasNoMeal = true;
    //  Last time wanted to eat
    private volatile long startHungryTime;
    // Time being hungry during whole program running time
    private long sumTimeBeingHungry;
    private PriorityBlockingQueue<Philosopher> hungryPhils;

    private Waiter waiter = Waiter.getInstance(); // Performance!

    public Philosopher(int id, PriorityBlockingQueue<Philosopher> queue) {
        this.id = id;
        this.hungryPhils = queue;
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                think(Math.round(Math.random() * (thinkTime + 1)) + 1);
                eat(Math.round(Math.random() * (eatTime + 1)) + 1);
            }
        } catch (InterruptedException e) {
            System.out.println("[" + id + "] " + timesAte + " " + sumTimeBeingHungry);
        }
    }

    public synchronized void think(long time) throws InterruptedException {
        if (FireStarter.isDebugEnabled) {
            System.out.println("[" + id + "] thinking for " + time);
        }
        wait(time);
    }

    public synchronized void startEating(long time, Waiter waiter) throws InterruptedException {
        if (FireStarter.isDebugEnabled) {
            System.out.println("[" + id + "] eating for " + time);
        }
        wait(time);
        waiter.releaseForks(id);
        hasNoMeal = true;
    }

    public void eat(long time) throws InterruptedException {
        if (FireStarter.isDebugEnabled) {
            System.out.println("[" + id + "] hungry");
        }
        startHungryTime = System.currentTimeMillis();
        hungryPhils.put(this); // Making an order
        synchronized (this) {
            while (!Thread.currentThread().isInterrupted() && hasNoMeal) { // Waiting being hungry
                this.wait();
            }
        }
        sumTimeBeingHungry += System.currentTimeMillis() - startHungryTime;
        startEating(time, waiter);
        ++timesAte;
    }

    public void giveMeal() {
        hasNoMeal = false;
        synchronized (this) {
            this.notify();
        }
    }

    public long getStartHungryTime() {
        return startHungryTime;
    }

    public int getTimesAte() {
        return timesAte;
    }
}
