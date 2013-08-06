import com.sun.corba.se.impl.orbutil.concurrent.Mutex;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class Waiter implements Runnable {
    private static Waiter instance;
    private static int runningTime;
    private static final long MAX_ATTEMPT_TIME = 7; // Play with this value

    private static ArrayList<Mutex> forks; // Or may be AtomicBoolean?
    private PriorityBlockingQueue<Philosopher> hungryPhils;

    private Waiter() {}
    private Waiter(PriorityBlockingQueue<Philosopher> queue) {
        this.hungryPhils = queue;
    }

    public static synchronized Waiter getInstance(PriorityBlockingQueue<Philosopher> queue) {
        if (instance == null) {
            instance = new Waiter(queue);
        }
        return instance;
    }

    public static synchronized Waiter getInstance() {
        if (instance == null) {
            instance = new Waiter();
        }
        return instance;
    }

    public void setPhilNumber(int philNumber) {
        forks = new ArrayList<Mutex>(philNumber + 1);
        for (int i = 0; i < philNumber; ++i) {
            forks.add(i, new Mutex());
        }
        forks.add(philNumber, forks.get(0)); // Emulate circled buffer
    }

    public void setRunningTime(int runningTime) {
        Waiter.runningTime = runningTime * 1000;
    }

    // When phil finished his lunch he should put his forks back on table
    public void releaseForks(int number) {
        forks.get(number).release();
        if (FireStarter.isDebugEnabled) {
            System.out.println("[" + number + "] put left fork");
        }
        forks.get(number + 1).release();
        if (FireStarter.isDebugEnabled) {
            System.out.println("[" + number + "] put right fork");
        }
    }

    public void run() {
        // Collection of phils who cannot eat right now due to fork absence
        Set<Philosopher> disabledPhils = new HashSet<Philosopher>();
        // True if last philosopher who tried to eat succeed
        boolean wasAttemptSuccessful = false;
        final long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < runningTime) {
            if (wasAttemptSuccessful || hungryPhils.isEmpty()) {
                hungryPhils.addAll(disabledPhils);
                disabledPhils.clear();
            }
            Philosopher philBeingServed = null;
            try {
                philBeingServed = hungryPhils.take(); // get phil with the highest priority
                disabledPhils.add(philBeingServed);
                if (forks.get(philBeingServed.id).attempt(1)) { // Could use tryLock instead
                    if (FireStarter.isDebugEnabled) {
                        System.out.println("[" + philBeingServed.id + "] took left fork");
                    }
                    if (forks.get(philBeingServed.id + 1).attempt(Math.round(Math.random() *
                            (MAX_ATTEMPT_TIME + 1)))) {
                        if (FireStarter.isDebugEnabled) {
                            System.out.println("[" + philBeingServed.id + "] took right fork");
                        }
                        philBeingServed.giveMeal();
                        disabledPhils.remove(philBeingServed);
                        wasAttemptSuccessful = true;
                    } else {
                        wasAttemptSuccessful = false;
                        forks.get(philBeingServed.id).release();
                        if (FireStarter.isDebugEnabled) {
                            System.out.println("[" + philBeingServed.id + "] put left fork");
                        }
                    }
                } else {
                    wasAttemptSuccessful = false;
                }
            } catch (InterruptedException e) {
                if (philBeingServed != null) {
                    forks.get(philBeingServed.id).release();
                    forks.get(philBeingServed.id + 1).release();
                }
                e.printStackTrace();
            }
            if (FireStarter.isDebugEnabled) {
                System.out.printf("\n\n"); // Kinda delimiter
                FireStarter.isDebugEnabled = false; // Stop producing messages from philosophers
            }
        }
    }
}
