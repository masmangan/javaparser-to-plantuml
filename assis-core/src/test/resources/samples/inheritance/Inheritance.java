package samples.inheritance;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

interface A { }

interface B extends A { }      

class Base { }

class Child extends Base implements B { }   



enum E implements A {  }
