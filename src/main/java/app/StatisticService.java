package app;

public class StatisticService {
    private long sum = 0;
    private long count = 0;
    private boolean stopped = false;

    public synchronized void add(long addition) {
        if (!stopped) {
            sum += addition;
            count += 1;
        }
    }

    public synchronized void stop() {
        stopped = true;
    }

    public long get() {
        if (count == 0) {
            return 0;
        }
        return sum / count;
    }
}