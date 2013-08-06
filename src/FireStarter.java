import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

public class FireStarter {
    public static boolean isDebugEnabled;
    private static PriorityBlockingQueue<Philosopher> hungryPhils;


    public static void main(String[] args) throws InterruptedException {
        // Reading params
        int philNumber = Integer.parseInt(args[0]);
        int runningTime = Integer.parseInt(args[1]);
        Philosopher.thinkTime = Long.parseLong(args[2]);
        Philosopher.eatTime = Long.parseLong(args[3]);
        int debugFlag = Integer.parseInt(args[4]);
        if (debugFlag == 1) {
            isDebugEnabled = true;
        }

        hungryPhils = new PriorityBlockingQueue<Philosopher>(11,
                new Comparator<Philosopher>() {
                    public int compare(Philosopher o1, Philosopher o2) {
                        // Play with formula
                        double priority1 = 1 / ((o1.getStartHungryTime() + 1) * (o1.getTimesAte() + 1));
                        double priority2 = 1 / ((o2.getStartHungryTime() + 1) * (o2.getTimesAte() + 1));
                        if (priority1 > priority2) {
                            return -1;
                        }
                        return 1;
                    }
                });

        // Init
        Waiter waiter = Waiter.getInstance(hungryPhils);
        waiter.setPhilNumber(philNumber);
        waiter.setRunningTime(runningTime);

        fire(philNumber);
    }

    private static void fire(int philNumber) throws InterruptedException {
        Thread waiter = new Thread(Waiter.getInstance());
        waiter.start(); // Waiter should be already working to serve correctly

        List<Thread> phils = new ArrayList<Thread>(philNumber);
        for (int i = 0; i < philNumber; ++i) { // Philosophers are coming onto the scene
            phils.add(i, new Thread(new Philosopher(i, hungryPhils)));
            phils.get(i).start();
        }
        waiter.join();
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        for (int i = 0; i < philNumber; ++i) { // Kill 'em all
            phils.get(i).interrupt();
        }
    }
}
