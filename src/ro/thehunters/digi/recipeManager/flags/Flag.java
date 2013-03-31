package ro.thehunters.digi.recipeManager.flags;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

import ro.thehunters.digi.recipeManager.Permissions;
import ro.thehunters.digi.recipeManager.RecipeErrorReporter;
import ro.thehunters.digi.recipeManager.flags.FlagType.Bit;
import ro.thehunters.digi.recipeManager.recipes.BaseRecipe;
import ro.thehunters.digi.recipeManager.recipes.ItemResult;

public class Flag implements Cloneable
{
    // Flag documentation
    
    public static final String[] A;
    public static final String[] D;
    public static final String[] E;
    
    static
    {
        A = new String[]
        {
            "{flag}",
        };
        
        D = new String[]
        {
            "Flag not yet documented.",
        };
        
        E = null;
    }
    
    // Flag code
    
    protected FlagType type;
    protected Flags flagsContainer;
    
    protected Flag()
    {
    }
    
    /*
     *  Public tools/final methods
     */
    
    /**
     * @return The Flags object that holds this flag
     */
    final public Flags getFlagsContainer()
    {
        return flagsContainer;
    }
    
    /**
     * Parses a string to get the values for this flag.<br>
     * Has diferent effects for each extension of Flag object.
     * 
     * @param value
     *            the flag's value (not containing the <code>@flag</code> string)
     * @return
     * @return false if an error occured and the flag should not be added
     */
    final public boolean parse(String value)
    {
        return onParse(value);
    }
    
    /**
     * Check if player has the required permissions to skip this flag
     * 
     * @param player
     * @return
     */
    final public boolean hasSkipPermission(Player player)
    {
        if(player == null)
        {
            return false; // no player, no skip
        }
        
        if(player.hasPermission(Permissions.SKIPFLAG_ALL))
        {
            return true; // has skip permission for all flags
        }
        
        for(String name : getType().getNames())
        {
            if(player.hasPermission(Permissions.SKIPFLAG_PREFIX + name))
            {
                return true; // has skip permission for this flag
            }
        }
        
        return false; // don't skip flag
    }
    
    /**
     * Check if the flag allows to craft with these arguments.<br>
     * Any and all arguments can be null if you don't have values for them.<br>
     * To make the check fail you <b>must</b> add a reason to the argument!
     * 
     * @param a
     *            the arguments class for easily maintainable argument class
     */
    final public void check(Args a)
    {
        if(!hasSkipPermission(a.player()))
            onCheck(a);
    }
    
    /**
     * Apply the flag's effects - triggered when recipe is prepared or result is displayed
     * 
     * @param a
     *            the arguments class for easily maintainable argument class
     * @return true if succesful or skipped, false if some required argument was null/invalid.
     */
    final public boolean prepare(Args a)
    {
        return (hasSkipPermission(a.player()) ? true : onPrepare(a));
    }
    
    /**
     * Apply the flag's effects to the arguments.<br>
     * Any and all arguments can be null if you don't have values for them.<br>
     * To make the check fail you <b>must</b> add a reason to the argument!
     * 
     * @param a
     *            the arguments class for easily maintainable argument class
     * @return true if succesful or skipped, false if some required argument was null/invalid.
     */
    final public boolean crafted(Args a)
    {
        return (hasSkipPermission(a.player()) ? true : onCrafted(a));
    }
    
    /**
     * Trigger flag failure as if it failed due to multi-result chance.<br>
     * Any and all arguments can be null if you don't have values for them.<br>
     * Adding reasons to this will display them to the crafter.
     * 
     * @param a
     */
    final public void failed(Args a)
    {
        onFailed(a);
    }
    
    /**
     * Removes the flag from its flag list container.<br>
     * This also notifies the flag of removal, it might do some stuff before removal.<br>
     * If the flag hasn't been added to any flag list, this method won't do anything.
     */
    final public void remove()
    {
        if(flagsContainer != null)
        {
            flagsContainer.removeFlag(this);
            onRemove();
        }
    }
    
    /**
     * Clones the flag and asigns it to a new flag container
     * 
     * @param container
     * @return
     */
    final public Flag clone(Flags container)
    {
        Flag flag = clone();
        flag.flagsContainer = container;
        return flag;
    }
    
    /**
     * Returns the hashcode of the flag's type enum.
     */
    @Override
    public int hashCode()
    {
        return (getType() == null ? 0 : getType().hashCode());
    }
    
    /**
     * Warning: this method doesn't check flag's values, it only compares flag type!
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj == this)
            return true;
        
        if(obj == null || obj instanceof Flag == false)
            return false;
        
        return obj.hashCode() == hashCode();
    }
    
    /*
     *  Non-public tools/final methods
     */
    
    final protected Flaggable getFlaggable()
    {
        return (flagsContainer != null ? flagsContainer.flaggable : null);
    }
    
    final protected BaseRecipe getRecipe()
    {
        Flaggable flaggable = getFlaggable();
        
        return (flaggable instanceof BaseRecipe ? (BaseRecipe)flaggable : null);
    }
    
    /*
    final protected BaseRecipe getRecipeDeep()
    {
        Flaggable flaggable = getFlaggable();
        
        if(flaggable instanceof BaseRecipe)
        {
            return (BaseRecipe)flaggable;
        }
        else
        {
            ItemResult result = getResult();
            
            if(result != null)
            {
                return result.getRecipe();
            }
        }
        
        return null;
    }
    */
    
    final protected ItemResult getResult()
    {
        Flaggable flaggable = getFlaggable();
        
        return (flaggable instanceof ItemResult ? (ItemResult)flaggable : null);
    }
    
    final protected boolean validateParse(String value)
    {
        Validate.notNull(getType());
        
        if(!getType().hasBit(Bit.NO_VALUE) && value == null)
        {
            RecipeErrorReporter.error("Flag " + getType() + " needs a value!");
            return false;
        }
        
        if(!getType().hasBit(Bit.NO_FALSE) && value != null && (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("remove")))
        {
            remove();
            return false;
        }
        
        return validate();
    }
    
    final protected boolean validate()
    {
        Flaggable flaggable = getFlaggable();
        
        if(getType().hasBit(Bit.RESULT) && flaggable instanceof ItemResult == false)
        {
            RecipeErrorReporter.error("Flag " + getType() + " only works on results!");
            return false;
        }
        
        if(getType().hasBit(Bit.RECIPE) && flaggable instanceof BaseRecipe == false && flaggable instanceof ItemResult)
        {
            RecipeErrorReporter.error("Flag " + getType() + " only works on recipes!");
            return false;
        }
        
        return onValidate();
    }
    
    /*
     *  Overwriteable methods/events
     */
    
    /**
     * @return the flag name enum
     */
    public FlagType getType()
    {
        return type;
    }
    
    @Override
    public Flag clone()
    {
        return this; // pointless to clone an empty flag
    }
    
    protected boolean onValidate()
    {
        return (getType() != null);
    }
    
    protected boolean onParse(String value)
    {
        return false; // it didn't parse anything
    }
    
    protected void onRemove()
    {
    }
    
    protected void onCheck(Args a)
    {
    }
    
    protected boolean onPrepare(Args a)
    {
        return true;
    }
    
    protected boolean onCrafted(Args a)
    {
        return true;
    }
    
    protected void onFailed(Args a)
    {
    }
}