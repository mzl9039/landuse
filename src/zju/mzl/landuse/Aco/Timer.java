package zju.mzl.landuse.Aco;

/**
 * Created by mzl on 2016/11/17.
 */
public class Timer {
    private long startTime;

    public void start_timers() {
        startTime = System.currentTimeMillis();
    }

    public double elapsed_time() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }
}
