
package p1;

import java.util.Optional;

class F {
}

class OptionalField {
	Optional<F> f;
}

record OptionalRecord(Optional<F> f) {
}
