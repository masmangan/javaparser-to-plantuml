package samples.mmodifiers;

abstract class AbstractMethods {
    public abstract void abstractMethod();
}

class ConcreteMethods {
    public final void finalMethod() {}
    public static void staticMethod() {}
    public void normalMethod() {}
}