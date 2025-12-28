package samples.pannotation;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.NotNull;

public class SampleController {

    public SampleController(@Inject int id) { }

    public String show(
        @PathVariable int id,
        @RequestParam @NotNull String q
    ) {
        return "ok";
    }
}