package haveric.recipeManager.flags;

public interface Flaggable {
    /**
     * Shortcut for {@link Flags#hasFlag(FlagType)}
     */
    boolean hasFlag(FlagType type);

    /**
     * Checks if flag storage is null.<br>
     * This is useful to check if {@link #getFlags()} would create a new Flags object when called.
     *
     * @return flags != null
     */
    boolean hasFlags();

    /**
     * @return true if flags prevent shift+click from creating more than one item
     */
    boolean hasNoShiftBit();

    /**
     * Shortcut for {@link Flags#getFlag(FlagType)}
     */
    Flag getFlag(FlagType type);

    /**
     * Shortcut for {@link Flags#getFlag(Class)}
     */
    <T extends Flag> T getFlag(Class<T> flagClass);

    /**
     * Gets the Flag object that holds a list of flags.<br>
     * Can't be null but creates a new instance of Flag when called.<br>
     * You can check if flags is null with {@link #hasFlags()}
     *
     * @return Flag object, never null
     */
    Flags getFlags();

    /**
     * Removes all flags.
     */
    void clearFlags();

    /**
     * Shortcut for {@link Flags#addFlag(Flag)}
     */
    void addFlag(Flag flag);

    /**
     * Check with flags if recipe/result can be crafted/used
     *
     * @param a
     *            use {@link Args#create()}
     * @return if recipe can be crafted
     */
    boolean checkFlags(Args a);

    /**
     * Apply flags when recipe/result is crafted/taken
     *
     * @param a
     *            use {@link Args#create()}
     * @return
     */
    boolean sendCrafted(Args a);

    /**
     * Apply flags when recipe/result is prepared/displayed
     *
     * @param a
     *            {@link Args#create()}
     * @return
     */
    boolean sendPrepare(Args a);
}
