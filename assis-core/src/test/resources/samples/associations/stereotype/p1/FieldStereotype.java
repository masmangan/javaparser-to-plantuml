package p1;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@interface AssocTag {}

class J {}

class FieldStereotype {
    @AssocTag
    J x;
}

record RecordStereotype(@AssocTag J y) {}