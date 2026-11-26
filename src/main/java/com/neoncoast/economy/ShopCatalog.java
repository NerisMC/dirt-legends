package com.neoncoast.economy;

/** What the shop terminal sells. Shared by the client screen and the server. */
public final class ShopCatalog {
    public static final String[] NAMES = {
            "Sedan (car key)", "Sports Car (car key)", "Body Armor", "Repair Kit", "Food x8",
            "Pay off the cops", "ROB THE STORE"};
    public static final long[] PRICES = {800, 3000, 400, 150, 40, 1000, 0};
    public static final int PAY_OFF = 5;
    public static final int ROB = 6;

    private ShopCatalog() {
    }
}
