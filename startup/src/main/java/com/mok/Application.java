package com.mok;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.runtime.Micronaut;

@Introspected(packages = "com.mok")
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}