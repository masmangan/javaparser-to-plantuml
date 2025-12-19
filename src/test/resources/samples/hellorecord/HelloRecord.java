package samples.hellorecord;

public record Greeting(String message) {
}  

public class HelloRecord {
    public static void main(String[] args) {
        Greeting g = new Greeting("Hello, world");
        System.out.println(g.message());
    }
}
