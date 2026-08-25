package com.aaax.core.utils.generator.id;

public class SnowflakeIdGeneratorConfiguration {
    //hibernate generator workaround...
    public static int MACHINE_ID;
    public static String IDENTIFIER; // e.g. POD_IP
    private int machineId;

    public SnowflakeIdGeneratorConfiguration(int machineId) {
        this.setMachineId(machineId);
        IDENTIFIER = "0.0.0.0";
    }

    public SnowflakeIdGeneratorConfiguration(int machineId, String identifier) {
        this.setMachineId(machineId);
        IDENTIFIER = identifier;
    }

    public void setMachineId(int machineId) {
        this.machineId = machineId;
        MACHINE_ID = machineId;
    }
}
