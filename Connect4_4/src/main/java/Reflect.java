import java.lang.reflect.InvocationTargetException;

class Reflect {

    public void hello(String param) {
        System.out.println("Hello triggered with " + param);
    }
    public void hi(String param) {
        System.out.println("Hi triggered with " + param);
    }
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Reflect.class.getMethod(args[0], String.class).invoke(new Reflect(), args[1]);
    }
}