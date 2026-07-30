package com.assistivecraft;

/**
 * Central store for every feature's state. Every feature has its OWN
 * independent toggle — nothing here is gated behind a shared master switch.
 */
public class ModuleManager {

    public static final ModuleManager INSTANCE = new ModuleManager();

    // ===================== COMBAT =====================
    public boolean aimAssistEnabled = false;
    public float aimAssistFovDegrees = 12.0f;
    public float aimAssistSpeed = 0.25f;

    public boolean autoAttackEnabled = false;
    public double autoAttackRange = 4.5;

    public boolean autoTotemEnabled = false;

    // ===================== OUTLINES / ESP =====================
    public boolean mobOutlineEnabled = false;
    public double mobOutlineRange = 48.0;

    public boolean diamondOutlineEnabled = false;
    public int diamondOutlineRadius = 16;

    // ===================== AUTOMATION =====================
    public boolean autoEatEnabled = false;
    public float autoEatHungerThreshold = 14.0f;

    public boolean fallMitigationEnabled = false;

    // ===================== UTILITY =====================
    public boolean projectilePathEnabled = false;
    public boolean highContrastMode = true;

    private ModuleManager() {}
}
