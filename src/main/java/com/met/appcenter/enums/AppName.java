package com.met.appcenter.enums;

public enum AppName {
    ONE_KEY_TEST_NA("OneKey-Test-QA"),
    ONE_KEY_TFD_NA("OneKey-TFD-QA"),
    ONE_KEY_STAGE_NA("OneKey-Stage-NA"),
    ONE_KEY_STAGE_EMEA("OneKey-Stage-EMEA"),
    ONE_KEY_STAGE_ANZ("OneKey-Stage-ANZ"),
    HEATED_GEAR("Heated-Gear");

    private final String name;

    public String getName() {
        return name;
    }

    AppName(String name) {
        this.name = name;
    }
}