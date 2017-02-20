package gr.iti.mklab.reveal;

/**
 * Created by marzampoglou on 11/3/15.
 */

import gr.iti.mklab.reveal.dnn.api.QueueObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;


public class SingleDetectionThread implements Callable<QueueObject>{

    private final String USER_AGENT = "Mozilla/5.0";
    private QueueObject input;

    public SingleDetectionThread(QueueObject input) {
        this.input = input;
    }

    @Override
    public QueueObject call() throws Exception {
        QueueObject output = input;
        String value = classifyDisturbingImage();
        String[] values=value.split(":");

        output.value=Double.valueOf(values[0]);
        output.value_nsfw=Double.valueOf(values[1]);
        output.processing=false;
        return output;
    }

    public String classifyDisturbingImage() throws IOException {

        String queuedFilePath=Configuration.QUEUE_IMAGE_PATH + input.id + ".jpg";
        URL serviceUrl = new URL("http://localhost:5000/classify_violent?imagepath=" + queuedFilePath);
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
        (new File(queuedFilePath)).delete();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }
}
