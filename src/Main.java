public class Main
{
    public static void main(String args[])
    {
        String apikey=System.getenv("OPENAI_API_KEY");
        String model=System.getenv("OPENAI_MODEL");

        System.out.println("Wine&DineAI is starting...");

        if(apikey==null || apikey.isEmpty())
        {
            System.err.println("Error: OPENAI_API_KEY environment variable is not found");
            return;
        }
        else
        {
            System.out.println("OpenAI API key successfully loaded from environment variable.");
        }

        if(model==null || model.isEmpty())
        {
            System.err.println("Error: OPENAI_MODEL environment variable is not found");
            return;
        }
        else
        {
            System.out.println("Model successfully loaded: " + model);
        }
    }
}