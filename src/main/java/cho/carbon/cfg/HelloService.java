package cho.carbon.cfg;

public class HelloService {

    HelloProperties helloProperties;

    public HelloProperties getHelloProperties() {
        return helloProperties;
    }

    public void setHelloProperties(HelloProperties helloProperties) {
        this.helloProperties = helloProperties;
    }

    public String sayHellAtguigu(String name){
        return "我是:  " + helloProperties.getPrefix()+"-" +name + helloProperties.getSuffix();
    }
}
