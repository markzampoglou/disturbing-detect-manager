package gr.iti.mklab.reveal;

import org.apache.commons.configuration.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by kandreadou on 2/2/15.
 */
public class Configuration {

    public static String MONGO_HOST;
    public static String CRAWLER_HOST;
    public static String QUEUE_IMAGE_PATH;
    public static int NUM_THREADS;


    public static void load(InputStream stream) throws ConfigurationException, IOException {
        Properties conf = new Properties();
        conf.load(stream);
    
        MONGO_HOST = conf.getProperty("mongoHost");
        CRAWLER_HOST = conf.getProperty("crawlerHost");
        QUEUE_IMAGE_PATH = conf.getProperty("queueImagePath");
        NUM_THREADS=Integer.parseInt(conf.getProperty("numThreads"));

    }
}
