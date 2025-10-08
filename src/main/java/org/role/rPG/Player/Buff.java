package org.role.rPG.Player;

public class Buff {
    private final String stat;
    private final double value;
    private int durationTicks; // 남은 시간 (tick)

    public Buff(String stat, double value, int durationTicks) {
        this.stat = stat;
        this.value = value;
        this.durationTicks = durationTicks;
    }

    public String getStat() {
        return stat;
    }

    public double getValue() {
        return value;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public void setDurationTicks(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    // 매 틱마다 남은 시간을 1씩 감소시킵니다.
    public void tickDown() {
        this.durationTicks--;
    }
}