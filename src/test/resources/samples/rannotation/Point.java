package samples.rannotation;

public record Point(
    @Deprecated int px,
    int py
) {
}