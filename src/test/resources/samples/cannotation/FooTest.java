package samples.cannotation;

import org.junit.jupiter.api.Test;
import jakarta.validation.constraints.NotNull;

@Deprecated
class FooTest {

    @NotNull
    private String name;

    @Inject
    FooTest() {}
    
    @Test
    void testFoo() {}
}