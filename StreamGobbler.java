import java.io.InputStream;
import java.util.Scanner;

public class StreamGobbler
{
	
	public StreamGobbler(InputStream stream)
	{
		final InputStream fStream = stream;
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				Scanner in = new Scanner(fStream);
				while (in.hasNext())
					in.next();
				in.close();
			}
		}).start();
	}
	
}
