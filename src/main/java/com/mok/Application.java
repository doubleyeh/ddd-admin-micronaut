package com.mok;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.runtime.Micronaut;
import jakarta.persistence.Entity;

@Introspected(packages = {
        "com.mok.domain.sys.model",
        "com.mok.domain.common"
}, includedAnnotations = Entity.class)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
