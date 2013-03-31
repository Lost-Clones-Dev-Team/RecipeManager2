package ro.thehunters.digi.recipeManager.flags;

public class FlagSecret extends Flag
{
    // Flag documentation
    
    public static final String[] A;
    public static final String[] D;
    public static final String[] E;
    
    static
    {
        A = new String[1];
        A[0] = "{flag} < ??? >";
        
        D = new String[1];
        D[0] = "Flag not yet documented.";
        
        E = null;
    }
    
    // Flag code
    
    public FlagSecret()
    {
        type = FlagType.SECRET;
    }
    
    @Override
    public boolean onParse(String value)
    {
        return true;
    }
}