package districall;

import org.caffy.districall.DistricallClient;
import org.caffy.districall.interf.IServicePool;
import org.caffy.districall.pool.StaticServicePool;
import org.xml.sax.SAXException;

import javax.management.ServiceNotFoundException;
import java.io.IOException;

public class ClientTest {

    public static void main(String[] args) {
        try {
            client();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试代码
     */
    private static void client() throws NoSuchMethodException, IOException, SAXException {
        IServicePool pool = new StaticServicePool("classpath:services.xml");  // 发现服务
        DistricallClient client = new DistricallClient(pool);
        IRemoteInterface singleton;
        try {
            singleton = client.getSingleton(IRemoteInterface.class);
        } catch (ServiceNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        assert singleton != null;
        singleton.doSomething();
        Object myTest;
        myTest = singleton.myTest("MyTest", 50);
        System.out.println(myTest);

        try {
            // ISession
            ISessionTest session = client.createObject("10", ISessionTest.class);

            ISessionClientTest clientTest = new ISessionClientTest() {
                @Override
                public String name() {
                    return "My client name";
                }
            };
            String s = session.doRemote(clientTest);
            System.out.println(s);

            client.destroySession(session);

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}