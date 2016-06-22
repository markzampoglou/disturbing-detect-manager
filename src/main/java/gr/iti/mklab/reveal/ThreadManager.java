package gr.iti.mklab.reveal;

import gr.iti.mklab.reveal.dnn.api.QueueObject;

import java.util.concurrent.*;

/**
 * Created by marzampoglou on 11/4/15.
 */
public class ThreadManager {

    /**
     * This class implements multi-threaded image Ghost extraction.
     *
     * @author Markos Zampoglou, based on the work of Eleftherios Spyromitros-Xioufis
     *
     */

        private ExecutorService executor;
        private CompletionService<QueueObject> pool;

        /** The current number of tasks whose termination is pending. **/
        private int numPendingTasks;

        /**
         * The maximum allowable number of pending tasks, used to limit the memory usage.
         */
        private final int maxNumPendingTasks;

        public ThreadManager(int numThreads) {
            System.out.println("Threads " + numThreads);
            executor = Executors.newFixedThreadPool(numThreads);
            pool = new ExecutorCompletionService<QueueObject>(executor);
            numPendingTasks = 0;
            maxNumPendingTasks = numThreads;
        }

        public void submitTask(QueueObject taskObject) {
            Callable<QueueObject> call = new SingleDetectionThread(taskObject);
            pool.submit(call);
            numPendingTasks++;
        }

        /**
         * Gets an image download results from the pool.
         *
         * @return the download result, or null in no results are ready
         * @throws Exception
         *             for a failed download task
         */
        public QueueObject getThreadCalculationResult() throws Exception {
            Future<QueueObject> future = pool.poll();
            if (future == null) { // no completed tasks in the pool
                return null;
            } else {
                try {
                    QueueObject res = future.get();
                    return res;
                } catch (Exception e) {
                    System.out.println("future.get() has crashed");
                    throw e;
                } finally {
                    // in any case (Exception or not) the numPendingTask should be reduced
                    numPendingTasks--;
                }
            }
        }

        /**
         * Gets an image download result from the pool, waiting if necessary.
         *
         * @return the download result
         * @throws Exception
         *             for a failed download task
         */
        public QueueObject getThreadCalculationResultWait() throws Exception {
            try {
                QueueObject ghor = pool.take().get();
                return ghor;
            } catch (Exception e) {
                throw e;
            } finally {
                // in any case (Exception or not) the numPendingTask should be reduced
                numPendingTasks--;
            }
        }

        /**
         * Returns true if the number of pending tasks is smaller than the maximum allowable number.
         *
         * @return
         */
        public boolean canAcceptMoreTasks() {
            if (numPendingTasks < maxNumPendingTasks) {
                return true;
            } else {
                return false;
            }
        }

        public void shutDown() {
            executor.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                        System.err.println("Pool did not terminate");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                executor.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

