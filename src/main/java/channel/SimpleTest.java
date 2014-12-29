package channel;

import java.security.Security;


public class SimpleTest {

    public static void main (String[] args)
    {
        String Name= "BC";
        if (Security.getProvider(Name) == null)
        {
            System.out.println("not installed");
        }
        else
        {
            System.out.println("installed");
        }
    }
}
