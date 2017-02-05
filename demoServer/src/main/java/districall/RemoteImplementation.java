package districall;

/**
 * Created by Chen Wang on 2016/6/23 0023.
 */
public class RemoteImplementation implements IRemoteInterface {
    @Override
    public void doSomething() {
        System.out.println("doSomething");
    }

    @Override
    public void doSomething(String text) {
        System.out.println("doSomething: " + text);
    }

    @Override
    public int myTest(String key, int value) {
        return 0;
    }
}
