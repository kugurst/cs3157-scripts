import java.io.InputStream;
import java.util.Scanner;

public class StreamGobbler implements Runnable
{
	InputStream	fStream;

	public StreamGobbler(InputStream stream)
	{
		fStream = stream;
	}

	@Override
	public void run()
	{
		Scanner in = new Scanner(fStream);
		while (in.hasNext())
			in.next();
		in.close();
	}

}
