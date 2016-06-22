package gr.iti.mklab.reveal;

import java.net.UnknownHostException;

/**
 * Created by marzampoglou on 6/21/16.
 */

public class Main {
    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        Manager threadManager=new Manager();
        threadManager.begin();
    }
}
