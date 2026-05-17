package net.cn.badges;

public class SpecialBadgeData {
    private boolean hasPermission;
    private String symbol;
    private String color;

    public SpecialBadgeData(boolean hasPermission, String symbol, String color) {
        this.hasPermission = hasPermission;
        this.symbol = symbol;
        this.color = color;
    }

    public boolean hasPermission() {
        return hasPermission;
    }

    public void setPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
