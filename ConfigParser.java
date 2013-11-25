import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public final class ConfigParser
{

	private ConfigParser() throws AssertionError
	{
		throw new AssertionError();
	}

	@SuppressWarnings ("unchecked")
	public static LinkedHashMap<String, Object> loadConfig(File file)
	{
		Yaml yaml = new Yaml();
		LinkedHashMap<String, Object> options = null;
		try {
			for (Object o : yaml.loadAll(new FileInputStream(file)))
				options = (LinkedHashMap<String, Object>) o;
		} catch (FileNotFoundException e) {
			System.err.println("File \"" + file.getAbsolutePath() + "\" not found.");
			System.exit(1);
		}
		return options;
	}

	@SuppressWarnings ("unchecked")
	public static Object[] parseConfig(LinkedHashMap<String, Object> options,
		LinkedList<LinkedHashMap<String, Object>> partAnswers)
	{
		int threads =
			options.containsKey("threads") ? (Integer) options.remove("threads") : Runtime
				.getRuntime().availableProcessors() / 2;
		boolean checkGit =
			options.containsKey("check-git") ? (Boolean) options.remove("check-git") : true;
		String rootDir = options.containsKey("uni-dir") ? (String) options.remove("uni-dir") : "./";
		// for each part
		for (Map.Entry<String, Object> partMap : options.entrySet()) {
			// Get the actual mappigns
			LinkedHashMap<String, Object> partOptions =
				(LinkedHashMap<String, Object>) partMap.getValue();
			partAnswers.add(partOptions);
			// System.out.println(partOptions);
		}
		return new Object[] {threads, checkGit, rootDir};
	}

}
