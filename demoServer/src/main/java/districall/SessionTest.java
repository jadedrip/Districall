package districall;


/**
 * Created by 琛 on 2014/10/20 0020.
 */
public class SessionTest implements ISessionTest {

    @Override
    public String doRemote(ISessionClientTest iSessionClientTest) throws Exception {
        throw new Exception("Unknown");
        //return "SessionTest - " + iSessionClientTest.name();
    }

}
