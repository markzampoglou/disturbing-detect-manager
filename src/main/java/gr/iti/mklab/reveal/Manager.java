package gr.iti.mklab.reveal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.mongodb.*;
import gr.iti.mklab.reveal.dnn.api.QueueObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by marzampoglou on 6/21/16.
 */
public class Manager {
    private Datastore ds;
    private final String USER_AGENT = "Mozilla/5.0";

    public Manager () {
        try {
            Configuration.load(getClass().getResourceAsStream("/remote.properties"));
            System.out.println(Configuration.MONGO_URI);
            MongoClientURI mongoURI = new MongoClientURI(Configuration.MONGO_URI);
            MongoCredential credential = MongoCredential.createCredential ( Configuration.MONGO_USER, "admin", Configuration.MONGO_PASS.toCharArray());
            MongoClient mongoclient = new MongoClient(new ServerAddress(Configuration.MONGO_HOST), Arrays.asList(credential));
            Morphia morphia = new Morphia();
            morphia.map(QueueObject.class);
            ds = new Morphia().createDatastore(mongoclient, "DisturbingQueue");
            ds.ensureCaps();
            System.out.println("Will now get an item");

            QueueObject report = ds.get(QueueObject.class, "testid");
            System.out.println("Got it, beginning");

            begin();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Suppress MongoDB logging
    static Logger root = (Logger) LoggerFactory
            .getLogger(Logger.ROOT_LOGGER_NAME);
    static {
        root.setLevel(Level.WARN);
    }

    public void begin() throws InterruptedException {
        ThreadManager calculator = new ThreadManager(Configuration.NUM_THREADS);

        Query<QueueObject> queue = ds.createQuery(QueueObject.class).limit(20).filter("processing",false);
        while (true) {
            if (queue.countAll() > 0 && calculator.canAcceptMoreTasks()) {
                QueueObject submission = queue.get();
                //submission.processing = true;
                //ds.save(submission);
                ds.findAndDelete(ds.find(QueueObject.class).filter("id", submission.id));
                calculator.submitTask(submission);
            }
            try {
                QueueObject output = calculator.getThreadCalculationResult();
                //System.out.println(output.sourceURL + String.valueOf(output.value));

                if (output!=null) {
                    String URLString=Configuration.CRAWLER_HOST + "/mmapi/media/update/disturbing?collection=" + output.collection + "&url=" + output.sourceURL + "&score="+String.valueOf(output.value)+ "&nsfwScore="+String.valueOf(output.value_nsfw) + "&type="+output.type;
                    if (output.itemId!=null){
                        URLString=URLString+ "&id=" + output.itemId;
                    }
                    URLString=URLString.replace("#","%23");
                    System.out.println("Sending: " + URLString);
                    URL serviceUrl = new URL(URLString);
                    HttpURLConnection con = (HttpURLConnection) serviceUrl.openConnection();
                    // optional default is GET
                    con.setRequestMethod("GET");
                    //add request header
                    con.setRequestProperty("User-Agent", USER_AGENT);
                    int responseCode = con.getResponseCode();

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    System.out.println("Received: " + response.toString());
                }
            }
            catch (Exception ex) {
                System.out.println("Exception:");
                System.out.println(ex.getMessage());
            }

            if (queue.countAll()==0) {
                queue = ds.createQuery(QueueObject.class).limit(20);
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}