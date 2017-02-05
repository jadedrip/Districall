import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by Chen Wang on 2016/7/13 0013.
 */
public class Test {
    @org.junit.Test
    public void run() throws MalformedURLException, URISyntaxException {
        URI aURL = new URI("tcp://java.sun.com:80/docs/books/tutorial/index.html?name=networking&id=1#DOWNLOADING");

        System.out.println("scheme = " + aURL.getScheme());

        System.out.println("authority = " + aURL.getAuthority()); System.out.println("host = " + aURL.getHost());

        System.out.println("port = " + aURL.getPort());

        System.out.println("path = " + aURL.getPath());

        System.out.println("query = " + aURL.getQuery());
    }
}
