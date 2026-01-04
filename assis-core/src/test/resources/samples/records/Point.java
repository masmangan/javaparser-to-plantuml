package samples.records;

public record Point(int x, int y) {
}

class Outer { class Inner {} }
record R(Outer.Inner x) {}